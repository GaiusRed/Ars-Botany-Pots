package io.github.gaiusred.sourcepots.source;

import io.github.gaiusred.sourcepots.compat.mekanism.MekanismSourceStorage;
import java.util.Objects;
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
    private final FluidTank liquid;
    private final boolean liquidAvailable;
    private int dormantLiquid;
    private final GasStorage gas;
    private final IFluidHandler fluidInput;
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
        liquid = new FluidTank(Math.toIntExact(settings.liquid().cost()), PotSourceAccess::isSourceFluid) {
            @Override protected void onContentsChanged() { changed.run(); }
        };
        gas = ModList.get().isLoaded("mekanism")
                ? MekanismSourceStorage.create(level, this.pos, settings.gas().cost(), eligible, changed)
                : new UnavailableGasStorage(settings.gas().cost());
        fluidInput = new FluidInput();
    }

    public boolean liquidAvailable() { return liquidAvailable; }
    public IFluidHandler fluidHandler() { return fluidInput; }
    public GasStorage gasStorage() { return gas; }
    public int storedLiquid() { return liquidAvailable ? liquid.getFluidAmount() : dormantLiquid; }
    public long storedGas() { return gas.stored(); }
    public long gasCapacity() { return gas.capacity(); }

    /** Restores saved quantities even when their integration is unavailable.
     * @return whether a saved quantity exceeded the configured capacity
     */
    public boolean restoreBuffers(long liquidAmount, long gasAmount) {
        int restoredLiquid = (int) Math.clamp(liquidAmount, 0L, liquid.getCapacity());
        if (liquidAvailable) {
            liquid.setFluid(restoredLiquid == 0 ? FluidStack.EMPTY
                    : new FluidStack(BuiltInRegistries.FLUID.get(SOURCE_FLUID), restoredLiquid));
        } else dormantLiquid = restoredLiquid;
        gas.restore(gasAmount);
        return liquidAmount > liquid.getCapacity() || gasAmount > gas.capacity();
    }

    @Override public boolean canPay(SourceMode mode) {
        return switch (mode) {
            case LIQUID -> liquidAvailable && liquid.getFluidAmount() == liquid.getCapacity();
            case GAS -> gas.available() && gas.stored() == gas.capacity();
            case JAR -> jars.canPay(level, pos, settings);
        };
    }

    @Override public boolean pay(SourceMode mode) {
        if (!canPay(mode)) return false;
        return switch (mode) {
            case LIQUID -> liquid.drain(liquid.getCapacity(), IFluidHandler.FluidAction.EXECUTE).getAmount() == liquid.getCapacity();
            case GAS -> gas.pay();
            case JAR -> jars.pay(level, pos, settings);
        };
    }

    public void pullAdjacent() {
        if (!eligible.getAsBoolean()) return;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.isLoaded(neighbor)) continue;
            if (liquidAvailable && liquid.getSpace() > 0) {
                var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, neighbor, direction.getOpposite());
                if (handler != null) pullFluid(handler);
            }
        }
        gas.pullAdjacent();
    }

    private void pullFluid(IFluidHandler handler) {
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
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return tank == 0 ? liquid.getFluid().copy() : FluidStack.EMPTY; }
        public int getTankCapacity(int tank) { return tank == 0 ? liquid.getCapacity() : 0; }
        public boolean isFluidValid(int tank, FluidStack stack) { return liquidAvailable && tank == 0 && isSourceFluid(stack); }
        public int fill(FluidStack stack, FluidAction action) { return liquidAvailable && eligible.getAsBoolean() ? liquid.fill(stack, action) : 0; }
        public FluidStack drain(FluidStack stack, FluidAction action) { return FluidStack.EMPTY; }
        public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    }
}
