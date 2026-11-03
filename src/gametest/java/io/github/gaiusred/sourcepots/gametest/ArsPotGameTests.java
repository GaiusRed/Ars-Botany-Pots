package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity;
import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import io.github.gaiusred.sourcepots.source.SourceMode;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.darkhax.botanypots.common.impl.block.menu.BotanyPotMenu;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class ArsPotGameTests {
    private static final BlockPos CENTER = new BlockPos(5, 2, 5);

    @GameTest(template="empty", batch="ars-pots")
    public static void unpoweredMatchesUpstreamHopper(GameTestHelper h) {
        var ars = pot(h);
        var upstream = upstream(h, new BlockPos(8,2,5), "terracotta_hopper_botany_pot");
        for (int i=0;i<100;i++) { tick(h,ars); BotanyPotBlockEntity.tickPot(h.getLevel(),upstream.getBlockPos(),upstream.getBlockState(),upstream); }
        h.assertTrue(ars.growthTime()==upstream.growthTime() && ars.comparatorLevel==upstream.comparatorLevel,"Unpowered growth must equal one upstream tick per tick");
        h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void poweredPotHarvestsAndExports(GameTestHelper h) {
        var ars=pot(h); h.setBlock(CENTER.below(),Blocks.CHEST);
        int before=ars.getRequiredGrowthTicks();
        for(int i=0;i<100;i++) { fill(ars); tick(h,ars); }
        var chest=(ChestBlockEntity)h.getBlockEntity(CENTER.below());
        h.assertTrue(before>100 && !chest.isEmpty(),"Source boosts must complete a harvest and retain upstream chest export"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void cropChangeDoesNotGrantPower(GameTestHelper h) {
        var ars=pot(h); fill(ars); tick(h,ars);
        var saved=power(ars);
        h.assertTrue(saved.getLong("boost_cooldown")==1,"Gas boost must establish shared cooldown");
        ars.setSeed(new ItemStack(Items.BEETROOT_SEEDS)); ars.setSoilItem(new ItemStack(Items.DIRT));
        var changed=power(ars);
        saved.remove("growth_credit"); saved.remove("source_assisted");
        h.assertTrue(changed.getDouble("growth_credit")==0 && !changed.getBoolean("source_assisted"),"Changing crop discards paid growth and assisted flag");
        changed.remove("growth_credit"); changed.remove("source_assisted");
        h.assertTrue(changed.equals(saved),"Changing crop and soil must preserve buffers and payment cooldowns"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void saveLoadKeepsBothBuffersAndTimers(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(100, 100)) {
            var ars=pot(h); var jar=jar(h); tick(h,ars); fill(ars);
            h.assertTrue(jar.getSource()==0 && power(ars).getLong("jar_remaining")==100 && power(ars).getLong("jar_retry")==80 && power(ars).getLong("boost_cooldown")==10,"Jar fixture must establish all three nonzero timers");
            var nbt=ars.saveWithoutMetadata(h.getLevel().registryAccess());
            var restored=new ArsBotanyPotBlockEntity(ars.getBlockPos(),ars.getBlockState());
            restored.loadAdditional(nbt,h.getLevel().registryAccess()); // Real chunk load has no level yet.
            var deferred=restored.saveWithoutMetadata(h.getLevel().registryAccess());
            h.assertTrue(deferred.getCompound("sourcepots").equals(nbt.getCompound("sourcepots")),"Save before setLevel must retain deferred power");
            restored.setLevel(h.getLevel());
            h.assertTrue(restored.sourceAccess().fluidHandler().getFluidInTank(0).getAmount()==500 && restored.sourceAccess().chemicalHandler().getChemicalInTank(0).getAmount()==250,"Both buffers must survive chunk load");
            h.assertTrue(power(restored).equals(power(ars)) && restored.growthTime()==ars.growthTime(),"Timers and upstream growth must survive load");
            restored.setSeed(ItemStack.EMPTY);
            var inactive=restored.saveWithoutMetadata(h.getLevel().registryAccess()); restored.loadAdditional(inactive,h.getLevel().registryAccess());
            h.assertTrue(restored.sourceAccess().chemicalHandler().getChemicalInTank(0).getAmount()==250,"Ineligible crops must not discard saved power"); h.succeed();

        }
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void smallerConfiguredCapacityClampsSave(GameTestHelper h) {
        var ars=pot(h); fill(ars);
        var saved=ars.saveWithoutMetadata(h.getLevel().registryAccess());
        ModConfigSpec.IntValue liquid=SourcePotsConfig.SPEC.getValues().get("liquid.costMb");
        ModConfigSpec.LongValue gas=SourcePotsConfig.SPEC.getValues().get("gas.costUnits");
        int oldLiquid=liquid.get(); long oldGas=gas.get();
        try {
            liquid.set(100); gas.set(50L);
            // These are world-restart settings; reproduce a fresh world's uncached snapshot.
            liquid.clearCache(); gas.clearCache();
            var restored=new ArsBotanyPotBlockEntity(ars.getBlockPos(),ars.getBlockState());
            restored.loadAdditional(saved,h.getLevel().registryAccess()); restored.setLevel(h.getLevel());
            h.assertTrue(restored.sourceAccess().fluidHandler().getFluidInTank(0).getAmount()==100 && restored.sourceAccess().chemicalHandler().getChemicalInTank(0).getAmount()==50,"Reduced capacities must clamp saved resources");
        } finally { liquid.set(oldLiquid); gas.set(oldGas); liquid.clearCache(); gas.clearCache(); }
        h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void malformedAndOversizedSavedPowerIsBounded(GameTestHelper h) {
        var ars=pot(h); var nbt=ars.saveWithoutMetadata(h.getLevel().registryAccess()); var state=new CompoundTag();
        state.putLong("liquid",Long.MAX_VALUE); state.putLong("gas",Long.MAX_VALUE);
        state.putLong("jar_remaining",Long.MAX_VALUE); state.putLong("jar_retry",Long.MAX_VALUE); state.putLong("boost_cooldown",Long.MAX_VALUE); nbt.put("sourcepots",state);
        ars.loadAdditional(nbt,h.getLevel().registryAccess());
        var bounded=power(ars);
        h.assertTrue(bounded.getLong("liquid")==500 && bounded.getLong("gas")==250,"Oversized resources must saturate to one operation");
        h.assertTrue(bounded.getLong("jar_remaining")==Integer.MAX_VALUE && bounded.getLong("jar_retry")==Integer.MAX_VALUE && bounded.getLong("boost_cooldown")==Integer.MAX_VALUE,"Long timers must saturate instead of wrapping");
        state.putLong("liquid",-2); state.putString("gas","invalid"); state.putLong("jar_remaining",-1); state.putString("jar_retry","invalid"); state.remove("boost_cooldown");
        ars.loadAdditional(nbt,h.getLevel().registryAccess()); bounded=power(ars);
        for(var key: new String[]{"liquid","gas","jar_remaining","jar_retry","boost_cooldown"}) h.assertTrue(bounded.getLong(key)==0,"Negative, malformed, and missing values default to zero: "+key);
        nbt.remove("sourcepots"); ars.loadAdditional(nbt,h.getLevel().registryAccess()); h.assertTrue(power(ars).getLong("gas")==0,"Old saves start empty"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void allVariantsHaveIndependentSidedCapabilities(GameTestHelper h) {
        h.assertTrue(io.github.gaiusred.sourcepots.registry.ModContent.POTS.size()==61,"Every pinned variant must register");
        for(var block:io.github.gaiusred.sourcepots.registry.ModContent.POTS.values()) {
            h.setBlock(CENTER,block.get()); var ars=(ArsBotanyPotBlockEntity)h.getBlockEntity(CENTER); initialize(ars);
            h.assertTrue(ars.getType()==io.github.gaiusred.sourcepots.registry.ModContent.POT_ENTITY.get(),"Shared type must accept every variant");
            for(var side:Direction.values()) {
                var fluid=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,ars.getBlockPos(),side);
                var gas=h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),ars.getBlockPos(),side);
                var items=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,ars.getBlockPos(),side);
                h.assertTrue(fluid!=null && gas!=null,"Both independent inputs must exist on "+side);
                h.assertTrue(fluid.getFluidInTank(0).isEmpty() && gas.getChemicalInTank(0).isEmpty(),"New variants cannot inherit another pot's resources");
                h.assertTrue((items!=null)==(side==Direction.DOWN),"Item automation must match upstream bottom-only extraction");
                if(items!=null) {
                    ars.setItem(14,new ItemStack(Items.WHEAT,3));
                    h.assertTrue(items.getSlots()==12 && items.extractItem(11,1,false).getCount()==1,"Bottom handler must expose final output slot");
                    h.assertTrue(items.insertItem(0,new ItemStack(Items.WHEAT),false).getCount()==1,"Item input must remain blocked");
                }
            }
            h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,ars.getBlockPos(),null)==null && h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),ars.getBlockPos(),null)==null,"Resources connect through faces only");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void liveNbtLoadKeepsCachedCapabilityViews(GameTestHelper h) {
        var ars=pot(h); fill(ars);
        var fluid=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,ars.getBlockPos(),Direction.UP);
        var gas=h.getLevel().getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),ars.getBlockPos(),Direction.UP);
        var saved=ars.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.getCompound("sourcepots").putLong("liquid",40); saved.getCompound("sourcepots").putLong("gas",30);
        ars.loadAdditional(saved,h.getLevel().registryAccess());
        h.assertTrue(fluid==ars.sourceAccess().fluidHandler() && gas==ars.sourceAccess().chemicalHandler(),"Loading live NBT must keep cached input capability identity");
        h.assertTrue(fluid.getFluidInTank(0).getAmount()==40 && gas.getChemicalInTank(0).getAmount()==30,"Cached views must observe restored buffers");
        fill(ars);
        h.assertTrue(fluid.getFluidInTank(0).getAmount()==500 && gas.getChemicalInTank(0).getAmount()==250,"Cached views and current pot must share subsequent intake"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void paidJarWindowSurvivesCropChange(GameTestHelper h) {
        var ars=pot(h); var jar=jar(h); tick(h,ars); var before=power(ars);
        h.assertTrue(jar.getSource()==0 && before.getLong("jar_remaining")==100,"One ordinary Jar payment must buy the powered window");
        ars.setSeed(new ItemStack(Items.BEETROOT_SEEDS)); ars.setSoilItem(new ItemStack(Items.DIRT));
        var changed=power(ars);
        h.assertTrue(changed.getDouble("growth_credit")==0 && !changed.getBoolean("source_assisted"),"Seed and soil reset discard growth credit");
        before.remove("growth_credit"); before.remove("source_assisted");
        changed.remove("growth_credit"); changed.remove("source_assisted");
        h.assertTrue(changed.equals(before),"Seed and soil changes cannot reset paid Jar power, buffers or timers");
        float growth=ars.growthTime(); tick(h,ars);
        h.assertTrue(ars.growthTime()==growth+1 && power(ars).getLong("jar_remaining")==99 && power(ars).getLong("boost_cooldown")==9,"Changed crop must wait for the existing shared cooldown"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-integrated-input")
    public static void integratedTickPullsThenBuysAtMostOneBoost(GameTestHelper h) {
        var ars=pot(h); var supplier=ResourceTestEnvironment.place(h,CENTER.below(),Direction.UP,false);
        try {
            tick(h,ars);
            h.assertTrue(ars.growthTime()>=401 && ars.growthTime()<=601,"One server tick must pull and apply exactly one configured Source increment after natural growth");
            h.assertTrue(supplier.fluid.getFluidAmount()==500 && supplier.gas.getStored()==750,"Pulling fills one operation of each independent resource");
            h.assertTrue(power(ars).getLong("liquid")==500 && power(ars).getLong("gas")==0 && power(ars).getLong("boost_cooldown")==1,"Faster gas wins once; liquid remains stored");
            ars.setSeed(ItemStack.EMPTY); tick(h,ars);
            h.assertTrue(supplier.fluid.getFluidAmount()==500 && supplier.gas.getStored()==750 && power(ars).getLong("liquid")==500,"Ineligible crops stop intake and spending while preserving saved liquid");
        } finally { ResourceTestEnvironment.remove(h,CENTER.below()); }
        h.succeed();
    }

    @GameTest(template="empty", batch="ars-recipe-reload", timeoutTicks=1200)
    public static void datapackReloadKeepsSharedCooldown(GameTestHelper h) {
        var placed=pot(h);
        try (var ignored = new GrowthConfigFixture(100, 100)) { fillLiquid(placed); tick(h,placed); fill(placed); }
        // An unattached entity keeps the test independent of the reload's elapsed server ticks.
        var detached=new ArsBotanyPotBlockEntity(placed.getBlockPos(),placed.getBlockState());
        detached.loadAdditional(placed.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess()); detached.setLevel(h.getLevel());
        GrowthReloadFixture.runWithReloadAssertion(h,new io.github.gaiusred.sourcepots.growth.BonemealGrowth(),detached,()->{
            long cooldown=power(detached).getLong("boost_cooldown");
            float growth=detached.growthTime(); tick(h,detached);
            h.assertTrue(cooldown>0 && power(detached).getLong("boost_cooldown")==cooldown-1 && power(detached).getLong("gas")==250 && detached.growthTime()==growth+1,"Real datapack reload cannot reset shared cooldown or purchase an extra boost");
        });
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void normalPotUnchanged(GameTestHelper h) {
        var normal=upstream(h,CENTER,"terracotta_botany_pot");
        h.setBlock(CENTER.above(2),BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ars_nouveau:source_jar")));
        var jar=(com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile)h.getBlockEntity(CENTER.above(2)); jar.setSource(2500);
        for(int i=0;i<50;i++) BotanyPotBlockEntity.tickPot(h.getLevel(),normal.getBlockPos(),normal.getBlockState(),normal);
        h.assertTrue(normal.growthTime()==50 && jar.getSource()==2500,"Ordinary pots must keep ordinary growth and never spend nearby Source");
        h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,normal.getBlockPos(),Direction.UP)==null,"Normal pots gain no fluid capability"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void hopperMenuUsesAll15Slots(GameTestHelper h) {
        var ars=pot(h); var player=h.makeMockPlayer(GameType.SURVIVAL);
        var menu=ars.createMenu(1,player.getInventory(),player);
        h.assertTrue(menu instanceof BotanyPotMenu && menu.getType()==BotanyPotMenu.HOPPER_MENU.get() && menu.slots.size()==51,"Inherited hopper menu must include 15 pot and 36 player slots");
        for(int i=0;i<15;i++) h.assertTrue(menu.getSlot(i).container==ars && menu.getSlot(i).getContainerSlot()==i,"All 15 slots must bind to the actual pot inventory");
        menu.getSlot(14).set(new ItemStack(Items.WHEAT,4));
        h.assertTrue(ars.getItem(14).getCount()==4,"Last output slot must be usable"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void comparatorTracksGrowth(GameTestHelper h) {
        var ars=pot(h); ars.updateGrowthTime(ars.getRequiredGrowthTicks()/2f); tick(h,ars);
        int signal=ars.getBlockState().getAnalogOutputSignal(h.getLevel(),ars.getBlockPos());
        h.assertTrue(signal==8,"Inherited comparator must report half-grown crop as level eight"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void toolAndOutputBehaviorMatchesUpstream(GameTestHelper h) {
        var ars=pot(h); var upstream=upstream(h,CENTER.offset(3,0,0),"terracotta_hopper_botany_pot");
        for(var pot:new BotanyPotBlockEntity[]{ars,upstream}) {
            pot.setHoe(new ItemStack(Items.IRON_HOE));
            for(int slot=3;slot<15;slot++)pot.setItem(slot,new ItemStack(Items.WHEAT,64));
            pot.updateGrowthTime(pot.getRequiredGrowthTicks());
        }
        tick(h,ars); BotanyPotBlockEntity.tickPot(h.getLevel(),upstream.getBlockPos(),upstream.getBlockState(),upstream);
        h.assertTrue(ars.getHarvestItem().getDamageValue()==upstream.getHarvestItem().getDamageValue() && ars.getHarvestItem().getDamageValue()>0,"Harvest tool wear must retain upstream behavior");
        for(int slot=0;slot<15;slot++)h.assertTrue(ItemStack.matches(ars.getItem(slot),upstream.getItem(slot)),"Full output and harvest inventory must match upstream at slot "+slot);
        h.assertTrue(ars.growthTime()==upstream.growthTime(),"Full-output harvest reset must match upstream"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void breakDropsInventoryButNotStoredPower(GameTestHelper h) {
        var ars=pot(h);
        try (var ignored = new GrowthConfigFixture(Integer.MAX_VALUE, Integer.MAX_VALUE)) { fill(ars); tick(h,ars); }
        h.assertTrue(power(ars).getDouble("growth_credit")>0,"Break fixture must contain earned paid overflow");
        ars.setHoe(new ItemStack(Items.IRON_HOE)); ars.setItem(14,new ItemStack(Items.WHEAT,7));
        h.setBlock(CENTER,Blocks.AIR);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(CENTER)).inflate(1));
        h.assertTrue(drops.stream().map(ItemEntity::getItem).filter(s->s.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum()==7,"Breaking must drop output inventory");
        h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(Items.DIRT)) && drops.stream().anyMatch(e->e.getItem().is(Items.WHEAT_SEEDS)) && drops.stream().anyMatch(e->e.getItem().is(Items.IRON_HOE)),"Breaking must also drop soil, seed, and tool");
        var fresh=pot(h); var state=power(fresh);
        h.assertTrue(state.getLong("liquid")==0 && state.getLong("gas")==0 && state.getLong("jar_remaining")==0 && state.getLong("boost_cooldown")==0 && state.getDouble("growth_credit")==0 && !state.getBoolean("source_assisted"),"New placement must start with empty power and no paid growth"); h.succeed();
    }

    @GameTest(template="empty", batch="ars-unload", timeoutTicks=160)
    public static void unloadedPotsDoNotCatchUpOrSpendSource(GameTestHelper h) {
        var ars=pot(h);
        try (var fixture = new GrowthConfigFixture(100, 100)) { fillLiquid(ars); tick(h,ars); fill(ars); }
        var saved=ars.saveWithoutMetadata(h.getLevel().registryAccess());
        h.setBlock(CENTER,Blocks.AIR); var before=power(ars);
        tick(h,ars);
        h.assertTrue(power(ars).equals(before),"Removed block entities cannot run resource scheduler");
        h.runAfterDelay(100,()->{
            var restored=new ArsBotanyPotBlockEntity(ars.getBlockPos(),ars.getBlockState()); restored.loadAdditional(saved,h.getLevel().registryAccess()); restored.setLevel(h.getLevel());
            h.assertTrue(power(restored).equals(before),"Unloaded time must not change buffers or timers");
            float growth=restored.growthTime(); tick(h,restored);
            h.assertTrue(restored.growthTime()==growth+1 && power(restored).getLong("boost_cooldown")==4 && power(restored).getLong("gas")==250,"Reloaded pot receives one ordinary tick and no catch-up or early payment"); h.succeed();
        });
    }

    @GameTest(template="empty", batch="ars-pots")
    public static void reloadDoesNotGrantImmediateExtraBoost(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(100, 100)) {
            var ars=pot(h);
            fillLiquid(ars); tick(h,ars); fill(ars);
            var saved=ars.saveWithoutMetadata(h.getLevel().registryAccess());
            var restored=new ArsBotanyPotBlockEntity(ars.getBlockPos(),ars.getBlockState()); restored.loadAdditional(saved,h.getLevel().registryAccess()); restored.setLevel(h.getLevel());
            float before=restored.growthTime(); tick(h,restored);
            h.assertTrue(restored.growthTime()==before+1 && power(restored).getLong("gas")==250 && power(restored).getLong("boost_cooldown")==4,"Chunk reload must retain shared cooldown and avoid immediate extra boost");
            for(int i=0;i<3;i++)tick(h,restored);
            h.assertTrue(power(restored).getLong("gas")==250,"Stored gas waits for the fifth tick");
            tick(h,restored); h.assertTrue(power(restored).getLong("gas")==0 && restored.growthTime()==before+105,"Fifth tick must purchase exactly one new boost");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="configured-growth")
    public static void mixedInputsUseAuthorizedModeAndKeepSharedCooldown(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(37, 37, 47, 47, 57, 57)) {
            var ars = pot(h);
            var sourceJar = jar(h);
            fillLiquid(ars);
            fill(ars);
            tick(h, ars);
            h.assertTrue(ars.growthTime() == 58 && power(ars).getLong("gas") == 0
                    && power(ars).getLong("liquid") == 500 && sourceJar.getSource() == 2500,
                    "Simultaneous inputs must pay gas and apply only gas growth");
            tick(h, ars);
            h.assertTrue(ars.growthTime() == 106 && power(ars).getLong("liquid") == 0
                    && sourceJar.getSource() == 2500, "Exhausted gas switches to liquid growth");
            tick(h, ars);
            h.assertTrue(ars.growthTime() == 107 && sourceJar.getSource() == 0,
                    "Jar can pay its window without authorizing growth during the shared cooldown");
            for (int i = 3; i <= 5; i++) tick(h, ars);
            h.assertTrue(ars.growthTime() == 110, "Switching modes cannot shorten the liquid cooldown");
            tick(h, ars);
            h.assertTrue(ars.growthTime() == 148 && power(ars).getLong("boost_cooldown") == 10,
                    "Paid Jar fallback must apply Jar growth when the existing cooldown expires");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="configured-growth")
    public static void invalidRangeInAnyModeCannotChargeFundedGas(GameTestHelper h) {
        var ars = pot(h);
        var gas = ars.sourceAccess().chemicalHandler();
        fill(ars);
        int[][] invalid = {{2,1,1,1,1,1}, {1,1,2,1,1,1}, {1,1,1,1,2,1}};
        for (var bounds : invalid) {
            boolean rejected = false;
            try (var ignored = new GrowthConfigFixture(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5])) {
                try { tick(h, ars); } catch (IllegalArgumentException expected) { rejected = true; }
            }
            h.assertTrue(rejected && gas.getChemicalInTank(0).getAmount() == 250,
                    "Every range must be validated before any funded mode is charged");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="configured-growth")
    public static void everyPaymentModeUsesItsOwnConfiguredGrowth(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(37, 37, 47, 47, 57, 57)) {
            for (var mode : SourceMode.values()) {
                h.setBlock(CENTER, Blocks.AIR);
                var ars = pot(h);
                if (mode == SourceMode.JAR) jar(h);
                else if (mode == SourceMode.LIQUID) fillLiquid(ars);
                else ars.sourceAccess().chemicalHandler().insertChemical(SourceAccessGameTests.gas(250), Action.EXECUTE);
                tick(h, ars);
                h.assertTrue(ars.growthTime() == 1 + (mode == SourceMode.JAR ? 37 : mode == SourceMode.LIQUID ? 47 : 57), mode + " must use its own configured growth range");
                h.assertTrue(power(ars).getLong("boost_cooldown") ==
                        (mode == SourceMode.JAR ? 10 :
                                mode == SourceMode.LIQUID ? 5 : 1),
                        "Configured growth must preserve mode cadence");
                h.assertTrue(power(ars).getLong("liquid") == 0 && power(ars).getLong("gas") == 0,
                        "The selected operation must spend its buffer");
                if (mode == SourceMode.JAR) {
                    h.assertTrue(((com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile)
                            h.getBlockEntity(CENTER.above(2))).getSource() == 0, "Jar must spend exactly 2500 Source");
                    h.setBlock(CENTER.above(2), Blocks.AIR);
                }
            }
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="configured-growth")
    public static void everyPaymentModeUsesDefaultGrowthRange(GameTestHelper h) {
        for (var mode : SourceMode.values()) {
            h.setBlock(CENTER, Blocks.AIR);
            var ars = pot(h);
            if (mode == SourceMode.JAR) jar(h);
            else if (mode == SourceMode.LIQUID) fillLiquid(ars);
            else ars.sourceAccess().chemicalHandler().insertChemical(SourceAccessGameTests.gas(250), Action.EXECUTE);
            tick(h, ars);
            h.assertTrue(ars.growthTime() >= 1 + (mode == SourceMode.JAR ? 600 : mode == SourceMode.LIQUID ? 500 : 400)
                    && ars.growthTime() <= 1 + (mode == SourceMode.JAR ? 800 : mode == SourceMode.LIQUID ? 700 : 600),
                    mode + " must add one natural tick plus its default growth range");
            h.setBlock(CENTER.above(2), Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="configured-growth")
    public static void everyPaymentModeReachesMaturityWithExactMaximumOverflow(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(Integer.MAX_VALUE, Integer.MAX_VALUE)) {
            for (var mode : SourceMode.values()) {
                h.setBlock(CENTER, Blocks.AIR);
                var ars = pot(h);
                ars.updateGrowthTime(ars.getRequiredGrowthTicks() - 30);
                if (mode == SourceMode.JAR) jar(h);
                else if (mode == SourceMode.LIQUID) fillLiquid(ars);
                else ars.sourceAccess().chemicalHandler().insertChemical(SourceAccessGameTests.gas(250), Action.EXECUTE);
                tick(h, ars);
                h.assertTrue(ars.growthTime() >= ars.getRequiredGrowthTicks() && ars.canHarvest(),
                        mode + " must reach maturity without a finishing delay");
                h.assertTrue(power(ars).getDouble("growth_credit") == 2147483618d,
                        "Maximum boost minus exactly 29 needed ticks must remain exact");
                h.setBlock(CENTER.above(2), Blocks.AIR);
            }
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="source-overflow")
    public static void overflowPersistsAndFundsConsecutiveHarvestsWithoutRepayment(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(1000, 1000)) {
            var ars=pot(h);
            ars.updateGrowthTime(ars.getRequiredGrowthTicks()-301);
            ars.sourceAccess().chemicalHandler().insertChemical(SourceAccessGameTests.gas(250),Action.EXECUTE); tick(h,ars);
            h.assertTrue(power(ars).getDouble("growth_credit")==700, "1000 paid minus 300 needed must retain exactly 700");
            var saved=ars.saveWithoutMetadata(h.getLevel().registryAccess());
            ars.loadAdditional(saved,h.getLevel().registryAccess());
            h.assertTrue(power(ars).getDouble("growth_credit")==700, "Save/load retains exact paid overflow");
            tick(h,ars);
            h.assertTrue(ars.growthTime()==699 && power(ars).getDouble("growth_credit")==0,
                    "Next tick harvests once and applies 700 credit from upstream reset minus one");
            h.assertTrue(power(ars).getLong("liquid")==0 && power(ars).getLong("gas")==0,
                    "Retained growth is used without a new resource payment");
            h.assertTrue(ars.saveWithoutMetadata(h.getLevel().registryAccess()).getFloat("grow_cooldown")==0,
                    "Source-assisted harvest skips the five-tick pause");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="source-overflow")
    public static void hugeCreditAllowsOnlyOneHarvestPerTickAndResetsWithCrop(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(Integer.MAX_VALUE, Integer.MAX_VALUE)) {
            var ars=pot(h); fill(ars); tick(h,ars);
            fill(ars);
            for(int tick=1;tick<=8;tick++) {
                tick(h,ars);
                int wheat=0;
                for(int slot:BotanyPotBlockEntity.STORAGE_SLOTS) if(ars.getItem(slot).is(Items.WHEAT)) wheat+=ars.getItem(slot).getCount();
                h.assertTrue(wheat==tick,"Huge credit must produce exactly one stock wheat harvest per tick");
                h.assertTrue(power(ars).getLong("gas")==0 && power(ars).getLong("liquid")==500,
                        "Sufficient backlog rejects new gas and cannot spend already buffered liquid");
            }
            ars.setSeed(new ItemStack(Items.BEETROOT_SEEDS));
            h.assertTrue(power(ars).getDouble("growth_credit")==0 && !power(ars).getBoolean("source_assisted"),
                    "Crop reset discards paid growth and assisted flag");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="source-overflow")
    public static void unsupportedRecipeHoldsCreditWithoutAcceptingOrSpendingSource(GameTestHelper h) {
        try(var ignored=new GrowthConfigFixture(1000,1000)) {
            var ars=pot(h); ars.updateGrowthTime(ars.getRequiredGrowthTicks()-301); fill(ars); tick(h,ars);
            BonemealGrowthGameTests.withFertilizer(h,null,ars,()-> {
                tick(h,ars);
                h.assertTrue(power(ars).getDouble("growth_credit")==700 && ars.growthTime()==-1,
                        "Missing recipe holds credit after upstream harvest instead of applying it");
                h.assertTrue(ars.sourceAccess().chemicalHandler().insertChemical(SourceAccessGameTests.gas(250),Action.EXECUTE).getAmount()==250,
                        "Missing recipe refuses resource intake despite saved credit");
                tick(h,ars);
                h.assertTrue(power(ars).getDouble("growth_credit")==700 && power(ars).getLong("liquid")==500,
                        "Unsupported cycles preserve existing credit and buffered liquid");
            });
            tick(h,ars);
            h.assertTrue(power(ars).getDouble("growth_credit")==501 && ars.growthTime()==1200 && power(ars).getLong("liquid")==0,
                    "Restored recipe uses the bank first, then one due liquid boost when partial credit cannot mature the crop");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="source-overflow")
    public static void malformedCreditIsBoundedAndSoilResetDiscardsIt(GameTestHelper h) {
        var ars=pot(h);
        for(double bad:new double[]{Double.NaN, Double.POSITIVE_INFINITY, -7, Double.MAX_VALUE, 2147483648d}) {
            var saved=ars.saveWithoutMetadata(h.getLevel().registryAccess());
            saved.getCompound("sourcepots").putDouble("growth_credit",bad);
            saved.getCompound("sourcepots").putBoolean("source_assisted",true);
            ars.loadAdditional(saved,h.getLevel().registryAccess());
            double credit=power(ars).getDouble("growth_credit");
            h.assertTrue(Double.isFinite(credit) && credit>=0 && credit<=2147483647d,"Malformed credit must be finite and bounded by one maximum earned boost");
        }
        var saved=ars.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.getCompound("sourcepots").putDouble("growth_credit",700.5);
        ars.loadAdditional(saved,h.getLevel().registryAccess());
        h.assertTrue(power(ars).getDouble("growth_credit")==700.5,"Fractional paid credit must survive saving exactly");
        ars.setSoilItem(new ItemStack(Items.SAND));
        h.assertTrue(power(ars).getDouble("growth_credit")==0 && !power(ars).getBoolean("source_assisted"),"Soil reset discards credit and cycle ownership");
        h.succeed();
    }

    @GameTest(template="empty", batch="source-overflow")
    public static void blockedHarvestPreservesCreditAndCannotRepeatLootOrPayment(GameTestHelper h) {
        boolean[] harvestAllowed={true};
        int[] harvests={0};
        var properties=new net.darkhax.botanypots.common.impl.data.recipe.crop.BasicCrop.Properties(
                net.minecraft.world.item.crafting.Ingredient.of(Items.WHEAT_SEEDS),
                net.minecraft.world.item.crafting.Ingredient.of(Items.DIRT),20,java.util.List.of(),0,java.util.List.of(),java.util.Optional.empty(),java.util.Optional.empty(),1,1);
        var crop=new net.darkhax.botanypots.common.impl.data.recipe.crop.BasicCrop(properties) {
            @Override public boolean canHarvest(net.darkhax.botanypots.common.api.context.BotanyPotContext context,net.minecraft.world.level.Level level) {return harvestAllowed[0];}
            @Override public void onHarvest(net.darkhax.botanypots.common.api.context.BotanyPotContext context,net.minecraft.world.level.Level level,java.util.function.Consumer<ItemStack> drops) {harvests[0]++;}
        };
        try(var ignored=new GrowthConfigFixture(1000,1000)) {
            var ars=pot(h);
            var seed=new ItemStack(Items.WHEAT_SEEDS); net.darkhax.botanypots.common.api.data.components.CropOverride.set(seed,crop);
            ars.setSeed(seed); ars.updateGrowthTime(0); fill(ars); tick(h,ars);
            h.assertTrue(power(ars).getDouble("growth_credit")==981,"Short crop retains exactly 981 excess ticks");
            harvestAllowed[0]=false;
            for(int tick=0;tick<8;tick++) tick(h,ars);
            h.assertTrue(harvests[0]==0 && power(ars).getDouble("growth_credit")==981 && power(ars).getLong("liquid")==500,
                    "Blocked harvest must retain credit and buffered payment without generating loot");
            h.assertTrue(ars.sourceAccess().chemicalHandler().insertChemical(SourceAccessGameTests.gas(250),Action.EXECUTE).getAmount()==250,
                    "Blocked pot rejects new resource intake");
            harvestAllowed[0]=true;
            for(int tick=0;tick<6 && harvests[0]==0;tick++) tick(h,ars);
            h.assertTrue(harvests[0]==1 && power(ars).getDouble("growth_credit")==960,
                    "Unblocked crop uses the native harvest gate once and carries the remaining credit");
            tick(h,ars);
            h.assertTrue(harvests[0]==2 && power(ars).getDouble("growth_credit")==939,
                    "Following assisted harvest runs on the next tick with no five-tick pause");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="source-overflow")
    public static void unpoweredAndManualOnlyHarvestsRetainUpstreamPause(GameTestHelper h) {
        for(boolean manual:new boolean[]{false,true}) {
            h.setBlock(CENTER,Blocks.AIR);
            var ars=pot(h); var plain=upstream(h,new BlockPos(8,2,5),"terracotta_hopper_botany_pot");
            for(var pot:java.util.List.of(ars,plain)) {
                pot.updateGrowthTime(pot.getRequiredGrowthTicks()-1);
                if(manual) {
                    var player=h.makeMockPlayer(GameType.SURVIVAL);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.BONE_MEAL));
                    var context=new net.darkhax.botanypots.common.api.context.BlockEntityContext(pot,player,net.minecraft.world.InteractionHand.MAIN_HAND);
                    net.darkhax.botanypots.common.api.data.recipes.fertilizer.Fertilizer.getFertilizer(h.getLevel(),context,context.getInteractionItem()).value().apply(context,h.getLevel());
                }
            }
            for(int tick=0;tick<7;tick++) {
                tick(h,ars); BotanyPotBlockEntity.tickPot(h.getLevel(),plain.getBlockPos(),plain.getBlockState(),plain);
                h.assertTrue(ars.growthTime()==plain.growthTime() && ars.saveWithoutMetadata(h.getLevel().registryAccess()).getFloat("grow_cooldown")==plain.saveWithoutMetadata(h.getLevel().registryAccess()).getFloat("grow_cooldown"),
                        "Unpowered and manual-only mature cycles must preserve the native pause at tick "+tick);
            }
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="source-overflow")
    public static void loadedSufficientBankRejectsPushUntilCreditIsUsed(GameTestHelper h) {
        var ars=pot(h);
        var gas=ars.sourceAccess().chemicalHandler(); var liquid=ars.sourceAccess().fluidHandler();
        var saved=ars.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.getCompound("sourcepots").putDouble("growth_credit",2147483647d);
        saved.getCompound("sourcepots").putBoolean("source_assisted",true);
        ars.loadAdditional(saved,h.getLevel().registryAccess());
        h.assertTrue(ars.sourceAccess().chemicalHandler()==gas && ars.sourceAccess().fluidHandler()==liquid,"Loading must keep neighbor-held handler identities");
        h.assertTrue(gas.insertChemical(SourceAccessGameTests.gas(250),Action.EXECUTE).getAmount()==250
                && liquid.fill(SourceAccessGameTests.fluid(500),FluidAction.EXECUTE)==0,
                "Before first loaded tick, sufficient credit rejects direct pushes through existing views");
        tick(h,ars);
        h.assertTrue(ars.canHarvest() && power(ars).getDouble("growth_credit")==2147483647d-(ars.getRequiredGrowthTicks()-1),
                "First loaded tick uses exactly the growth still needed after one natural tick");
        h.succeed();
    }

    @GameTest(template="empty", batch="source-credit-unload", timeoutTicks=100)
    public static void unloadedPaidGrowthHasNoCatchUp(GameTestHelper h) {
        try(var ignored=new GrowthConfigFixture(Integer.MAX_VALUE,Integer.MAX_VALUE)) {
            var ars=pot(h); fill(ars); tick(h,ars);
            var saved=ars.saveWithoutMetadata(h.getLevel().registryAccess());
            h.setBlock(CENTER,Blocks.AIR);
            tick(h,ars);
            h.assertTrue(power(ars).equals(saved.getCompound("sourcepots")),"Detached entity cannot spend paid credit");
            h.runAfterDelay(40,()-> {
                var restored=new ArsBotanyPotBlockEntity(ars.getBlockPos(),ars.getBlockState());
                restored.loadAdditional(saved,h.getLevel().registryAccess()); restored.setLevel(h.getLevel());
                h.assertTrue(power(restored).equals(saved.getCompound("sourcepots")),"Forty unloaded ticks do not change bank or timers");
                tick(h,restored);
                int wheat=0; for(int slot:BotanyPotBlockEntity.STORAGE_SLOTS) if(restored.getItem(slot).is(Items.WHEAT)) wheat+=restored.getItem(slot).getCount();
                h.assertTrue(wheat==1 && power(restored).getDouble("growth_credit")==2147483647d-2d*restored.getRequiredGrowthTicks(),
                        "Reload grants one native harvest and spends only one following cycle of saved credit");
                h.succeed();
            });
        }
    }

    @GameTest(template="empty", batch="source-variable-fertilizer")
    public static void paymentUsesOneValidatedRecipeResult(GameTestHelper h) {
        try(var ignored=new GrowthConfigFixture(37,37)) {
            var ars=pot(h);
            ars.sourceAccess().chemicalHandler().insertChemical(SourceAccessGameTests.gas(250),Action.EXECUTE);
            int[] evaluations={0};
            var varyingAmount=new net.darkhax.botanypots.common.api.data.growthamount.GrowthAmount() {
                public int getAmount(net.darkhax.botanypots.common.api.context.BotanyPotContext context,net.minecraft.world.level.Level level) {return evaluations[0]++==0?500:0;}
                public net.darkhax.botanypots.common.api.data.growthamount.GrowthAmountType<?> getType() {return net.darkhax.botanypots.common.impl.data.growthamount.ConstantGrowthAmount.TYPE.get();}
            };
            var fertilizer=new net.darkhax.botanypots.common.impl.data.recipe.fertilizer.BasicFertilizer(
                    new net.darkhax.botanypots.common.impl.data.recipe.fertilizer.BasicFertilizer.Properties(
                            net.minecraft.world.item.crafting.Ingredient.of(Items.BONE_MEAL),java.util.Optional.empty(),java.util.Optional.empty(),
                            varyingAmount,20,false,false,java.util.Optional.empty()));
            BonemealGrowthGameTests.withFertilizer(h,fertilizer,ars,()-> {
                tick(h,ars);
                h.assertTrue(ars.growthTime()==38 && power(ars).getLong("gas")==0,
                        "One validated positive recipe result must fund exactly the prepared 37-tick boost even if a later evaluation would be zero");
                tick(h,ars);
                h.assertTrue(ars.growthTime()==39,"Next tick reevaluates the now nonpositive fertilizer and gives only natural growth");
            });
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="source-new-default-rates")
    public static void defaultJarPays2500AtZeroAnd80AndBoostsEveryTenTicks(GameTestHelper h) {
        try(var ignored=new GrowthConfigFixture(1,1)) {
            var ars=pot(h); var jar=jar(h); jar.setSource(10000);
            for(int tick=0;tick<=80;tick++) {
                tick(h,ars);
                h.assertTrue(ars.growthTime()==tick+1+tick/10+1,
                        "Default Jar adds exactly one configured boost at ticks 0,10,...80");
                h.assertTrue(jar.getSource()==(tick<80?7500:5000),
                        "Default Jar pays exactly 2500 at tick zero and again at eighty");
                h.assertTrue(power(ars).getLong("boost_cooldown")==10-tick%10,
                        "Jar uses one shared ten-tick cooldown");
            }
        }
        h.succeed();
    }

    static ArsBotanyPotBlockEntity pot(GameTestHelper h) { return pot(h,CENTER); }
    static ArsBotanyPotBlockEntity pot(GameTestHelper h,BlockPos pos) {
        h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("sourcepots:terracotta_hopper_botany_pot")));
        var pot=(ArsBotanyPotBlockEntity)h.getBlockEntity(pos); initialize(pot); return pot;
    }
    static BotanyPotBlockEntity upstream(GameTestHelper h,BlockPos pos,String path) {
        h.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("botanypots:"+path)));
        var pot=(BotanyPotBlockEntity)h.getBlockEntity(pos); initialize(pot); return pot;
    }
    private static void initialize(BotanyPotBlockEntity pot) { pot.setSoilItem(new ItemStack(Items.DIRT)); pot.setSeed(new ItemStack(Items.WHEAT_SEEDS)); pot.updateGrowthTime(0); }
    static void tick(GameTestHelper h,ArsBotanyPotBlockEntity pot) {
        var block=(io.github.gaiusred.sourcepots.block.ArsBotanyPotBlock)pot.getBlockState().getBlock();
        var ticker=block.getTicker(h.getLevel(),pot.getBlockState(),io.github.gaiusred.sourcepots.registry.ModContent.POT_ENTITY.get());
        h.assertTrue(ticker!=null,"Registered custom type must receive its block ticker");
        ticker.tick(h.getLevel(),pot.getBlockPos(),pot.getBlockState(),pot);
    }
    private static void fill(ArsBotanyPotBlockEntity pot) {
        fillLiquid(pot);
        pot.sourceAccess().chemicalHandler().insertChemical(new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.parse("starbunclemania:source_gas")).orElseThrow(),250),Action.EXECUTE);
    }
    private static void fillLiquid(ArsBotanyPotBlockEntity pot) {
        pot.sourceAccess().fluidHandler().fill(SourceAccessGameTests.fluid(500),FluidAction.EXECUTE);
    }
    private static CompoundTag power(ArsBotanyPotBlockEntity pot) { return pot.saveWithoutMetadata(pot.getLevel().registryAccess()).getCompound("sourcepots"); }
    private static com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile jar(GameTestHelper h) {
        h.setBlock(CENTER.above(2),BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ars_nouveau:source_jar")));
        var jar=(com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile)h.getBlockEntity(CENTER.above(2)); jar.setSource(2500); return jar;
    }
}
