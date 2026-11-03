package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity;
import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class SourceCadenceGameTests {
    @GameTest(template="empty", batch="source-cadence")
    public static void sixLiquidSuppliersMatchOneAtFiveTickCadence(GameTestHelper h) {
        compareSuppliers(h, false);
    }

    @GameTest(template="empty", batch="source-cadence")
    public static void sixGasSuppliersMatchOneAtOneTickCadence(GameTestHelper h) {
        compareSuppliers(h, true);
    }

    @GameTest(template="empty", batch="source-cadence")
    public static void fixed600TickBoostsHarvestWithoutPauseAtSharedFaceCadence(GameTestHelper h) {
        var one=ArsPotGameTests.pot(h,new BlockPos(2,2,5));
        var six=ArsPotGameTests.pot(h,new BlockPos(8,2,5));
        var single=suppliers(h,new BlockPos(2,2,5),false);
        var multiple=suppliers(h,new BlockPos(8,2,5),true);
        try(var ignored=new GrowthConfigFixture(600,600)) {
            for(int tick=0;tick<12;tick++) {
                h.assertTrue(supplyAndTick(h,one,single,true)==250 && supplyAndTick(h,six,multiple,true)==250,
                        "One and six faces must buy exactly one gas operation when partial bank still needs growth");
                for(var pot:List.of(one,six)) {
                    int wheat=0;
                    for(int slot:net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity.STORAGE_SLOTS)
                        if(pot.getItem(slot).is(net.minecraft.world.item.Items.WHEAT)) wheat+=pot.getItem(slot).getCount();
                    h.assertTrue(wheat==tick/2 && pot.growthTime()==(tick%2==0?601:1200),
                            "Fixed 600-tick boosts must harvest every second tick after initial maturity, with no added pause");
                    h.assertTrue(pot.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("sourcepots").getDouble("growth_credit")== (tick%2==0?0:2),
                            "Two exact paid ticks carry across each mature cycle");
                }
            }
        } finally {
            for(var entry:single) ResourceTestEnvironment.remove(h,entry.position());
            for(var entry:multiple) ResourceTestEnvironment.remove(h,entry.position());
        }
        h.succeed();
    }

    private static void compareSuppliers(GameTestHelper h, boolean gas) {
        var one = ArsPotGameTests.pot(h, new BlockPos(2,2,5));
        var six = ArsPotGameTests.pot(h, new BlockPos(8,2,5));
        h.assertTrue(SourcePotsConfig.snapshot().liquid().intervalTicks()==5
                && SourcePotsConfig.snapshot().gas().intervalTicks()==1,"Test server must use the new 5/1 defaults");
        assertSharedHandlers(h, one);
        assertSharedHandlers(h, six);
        var single = suppliers(h,new BlockPos(2,2,5),false);
        var multiple = suppliers(h,new BlockPos(8,2,5),true);
        try (var ignored = new GrowthConfigFixture(10, 10)) {
            long singleIntake=0, multipleIntake=0;
            for (int tick=0;tick<20;tick++) {
                singleIntake+=supplyAndTick(h,one,single,gas);
                multipleIntake+=supplyAndTick(h,six,multiple,gas);
                int payments=gas?tick+1:tick/5+1;
                int expectedGrowth=tick+1+10*payments;
                h.assertTrue(one.growthTime()==expectedGrowth && six.growthTime()==expectedGrowth,
                        "One and six suppliers must give exactly natural ticks plus one growth increment per paid operation at tick "+tick);
                long expectedCooldown=gas?1:5-tick%5;
                for (var pot:List.of(one,six)) {
                    var power=pot.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("sourcepots");
                    h.assertTrue(power.getLong("boost_cooldown")==expectedCooldown,"All faces must share one cooldown");
                    h.assertTrue(stored(pot,gas)<= (gas?250:500),"Shared buffer must never exceed one operation");
                    h.assertTrue(stored(pot,!gas)==0,"Other resource must remain independently empty");
                }
            }
            long spent=gas?5000:2000;
            long buffered=gas?0:500;
            h.assertTrue(singleIntake==spent+buffered && multipleIntake==spent+buffered,
                    "One and six suppliers must fund the same exact payments, allowing one retained liquid operation");
            h.assertTrue(stored(one,gas)==buffered && stored(six,gas)==buffered,"Intake retained in storage is not an extra boost");

        } finally {
            for(var entry:single) ResourceTestEnvironment.remove(h,entry.position());
            for(var entry:multiple) ResourceTestEnvironment.remove(h,entry.position());
        }
        h.succeed();
    }

    private static void assertSharedHandlers(GameTestHelper h, ArsBotanyPotBlockEntity pot) {
        var fluid=pot.sourceAccess().fluidHandler();
        var chemical=MekanismTestAccess.chemical(pot.sourceAccess());
        for(var side:Direction.values()) {
            h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pot.getBlockPos(),side)==fluid,
                    "All six fluid faces must expose the same handler and storage");
            h.assertTrue(h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),pot.getBlockPos(),side)==chemical,
                    "All six gas faces must expose the same handler and storage");
        }
    }

    private static List<Supply> suppliers(GameTestHelper h, BlockPos center, boolean six) {
        var result=new ArrayList<Supply>();
        for(var side:Direction.values()) {
            var pos=center.relative(side);
            result.add(new Supply(pos,ResourceTestEnvironment.place(h,pos,side.getOpposite(),false)));
            if(!six) break;
        }
        return result;
    }

    private static long supplyAndTick(GameTestHelper h, ArsBotanyPotBlockEntity pot, List<Supply> suppliers, boolean gas) {
        for(var entry:suppliers) {
            entry.supplier().fluid.setFluid(gas?FluidStack.EMPTY:SourceAccessGameTests.fluid(1000));
            entry.supplier().gas.setStack(gas?SourceAccessGameTests.gas(1000):ChemicalStack.EMPTY);
        }
        ArsPotGameTests.tick(h,pot);
        return suppliers.stream().mapToLong(entry->1000-(gas?entry.supplier().gas.getStored():entry.supplier().fluid.getFluidAmount())).sum();
    }

    private static long stored(ArsBotanyPotBlockEntity pot, boolean gas) {
        return gas?MekanismTestAccess.chemical(pot.sourceAccess()).getChemicalInTank(0).getAmount()
                :pot.sourceAccess().fluidHandler().getFluidInTank(0).getAmount();
    }

    private record Supply(BlockPos position, ResourceTestEnvironment.Supplier supplier) {}
}
