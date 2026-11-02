package io.github.gaiusred.sourcepots.source;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Owns resource storage and world access without growth or timer policy. */
public final class PotSourceAccess implements SourceAccess {
    private static final ResourceLocation SOURCE_FLUID = ResourceLocation.parse("sauce:source_fluid");
    private static final ResourceLocation SOURCE_GAS = ResourceLocation.parse("starbunclemania:source_gas");
    private final ServerLevel level;
    private final BlockPos pos;
    private final SourceSettings settings;
    private final BooleanSupplier eligible;
    private final FluidTank liquid;
    private final IChemicalTank gas;
    private final IFluidHandler fluidInput;
    private final IChemicalHandler chemicalInput;
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
        liquid = new FluidTank(Math.toIntExact(settings.liquid().cost()), PotSourceAccess::isSourceFluid) {
            @Override protected void onContentsChanged() { changed.run(); }
        };
        gas = BasicChemicalTank.inputModern(settings.gas().cost(), stack -> eligible.getAsBoolean(),
                PotSourceAccess::isSourceGas, changed::run);
        fluidInput = new FluidInput();
        chemicalInput = new ChemicalInput();
    }

    public IFluidHandler fluidHandler() { return fluidInput; }
    public IChemicalHandler chemicalHandler() { return chemicalInput; }

    /** Restores only canonical Source, including when the crop is currently ineligible.
     * @return whether a saved quantity exceeded the configured capacity
     */
    public boolean restoreBuffers(long liquidAmount, long gasAmount) {
        int restoredLiquid = (int) Math.clamp(liquidAmount, 0L, liquid.getCapacity());
        long restoredGas = Math.clamp(gasAmount, 0L, gas.getCapacity());
        liquid.setFluid(restoredLiquid == 0 ? FluidStack.EMPTY
                : new FluidStack(BuiltInRegistries.FLUID.get(SOURCE_FLUID), restoredLiquid));
        gas.setStack(restoredGas == 0 ? ChemicalStack.EMPTY
                : new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(SOURCE_GAS).orElseThrow(), restoredGas));
        return liquidAmount > liquid.getCapacity() || gasAmount > gas.getCapacity();
    }

    @Override public boolean canPay(SourceMode mode) {
        return switch (mode) {
            case LIQUID -> liquid.getFluidAmount() == liquid.getCapacity();
            case GAS -> gas.getStored() == gas.getCapacity();
            case JAR -> jars.canPay(level, pos, settings);
        };
    }

    @Override public boolean pay(SourceMode mode) {
        if (!canPay(mode)) return false;
        return switch (mode) {
            case LIQUID -> liquid.drain(liquid.getCapacity(), IFluidHandler.FluidAction.EXECUTE).getAmount() == liquid.getCapacity();
            case GAS -> gas.extract(gas.getCapacity(), Action.EXECUTE, AutomationType.INTERNAL).getAmount() == gas.getCapacity();
            case JAR -> jars.pay(level, pos, settings);
        };
    }

    public void pullAdjacent() {
        if (!eligible.getAsBoolean()) return;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.isLoaded(neighbor)) continue;
            if (liquid.getSpace() > 0) {
                var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, neighbor, direction.getOpposite());
                if (handler != null) pullFluid(handler);
            }
            if (gas.getNeeded() > 0) {
                var handler = level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), neighbor, direction.getOpposite());
                if (handler != null) pullChemical(handler);
            }
        }
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

    private void pullChemical(IChemicalHandler handler) {
        var request = new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(SOURCE_GAS).orElseThrow(), gas.getNeeded());
        var simulated = handler.extractChemical(request, Action.SIMULATE);
        if (!isSourceGas(simulated)) return;
        long accepted = simulated.getAmount() - chemicalInput.insertChemical(simulated, Action.SIMULATE).getAmount();
        if (accepted > 0) {
            var actual = handler.extractChemical(simulated.copyWithAmount(accepted), Action.EXECUTE);
            chemicalInput.insertChemical(actual, Action.EXECUTE);
        }
    }

    private static boolean isSourceFluid(FluidStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(SOURCE_FLUID);
    }

    private static boolean isSourceGas(ChemicalStack stack) {
        return !stack.isEmpty() && MekanismAPI.CHEMICAL_REGISTRY.getKey(stack.getChemical()).equals(SOURCE_GAS);
    }

    private final class FluidInput implements IFluidHandler {
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return tank == 0 ? liquid.getFluid().copy() : FluidStack.EMPTY; }
        public int getTankCapacity(int tank) { return tank == 0 ? liquid.getCapacity() : 0; }
        public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && isSourceFluid(stack); }
        public int fill(FluidStack stack, FluidAction action) { return eligible.getAsBoolean() ? liquid.fill(stack, action) : 0; }
        public FluidStack drain(FluidStack stack, FluidAction action) { return FluidStack.EMPTY; }
        public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    }
    private final class ChemicalInput implements IChemicalHandler {
        public int getChemicalTanks() { return 1; }
        public ChemicalStack getChemicalInTank(int tank) { return tank == 0 ? gas.getStack().copy() : ChemicalStack.EMPTY; }
        public void setChemicalInTank(int tank, ChemicalStack stack) { throw new UnsupportedOperationException("Source input handler cannot replace stored chemicals"); }
        public long getChemicalTankCapacity(int tank) { return tank == 0 ? gas.getCapacity() : 0; }
        public boolean isValid(int tank, ChemicalStack stack) { return tank == 0 && isSourceGas(stack); }
        public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
            return tank == 0 ? gas.insert(stack, action, AutomationType.EXTERNAL) : stack.copy();
        }
        public ChemicalStack extractChemical(int tank, long amount, Action action) { return ChemicalStack.EMPTY; }
    }
}
