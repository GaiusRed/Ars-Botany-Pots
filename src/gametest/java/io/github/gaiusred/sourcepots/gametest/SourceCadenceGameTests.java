package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity;
import io.github.gaiusred.sourcepots.source.SourceMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import mekanism.api.Action;

@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class SourceCadenceGameTests {
    @GameTest(template="empty", batch="source-cadence")
    public static void fourLiquidFacesShareFiveTickCooldownAndOneAggregateBoost(GameTestHelper h) {
        verifyCadence(h,false);
    }

    @GameTest(template="empty", batch="source-cadence")
    public static void fourGasFacesShareOneTickCooldownAndOneAggregateBoost(GameTestHelper h) {
        verifyCadence(h,true);
    }

    @GameTest(template="empty", batch="source-cadence")
    public static void fixed600TickAggregateBoostsHarvestWithoutPauseWithOneOrFourFaces(GameTestHelper h) {
        var one=ArsPotGameTests.pot(h,new BlockPos(2,2,5));
        var four=ArsPotGameTests.pot(h,new BlockPos(8,2,5));
        var suppliers=new java.util.ArrayList<ResourceTestEnvironment.Supplier>();
        var positions=new java.util.ArrayList<BlockPos>();
        for(var center:java.util.List.of(new BlockPos(2,2,5),new BlockPos(8,2,5))) {
            for(Direction side:Direction.Plane.HORIZONTAL) {
                var position=center.relative(side);
                suppliers.add(ResourceTestEnvironment.place(h,position,side.getOpposite(),false));
                positions.add(position);
                if(center.getX()==2) break;
            }
        }
        try(var ignored=new GrowthConfigFixture(600,600)) {
            for(int tick=0;tick<12;tick++) {
                for(var supplier:suppliers) {
                    supplier.fluid.setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
                    supplier.gas.setStack(SourceAccessGameTests.gas(1000));
                }
                ArsPotGameTests.tick(h,one); ArsPotGameTests.tick(h,four);
                for(var supplier:suppliers) h.assertTrue(supplier.gas.getStored()==750,"Each funded face pays exactly 250 for the single aggregate boost");
                for(var pot:java.util.List.of(one,four)) {
                    int wheat=0;
                    for(int slot:net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity.STORAGE_SLOTS)
                        if(pot.getItem(slot).is(net.minecraft.world.item.Items.WHEAT)) wheat+=pot.getItem(slot).getCount();
                    h.assertTrue(wheat==(tick+1)/2 && pot.growthTime()==(tick%2==0?601+tick/2:(tick+1)/2),"Fixed 600-tick aggregate boosts harvest immediately and preserve native reset costs");
                    h.assertTrue(pot.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("sourcepots_v2").getDouble("growth_credit")==0,"Unused paid growth advances the following partial cycle immediately");
                }
            }
        } finally {
            for(var position:positions) ResourceTestEnvironment.remove(h,position);
        }
        h.succeed();
    }

    private static void verifyCadence(GameTestHelper h, boolean gas) {
        var pot=ArsPotGameTests.pot(h,new BlockPos(5,2,5));
        try(var ignored=new GrowthConfigFixture(10,10)) {
            for(int tick=0;tick<20;tick++) {
                for(Direction side:Direction.Plane.HORIZONTAL) {
                    if(gas) ((io.github.gaiusred.sourcepots.compat.mekanism.MekanismSourceStorage)pot.sourceAccess().gasStorage()).handler(side)
                            .insertChemical(SourceAccessGameTests.gas(250),Action.EXECUTE);
                    else pot.sourceAccess().fluidHandler(side).fill(SourceAccessGameTests.fluid(500),FluidAction.EXECUTE);
                }
                ArsPotGameTests.tick(h,pot);
                int payments=gas?tick+1:tick/5+1;
                h.assertTrue(pot.growthTime()==tick+1+10*payments,"One aggregate configured growth increment per purchase, never multiplied by faces");
                var power=pot.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("sourcepots_v2");
                h.assertTrue(power.getLong("boost_cooldown")== (gas?1:5-tick%5),"One cooldown advances once per real tick");
                for(Direction side:Direction.Plane.HORIZONTAL) {
                    long amount=gas?pot.sourceAccess().storedGas(side):pot.sourceAccess().storedLiquid(side);
                    h.assertTrue(amount==(gas||tick%5==0?0:500),"Each face pays once on purchase ticks and retains storage between them");
                }
            }
        }
        for(var side:Direction.values()) {
            var fluid=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pot.getBlockPos(),side);
            var chemical=h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),pot.getBlockPos(),side);
            h.assertTrue((fluid!=null)==side.getAxis().isHorizontal() && (chemical!=null)==side.getAxis().isHorizontal(),"Only horizontal Source capabilities exist");
            if(side.getAxis().isHorizontal()) {
                h.assertTrue(fluid==pot.sourceAccess().fluidHandler(side),"Cached handler has matching face storage");
                for(Direction other:Direction.Plane.HORIZONTAL) if(side!=other)
                    h.assertTrue(fluid!=pot.sourceAccess().fluidHandler(other),"Faces have independent stable views");
            }
        }
        h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pot.getBlockPos(),null)==null,"Unsided fluid access absent");
        h.succeed();
    }
}
