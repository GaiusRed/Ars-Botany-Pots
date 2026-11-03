package io.github.gaiusred.sourcepots.gametest;

import java.util.HashMap;
import java.util.Map;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Development-only real block capabilities backed by native resource tanks. */
public final class ResourceTestEnvironment {
    private static final Map<BlockPos,Supplier> SUPPLIERS = new HashMap<>();
    private ResourceTestEnvironment() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK,(level,pos,state,entity,side)-> {
            var supplier=SUPPLIERS.get(pos);
            return supplier!=null && supplier.allows(side)?supplier.fluid:null;
        },Blocks.GLASS);
        event.registerBlock(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),(level,pos,state,entity,side)-> {
            var supplier=SUPPLIERS.get(pos);
            return supplier!=null && supplier.allows(side)?supplier.chemical:null;
        },Blocks.GLASS);
    }

    static Supplier place(GameTestHelper helper,BlockPos relative,Direction allowed,boolean shortExecute) {
        var supplier=new Supplier(allowed,shortExecute);
        SUPPLIERS.put(helper.absolutePos(relative).immutable(),supplier);
        helper.setBlock(relative,Blocks.GLASS);
        helper.getLevel().invalidateCapabilities(helper.absolutePos(relative));
        return supplier;
    }

    static void remove(GameTestHelper helper,BlockPos relative) {
        SUPPLIERS.remove(helper.absolutePos(relative)); helper.setBlock(relative,Blocks.AIR);
    }

    public static void clear(ServerLevel level) { SUPPLIERS.clear(); }

    static final class Supplier {
        Direction allowed;
        final FluidTank fluid;
        final IChemicalTank gas=BasicChemicalTank.create(1000,null);
        final IChemicalHandler chemical;
        Supplier(Direction allowed,boolean shortExecute) {
            this.allowed=allowed;
            fluid=new FluidTank(1000) {
                @Override public FluidStack drain(int amount,FluidAction action) {
                    return super.drain(shortExecute && action.execute()?Math.min(40,amount):amount,action);
                }
            };
            fluid.fill(SourceAccessGameTests.fluid(1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            gas.setStack(SourceAccessGameTests.gas(1000));
            chemical=new IChemicalHandler() {
                public int getChemicalTanks() { return 1; }
                public ChemicalStack getChemicalInTank(int tank) { return gas.getStack().copy(); }
                public void setChemicalInTank(int tank,ChemicalStack stack) { gas.setStack(stack); }
                public long getChemicalTankCapacity(int tank) { return gas.getCapacity(); }
                public boolean isValid(int tank,ChemicalStack stack) { return true; }
                public ChemicalStack insertChemical(int tank,ChemicalStack stack,Action action) { return gas.insert(stack,action,AutomationType.EXTERNAL); }
                public ChemicalStack extractChemical(int tank,long amount,Action action) {
                    return gas.extract(shortExecute && action.execute()?Math.min(40,amount):amount,action,AutomationType.EXTERNAL);
                }
            };
        }
        boolean allows(Direction side) { return allowed==null || side==allowed; }
    }
}
