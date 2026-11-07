package io.github.gaiusred.sourcepots.gametest;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import io.github.gaiusred.sourcepots.growth.BonemealGrowth;
import io.github.gaiusred.sourcepots.source.*;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class SourceAccessGameTests {
    static final BlockPos CENTER = new BlockPos(5, 2, 5);
    private static final SourceSettings SETTINGS = SourceSettings.defaults();

    @net.minecraft.gametest.framework.AfterBatch(batch="resources")
    public static void clearResourceFixtures(net.minecraft.server.level.ServerLevel level) { ResourceTestEnvironment.clear(level); }

    @GameTest(template="empty", batch="resources")
    public static void buffersCoexistAt500And250(GameTestHelper h) {
        int[] changes = {0};
        var access = new PotSourceAccess(h.getLevel(), h.absolutePos(CENTER), SETTINGS, () -> true, () -> changes[0]++);
        h.assertTrue(access.fluidHandler(Direction.NORTH).getTankCapacity(0) == 500 && MekanismTestAccess.chemical(access).getChemicalTankCapacity(0) == 250, "Independent capacities must be 500 and 250");
        h.assertTrue(access.fluidHandler(Direction.NORTH).fill(fluid(1000), FluidAction.SIMULATE) == 500, "Simulated fluid acceptance is bounded");
        h.assertTrue(MekanismTestAccess.chemical(access).insertChemical(gas(400), Action.SIMULATE).getAmount() == 150, "Simulated chemical insertion returns remainder");
        h.assertTrue(changes[0] == 0 && storedFluid(access) == 0 && storedGas(access) == 0, "Simulation must not mutate or notify");
        fill(h, access);
        h.assertTrue(changes[0] == 2, "Each actual fill notifies once");
        var fluidCopy = access.fluidHandler(Direction.NORTH).getFluidInTank(0);
        var gasCopy = MekanismTestAccess.chemical(access).getChemicalInTank(0);
        fluidCopy.setAmount(1); gasCopy.setAmount(1);
        h.assertTrue(storedFluid(access) == 500 && storedGas(access) == 250, "Stack views cannot mutate storage");
        h.assertTrue(access.pay(SourceMode.GAS, 1 << Direction.NORTH.get2DDataValue()) && storedFluid(access) == 500 && storedGas(access) == 0, "Gas payment leaves liquid untouched");
        h.assertTrue(access.pay(SourceMode.LIQUID, 1 << Direction.NORTH.get2DDataValue()) && storedFluid(access) == 0, "Liquid payment consumes exactly one operation");
        h.assertTrue(!access.pay(SourceMode.GAS, 1 << Direction.NORTH.get2DDataValue()) && !access.pay(SourceMode.LIQUID, 1 << Direction.NORTH.get2DDataValue()) && changes[0] == 4, "Empty buffers cannot pay or notify");
        h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void rejectsWrongResources(GameTestHelper h) {
        var a = access(h); fill(h, a); a.pay(SourceMode.GAS, 1 << Direction.NORTH.get2DDataValue()); a.pay(SourceMode.LIQUID, 1 << Direction.NORTH.get2DDataValue());
        h.assertTrue(a.fluidHandler(Direction.NORTH).fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE) == 0, "Water is not Source");
        var wrong = new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.parse("mekanism:oxygen")).orElseThrow(), 250);
        h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(wrong, Action.EXECUTE).getAmount() == 250, "Oxygen must remain unaccepted");
        h.assertTrue(storedFluid(a) == 0 && storedGas(a) == 0, "Wrong resources cannot enter storage"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void partialFillCannotPay(GameTestHelper h) {
        var a = access(h);
        h.assertTrue(a.fluidHandler(Direction.NORTH).fill(fluid(499), FluidAction.EXECUTE) == 499, "Partial fluid accepted");
        h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(gas(199), Action.EXECUTE).isEmpty(), "Partial gas accepted");
        h.assertTrue(!(a.fundedSideMask(SourceMode.GAS) != 0) && !(a.fundedSideMask(SourceMode.LIQUID) != 0) && !a.pay(SourceMode.GAS, 1 << Direction.NORTH.get2DDataValue()) && !a.pay(SourceMode.LIQUID, 1 << Direction.NORTH.get2DDataValue()), "Partial operation cannot pay");
        h.assertTrue(storedFluid(a) == 499 && storedGas(a) == 199, "Failed payment preserves partial contents"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void fullBufferRejectsMore(GameTestHelper h) {
        var a = access(h); fill(h,a);
        for (var action : FluidAction.values()) h.assertTrue(a.fluidHandler(Direction.NORTH).fill(fluid(1), action) == 0, "Full liquid buffer rejects more");
        for (var action : Action.values()) h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(gas(1), action).getAmount() == 1, "Full chemical buffer returns all");
        h.assertTrue(storedFluid(a) == 500 && storedGas(a) == 250, "Buffers cannot overfill"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void invalidGrowthRejectsIntake(GameTestHelper h) {
        var pot = BonemealGrowthGameTests.pot(h);
        var a = new PotSourceAccess(h.getLevel(), pot.getBlockPos(), SETTINGS, () -> new BonemealGrowth().isEligible(pot,h.getLevel()));
        fill(h,a); a.pay(SourceMode.GAS, 1 << Direction.NORTH.get2DDataValue()); a.pay(SourceMode.LIQUID, 1 << Direction.NORTH.get2DDataValue());
        pot.setSeed(net.minecraft.world.item.ItemStack.EMPTY);
        assertGated(h,a); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void matureCropRejectsIntake(GameTestHelper h) {
        var pot = BonemealGrowthGameTests.pot(h);
        var a = new PotSourceAccess(h.getLevel(), pot.getBlockPos(), SETTINGS, () -> new BonemealGrowth().isEligible(pot,h.getLevel()));
        fill(h,a); a.pay(SourceMode.GAS, 1 << Direction.NORTH.get2DDataValue()); a.pay(SourceMode.LIQUID, 1 << Direction.NORTH.get2DDataValue());
        pot.updateGrowthTime(pot.getRequiredGrowthTicks());
        assertGated(h,a); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void automationCannotExtract(GameTestHelper h) {
        var a=access(h); fill(h,a);
        for(var action:FluidAction.values()) {
            h.assertTrue(a.fluidHandler(Direction.NORTH).drain(500,action).isEmpty(), "Amount drain denied");
            h.assertTrue(a.fluidHandler(Direction.NORTH).drain(fluid(500),action).isEmpty(), "Stack drain denied");
        }
        for(var action:Action.values()) {
            h.assertTrue(MekanismTestAccess.chemical(a).extractChemical(0,200,action).isEmpty(), "Indexed extraction denied");
            h.assertTrue(MekanismTestAccess.chemical(a).extractChemical(200,action).isEmpty(), "Amount extraction denied");
            h.assertTrue(MekanismTestAccess.chemical(a).extractChemical(gas(250),action).isEmpty(), "Stack extraction denied");
        }
        boolean rejected=false;
        try { MekanismTestAccess.chemical(a).setChemicalInTank(0,ChemicalStack.EMPTY); } catch(UnsupportedOperationException expected) { rejected=true; }
        h.assertTrue(rejected && storedFluid(a)==500 && storedGas(a)==250,"Setter cannot bypass input-only policy"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void horizontalFacesPullIntoIndependentMatchingBuffers(GameTestHelper h) {
        var a=access(h);
        for(var direction:Direction.Plane.HORIZONTAL) {
            var supplier=ResourceTestEnvironment.place(h,CENTER.relative(direction),direction.getOpposite(),false);
            a.pullAdjacent();
            h.assertTrue(a.storedLiquid(direction)==500 && a.storedGas(direction)==250,"Matching face must receive pulled resources: "+direction);
            h.assertTrue(supplier.fluid.getFluidAmount()==500 && supplier.gas.getStored()==750,"Exactly one payment pulled from "+direction);
            ResourceTestEnvironment.remove(h,CENTER.relative(direction));
        }
        h.assertTrue(a.fundedSideMask(SourceMode.GAS)==15 && a.fundedSideMask(SourceMode.LIQUID)==15,"All four faces remain independently funded");
        var verticalAccess=access(h);
        for (var direction : new Direction[]{Direction.UP, Direction.DOWN}) {
            var supplier=ResourceTestEnvironment.place(h,CENTER.relative(direction),direction.getOpposite(),false);
            verticalAccess.pullAdjacent();
            h.assertTrue(verticalAccess.fluidHandler(direction)==null && ((io.github.gaiusred.sourcepots.compat.mekanism.MekanismSourceStorage)verticalAccess.gasStorage()).handler(direction)==null,"Vertical handler absent");
            h.assertTrue(supplier.fluid.getFluidAmount()==1000 && supplier.gas.getStored()==1000,"Vertical neighbors remain untouched");
            for (var side : Direction.Plane.HORIZONTAL) h.assertTrue(verticalAccess.storedLiquid(side)==0 && verticalAccess.storedGas(side)==0,"Vertical suppliers cannot fill empty horizontal buffers");
            ResourceTestEnvironment.remove(h,CENTER.relative(direction));
        }
        h.assertTrue(a.fluidHandler(null)==null,"Unsided handler absent");
        h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void fundedFacesBuyEachAggregateTierAndPreservePartialFaces(GameTestHelper h) {
        for (SourceMode mode : new SourceMode[]{SourceMode.LIQUID, SourceMode.GAS}) {
            for (int count=1;count<=4;count++) {
                var a=access(h);
                int mask=0,index=0;
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    boolean funded=index++<count;
                    long amount=mode==SourceMode.LIQUID ? (funded?500:40) : (funded?250:40);
                    if(mode==SourceMode.LIQUID) a.fluidHandler(side).fill(fluid((int)amount),FluidAction.EXECUTE);
                    else ((io.github.gaiusred.sourcepots.compat.mekanism.MekanismSourceStorage)a.gasStorage()).handler(side).insertChemical(gas(amount),Action.EXECUTE);
                    if(funded) mask|=1<<side.get2DDataValue();
                }
                h.assertTrue(a.fundedSideMask(mode)==mask,"Partial faces cannot contribute to a tier");
                h.assertTrue(!a.pay(mode,0) && !a.pay(mode,16),"Zero and invalid masks cannot spend");
                if(count<4) h.assertTrue(!a.pay(mode,15),"Failed batch cannot drain any funded face");
                var boost=new SourceScheduler().tick(new SourceState(),SETTINGS,true,a);
                h.assertTrue(boost!=null && boost.mode()==mode && boost.sideMask()==mask && boost.tier().equals(SETTINGS.tier(mode,count)),"Exact funded count selects aggregate tier");
                for(Direction side:Direction.Plane.HORIZONTAL) {
                    long amount=mode==SourceMode.LIQUID?a.storedLiquid(side):a.storedGas(side);
                    h.assertTrue(amount==((mask&(1<<side.get2DDataValue()))!=0?0:40),"One cost per funded face; partial quantities retained");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void independentCapacityStoresSeveralPayments(GameTestHelper h) {
        var settings=new SourceSettings(2500,80,100,10,3,
                new SourceSettings.ModeSettings(500,1500,SETTINGS.liquid().tiers()),
                new SourceSettings.ModeSettings(250,750,SETTINGS.gas().tiers()),SETTINGS.jarGrowth(),16);
        var a=new PotSourceAccess(h.getLevel(),h.absolutePos(CENTER),settings,()->true);
        for(Direction side:Direction.Plane.HORIZONTAL) {
            h.assertTrue(a.fluidHandler(side).fill(fluid(2000),FluidAction.EXECUTE)==1500,"Capacity bounds liquid intake");
            h.assertTrue(((io.github.gaiusred.sourcepots.compat.mekanism.MekanismSourceStorage)a.gasStorage()).handler(side).insertChemical(gas(1000),Action.EXECUTE).getAmount()==250,"Capacity bounds gas intake");
        }
        for(int payment=0;payment<3;payment++) h.assertTrue(a.pay(SourceMode.LIQUID,15)&&a.pay(SourceMode.GAS,15),"Three exact batches stored independently");
        h.assertTrue(!a.pay(SourceMode.LIQUID,15)&&!a.pay(SourceMode.GAS,15),"Fourth payment unavailable");
        h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void diagonalFluidAndGasIgnored(GameTestHelper h) {
        var control=ResourceTestEnvironment.place(h,CENTER.north(),Direction.SOUTH,false);
        var a=access(h); a.pullAdjacent();
        h.assertTrue((a.fundedSideMask(SourceMode.GAS) != 0),"Touching control must pull");
        ResourceTestEnvironment.remove(h,CENTER.north()); a.pay(SourceMode.GAS, 1 << Direction.NORTH.get2DDataValue()); a.pay(SourceMode.LIQUID, 1 << Direction.NORTH.get2DDataValue());
        var diagonal=ResourceTestEnvironment.place(h,CENTER.offset(1,1,1),null,false);
        a.pullAdjacent();
        h.assertTrue(storedFluid(a)==0 && storedGas(a)==0 && diagonal.fluid.getFluidAmount()==1000 && diagonal.gas.getStored()==1000,"Diagonal suppliers must remain untouched"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void deniedSideIsNotPulled(GameTestHelper h) {
        var supplier=ResourceTestEnvironment.place(h,CENTER.north(),Direction.NORTH,false);
        var a=access(h); a.pullAdjacent();
        h.assertTrue(storedFluid(a)==0 && storedGas(a)==0 && supplier.fluid.getFluidAmount()==1000 && supplier.gas.getStored()==1000,"Capability on wrong face cannot be pulled");
        supplier.allowed=Direction.SOUTH; h.getLevel().invalidateCapabilities(h.absolutePos(CENTER.north())); a.pullAdjacent();
        h.assertTrue(storedFluid(a)==500 && storedGas(a)==250,"Allowed opposite face must pull"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void severalNeighborsFillOnlyTheirOwnFaces(GameTestHelper h) {
        var first=ResourceTestEnvironment.place(h,CENTER.north(),Direction.SOUTH,false);
        var second=ResourceTestEnvironment.place(h,CENTER.south(),Direction.NORTH,false);
        var a=access(h); a.pullAdjacent(); a.pullAdjacent();
        h.assertTrue(a.storedLiquid(Direction.NORTH)==500 && a.storedLiquid(Direction.SOUTH)==500 && a.storedGas(Direction.NORTH)==250 && a.storedGas(Direction.SOUTH)==250,"Independent face capacities cannot overfill");
        h.assertTrue(first.fluid.getFluidAmount()==500 && second.fluid.getFluidAmount()==500 && first.gas.getStored()==750 && second.gas.getStored()==750,"Each matching neighbor supplies its face exactly once");
        h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void shortExecuteTransferDoesNotCreateFreeBoost(GameTestHelper h) {
        var supplier=ResourceTestEnvironment.place(h,CENTER.north(),Direction.SOUTH,true);
        var a=access(h); a.pullAdjacent();
        h.assertTrue(storedFluid(a)==40 && storedGas(a)==40,"Only actual executed amounts enter buffers");
        h.assertTrue(supplier.fluid.getFluidAmount()==960 && supplier.gas.getStored()==960,"Short transfer removes exactly actual amount");
        h.assertTrue(!a.pay(SourceMode.GAS, 1 << Direction.NORTH.get2DDataValue()) && !a.pay(SourceMode.LIQUID, 1 << Direction.NORTH.get2DDataValue()),"Simulated full operation cannot fund boost"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void jarAtThreeBlockDiagonalWorks(GameTestHelper h) {
        var jar=jar(h,CENTER.offset(3,3,3),2500,false); var a=access(h);
        h.assertTrue(a.canPayJar() && jar.getSource()==2500,"Simulation sees range-three diagonal without spending");
        h.assertTrue(a.payJar() && jar.getSource()==0,"Range-three diagonal pays 2500"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void jarAtFourBlocksDoesNot(GameTestHelper h) {
        var jar=jar(h,CENTER.offset(4,0,0),2500,false); var a=access(h);
        h.assertTrue(!a.canPayJar() && !a.payJar() && jar.getSource()==2500,"Offset four must remain outside configured range");
        h.setBlock(CENTER.offset(4,0,0),net.minecraft.world.level.block.Blocks.AIR);
        jar(h,CENTER.offset(3,0,0),2500,false);
        h.assertTrue(a.payJar(),"Offset three control must pay"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void jarsCombineTo2500(GameTestHelper h) {
        var one=jar(h,CENTER.offset(2,0,0),900,false); var two=jar(h,CENTER.offset(-2,0,0),1600,false); var a=access(h);
        h.assertTrue(a.canPayJar() && one.getSource()+two.getSource()==2500,"Simulation combines without spending");
        h.assertTrue(a.payJar() && one.getSource()==0 && two.getSource()==0,"Payment combines both providers"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void insufficientJarsDoNotFundBoost(GameTestHelper h) {
        var one=jar(h,CENTER.offset(2,0,0),900,false); var two=jar(h,CENTER.offset(-2,0,0),1599,false); var a=access(h);
        h.assertTrue(!a.canPayJar() && !a.payJar() && one.getSource()==900 && two.getSource()==1599,"Insufficient combined jars preserve every unit");
        two.setSource(1600); h.assertTrue(a.payJar(),"Exactly 2500 control must pay"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void creativeJarWorks(GameTestHelper h) {
        var normal=jar(h,CENTER.below(),1000,false); var creative=jar(h,CENTER.above(),0,true); var a=access(h);
        int before=creative.getSource();
        h.assertTrue(a.canPayJar() && a.payJar() && a.payJar(),"Creative source supports repeated operations");
        h.assertTrue(normal.getSource()==1000 && creative.getSource()==before,"Creative semantics preserve ordinary and creative storage"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void pullableGasPreventsJarPurchase(GameTestHelper h) {
        var jar=jar(h,CENTER.offset(2,0,0),2500,false);
        ResourceTestEnvironment.place(h,CENTER.north(),Direction.SOUTH,false);
        var a=access(h); a.pullAdjacent();
        h.assertTrue(new SourceScheduler().tick(new SourceState(),SETTINGS,true,a).mode() == SourceMode.GAS,"Pulled gas must fund boost");
        h.assertTrue(storedGas(a)==0 && storedFluid(a)==500 && jar.getSource()==2500,"Faster gas payment prevents jar spending"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void pullingDoesNotLoadNeighborChunks(GameTestHelper h) {
        var position=new BlockPos(24000000,64,24000000);
        for(Direction side:Direction.Plane.HORIZONTAL) h.assertTrue(!h.getLevel().isLoaded(position.relative(side)),"Fixture starts with unloaded neighbors");
        var access=new PotSourceAccess(h.getLevel(),position,SETTINGS,()->true);
        access.pullAdjacent();
        for(Direction side:Direction.Plane.HORIZONTAL) h.assertTrue(!h.getLevel().isLoaded(position.relative(side)) && access.storedLiquid(side)==0 && access.storedGas(side)==0,"Pulling skips unloaded neighbors without loading or inventing resources");
        h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void fundedBatchCannotSpendAfterGrowthBecomesIneligible(GameTestHelper h) {
        boolean[] eligible={true};
        var a=new PotSourceAccess(h.getLevel(),h.absolutePos(CENTER),SETTINGS,()->eligible[0]);
        fill(h,a);
        var jar=jar(h,CENTER.offset(2,0,0),2500,false);
        eligible[0]=false;
        int north=1<<Direction.NORTH.get2DDataValue();
        h.assertTrue(!a.pay(SourceMode.GAS,north) && !a.pay(SourceMode.LIQUID,north) && !a.payJar(),"Ineligible growth cannot spend funded buffers or Jar Source");
        h.assertTrue(a.storedGas(Direction.NORTH)==250 && a.storedLiquid(Direction.NORTH)==500 && jar.getSource()==2500,"Rejected payment preserves all quantities");
        h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void fourLargeGasPaymentsDoNotOverflow(GameTestHelper h) {
        var settings=new SourceSettings(2500,80,100,10,3,SETTINGS.liquid(),
                new SourceSettings.ModeSettings(Long.MAX_VALUE-1,Long.MAX_VALUE,SETTINGS.gas().tiers()),SETTINGS.jarGrowth(),16);
        var a=new PotSourceAccess(h.getLevel(),h.absolutePos(CENTER),settings,()->true);
        for(Direction side:Direction.Plane.HORIZONTAL) {
            var handler=((io.github.gaiusred.sourcepots.compat.mekanism.MekanismSourceStorage)a.gasStorage()).handler(side);
            h.assertTrue(handler.insertChemical(gas(Long.MAX_VALUE),Action.EXECUTE).isEmpty(),"Native gas storage accepts exact positive long capacity");
        }
        var boost=new SourceScheduler().tick(new SourceState(),settings,true,a);
        h.assertTrue(boost!=null && boost.sideMask()==15 && boost.tier().growth().maxTicks()==2400,"Four large funded faces select one aggregate tier without summing costs");
        for(Direction side:Direction.Plane.HORIZONTAL) h.assertTrue(a.storedGas(side)==1,"Each face subtracts exact long cost without overflow");
        h.succeed();
    }

    private static void assertGated(GameTestHelper h,PotSourceAccess a) {
        for(var action:FluidAction.values()) h.assertTrue(a.fluidHandler(Direction.NORTH).fill(fluid(500),action)==0,"Ineligible liquid intake denied");
        for(var action:Action.values()) h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(gas(250),action).getAmount()==250,"Ineligible gas intake denied");
        var supplier=ResourceTestEnvironment.place(h,CENTER.north(),Direction.SOUTH,false); a.pullAdjacent();
        h.assertTrue(supplier.fluid.getFluidAmount()==1000 && supplier.gas.getStored()==1000 && storedFluid(a)==0 && storedGas(a)==0,"Ineligible growth must not drain suppliers");
    }

    static FluidStack fluid(int amount) { return new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse("sauce:source_fluid")),amount); }
    static ChemicalStack gas(long amount) { return new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.parse("starbunclemania:source_gas")).orElseThrow(),amount); }
    private static PotSourceAccess access(GameTestHelper h) { return new PotSourceAccess(h.getLevel(),h.absolutePos(CENTER),SETTINGS,()->true); }
    private static int storedFluid(PotSourceAccess a) { return a.fluidHandler(Direction.NORTH).getFluidInTank(0).getAmount(); }
    private static long storedGas(PotSourceAccess a) { return MekanismTestAccess.chemical(a).getChemicalInTank(0).getAmount(); }
    private static void fill(GameTestHelper h,PotSourceAccess a) {
        h.assertTrue(a.fluidHandler(Direction.NORTH).fill(fluid(500),FluidAction.EXECUTE)==500,"Accept one liquid operation");
        h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(gas(250),Action.EXECUTE).isEmpty(),"Accept one gas operation");
        h.assertTrue((a.fundedSideMask(SourceMode.LIQUID) != 0) && (a.fundedSideMask(SourceMode.GAS) != 0),"Both full buffers can independently pay");
    }
    private static SourceJarTile jar(GameTestHelper h,BlockPos pos,int amount,boolean creative) {
        h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ars_nouveau:"+(creative?"creative_source_jar":"source_jar"))));
        var jar=(SourceJarTile)h.getBlockEntity(pos); if(!creative) jar.setSource(amount); return jar;
    }
}
