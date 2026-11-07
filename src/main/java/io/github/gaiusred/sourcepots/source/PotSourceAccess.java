package io.github.gaiusred.sourcepots.source;

import io.github.gaiusred.sourcepots.compat.mekanism.MekanismSourceStorage;
import java.util.Objects;
import java.util.EnumMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Owns resource storage and world access without growth or timer policy. */
public final class PotSourceAccess implements SourceAccess {
    private static final ResourceLocation SOURCE_FLUID = ResourceLocation.parse("sauce:source_fluid");
    private final ServerLevel level;
    private final BlockPos pos;
    private final SourceSettings settings;
    private final BooleanSupplier eligible;
    private final EnumMap<Direction, FluidTank> liquids = new EnumMap<>(Direction.class);
    private final boolean liquidAvailable;
    private final EnumMap<Direction, Long> dormantLiquids = new EnumMap<>(Direction.class);
    private final GasStorage gas;
    private final EnumMap<Direction, IFluidHandler> fluidInputs = new EnumMap<>(Direction.class);
    private final JarSourceAccess jars = new JarSourceAccess();

    public PotSourceAccess(ServerLevel level, BlockPos pos, SourceSettings settings, BooleanSupplier eligible) {
        this(level, pos, settings, eligible, () -> {});
    }

    public PotSourceAccess(ServerLevel level, BlockPos pos, SourceSettings settings, BooleanSupplier eligible, Runnable changed) {
        this.level = Objects.requireNonNull(level);
        this.pos = pos.immutable();
        this.settings = Objects.requireNonNull(settings);
        this.eligible = Objects.requireNonNull(eligible);
        Objects.requireNonNull(changed);
        liquidAvailable = BuiltInRegistries.FLUID.getOptional(SOURCE_FLUID).filter(fluid -> fluid != Fluids.EMPTY).isPresent();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            liquids.put(side, new FluidTank(Math.toIntExact(settings.liquid().capacity()), PotSourceAccess::isSourceFluid) {
                @Override protected void onContentsChanged() { changed.run(); }
            });
            fluidInputs.put(side, new FluidInput(liquids.get(side)));
        }
        gas = ModList.get().isLoaded("mekanism")
                ? MekanismSourceStorage.create(level, this.pos, settings.gas().cost(), settings.gas().capacity(), eligible, changed)
                : new UnavailableGasStorage(settings.gas().capacity());
    }

    public boolean liquidAvailable() { return liquidAvailable; }
    public IFluidHandler fluidHandler(Direction side) { return side == null ? null : fluidInputs.get(side); }
    public GasStorage gasStorage() { return gas; }
    public long storedLiquid(Direction side) {
        if (side == null || !side.getAxis().isHorizontal()) return 0;
        return liquidAvailable ? liquids.get(side).getFluidAmount() : dormantLiquids.getOrDefault(side, 0L);
    }
    public long storedGas(Direction side) { return gas.stored(side); }
    public long gasCapacity() { return gas.capacity(); }

    public void saveBuffers(CompoundTag power) {
        var buffers = new CompoundTag();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            var face = new CompoundTag();
            face.putLong("liquid", storedLiquid(side));
            face.putLong("gas", storedGas(side));
            buffers.put(side.getName(), face);
        }
        power.put("buffers", buffers);
    }

    /** Restores into existing views, retaining unavailable resources as dormant quantities. */
    public boolean restoreBuffers(CompoundTag power) {
        boolean truncated = false;
        var buffers = power.getCompound("buffers");
        for (Direction side : Direction.Plane.HORIZONTAL) {
            var face = buffers.getCompound(side.getName());
            long liquidAmount = savedLong(face, "liquid"), gasAmount = savedLong(face, "gas");
            var liquid = liquids.get(side);
            int restored = (int) Math.clamp(liquidAmount, 0L, liquid.getCapacity());
            if (liquidAvailable) liquid.setFluid(restored == 0 ? FluidStack.EMPTY
                    : new FluidStack(BuiltInRegistries.FLUID.get(SOURCE_FLUID), restored));
            else dormantLiquids.put(side, (long) restored);
            gas.restore(side, gasAmount);
            truncated |= liquidAmount > liquid.getCapacity() || gasAmount > gas.capacity();
        }
        return truncated;
    }

    private static long savedLong(CompoundTag tag, String key) {
        return tag.get(key) instanceof NumericTag number ? number.getAsLong() : 0;
    }

    @Override public int fundedSideMask(SourceMode mode) {
        int mask = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            boolean funded = switch (mode) {
                case LIQUID -> liquidAvailable && storedLiquid(side) >= settings.liquid().cost();
                case GAS -> gas.available() && gas.canPay(side);
                case JAR -> false;
            };
            if (funded) mask |= 1 << side.get2DDataValue();
        }
        return mask;
    }

    @Override public boolean pay(SourceMode mode, int sideMask) {
        if (!eligible.getAsBoolean() || mode == SourceMode.JAR || sideMask == 0 || (sideMask & ~15) != 0
                || (fundedSideMask(mode) & sideMask) != sideMask) return false;
        // Every requested internal tank is checked before any quantity is consumed.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if ((sideMask & (1 << side.get2DDataValue())) == 0) continue;
            if (mode == SourceMode.LIQUID) liquids.get(side).drain(Math.toIntExact(settings.liquid().cost()), IFluidHandler.FluidAction.EXECUTE);
            else gas.pay(side);
        }
        return true;
    }

    @Override public boolean canPayJar() { return jars.canPay(level, pos, settings); }
    @Override public boolean payJar() { return eligible.getAsBoolean() && jars.pay(level, pos, settings); }

    public void pullAdjacent() {
        if (!eligible.getAsBoolean()) return;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var liquid = liquids.get(direction);
            BlockPos neighbor = pos.relative(direction);
            if (!level.isLoaded(neighbor)) continue;
            if (liquidAvailable && liquid.getSpace() > 0) {
                var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, neighbor, direction.getOpposite());
                if (handler != null) pullFluid(handler, direction);
            }
        }
        gas.pullAdjacent();
    }

    private void pullFluid(IFluidHandler handler, Direction side) {
        var liquid = liquids.get(side);
        var fluidInput = fluidInputs.get(side);
        var request = new FluidStack(BuiltInRegistries.FLUID.get(SOURCE_FLUID), liquid.getSpace());
        var simulated = handler.drain(request, IFluidHandler.FluidAction.SIMULATE);
        if (!isSourceFluid(simulated)) return;
        int accepted = fluidInput.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
        if (accepted > 0) {
            var actual = handler.drain(simulated.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
            fluidInput.fill(actual, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private static boolean isSourceFluid(FluidStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(SOURCE_FLUID);
    }

    private final class FluidInput implements IFluidHandler {
        private final FluidTank liquid;
        private FluidInput(FluidTank liquid) { this.liquid = liquid; }
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return tank == 0 ? liquid.getFluid().copy() : FluidStack.EMPTY; }
        public int getTankCapacity(int tank) { return tank == 0 ? liquid.getCapacity() : 0; }
        public boolean isFluidValid(int tank, FluidStack stack) { return liquidAvailable && tank == 0 && isSourceFluid(stack); }
        public int fill(FluidStack stack, FluidAction action) { return liquidAvailable && eligible.getAsBoolean() ? liquid.fill(stack, action) : 0; }
        public FluidStack drain(FluidStack stack, FluidAction action) { return FluidStack.EMPTY; }
        public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    }
}
