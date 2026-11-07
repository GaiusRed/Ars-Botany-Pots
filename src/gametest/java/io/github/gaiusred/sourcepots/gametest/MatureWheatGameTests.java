package io.github.gaiusred.sourcepots.gametest;

import java.util.List;
import mekanism.api.Action;
import net.darkhax.botanypots.common.impl.Helpers;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.darkhax.botanypots.common.impl.data.itemdrops.BlockStateDrops;
import net.darkhax.botanypots.common.impl.data.recipe.crop.BlockDerivedCrop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class MatureWheatGameTests {
    private static final ResourceLocation WHEAT_LOOT=ResourceLocation.parse("minecraft:blocks/wheat");

    @GameTest(template="empty", batch="mature-wheat")
    public static void matureArsAndHopperMatchLoadedWheatLootAndBasicManualHarvest(GameTestHelper h) {
        var ars=ArsPotGameTests.pot(h,new BlockPos(2,2,5));
        var hopper=ArsPotGameTests.upstream(h,new BlockPos(8,2,5),"terracotta_hopper_botany_pot");
        var basic=ArsPotGameTests.upstream(h,new BlockPos(5,2,8),"terracotta_botany_pot");
        var crop=ars.getOrInvalidateCrop();
        h.assertTrue(crop instanceof BlockDerivedCrop,"Loaded wheat recipe must derive its mature block drops");
        var drops=((BlockDerivedCrop)crop).getProperties().toBasic().drops();
        h.assertTrue(drops.size()==1 && drops.getFirst() instanceof BlockStateDrops,"Loaded wheat must have one block-state drop provider");
        var provider=(BlockStateDrops)drops.getFirst();
        h.assertTrue(provider.getHarvestState().is(Blocks.WHEAT)
                && provider.getHarvestState().getValue(CropBlock.AGE)==7
                && provider.getTableId().equals(WHEAT_LOOT) && provider.hasCachedTable(),
                "Loaded recipe must use age-seven wheat and its actual loaded loot table, without fallback");
        var table=h.getLevel().getServer().reloadableRegistries().getLootTable(Blocks.WHEAT.getLootTable());
        h.assertTrue(table!=LootTable.EMPTY,"Actual mature-wheat loot table must be loaded");
        for(var pot:List.of(ars,hopper,basic)) {
            h.assertTrue(pot.getHarvestItem().isEmpty() && pot.getSoilItem().is(Items.DIRT),"Parity uses dirt soil and no harvest tool");
            h.assertTrue(Helpers.getTotalYield(pot.getRecipeContext(),h.getLevel(),pot.getOrInvalidateCrop(),pot.getOrInvalidateSoil())==1,
                    "Default hopper context must roll loot exactly once; basic manual harvesting calls onHarvest once");
        }
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(int seed=0;seed<16;seed++) {
            for(var pot:List.of(ars,hopper,basic)) {
                for(int slot:BotanyPotBlockEntity.STORAGE_SLOTS) pot.setItem(slot,ItemStack.EMPTY);
                // Let upstream's five-tick post-harvest pause expire before the next independent mature cycle.
                for(int tick=0;tick<5;tick++) registeredTick(h,pot);
                pot.updateGrowthTime(pot.getRequiredGrowthTicks());
            }
            resetRandom(h,seed);
            var reference=count(table.getRandomItems(ars.getRecipeContext().createLootParams(provider.getHarvestState())));
            h.assertTrue(reference.wheat()==1 && reference.seeds()>=1 && reference.seeds()<=4,
                    "Age-seven wheat with no Fortune must drop one wheat and one to four seeds");
            resetRandom(h,seed); registeredTick(h,ars);
            resetRandom(h,seed); registeredTick(h,hopper);
            var area=new AABB(basic.getBlockPos()).inflate(1);
            h.getLevel().getEntitiesOfClass(ItemEntity.class,area).forEach(ItemEntity::discard);
            resetRandom(h,seed);
            basic.getBlockState().useItemOn(ItemStack.EMPTY,h.getLevel(),player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(basic.getBlockPos()),Direction.UP,basic.getBlockPos(),false));
            var manual=count(h.getLevel().getEntitiesOfClass(ItemEntity.class,area).stream().map(ItemEntity::getItem).toList());
            h.assertTrue(outputs(ars).equals(reference) && outputs(hopper).equals(reference) && manual.equals(reference),
                    "Matched mature wheat harvests must equal actual loaded loot at seed "+seed+": expected "+reference
                            +", Ars "+outputs(ars)+", hopper "+outputs(hopper)+", basic manual "+manual);
            for(var pot:List.of(ars,hopper,basic)) {
                h.assertTrue(pot.growthTime()==-1 && pot.getSeedItem().getCount()==1,"Each harvest resets upstream growth to minus one and retains the planted seed");
            }
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="mature-wheat")
    public static void sourceHarvestsImmediatelyAndSpendsRemainderOnNextCycle(GameTestHelper h) {
        try(var ignored=new GrowthConfigFixture(600,600)) {
            var ars=ArsPotGameTests.pot(h);
            ars.updateGrowthTime(ars.getRequiredGrowthTicks()-22);
            h.assertTrue(MekanismTestAccess.chemical(ars.sourceAccess()).insertChemical(SourceAccessGameTests.gas(250),Action.EXECUTE).isEmpty(),"Near-mature crop accepts one operation");
            resetRandom(h,42); registeredTick(h,ars);
            var harvested=outputs(ars);
            h.assertTrue(harvested.wheat()==1 && harvested.seeds()>=1 && harvested.seeds()<=4,"Source mature harvest retains actual wheat loot");
            h.assertTrue(ars.growthTime()==578,"Remaining 579 paid ticks advance the next cycle immediately from upstream minus-one reset");
            registeredTick(h,ars);
            h.assertTrue(outputs(ars).equals(harvested) && ars.growthTime()==579,"Powered harvest resumes ordinary growth immediately without duplicate loot");
        }
        h.succeed();
    }

    private static void registeredTick(GameTestHelper h,BotanyPotBlockEntity pot) {
        if(pot instanceof io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity ars) {
            ArsPotGameTests.tick(h,ars);
        } else {
            var block=(net.darkhax.botanypots.common.impl.block.BotanyPotBlock)pot.getBlockState().getBlock();
            var ticker=block.getTicker(h.getLevel(),pot.getBlockState(),BotanyPotBlockEntity.TYPE.get());
            h.assertTrue(ticker!=null,"Upstream registered pot must have its normal ticker");
            ticker.tick(h.getLevel(),pot.getBlockPos(),pot.getBlockState(),pot);
        }
    }

    private static void resetRandom(GameTestHelper h,long seed) {
        h.getLevel().random.setSeed(seed);
        h.getLevel().getRandomSequence(WHEAT_LOOT).setSeed(seed);
    }

    private static Counts outputs(BotanyPotBlockEntity pot) {
        var stacks=new java.util.ArrayList<ItemStack>();
        for(int slot:BotanyPotBlockEntity.STORAGE_SLOTS) stacks.add(pot.getItem(slot));
        return count(stacks);
    }

    private static Counts count(List<ItemStack> items) {
        int wheat=0,seeds=0;
        for(var stack:items) {
            if(stack.is(Items.WHEAT)) wheat+=stack.getCount();
            else if(stack.is(Items.WHEAT_SEEDS)) seeds+=stack.getCount();
            else if(!stack.isEmpty()) throw new AssertionError("Unexpected mature-wheat drop: "+stack);
        }
        return new Counts(wheat,seeds);
    }

    private record Counts(int wheat,int seeds) {}
}
