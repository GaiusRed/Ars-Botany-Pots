package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import io.github.gaiusred.sourcepots.source.SourceSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.List;

/** Changes only isolated GameTest values, including all tier caches, and restores them on close. */
final class GrowthConfigFixture implements AutoCloseable {
    private final List<ModConfigSpec.IntValue> values = new ArrayList<>();
    private final List<Integer> previous = new ArrayList<>();

    private final List<Runnable> settingRestorations = new ArrayList<>();

    GrowthConfigFixture(SourceSettings settings) {
        configure("jar.sourceCost", (int) settings.jarSourceCost());
        configure("jar.paymentIntervalTicks", settings.jarPaymentIntervalTicks());
        configure("jar.poweredDurationTicks", settings.jarPoweredDurationTicks());
        configure("jar.boostIntervalTicks", settings.jarBoostIntervalTicks());
        configure("jar.searchRange", settings.jarSearchRange());
        configure("jar.growthMinTicks", settings.jarGrowth().minTicks());
        configure("jar.growthMaxTicks", settings.jarGrowth().maxTicks());
        configure("growth.maxHarvestsPerTick", settings.maxHarvestsPerTick());
        configure("liquid.costMbPerSide", (int) settings.liquid().cost());
        configure("liquid.bufferCapacityMbPerSide", (int) settings.liquid().capacity());
        configure("gas.costUnitsPerSide", settings.gas().cost());
        configure("gas.bufferCapacityUnitsPerSide", settings.gas().capacity());
        for (String mode : List.of("liquid", "gas")) {
            var tiers = mode.equals("liquid") ? settings.liquid() : settings.gas();
            int sides = 1;
            for (String name : List.of("oneSide", "twoSides", "threeSides", "fourSides")) {
                var tier = tiers.tier(sides++);
                String key = mode + "." + name + ".";
                configure(key + "boostIntervalTicks", tier.intervalTicks());
                configure(key + "growthMinTicks", tier.growth().minTicks());
                configure(key + "growthMaxTicks", tier.growth().maxTicks());
            }
        }
    }

    private <T> void configure(String key, T replacement) {
        ModConfigSpec.ConfigValue<T> value = SourcePotsConfig.SPEC.getValues().get(key);
        T original = value.get();
        settingRestorations.add(() -> { value.set(original); value.clearCache(); });
        value.set(replacement);
        value.clearCache();
    }

    /** Fast timings keep accounting and multi-harvest regression cases short and explicit. */
    static void installRegressionProfile(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        if (!(event.getServer() instanceof net.minecraft.gametest.framework.GameTestServer)) return;
        new GrowthConfigFixture(regressionSettings());
    }

    static SourceSettings regressionSettings() {
        return new SourceSettings(2500, 80, 100, 10, 3,
                regressionMode(500, 5, 500, 700), regressionMode(250, 1, 400, 600),
                new SourceSettings.GrowthSettings(600, 800), 16);
    }

    private static SourceSettings.ModeSettings regressionMode(int cost, int interval, int min, int max) {
        return new SourceSettings.ModeSettings(cost, cost, java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(n -> new SourceSettings.TierSettings(interval, new SourceSettings.GrowthSettings(min * n, max * n))).toList());
    }

    GrowthConfigFixture(int minimum, int maximum) {
        this(minimum, maximum, minimum, maximum, minimum, maximum);
    }

    GrowthConfigFixture(int jarMin, int jarMax, int liquidMin, int liquidMax, int gasMin, int gasMax) {
        for (String mode : List.of("jar", "liquid", "gas")) {
            for (String tier : mode.equals("jar") ? List.of("") : List.of("oneSide.", "twoSides.", "threeSides.", "fourSides.")) {
                for (String bound : List.of("Min", "Max")) {
                    ModConfigSpec.IntValue value = SourcePotsConfig.SPEC.getValues().get(mode + "." + tier + "growth" + bound + "Ticks");
                    previous.add(value.get());
                    values.add(value);
                    int min = mode.equals("jar") ? jarMin : mode.equals("liquid") ? liquidMin : gasMin;
                    int max = mode.equals("jar") ? jarMax : mode.equals("liquid") ? liquidMax : gasMax;
                    set(values.size() - 1, bound.equals("Min") ? min : max);
                }
            }
        }
    }

    public void close() {
        for (int i = 0; i < values.size(); i++) set(i, previous.get(i));
        settingRestorations.forEach(Runnable::run);
    }

    GrowthConfigFixture harvestLimit(int limit) {
        ModConfigSpec.IntValue value = SourcePotsConfig.SPEC.getValues().get("growth.maxHarvestsPerTick");
        previous.add(value.get());
        values.add(value);
        set(values.size() - 1, limit);
        return this;
    }

    private void set(int index, int value) {
        values.get(index).set(value);
        values.get(index).clearCache();
    }
}
