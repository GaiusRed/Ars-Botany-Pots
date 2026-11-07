package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity;
import net.darkhax.botanypots.common.api.context.BlockEntityContext;
import net.darkhax.botanypots.common.api.context.BotanyPotContext;
import net.darkhax.botanypots.common.api.data.components.CropOverride;
import net.darkhax.botanypots.common.api.data.components.SoilOverride;
import net.darkhax.botanypots.common.impl.Helpers;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.darkhax.botanypots.common.impl.data.itemdrops.SimpleDropProvider;
import net.darkhax.botanypots.common.impl.data.recipe.crop.BasicCrop;
import net.darkhax.botanypots.common.impl.data.recipe.soil.BasicSoil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class MultipleHarvestGameTests {
    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void floatRoundingDownRetainsPaidGrowth(GameTestHelper h) {
        var pot = pot(h, 16777220, new BlockPos(5, 2, 5));
        restore(h, pot, 16777216, 1, 1000);
        ArsPotGameTests.tick(h, pot);
        h.assertTrue(pot.growthTime() == 16777216 && credit(pot) == 1,
                "One paid tick below the float step must remain credit, not disappear");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void floatRoundingUpCannotGrantUnpaidGrowth(GameTestHelper h) {
        var pot = pot(h, 16777224, new BlockPos(5, 2, 5));
        restore(h, pot, 16777218, 1, 1000);
        ArsPotGameTests.tick(h, pot);
        h.assertTrue(pot.growthTime() == 16777218 && credit(pot) == 1,
                "One paid tick cannot round up to two growth ticks");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void maximumRequirementStillChargesTheMinusOneResetTick(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(1, 1).harvestLimit(1)) {
            var pot = pot(h, Integer.MAX_VALUE, new BlockPos(5, 2, 5));
            restore(h, pot, -1, Integer.MAX_VALUE, 1000);
            ArsPotGameTests.tick(h, pot);
            h.assertTrue(wheat(pot) == 0 && pot.growthTime() == 2147483520f && credit(pot) == 126,
                    "The maximum purchase is one tick short from -1; retain the 126 ticks below the next float step");
            fill(pot);
            ArsPotGameTests.tick(h, pot);
            h.assertTrue(wheat(pot) == 1 && pot.growthTime() == -1 && credit(pot) == 0
                            && pot.sourceAccess().storedLiquid(Direction.NORTH) == 0,
                    "One further paid tick completes the effective integer requirement without an extra charge for float saturation");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void scheduledSingleTickPaymentsAccumulateAcrossRestore(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(1, 1)) {
            var pot = pot(h, 16777224, new BlockPos(5, 2, 5));
            restore(h, pot, 16777216, 0, 1000);
            for (int purchase = 1; purchase <= 4; purchase++) {
                fill(pot);
                h.assertTrue(pot.sourceAccess().storedLiquid(Direction.NORTH) == 500,
                        "Sub-step residual must permit the next payment to enter storage");
                if (purchase > 1) {
                    for (int waiting = 0; waiting < 4; waiting++) {
                        ArsPotGameTests.tick(h, pot);
                        h.assertTrue(pot.sourceAccess().storedLiquid(Direction.NORTH) == 500,
                                "A residual top-up must still wait for the five-tick liquid cooldown");
                    }
                }
                ArsPotGameTests.tick(h, pot);
                h.assertTrue(pot.sourceAccess().storedLiquid(Direction.NORTH) == 0,
                        "Each scheduled purchase consumes exactly one payment");
                h.assertTrue(pot.growthTime() == 16777216 + (purchase / 2) * 2 && credit(pot) == purchase % 2,
                        "Each pair of paid ticks must produce two growth ticks without stalling or overpayment: " + purchase);
                var saved = save(pot);
                pot.loadAdditional(saved, h.getLevel().registryAccess());
                h.assertTrue(credit(pot) == purchase % 2, "Live restore must preserve the exact sub-step residual");
            }
            h.assertTrue(wheat(pot) == 0, "Four paid ticks cannot complete the eight-tick remaining cycle");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void sufficientRetainedCreditNeverBuysAnotherBoost(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(1, 1).harvestLimit(1)) {
            var pot = pot(h, 16777218, new BlockPos(5, 2, 5));
            fill(pot);
            restore(h, pot, 16777216, 2, 1000);
            ArsPotGameTests.tick(h, pot);
            h.assertTrue(wheat(pot) == 1 && credit(pot) == 0 && pot.growthTime() == -1
                            && pot.sourceAccess().storedLiquid(Direction.NORTH) == 500,
                    "Enough retained credit completes the cycle without paying again at the harvest limit");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void residualPlusMaximumPurchaseSurvivesBoundedRestore(GameTestHelper h) {
        var pot = pot(h, 100, new BlockPos(5, 2, 5));
        restore(h, pot, 0, 2147483774.5d, 1000);
        h.assertTrue(credit(pot) == 2147483774.5d,
                "A 127.5-tick residual plus the maximum purchase must survive save/load");
        for (double corrupt : new double[]{4294967296d, Double.MAX_VALUE}) {
            restore(h, pot, 0, corrupt, 1000);
            h.assertTrue(credit(pot) == 4294967295d,
                    "Finite corrupt banks must be bounded by two maximum integers plus the native reset tick");
        }
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            restore(h, pot, 0, invalid, 1000);
            h.assertTrue(credit(pot) == 0, "Negative or nonfinite saved credit cannot fund growth");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest-functions", timeoutTicks = 1200)
    public static void loadedRecipeFunctionsRunForEveryHarvestLootRoll(GameTestHelper h) {
        var control = BonemealGrowthGameTests.pot(h);
        GrowthReloadFixture.runWithReloadAssertion(h, new io.github.gaiusred.sourcepots.growth.BonemealGrowth(), control, () -> {
            var server = h.getLevel().getServer();
            var commands = server.getCommands();
            var commandSource = server.createCommandSourceStack();
            commands.performPrefixedCommand(commandSource, "scoreboard objectives add sourcepots_harvest dummy");
            try {
                var crop = new BasicCrop(new BasicCrop.Properties(Ingredient.of(Items.WHEAT_SEEDS), Ingredient.of(Items.DIRT),
                        100, List.of(), 0, List.of(), Optional.of(net.minecraft.resources.ResourceLocation.parse("sourcepots:count_harvest")),
                        Optional.empty(), 2, 1));
                h.setBlock(new BlockPos(2, 2, 5), Blocks.AIR);
                var pot = pot(h, crop, new BlockPos(2, 2, 5));
                restore(h, pot, 0, 1000, 0);
                ArsPotGameTests.tick(h, pot);
                var objective = server.getScoreboard().getObjective("sourcepots_harvest");
                var score = server.getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("#harvest"), objective);
                h.assertTrue(score != null && score.value() == 18 && pot.growthTime() == 92,
                        "The actual loaded crop function must run twice for each of nine harvests");
            } finally {
                commands.performPrefixedCommand(commandSource, "scoreboard objectives remove sourcepots_harvest");
                h.setBlock(new BlockPos(2, 2, 5), Blocks.AIR);
            }
        });
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void paidThousandTicksPreserveEveryNativeReset(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(1000, 1000)) {
            var pot = pot(h, 100, new BlockPos(5, 2, 5));
            fill(pot);
            ArsPotGameTests.tick(h, pot);
            h.assertTrue(wheat(pot) == 9 && pot.growthTime() == 92 && credit(pot) == 0,
                    "1000 paid + 1 natural tick fund nine 100-tick harvests, eight intervening -1 resets, and 93 ticks after the last reset");
            h.assertTrue(pot.sourceAccess().storedLiquid(Direction.NORTH) == 0, "One payment buys all nine harvests");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void nativeHarvestCountsAtLimitsOneAndTwoIncludingOnTickMaturation(GameTestHelper h) {
        for (int limit : new int[]{1, 2}) {
            try (var ignored = new GrowthConfigFixture(1000, 1000).harvestLimit(limit)) {
                for (boolean matureInCallback : new boolean[]{false, true}) {
                    h.setBlock(new BlockPos(5, 2, 5), Blocks.AIR);
                    var crop = new CountingCrop(100);
                    crop.matureOnTick = matureInCallback;
                    var pot = pot(h, crop, new BlockPos(5, 2, 5));
                    restore(h, pot, matureInCallback ? -1 : 99, 1000, 0);
                    ArsPotGameTests.tick(h, pot);
                    h.assertTrue(crop.harvests == limit && wheat(pot) == limit,
                            "Native harvest must consume one of limit " + limit + " even when onTick matures growth from -1");
                    h.assertTrue(credit(pot) == (limit == 1 ? 1000 : 899) && pot.growthTime() == -1,
                            "Only extra harvests cost 101 credit and the cap preserves all remaining credit");
                    h.assertTrue(pot.sourceAccess().fluidHandler(Direction.NORTH).fill(SourceAccessGameTests.fluid(500), FluidAction.EXECUTE) == 0,
                            "A capped tick must reject further pushes");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void ordinaryCooldownAtFiveDoesNotConsumeHarvestAllowance(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(1000, 1000).harvestLimit(1)) {
            var pot = pot(h, 100, new BlockPos(5, 2, 5));
            restore(h, pot, -1, 1000, 6);
            ArsPotGameTests.tick(h, pot);
            h.assertTrue(wheat(pot) == 1 && credit(pot) == 899,
                    "Ordinary cooldown 6-to-5 with reset growth is not a native harvest; Source still gets one cycle");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void cappedHugePurchaseSurvivesRepeatedLiveRestoreWithoutAnotherPayment(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(Integer.MAX_VALUE, Integer.MAX_VALUE).harvestLimit(2)) {
            var pot = pot(h, 100, new BlockPos(5, 2, 5));
            var supplier = ResourceTestEnvironment.place(h, new BlockPos(5, 2, 4), Direction.SOUTH, false);
            try {
                ArsPotGameTests.tick(h, pot);
                h.assertTrue(wheat(pot) == 2 && credit(pot) == 2147483447d && pot.growthTime() == -1,
                        "Two harvests spend exactly 99 + 101 from the huge purchased budget");
                h.assertTrue(supplier.gas.getStored() == 750, "Exactly one purchase and refill occur in the tick");
                var saved = save(pot);
                for (int i = 0; i < 3; i++) pot.loadAdditional(saved, h.getLevel().registryAccess());
                h.assertTrue(wheat(pot) == 2 && credit(pot) == 2147483447d, "Repeated restore replaces inventory and credit without duplication");
                ArsPotGameTests.tick(h, pot);
                h.assertTrue(wheat(pot) == 4 && credit(pot) == 2147483246d && supplier.gas.getStored() == 750,
                        "Next tick spends 100 + 101 retained credit and does not refill or repay");
            } finally {
                ResourceTestEnvironment.remove(h, new BlockPos(5, 2, 4));
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void recipeTimesAndSoilModifiersSetTheActualCycleCost(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(1000, 1000)) {
            var fast = pot(h, 100, new BlockPos(2, 2, 5));
            var slow = pot(h, 400, new BlockPos(8, 2, 5));
            h.assertTrue(fast.getRequiredGrowthTicks() == 100 && slow.getRequiredGrowthTicks() == 400, "Unmodified recipe requirements must differ");
            fill(fast); fill(slow);
            ArsPotGameTests.tick(h, fast); ArsPotGameTests.tick(h, slow);
            h.assertTrue(wheat(fast) == 9 && fast.growthTime() == 92 && wheat(slow) == 2 && slow.growthTime() == 199,
                    "Equal credit must preserve 100- and 400-tick recipe costs");
            var soil = (BasicSoil) slow.getOrInvalidateSoil();
            var properties = soil.getProperties();
            var stack = new ItemStack(Items.DIRT);
            SoilOverride.set(stack, new BasicSoil(new BasicSoil.Properties(properties.input(), properties.display(), 1, 0, 0)));
            slow.setSoilItem(stack);
            h.assertTrue(slow.getRequiredGrowthTicks() == 200 && Helpers.getRequiredGrowthTicks(slow.getRecipeContext(), h.getLevel(), slow.getOrInvalidateCrop(), slow.getOrInvalidateSoil()) == 200,
                    "Active native soil modifier halves this recipe's requirement");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void fractionalCreditAndMinusOneResetNeverGrantFreeGrowth(GameTestHelper h) {
        var pot = pot(h, 100, new BlockPos(5, 2, 5));
        restore(h, pot, -1, 100.5, 5);
        ArsPotGameTests.tick(h, pot);
        h.assertTrue(wheat(pot) == 0 && pot.growthTime() == 99.5f && credit(pot) == 0,
                "With native growth paused, 100.5 credit from -1 cannot fund a 101-tick cycle");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void invalidSoilAndFailedHarvestKeepCreditAndResources(GameTestHelper h) {
        for (boolean wrongSoil : new boolean[]{false, true}) {
            h.setBlock(new BlockPos(5, 2, 5), Blocks.AIR);
            var crop = new CountingCrop(100);
            var pot = pot(h, crop, new BlockPos(5, 2, 5));
            fill(pot);
            if (wrongSoil) pot.setSoilItem(new ItemStack(Items.SAND));
            else crop.allowed = false;
            restore(h, pot, 0, 1000, 0);
            ArsPotGameTests.tick(h, pot);
            h.assertTrue(wheat(pot) == 0 && credit(pot) == 1000 && pot.sourceAccess().storedLiquid(Direction.NORTH) == 500,
                    "Invalid soil or failed harvest condition must consume neither credit nor resource");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void fullOutputStopsCreditAndPartialFillStopsTheNextCycle(GameTestHelper h) {
        for (int room : new int[]{0, 1}) {
            h.setBlock(new BlockPos(5, 2, 5), Blocks.AIR);
            var pot = pot(h, 100, new BlockPos(5, 2, 5));
            for (int slot : BotanyPotBlockEntity.STORAGE_SLOTS) pot.setItem(slot, new ItemStack(Items.WHEAT, 64));
            pot.getItem(BotanyPotBlockEntity.STORAGE_SLOTS[0]).shrink(room);
            restore(h, pot, 0, 1000, 0);
            ArsPotGameTests.tick(h, pot);
            h.assertTrue(credit(pot) == (room == 0 ? 1000 : 901), "Output saturation must stop before spending the next cycle");
            h.assertTrue(wheat(pot) == 768 && pot.growthTime() == (room == 0 ? 1 : -1), "Only available output room may be filled");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void cropSoilRemovalAndToolBreakCallbacksStopFurtherCycles(GameTestHelper h) {
        for (int change = 0; change < 5; change++) {
            h.setBlock(new BlockPos(5, 2, 5), Blocks.AIR);
            var crop = new CountingCrop(100);
            var pot = pot(h, crop, new BlockPos(5, 2, 5));
            int action = change;
            crop.afterHarvest = () -> {
                if (action == 0) pot.setSeed(new ItemStack(Items.STICK));
                if (action == 1) pot.setSoilItem(new ItemStack(Items.SAND));
                if (action == 2) pot.setRemoved();
                if (action == 4) crop.allowed = false;
            };
            if (change == 3) {
                var tool = new ItemStack(Items.WOODEN_HOE);
                tool.setDamageValue(tool.getMaxDamage() - 1);
                pot.setHoe(tool);
                crop.needsTool = true;
            }
            restore(h, pot, 0, 1000, 0);
            ArsPotGameTests.tick(h, pot);
            h.assertTrue(crop.harvests == 1 && wheat(pot) == 1, "A callback or final-durability tool must stop the next cycle: " + change);
            h.assertTrue(credit(pot) == (change < 2 ? 0 : 901), "Crop/soil reset discards credit; removal or missing tool holds unused credit");
            if (change == 3) h.assertTrue(pot.getHarvestItem().isEmpty(), "Native tool damage breaks the final-durability tool");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "multiple-harvest")
    public static void multipleLootRollsKeepPerTickHooksTimersAndExportSingle(GameTestHelper h) {
        var crop = new CountingCrop(100, 2);
        var pot = pot(h, crop, new BlockPos(5, 2, 5));
        int[] soilTicks = {0};
        var properties = ((BasicSoil) pot.getOrInvalidateSoil()).getProperties();
        var soil = new ItemStack(Items.DIRT);
        SoilOverride.set(soil, new BasicSoil(properties) {
            @Override public void onTick(BotanyPotContext context, Level level) { soilTicks[0]++; }
        });
        pot.setSoilItem(soil);
        pot.getOrInvalidateSoil();
        pot.getOrInvalidateCrop();
        pot.setHoe(new ItemStack(Items.IRON_HOE));
        restore(h, pot, 0, 1000, 0);
        var state = save(pot);
        state.putInt("bonemeal_cooldown", 7);
        state.putFloat("export_delay", 7);
        var power = state.getCompound("sourcepots_v2");
        power.putLong("jar_remaining", 50); power.putLong("jar_retry", 40); power.putLong("boost_cooldown", 30);
        pot.loadAdditional(state, h.getLevel().registryAccess());
        int[] notifications = {0};
        Consumer<net.neoforged.neoforge.event.VanillaGameEvent> listener = event -> {
            if (event.getVanillaEvent().equals(net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE)
                    && BlockPos.containing(event.getEventPosition()).equals(pot.getBlockPos())) notifications[0]++;
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try {
            ArsPotGameTests.tick(h, pot);
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
        }
        state = save(pot); power = state.getCompound("sourcepots_v2");
        h.assertTrue(crop.ticks == 1 && crop.growthTicks == 1 && soilTicks[0] == 1, "Crop/soil tick hooks must run exactly once");
        h.assertTrue(crop.harvests == 18 && wheat(pot) == 18 && pot.getHarvestItem().getDamageValue() == 9,
                "Nine cycles each calculate two loot rolls and damage the tool once");
        h.assertTrue(notifications[0] == 9, "Every actual harvest sends exactly one native BLOCK_CHANGE notification");
        h.assertTrue(state.getInt("bonemeal_cooldown") == 6 && state.getFloat("export_delay") == 6,
                "Native fertilizer and export timers must advance only once");
        h.assertTrue(power.getLong("jar_remaining") == 49 && power.getLong("jar_retry") == 39 && power.getLong("boost_cooldown") == 29,
                "All Source timers must advance only once");
        h.assertTrue(pot.growthTime() == 92 && state.getFloat("grow_cooldown") == 0 && pot.comparatorLevel == 15,
                "Each extra cycle uses native mature comparator and reset, bypassing only the assisted pause");
        h.succeed();
    }

    private static ArsBotanyPotBlockEntity pot(GameTestHelper h, int required, BlockPos pos) {
        return pot(h, new CountingCrop(required), pos);
    }

    private static ArsBotanyPotBlockEntity pot(GameTestHelper h, BasicCrop crop, BlockPos pos) {
        var pot = ArsPotGameTests.pot(h, pos);
        var seed = new ItemStack(Items.WHEAT_SEEDS);
        CropOverride.set(seed, crop);
        pot.setSeed(seed);
        pot.updateGrowthTime(0);
        h.assertTrue(pot.getOrInvalidateCrop() == crop && pot.getRequiredGrowthTicks() == crop.getBasicProperties().growTime(),
                "Controlled active crop fixture must load with its unmodified growth requirement");
        return pot;
    }

    private static void restore(GameTestHelper h, ArsBotanyPotBlockEntity pot, float progress, double credit, float cooldown) {
        var saved = save(pot);
        saved.putFloat("growth_time", progress);
        saved.putFloat("grow_cooldown", cooldown);
        saved.getCompound("sourcepots_v2").putDouble("growth_credit", credit);
        pot.loadAdditional(saved, h.getLevel().registryAccess());
    }

    private static void fill(ArsBotanyPotBlockEntity pot) {
        pot.sourceAccess().fluidHandler(Direction.NORTH).fill(SourceAccessGameTests.fluid(500), FluidAction.EXECUTE);
    }

    private static CompoundTag save(ArsBotanyPotBlockEntity pot) { return pot.saveWithoutMetadata(pot.getLevel().registryAccess()); }
    private static double credit(ArsBotanyPotBlockEntity pot) { return save(pot).getCompound("sourcepots_v2").getDouble("growth_credit"); }
    private static int wheat(BotanyPotBlockEntity pot) {
        int count = 0;
        for (int slot : BotanyPotBlockEntity.STORAGE_SLOTS) if (pot.getItem(slot).is(Items.WHEAT)) count += pot.getItem(slot).getCount();
        return count;
    }

    private static final class CountingCrop extends BasicCrop {
        int ticks, growthTicks, harvests;
        boolean matureOnTick, needsTool;
        boolean allowed = true;
        Runnable afterHarvest = () -> {};

        CountingCrop(int required) { this(required, 1); }
        CountingCrop(int required, int yield) {
            super(new Properties(Ingredient.of(Items.WHEAT_SEEDS), Ingredient.of(Items.DIRT), required, List.of(), 0,
                    List.of(new SimpleDropProvider(List.of(new SimpleDropProvider.SimpleDrop(new ItemStack(Items.WHEAT), 1)))),
                    Optional.empty(), Optional.empty(), yield, 1));
        }

        @Override public void onTick(BotanyPotContext context, Level level) {
            ticks++;
            if (matureOnTick) ((BlockEntityContext) context).pot().updateGrowthTime(100);
        }
        @Override public void onGrowthTick(BotanyPotContext context, Level level) { growthTicks++; }
        @Override public boolean canHarvest(BotanyPotContext context, Level level) {
            return allowed && (!needsTool || !context.getHarvestItem().isEmpty());
        }
        @Override public void onHarvest(BotanyPotContext context, Level level, Consumer<ItemStack> drops) {
            harvests++;
            super.onHarvest(context, level, drops);
            afterHarvest.run();
        }
    }
}
