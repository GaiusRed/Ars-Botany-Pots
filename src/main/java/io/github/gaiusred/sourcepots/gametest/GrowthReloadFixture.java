package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.growth.BonemealGrowth;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.darkhax.botanypots.common.api.data.recipes.fertilizer.Fertilizer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Exercises live recipe eligibility through real reloads, then restores packs and in-memory config. */
final class GrowthReloadFixture {
    private final GameTestHelper helper;
    private final MinecraftServer server;
    private final BonemealGrowth growth;
    private final BotanyPotBlockEntity pot;
    private final List<String> enabled;
    private final Path directory;
    private final Path recipe;
    private final List<RecipeHolder<?>> originalRecipes;
    private final WorldDataConfiguration originalWorldConfiguration;
    private final Runnable afterReload;

    private GrowthReloadFixture(GameTestHelper helper, BonemealGrowth growth, BotanyPotBlockEntity pot, Runnable afterReload) throws IOException {
        this.helper = helper;
        this.server = helper.getLevel().getServer();
        this.growth = growth;
        this.pot = pot;
        this.afterReload = afterReload;
        this.enabled = List.copyOf(server.getPackRepository().getSelectedIds());
        this.originalRecipes = List.copyOf(server.getRecipeManager().getRecipes());
        this.originalWorldConfiguration = server.getWorldData().getDataConfiguration();
        this.directory = Files.createTempDirectory(server.getWorldPath(LevelResource.DATAPACK_DIR), "sourcepots-growth-");
        this.recipe = directory.resolve("data/botanypots/recipe/minecraft/fertilizer/bone_meal.json");
        prepareFiles(directory);
    }

