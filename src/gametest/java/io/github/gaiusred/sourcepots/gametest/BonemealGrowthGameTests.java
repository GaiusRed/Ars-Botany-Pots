package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.growth.BonemealGrowth;
import io.github.gaiusred.sourcepots.source.SourceAccess;
import io.github.gaiusred.sourcepots.source.SourceMode;
import io.github.gaiusred.sourcepots.source.SourceScheduler;
import io.github.gaiusred.sourcepots.source.SourceSettings;
import io.github.gaiusred.sourcepots.source.SourceState;
import net.darkhax.botanypots.common.api.context.BlockEntityContext;
import net.darkhax.botanypots.common.api.context.BotanyPotContext;
import net.darkhax.botanypots.common.api.data.components.CropOverride;
import net.darkhax.botanypots.common.api.data.recipes.fertilizer.Fertilizer;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.darkhax.botanypots.common.impl.data.growthamount.ConstantGrowthAmount;
import net.darkhax.botanypots.common.impl.data.recipe.crop.BasicCrop;
import net.darkhax.botanypots.common.impl.data.recipe.fertilizer.BasicFertilizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.nio.file.Files;
import net.minecraft.world.level.storage.LevelResource;

@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class BonemealGrowthGameTests {
    private static final BonemealGrowth GROWTH = new BonemealGrowth();
    private static final ResourceLocation BONE_MEAL = ResourceLocation.parse("botanypots:minecraft/fertilizer/bone_meal");

    @GameTest(template = "empty", batch = "growth")
    public static void nonBinaryFractionsConserveActualStoredGrowth(GameTestHelper helper) {
        var pot = pot(helper);
        float[] starts = {10, 10.2f, 99.9f};
        double[] budgets = {0.1, 0.2, 0.05};
        for (int i = 0; i < starts.length; i++) {
            pot.updateGrowthTime(starts[i]);
            double remaining = GROWTH.applyEligibleCredit(pot, budgets[i]);
            double applied = (double) pot.growthTime() - starts[i];
            helper.assertTrue(applied > 0 && applied <= budgets[i],
                    "Stored fractional growth must advance without exceeding the paid budget: " + i);
            helper.assertTrue(remaining == budgets[i] - applied && remaining > 0,
                    "The exact unrepresentable fraction must remain paid credit: " + i);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "growth")
    public static void sourceJarDefaultsAdd600To800(GameTestHelper helper) {
        var pot = pot(helper);
        int amount = GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR);
        helper.assertTrue(amount >= 600 && amount <= 800, "Default Source amount must be 600..800, got " + amount);
        helper.assertTrue(GrowthTestSupport.apply(GROWTH, pot, amount), "A supported recipe must add growth");
        helper.assertTrue(pot.growthTime() == amount, "Growth delta must equal selected Source amount");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "growth")
    public static void sourceCanReachMaturityImmediately(GameTestHelper helper) {
        var pot = pot(helper);
        int required = pot.getRequiredGrowthTicks();
        pot.updateGrowthTime(required - 30);
        helper.assertTrue(GrowthTestSupport.apply(GROWTH, pot, Integer.MAX_VALUE), "Maximum growth must apply");
        helper.assertTrue(pot.growthTime() >= required && pot.canHarvest(), "Source must reach maturity without twenty natural ticks");
        helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) == 0, "Mature crop cannot accept another boost");
        helper.assertTrue(!GrowthTestSupport.apply(GROWTH, pot, 500), "Mature crop cannot apply another boost");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "growth")
    public static void shortCropCanReachMaturity(GameTestHelper helper) {
        var pot = pot(helper);
        var seed = new ItemStack(Items.WHEAT_SEEDS);
        CropOverride.set(seed, new BasicCrop(new BasicCrop.Properties(Ingredient.of(Items.WHEAT_SEEDS),
                Ingredient.of(Items.DIRT), 20, List.of(), 0, List.of(), Optional.empty(), Optional.empty(), 1, 1)));
        pot.setSeed(seed);
        pot.updateGrowthTime(0);
        helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) > 0, "Twenty-tick crops must be eligible");
        helper.assertTrue(GrowthTestSupport.apply(GROWTH, pot, 500) && pot.growthTime() == 20 && pot.canHarvest(), "Short crop must mature immediately");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "growth")
    public static void invalidSoilOrSeedReturnsZero(GameTestHelper helper) {
        var pot = pot(helper);
        helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) > 0, "Control crop must be eligible");
        pot.setSoilItem(new ItemStack(Items.SAND));
        helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) == 0, "Valid sand soil cannot sustain wheat");
        pot.setSoilItem(new ItemStack(Items.STICK));
        helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) == 0, "Unknown soil must be ineligible");
        pot.setSoilItem(new ItemStack(Items.DIRT));
        pot.setSeed(new ItemStack(Items.STICK));
        helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) == 0, "Unknown seed must be ineligible");
        pot.setSeed(new ItemStack(Items.WHEAT_SEEDS));
        withFertilizer(helper, fertilizer(500, Optional.of(Ingredient.of(Items.SAND)), Optional.empty()), () ->
                helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) == 0, "Fertilizer soil restriction must match"));
        withFertilizer(helper, fertilizer(500, Optional.empty(), Optional.of(Ingredient.of(Items.BEETROOT_SEEDS))), () ->
                helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) == 0, "Fertilizer seed restriction must match"));
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "growth")
    public static void sourceDoesNotSetManualBonemealCooldown(GameTestHelper helper) {
        var pot = pot(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 3));
        var context = new BlockEntityContext(pot, player, InteractionHand.MAIN_HAND);
        var manual = (BasicFertilizer) Fertilizer.getFertilizer(helper.getLevel(), context, context.getInteractionItem()).value();
        helper.assertTrue(GrowthTestSupport.apply(GROWTH, pot, GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR)), "Source must add growth without a player");
        helper.assertTrue(pot.canBonemeal(), "Source must preserve ready manual cooldown");
        helper.assertTrue(context.getInteractionItem().getCount() == 3, "Source must not consume manual item");
        pot.updateGrowthTime(0);
        manual.apply(context, helper.getLevel());
        helper.assertTrue(pot.growthTime() >= 400 && pot.growthTime() <= 600, "Manual fertilizer must still use its recipe");
        helper.assertTrue(!pot.canBonemeal(), "Manual fertilizer must still set its cooldown");
        helper.assertTrue(context.getInteractionItem().getCount() == 2, "Manual fertilizer must still consume one item");
        pot.updateGrowthTime(0);
        pot.setBonemealCooldown(7);
        helper.assertTrue(GrowthTestSupport.apply(GROWTH, pot, GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR)), "Source works during manual cooldown");
        helper.assertTrue(!pot.canBonemeal(), "Source must preserve an existing manual cooldown");
        helper.assertTrue(context.getInteractionItem().getCount() == 2, "Source must not consume item during cooldown");
        for (int tick = 0; tick < 6; tick++) {
            BotanyPotBlockEntity.tickPot(helper.getLevel(), pot.getBlockPos(), pot.getBlockState(), pot);
        }
        helper.assertTrue(!pot.canBonemeal(), "Existing seven-tick cooldown must remain for six ticks");
        BotanyPotBlockEntity.tickPot(helper.getLevel(), pot.getBlockPos(), pot.getBlockState(), pot);
        helper.assertTrue(pot.canBonemeal(), "Source must not extend the existing seven-tick cooldown");
        helper.assertTrue(pot.getSeedItem().getCount() == 1 && pot.getSoilItem().getCount() == 1, "Source must preserve seed and soil items");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "growth")
    public static void missingOrUnsupportedRecipeDoesNotSpend(GameTestHelper helper) {
        var pot = pot(helper);
        helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) > 0, "Control recipe must work");
        withFertilizer(helper, null, () -> assertNoSpend(helper, pot));
        withFertilizer(helper, new UnsupportedFertilizer(), () -> assertNoSpend(helper, pot));
        withFertilizer(helper, fertilizer(0, Optional.empty(), Optional.empty()), () -> assertNoSpend(helper, pot));
        withFertilizer(helper, fertilizer(-500, Optional.empty(), Optional.empty()), () -> assertNoSpend(helper, pot));
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "reload", timeoutTicks = 1200)
    public static void recipeReloadChangesEligibilityButKeepsConfiguredAmount(GameTestHelper helper) {
        GrowthReloadFixture.run(helper, GROWTH, pot(helper));
    }

    @GameTest(template = "empty", batch = "reload-failure", timeoutTicks = 1200)
    public static void reloadFailureRestoresPackSelectionAndWorldConfiguration(GameTestHelper helper) {
        GrowthReloadFixture.runWithFailedRestoration(helper, GROWTH, pot(helper));
    }

    @GameTest(template = "empty", batch = "fixture-setup-failure")
    public static void partialReloadFixtureSetupFailureRemovesDirectory(GameTestHelper helper) throws IOException {
        var directory = Files.createTempDirectory(helper.getLevel().getServer().getWorldPath(LevelResource.DATAPACK_DIR), "sourcepots-growth-setup-");
        try {
            // A real filesystem error after recipe directories have already been created.
            Files.createDirectory(directory.resolve("pack.mcmeta"));
            boolean failed = false;
            try {
                GrowthReloadFixture.prepareFiles(directory);
            } catch (IOException expected) {
                failed = true;
            }
            helper.assertTrue(failed, "Writing metadata over a directory must fail setup");
            helper.assertTrue(!Files.exists(directory), "Partial fixture setup must delete its temporary directory");
            helper.succeed();
        } finally {
            GrowthReloadFixture.deleteFixture(directory);
        }
    }

    @GameTest(template = "empty", batch = "growth")
    public static void configuredMaximumGrowthCapsWithoutOverflow(GameTestHelper helper) {
        try (var ignored = new GrowthConfigFixture(Integer.MAX_VALUE, Integer.MAX_VALUE)) {
            var pot = pot(helper);
            int amount = GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR);
            helper.assertTrue(amount == Integer.MAX_VALUE, "Equal maximum bounds must produce the exact maximum");
            helper.assertTrue(GrowthTestSupport.apply(GROWTH, pot, amount), "Maximum configured growth must apply");
            helper.assertTrue(pot.growthTime() == pot.getRequiredGrowthTicks(),
                    "Maximum configured growth must reach maturity without overflow");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "growth")
    public static void configuredRangeIncludesBothBounds(GameTestHelper helper) {
        try (var ignored = new GrowthConfigFixture(7, 8)) {
            var pot = pot(helper);
            helper.getLevel().random.setSeed(12345);
            boolean minimum = false;
            boolean maximum = false;
            for (int sample = 0; sample < 64; sample++) {
                int amount = GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR);
                helper.assertTrue(amount == 7 || amount == 8, "Samples must stay inside configured bounds");
                minimum |= amount == 7;
                maximum |= amount == 8;
            }
            helper.assertTrue(minimum && maximum, "Seeded samples must include both inclusive endpoints");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "growth")
    public static void fullPositiveIntegerRangeDoesNotOverflow(GameTestHelper helper) {
        try (var ignored = new GrowthConfigFixture(1, Integer.MAX_VALUE)) {
            var pot = pot(helper);
            for (int sample = 0; sample < 16; sample++) {
                helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) > 0,
                        "Inclusive range width must not overflow at Integer.MAX_VALUE");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "growth")
    public static void temporaryGrowthConfigurationRestoresAfterAssertionFailure(GameTestHelper helper) {
        var original = io.github.gaiusred.sourcepots.config.SourcePotsConfig.snapshot();
        boolean caught = false;
        try (var ignored = new GrowthConfigFixture(23, 23)) {
            helper.assertTrue(GrowthTestSupport.growthAmount(GROWTH, pot(helper), helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR) == 23,
                    "Temporary configured range must take effect");
            throw new AssertionError("Deliberate fixture assertion failure");
        } catch (AssertionError expected) {
            caught = true;
        }
        helper.assertTrue(caught && io.github.gaiusred.sourcepots.config.SourcePotsConfig.snapshot().equals(original),
                "Assertion failure must restore the original in-memory growth configuration");
        helper.succeed();
    }

    private static void assertNoSpend(GameTestHelper helper, BotanyPotBlockEntity pot) {
        int amount = GrowthTestSupport.growthAmount(GROWTH, pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR);
        helper.assertTrue(amount == 0, "Missing, unsupported, or nonpositive recipe must return zero, got " + amount);
        int[] balance = {1000};
        SourceAccess access = new SourceAccess() {
            public int fundedSideMask(SourceMode mode) { return 1; }
            public boolean canPayJar() { return true; }
            public boolean payJar() { balance[0] -= 125; return true; }
            public boolean pay(SourceMode mode, int sideMask) { balance[0] -= 125; return true; }
        };
        helper.assertTrue(new SourceScheduler().tick(new SourceState(), SourceSettings.defaults(), amount > 0, access) == null, "Ineligible growth must not authorize boost");
        helper.assertTrue(balance[0] == 1000 && pot.growthTime() == 0, "Ineligible recipe must preserve Source and growth");
        helper.assertTrue(!GrowthTestSupport.apply(GROWTH, pot, amount), "Zero growth must not apply");
    }

    static BotanyPotBlockEntity pot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("botanypots:terracotta_botany_pot")));
        var pot = (BotanyPotBlockEntity) helper.getBlockEntity(pos);
        pot.setSoilItem(new ItemStack(Items.DIRT));
        pot.setSeed(new ItemStack(Items.WHEAT_SEEDS));
        pot.updateGrowthTime(0);
        return pot;
    }

    static BasicFertilizer fertilizer(int amount, Optional<Ingredient> soil, Optional<Ingredient> seed) {
        return new BasicFertilizer(new BasicFertilizer.Properties(Ingredient.of(Items.BONE_MEAL), soil, seed,
                new ConstantGrowthAmount(amount), 20, false, false, Optional.empty()));
    }

    static void withFertilizer(GameTestHelper helper, Fertilizer replacement, Runnable assertion) {
        withFertilizer(helper,replacement,(BotanyPotBlockEntity)helper.getBlockEntity(new BlockPos(5,2,5)),assertion);
    }

    static void withFertilizer(GameTestHelper helper, Fertilizer replacement, BotanyPotBlockEntity pot, Runnable assertion) {
        var manager = helper.getLevel().getRecipeManager();
        var saved = List.copyOf(manager.getRecipes());
        var context = pot.getRecipeContext();
        try {
            var recipes = new ArrayList<RecipeHolder<?>>();
            for (var holder : saved) {
                if (!(holder.value() instanceof Fertilizer value) || !value.couldMatch(new ItemStack(Items.BONE_MEAL), context, helper.getLevel())) {
                    recipes.add(holder);
                }
            }
            if (replacement != null) recipes.add(new RecipeHolder<>(BONE_MEAL, replacement));
            manager.replaceRecipes(recipes);
            invalidate(helper);
            assertion.run();
        } finally {
            manager.replaceRecipes(saved);
            invalidate(helper);
        }
    }

    private static void invalidate(GameTestHelper helper) {
        Fertilizer.CACHE.invalidate(helper.getLevel());
        Fertilizer.RECIPES.invalidate(helper.getLevel());
    }

    private static final class UnsupportedFertilizer extends Fertilizer {
        public boolean matches(BotanyPotContext context, Level level) { return true; }
        public boolean couldMatch(ItemStack candidate, BotanyPotContext context, Level level) { return candidate.is(Items.BONE_MEAL); }
        public void apply(BotanyPotContext context, Level level) { throw new AssertionError("Unsupported fertilizer apply must never be called"); }
        public RecipeSerializer<?> getSerializer() { return BasicFertilizer.SERIALIZER; }
    }
}
