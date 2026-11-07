package io.github.gaiusred.sourcepots.config;

import io.github.gaiusred.sourcepots.source.SourceSettings;
import java.util.List;
import java.util.ArrayList;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class SourcePotsConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue JAR_SOURCE_COST;
    private static final ModConfigSpec.IntValue JAR_PAYMENT_INTERVAL;
    private static final ModConfigSpec.IntValue JAR_POWERED_DURATION;
    private static final ModConfigSpec.IntValue JAR_BOOST_INTERVAL;
    private static final ModConfigSpec.IntValue JAR_SEARCH_RANGE;
    private static final ModConfigSpec.IntValue LIQUID_COST;
    private static final ModConfigSpec.IntValue LIQUID_CAPACITY;
    private static final ModConfigSpec.LongValue GAS_COST;
    private static final ModConfigSpec.LongValue GAS_CAPACITY;
    private static final List<TierValues> LIQUID_TIERS;
    private static final List<TierValues> GAS_TIERS;
    private static final ModConfigSpec.IntValue MAX_HARVESTS;
    private static final ModConfigSpec.IntValue JAR_GROWTH_MIN;
    private static final ModConfigSpec.IntValue JAR_GROWTH_MAX;

    static {
        SourceSettings defaults = SourceSettings.defaults();
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("jar");
        JAR_SOURCE_COST = positiveInt(builder, "sourceCost", (int) defaults.jarSourceCost(),
                "Source consumed for each Jar payment.");
        JAR_PAYMENT_INTERVAL = positiveInt(builder, "paymentIntervalTicks", defaults.jarPaymentIntervalTicks(),
                "Ticks between Jar payment attempts.");
        JAR_POWERED_DURATION = positiveInt(builder, "poweredDurationTicks", defaults.jarPoweredDurationTicks(),
                "Powered ticks after a successful Jar payment; each payment resets this window.");
        JAR_BOOST_INTERVAL = positiveInt(builder, "boostIntervalTicks", defaults.jarBoostIntervalTicks(),
                "Ticks between growth boosts while powered by a Jar.");
        JAR_SEARCH_RANGE = builder.comment("Jar search radius in blocks, including diagonals; maximum 16.")
                .worldRestart().defineInRange("searchRange", defaults.jarSearchRange(), 1,
                        SourceSettings.MAX_JAR_SEARCH_RANGE);
        JAR_GROWTH_MIN = positiveInt(builder, "growthMinTicks", defaults.jarGrowth().minTicks(),
                "Inclusive minimum growth ticks per jar boost; must form a positive ordered range.");
        JAR_GROWTH_MAX = positiveInt(builder, "growthMaxTicks", defaults.jarGrowth().maxTicks(),
                "Inclusive maximum growth ticks per jar boost; must form a positive ordered range.");
        builder.pop();

        builder.push("liquid");
        LIQUID_COST = positiveInt(builder, "costMbPerSide", (int) defaults.liquid().cost(), "Source liquid consumed per funded face in mB.");
        LIQUID_CAPACITY = positiveInt(builder, "bufferCapacityMbPerSide", (int) defaults.liquid().capacity(), "Liquid capacity per horizontal face; must cover one payment.");
        LIQUID_TIERS = tiers(builder, defaults.liquid());
        builder.pop();

        builder.push("gas");
        GAS_COST = builder.comment("Source gas consumed per funded face in native chemical units.")
                .worldRestart().defineInRange("costUnitsPerSide", defaults.gas().cost(), 1L, Long.MAX_VALUE);
        GAS_CAPACITY = builder.comment("Gas capacity per horizontal face; must cover one payment.")
                .worldRestart().defineInRange("bufferCapacityUnitsPerSide", defaults.gas().capacity(), 1L, Long.MAX_VALUE);
        GAS_TIERS = tiers(builder, defaults.gas());
        builder.pop();
        builder.push("growth");
        MAX_HARVESTS = builder.comment("Maximum total harvests per pot per real tick.").worldRestart()
                .defineInRange("maxHarvestsPerTick", defaults.maxHarvestsPerTick(), 1, 256);
        builder.pop();

        SPEC = builder.build();
    }

    private SourcePotsConfig() {
    }

    public static SourceSettings snapshot() {
        return new SourceSettings(JAR_SOURCE_COST.get(), JAR_PAYMENT_INTERVAL.get(), JAR_POWERED_DURATION.get(),
                JAR_BOOST_INTERVAL.get(), JAR_SEARCH_RANGE.get(),
                new SourceSettings.ModeSettings(LIQUID_COST.get(), LIQUID_CAPACITY.get(), LIQUID_TIERS.stream().map(TierValues::snapshot).toList()),
                new SourceSettings.ModeSettings(GAS_COST.get(), GAS_CAPACITY.get(), GAS_TIERS.stream().map(TierValues::snapshot).toList()),
                new SourceSettings.GrowthSettings(JAR_GROWTH_MIN.get(), JAR_GROWTH_MAX.get()), MAX_HARVESTS.get());
    }

    private static List<TierValues> tiers(ModConfigSpec.Builder builder, SourceSettings.ModeSettings defaults) {
        var result = new ArrayList<TierValues>();
        int count = 1;
        for (String name : List.of("oneSide", "twoSides", "threeSides", "fourSides")) {
            var tier = defaults.tier(count++);
            builder.push(name);
            result.add(new TierValues(
                    positiveInt(builder, "boostIntervalTicks", tier.intervalTicks(), "Ticks between aggregate boosts."),
                    positiveInt(builder, "growthMinTicks", tier.growth().minTicks(), "Inclusive minimum aggregate growth ticks; must be at most maximum."),
                    positiveInt(builder, "growthMaxTicks", tier.growth().maxTicks(), "Inclusive maximum aggregate growth ticks.")));
            builder.pop();
        }
        return List.copyOf(result);
    }

    private record TierValues(ModConfigSpec.IntValue interval, ModConfigSpec.IntValue min, ModConfigSpec.IntValue max) {
        SourceSettings.TierSettings snapshot() {
            return new SourceSettings.TierSettings(interval.get(), new SourceSettings.GrowthSettings(min.get(), max.get()));
        }
    }

    private static ModConfigSpec.IntValue positiveInt(ModConfigSpec.Builder builder, String key, int defaultValue,
                                                     String description) {
        return builder.comment(description).worldRestart().defineInRange(key, defaultValue, 1, Integer.MAX_VALUE);
    }
}
