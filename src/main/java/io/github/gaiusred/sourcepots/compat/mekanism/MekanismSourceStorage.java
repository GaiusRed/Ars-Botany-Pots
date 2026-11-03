package io.github.gaiusred.sourcepots.compat.mekanism;

import io.github.gaiusred.sourcepots.source.GasStorage;
import io.github.gaiusred.sourcepots.source.UnavailableGasStorage;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** Loaded only after Mekanism is known to be installed. */
public final class MekanismSourceStorage implements GasStorage {
    private static final ResourceLocation SOURCE_GAS = ResourceLocation.parse("starbunclemania:source_gas");
    private final ServerLevel level;
    private final BlockPos pos;
    private final BooleanSupplier eligible;
    private final IChemicalTank tank;
    private final IChemicalHandler input = new ChemicalInput();

    public static GasStorage create(ServerLevel level, BlockPos pos, long capacity, BooleanSupplier eligible, Runnable changed) {
        if (MekanismAPI.CHEMICAL_REGISTRY.getHolder(SOURCE_GAS).isEmpty()) return new UnavailableGasStorage(capacity);
        return new MekanismSourceStorage(level, pos, capacity, eligible, changed);
    }

    private MekanismSourceStorage(ServerLevel level, BlockPos pos, long capacity, BooleanSupplier eligible, Runnable changed) {
        this.level = level;
        this.pos = pos;
        this.eligible = eligible;
        tank = BasicChemicalTank.inputModern(capacity, stack -> eligible.getAsBoolean(),
                MekanismSourceStorage::isSourceGas, changed::run);
    }

    public IChemicalHandler handler() { return input; }
    public boolean available() { return true; }
    public long stored() { return tank.getStored(); }
    public long capacity() { return tank.getCapacity(); }
    public void restore(long amount) {
        long restored = Math.clamp(amount, 0L, capacity());
        tank.setStack(restored == 0 ? ChemicalStack.EMPTY
                : new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(SOURCE_GAS).orElseThrow(), restored));
    }
    public boolean pay() {
        return stored() == capacity()
                && tank.extract(capacity(), Action.EXECUTE, AutomationType.INTERNAL).getAmount() == capacity();
    }
    public void pullAdjacent() {
        if (!eligible.getAsBoolean()) return;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.isLoaded(neighbor) || tank.getNeeded() <= 0) continue;
            var handler = level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), neighbor, direction.getOpposite());
            if (handler == null) continue;
            var request = new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(SOURCE_GAS).orElseThrow(), tank.getNeeded());
            var simulated = handler.extractChemical(request, Action.SIMULATE);
            if (!isSourceGas(simulated)) continue;
            long accepted = simulated.getAmount() - input.insertChemical(simulated, Action.SIMULATE).getAmount();
            if (accepted > 0) {
                var actual = handler.extractChemical(simulated.copyWithAmount(accepted), Action.EXECUTE);
                input.insertChemical(actual, Action.EXECUTE);
            }
        }
    }

    private static boolean isSourceGas(ChemicalStack stack) {
        return !stack.isEmpty() && MekanismAPI.CHEMICAL_REGISTRY.getKey(stack.getChemical()).equals(SOURCE_GAS);
    }

    private final class ChemicalInput implements IChemicalHandler {
        public int getChemicalTanks() { return 1; }
        public ChemicalStack getChemicalInTank(int tankIndex) { return tankIndex == 0 ? tank.getStack().copy() : ChemicalStack.EMPTY; }
        public void setChemicalInTank(int tankIndex, ChemicalStack stack) { throw new UnsupportedOperationException("Source input handler cannot replace stored chemicals"); }
        public long getChemicalTankCapacity(int tankIndex) { return tankIndex == 0 ? capacity() : 0; }
        public boolean isValid(int tankIndex, ChemicalStack stack) { return tankIndex == 0 && isSourceGas(stack); }
        public ChemicalStack insertChemical(int tankIndex, ChemicalStack stack, Action action) {
            return tankIndex == 0 ? tank.insert(stack, action, AutomationType.EXTERNAL) : stack.copy();
        }
        public ChemicalStack extractChemical(int tankIndex, long amount, Action action) { return ChemicalStack.EMPTY; }
    }
}
