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
        h.assertTrue(access.fluidHandler().getTankCapacity(0) == 500 && MekanismTestAccess.chemical(access).getChemicalTankCapacity(0) == 250, "Independent capacities must be 500 and 250");
        h.assertTrue(access.fluidHandler().fill(fluid(1000), FluidAction.SIMULATE) == 500, "Simulated fluid acceptance is bounded");
        h.assertTrue(MekanismTestAccess.chemical(access).insertChemical(gas(400), Action.SIMULATE).getAmount() == 150, "Simulated chemical insertion returns remainder");
        h.assertTrue(changes[0] == 0 && storedFluid(access) == 0 && storedGas(access) == 0, "Simulation must not mutate or notify");
        fill(h, access);
        h.assertTrue(changes[0] == 2, "Each actual fill notifies once");
        var fluidCopy = access.fluidHandler().getFluidInTank(0);
        var gasCopy = MekanismTestAccess.chemical(access).getChemicalInTank(0);
        fluidCopy.setAmount(1); gasCopy.setAmount(1);
        h.assertTrue(storedFluid(access) == 500 && storedGas(access) == 250, "Stack views cannot mutate storage");
        h.assertTrue(access.pay(SourceMode.GAS) && storedFluid(access) == 500 && storedGas(access) == 0, "Gas payment leaves liquid untouched");
        h.assertTrue(access.pay(SourceMode.LIQUID) && storedFluid(access) == 0, "Liquid payment consumes exactly one operation");
        h.assertTrue(!access.pay(SourceMode.GAS) && !access.pay(SourceMode.LIQUID) && changes[0] == 4, "Empty buffers cannot pay or notify");
        h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void rejectsWrongResources(GameTestHelper h) {
        var a = access(h); fill(h, a); a.pay(SourceMode.GAS); a.pay(SourceMode.LIQUID);
        h.assertTrue(a.fluidHandler().fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE) == 0, "Water is not Source");
        var wrong = new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.parse("mekanism:oxygen")).orElseThrow(), 250);
        h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(wrong, Action.EXECUTE).getAmount() == 250, "Oxygen must remain unaccepted");
        h.assertTrue(storedFluid(a) == 0 && storedGas(a) == 0, "Wrong resources cannot enter storage"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void partialFillCannotPay(GameTestHelper h) {
        var a = access(h);
        h.assertTrue(a.fluidHandler().fill(fluid(499), FluidAction.EXECUTE) == 499, "Partial fluid accepted");
        h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(gas(199), Action.EXECUTE).isEmpty(), "Partial gas accepted");
        h.assertTrue(!a.canPay(SourceMode.GAS) && !a.canPay(SourceMode.LIQUID) && !a.pay(SourceMode.GAS) && !a.pay(SourceMode.LIQUID), "Partial operation cannot pay");
        h.assertTrue(storedFluid(a) == 499 && storedGas(a) == 199, "Failed payment preserves partial contents"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void fullBufferRejectsMore(GameTestHelper h) {
        var a = access(h); fill(h,a);
        for (var action : FluidAction.values()) h.assertTrue(a.fluidHandler().fill(fluid(1), action) == 0, "Full liquid buffer rejects more");
        for (var action : Action.values()) h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(gas(1), action).getAmount() == 1, "Full chemical buffer returns all");
        h.assertTrue(storedFluid(a) == 500 && storedGas(a) == 250, "Buffers cannot overfill"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void invalidGrowthRejectsIntake(GameTestHelper h) {
        var pot = BonemealGrowthGameTests.pot(h);
        var a = new PotSourceAccess(h.getLevel(), pot.getBlockPos(), SETTINGS, () -> new BonemealGrowth().isEligible(pot,h.getLevel()));
        fill(h,a); a.pay(SourceMode.GAS); a.pay(SourceMode.LIQUID);
        pot.setSeed(net.minecraft.world.item.ItemStack.EMPTY);
        assertGated(h,a); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void matureCropRejectsIntake(GameTestHelper h) {
        var pot = BonemealGrowthGameTests.pot(h);
        var a = new PotSourceAccess(h.getLevel(), pot.getBlockPos(), SETTINGS, () -> new BonemealGrowth().isEligible(pot,h.getLevel()));
        fill(h,a); a.pay(SourceMode.GAS); a.pay(SourceMode.LIQUID);
        pot.updateGrowthTime(pot.getRequiredGrowthTicks());
        assertGated(h,a); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void automationCannotExtract(GameTestHelper h) {
        var a=access(h); fill(h,a);
        for(var action:FluidAction.values()) {
            h.assertTrue(a.fluidHandler().drain(500,action).isEmpty(), "Amount drain denied");
            h.assertTrue(a.fluidHandler().drain(fluid(500),action).isEmpty(), "Stack drain denied");
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
    public static void sixFacesAcceptInput(GameTestHelper h) {
        for(var direction:Direction.values()) {
            var supplier=ResourceTestEnvironment.place(h,CENTER.relative(direction),direction.getOpposite(),false);
            var a=access(h); a.pullAdjacent();
            h.assertTrue(storedFluid(a)==500 && storedGas(a)==250,"Touching "+direction+" must expose independent resources on opposite face");
            h.assertTrue(supplier.fluid.getFluidAmount()==500 && supplier.gas.getStored()==750,"Exactly one operation pulled from "+direction);
            ResourceTestEnvironment.remove(h,CENTER.relative(direction));
        } h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void diagonalFluidAndGasIgnored(GameTestHelper h) {
        var control=ResourceTestEnvironment.place(h,CENTER.below(),Direction.UP,false);
        var a=access(h); a.pullAdjacent();
        h.assertTrue(a.canPay(SourceMode.GAS),"Touching control must pull");
        ResourceTestEnvironment.remove(h,CENTER.below()); a.pay(SourceMode.GAS); a.pay(SourceMode.LIQUID);
        var diagonal=ResourceTestEnvironment.place(h,CENTER.offset(1,1,1),null,false);
        a.pullAdjacent();
        h.assertTrue(storedFluid(a)==0 && storedGas(a)==0 && diagonal.fluid.getFluidAmount()==1000 && diagonal.gas.getStored()==1000,"Diagonal suppliers must remain untouched"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void deniedSideIsNotPulled(GameTestHelper h) {
        var supplier=ResourceTestEnvironment.place(h,CENTER.below(),Direction.DOWN,false);
        var a=access(h); a.pullAdjacent();
        h.assertTrue(storedFluid(a)==0 && storedGas(a)==0 && supplier.fluid.getFluidAmount()==1000 && supplier.gas.getStored()==1000,"Capability on wrong face cannot be pulled");
        supplier.allowed=Direction.UP; h.getLevel().invalidateCapabilities(h.absolutePos(CENTER.below())); a.pullAdjacent();
        h.assertTrue(storedFluid(a)==500 && storedGas(a)==250,"Allowed opposite face must pull"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void multipleNeighborsFillOnlyOneOperation(GameTestHelper h) {
        var first=ResourceTestEnvironment.place(h,CENTER.below(),Direction.UP,false);
        var second=ResourceTestEnvironment.place(h,CENTER.above(),Direction.DOWN,false);
        var a=access(h); a.pullAdjacent(); a.pullAdjacent();
        h.assertTrue(storedFluid(a)==500 && storedGas(a)==250,"Neighbors cannot overfill");
        h.assertTrue(first.fluid.getFluidAmount()==500 && first.gas.getStored()==750,"Stable Direction order starts DOWN");
        h.assertTrue(second.fluid.getFluidAmount()==1000 && second.gas.getStored()==1000,"Full buffers must not drain next neighbor"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void shortExecuteTransferDoesNotCreateFreeBoost(GameTestHelper h) {
        var supplier=ResourceTestEnvironment.place(h,CENTER.below(),Direction.UP,true);
        var a=access(h); a.pullAdjacent();
        h.assertTrue(storedFluid(a)==40 && storedGas(a)==40,"Only actual executed amounts enter buffers");
        h.assertTrue(supplier.fluid.getFluidAmount()==960 && supplier.gas.getStored()==960,"Short transfer removes exactly actual amount");
        h.assertTrue(!a.pay(SourceMode.GAS) && !a.pay(SourceMode.LIQUID),"Simulated full operation cannot fund boost"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void jarAtThreeBlockDiagonalWorks(GameTestHelper h) {
        var jar=jar(h,CENTER.offset(3,3,3),2500,false); var a=access(h);
        h.assertTrue(a.canPay(SourceMode.JAR) && jar.getSource()==2500,"Simulation sees range-three diagonal without spending");
        h.assertTrue(a.pay(SourceMode.JAR) && jar.getSource()==0,"Range-three diagonal pays 2500"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void jarAtFourBlocksDoesNot(GameTestHelper h) {
        var jar=jar(h,CENTER.offset(4,0,0),2500,false); var a=access(h);
        h.assertTrue(!a.canPay(SourceMode.JAR) && !a.pay(SourceMode.JAR) && jar.getSource()==2500,"Offset four must remain outside configured range");
        h.setBlock(CENTER.offset(4,0,0),net.minecraft.world.level.block.Blocks.AIR);
        jar(h,CENTER.offset(3,0,0),2500,false);
        h.assertTrue(a.pay(SourceMode.JAR),"Offset three control must pay"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void jarsCombineTo2500(GameTestHelper h) {
        var one=jar(h,CENTER.offset(2,0,0),900,false); var two=jar(h,CENTER.offset(-2,0,0),1600,false); var a=access(h);
        h.assertTrue(a.canPay(SourceMode.JAR) && one.getSource()+two.getSource()==2500,"Simulation combines without spending");
        h.assertTrue(a.pay(SourceMode.JAR) && one.getSource()==0 && two.getSource()==0,"Payment combines both providers"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void insufficientJarsDoNotFundBoost(GameTestHelper h) {
        var one=jar(h,CENTER.offset(2,0,0),900,false); var two=jar(h,CENTER.offset(-2,0,0),1599,false); var a=access(h);
        h.assertTrue(!a.canPay(SourceMode.JAR) && !a.pay(SourceMode.JAR) && one.getSource()==900 && two.getSource()==1599,"Insufficient combined jars preserve every unit");
        two.setSource(1600); h.assertTrue(a.pay(SourceMode.JAR),"Exactly 2500 control must pay"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void creativeJarWorks(GameTestHelper h) {
        var normal=jar(h,CENTER.below(),1000,false); var creative=jar(h,CENTER.above(),0,true); var a=access(h);
        int before=creative.getSource();
        h.assertTrue(a.canPay(SourceMode.JAR) && a.pay(SourceMode.JAR) && a.pay(SourceMode.JAR),"Creative source supports repeated operations");
        h.assertTrue(normal.getSource()==1000 && creative.getSource()==before,"Creative semantics preserve ordinary and creative storage"); h.succeed();
    }

    @GameTest(template="empty", batch="resources")
    public static void pullableGasPreventsJarPurchase(GameTestHelper h) {
        var jar=jar(h,CENTER.offset(2,0,0),2500,false);
        ResourceTestEnvironment.place(h,CENTER.below(),Direction.UP,false);
        var a=access(h); a.pullAdjacent();
        h.assertTrue(new SourceScheduler().tick(new SourceState(),SETTINGS,true,a) == SourceMode.GAS,"Pulled gas must fund boost");
        h.assertTrue(storedGas(a)==0 && storedFluid(a)==500 && jar.getSource()==2500,"Faster gas payment prevents jar spending"); h.succeed();
    }

    private static void assertGated(GameTestHelper h,PotSourceAccess a) {
        for(var action:FluidAction.values()) h.assertTrue(a.fluidHandler().fill(fluid(500),action)==0,"Ineligible liquid intake denied");
        for(var action:Action.values()) h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(gas(250),action).getAmount()==250,"Ineligible gas intake denied");
        var supplier=ResourceTestEnvironment.place(h,CENTER.below(),Direction.UP,false); a.pullAdjacent();
        h.assertTrue(supplier.fluid.getFluidAmount()==1000 && supplier.gas.getStored()==1000 && storedFluid(a)==0 && storedGas(a)==0,"Ineligible growth must not drain suppliers");
    }

    static FluidStack fluid(int amount) { return new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse("sauce:source_fluid")),amount); }
    static ChemicalStack gas(long amount) { return new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.parse("starbunclemania:source_gas")).orElseThrow(),amount); }
    private static PotSourceAccess access(GameTestHelper h) { return new PotSourceAccess(h.getLevel(),h.absolutePos(CENTER),SETTINGS,()->true); }
    private static int storedFluid(PotSourceAccess a) { return a.fluidHandler().getFluidInTank(0).getAmount(); }
    private static long storedGas(PotSourceAccess a) { return MekanismTestAccess.chemical(a).getChemicalInTank(0).getAmount(); }
    private static void fill(GameTestHelper h,PotSourceAccess a) {
        h.assertTrue(a.fluidHandler().fill(fluid(500),FluidAction.EXECUTE)==500,"Accept one liquid operation");
        h.assertTrue(MekanismTestAccess.chemical(a).insertChemical(gas(250),Action.EXECUTE).isEmpty(),"Accept one gas operation");
        h.assertTrue(a.canPay(SourceMode.LIQUID) && a.canPay(SourceMode.GAS),"Both full buffers can independently pay");
    }
    private static SourceJarTile jar(GameTestHelper h,BlockPos pos,int amount,boolean creative) {
        h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ars_nouveau:"+(creative?"creative_source_jar":"source_jar"))));
        var jar=(SourceJarTile)h.getBlockEntity(pos); if(!creative) jar.setSource(amount); return jar;
    }
}