    static void prepareFiles(Path directory) throws IOException {
        try {
            Files.createDirectories(directory.resolve("data/botanypots/recipe/minecraft/fertilizer"));
            Files.writeString(directory.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":48,\"description\":\"Source Pots development reload fixture\"}}");
        } catch (IOException failure) {
            try {
                deleteFixture(directory);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    static void run(GameTestHelper helper, BonemealGrowth growth, BotanyPotBlockEntity pot) {
        try {
            new GrowthReloadFixture(helper, growth, pot, () -> {}).run(false);
        } catch (IOException failure) {
            helper.fail("Could not prepare reload fixture: " + failure);
        }
    }

    static void runWithFailedRestoration(GameTestHelper helper, BonemealGrowth growth, BotanyPotBlockEntity pot) {
        try {
            new GrowthReloadFixture(helper, growth, pot, () -> {}).run(true);
        } catch (IOException failure) {
            helper.fail("Could not prepare restoration failure fixture: " + failure);
        }
    }

    static void runWithReloadAssertion(GameTestHelper helper, BonemealGrowth growth, BotanyPotBlockEntity pot, Runnable assertion) {
        try {
            new GrowthReloadFixture(helper, growth, pot, assertion).run(false);
        } catch (IOException failure) {
            helper.fail("Could not prepare reload fixture: " + failure);
        }
    }

    private void run(boolean forceRestorationFailure) {
        var growthConfig = new GrowthConfigFixture(17, 17);
        var packs = new ArrayList<>(enabled);
        packs.add("file/" + directory.getFileName());
        reload(packs, 500).thenRunAsync(() -> { assertAmount(17); afterReload.run(); }, server)
                .thenComposeAsync(ignored -> reload(packs, 100), server)
                .thenRunAsync(() -> { assertAmount(17); afterReload.run(); }, server)
                .thenComposeAsync(ignored -> reload(packs, 0), server)
                .thenRunAsync(() -> assertAmount(0), server)
                .thenComposeAsync(ignored -> reload(packs, 600, true), server)
                .thenRunAsync(() -> assertAmount(0), server)
                .thenComposeAsync(ignored -> reload(packs, 600), server)
                .thenRunAsync(() -> assertAmount(17), server)
                .whenCompleteAsync((ignored, failure) -> {
                    CompletableFuture<Void> restoration = forceRestorationFailure
                            ? CompletableFuture.failedFuture(new IOException("Deliberate failure before restoration installation"))
                            : server.reloadResources(enabled);
                    restoration.whenCompleteAsync((restored, restoreFailure) -> {
                        try {
                        // Also restore explicitly if the restoration reload itself fails.
                        if (restoreFailure != null) {
                            server.getPackRepository().setSelected(enabled);
                            server.getWorldData().setDataConfiguration(originalWorldConfiguration);
                            server.getRecipeManager().replaceRecipes(originalRecipes);
                            Fertilizer.CACHE.invalidate(helper.getLevel());
                            Fertilizer.RECIPES.invalidate(helper.getLevel());
                        }
                        var restoredSelection = List.copyOf(server.getPackRepository().getSelectedIds());
                        var restoredConfiguration = server.getWorldData().getDataConfiguration();
                        GrowthTestEnvironment.createTemplate(helper.getLevel());
                        try {
                            deleteFixture(directory);
                            server.getPackRepository().reload();
                        } catch (IOException cleanupFailure) {
                            if (restoreFailure == null) restoreFailure = cleanupFailure;
                        }
                        Throwable result = failure != null ? failure : restoreFailure;
                        if (forceRestorationFailure && failure == null) {
                            try {
                                helper.assertTrue(restoredSelection.equals(enabled), "Failed restoration must restore original selected pack IDs before deleting fixture");
                                helper.assertTrue(restoredConfiguration.equals(originalWorldConfiguration), "Failed restoration must restore original world datapack configuration");
                                helper.assertTrue(!Files.exists(directory), "Failed restoration must remove fixture directory");
                                result = null;
                            } catch (Throwable assertionFailure) {
                                result = assertionFailure;
                            } finally {
                                // The regression must not contaminate later batches while testing a broken fallback.
                                server.getPackRepository().setSelected(enabled);
                                server.getWorldData().setDataConfiguration(originalWorldConfiguration);
                            }
                        }
                        Throwable completionFailure = result;
                        if (completionFailure != null) helper.runAfterDelay(1, () -> helper.fail("Real recipe reload assertion failed: " + completionFailure));
                        else helper.succeed();
                        } finally {
                            growthConfig.close();
                        }
                    }, server);
                }, server);
    }

    private CompletableFuture<Void> reload(List<String> packs, int amount) {
        return reload(packs, amount, false);
    }

    private CompletableFuture<Void> reload(List<String> packs, int amount, boolean restrictSoil) {
        try {
            Files.writeString(recipe, "{\"type\":\"botanypots:fertilizer\",\"held_item\":{\"item\":\"minecraft:bone_meal\"},"
                    + (restrictSoil ? "\"soil_item\":{\"item\":\"minecraft:sand\"}," : "")
                    + "\"growth\":{\"type\":\"botanypots:constant\",\"amount\":" + amount + "}}");
            server.getPackRepository().reload();
            return server.reloadResources(packs);
        } catch (IOException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private void assertAmount(int expected) {
        GrowthTestEnvironment.createTemplate(helper.getLevel());
        pot.reset();
        pot.updateGrowthTime(0);
        int amount = growth.growthAmount(pot, helper.getLevel(), io.github.gaiusred.sourcepots.source.SourceMode.JAR);
        helper.assertTrue(amount == expected, "Real reload must acquire amount " + expected + ", got " + amount);
        helper.assertTrue(growth.apply(pot, amount) == (expected > 0), "Reloaded eligibility must control growth application");
        helper.assertTrue(pot.growthTime() == expected, "Reloaded delta must equal " + expected);
    }

    static void deleteFixture(Path directory) throws IOException {
        Path recipe = directory.resolve("data/botanypots/recipe/minecraft/fertilizer/bone_meal.json");
        Files.deleteIfExists(recipe);
        Files.deleteIfExists(directory.resolve("pack.mcmeta"));
        Path parent = recipe.getParent();
        while (!parent.equals(directory)) {
            Files.deleteIfExists(parent);
            parent = parent.getParent();
        }
        Files.deleteIfExists(directory);
    }
}
