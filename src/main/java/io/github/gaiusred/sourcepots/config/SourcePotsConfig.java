package io.github.gaiusred.sourcepots.config;

import io.github.gaiusred.sourcepots.source.SourceSettings;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class SourcePotsConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue JAR_SOURCE_COST;
    private static final ModConfigSpec.IntValue JAR_PAYMENT_INTERVAL;
    private static final ModConfigSpec.IntValue JAR_POWERED_DURATION;
    private static final ModConfigSpec.IntValue JAR_BOOST_INTERVAL;
    private static final ModConfigSpec.IntValue JAR_SEARCH_RANGE;
    private static final ModConfigSpec.IntValue LIQUID_COST;
    private static final ModConfigSpec.IntValue LIQUID_BOOST_INTERVAL;
    private static final ModConfigSpec.LongValue GAS_COST;
    private static final ModConfigSpec.IntValue GAS_BOOST_INTERVAL;

    private static final ModConfigSpec.IntValue JAR_GROWTH_MIN;
    private static final ModConfigSpec.IntValue JAR_GROWTH_MAX;
    private static final ModConfigSpec.IntValue LIQUID_GROWTH_MIN;
    private static final ModConfigSpec.IntValue LIQUID_GROWTH_MAX;
    private static final ModConfigSpec.IntValue GAS_GROWTH_MIN;
    private static final ModConfigSpec.IntValue GAS_GROWTH_MAX;

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
        LIQUID_COST = positiveInt(builder, "costMb", (int) defaults.liquid().cost(),
                "Source liquid consumed per boost in mB; also the liquid buffer capacity.");
        LIQUID_BOOST_INTERVAL = positiveInt(builder, "boostIntervalTicks", defaults.liquid().intervalTicks(),
                "Ticks between growth boosts paid with Source liquid.");
        LIQUID_GROWTH_MIN = positiveInt(builder, "growthMinTicks", defaults.liquidGrowth().minTicks(),
                "Inclusive minimum growth ticks per liquid boost; must form a positive ordered range.");
        LIQUID_GROWTH_MAX = positiveInt(builder, "growthMaxTicks", defaults.liquidGrowth().maxTicks(),
                "Inclusive maximum growth ticks per liquid boost; must form a positive ordered range.");
        builder.pop();

        builder.push("gas");
        GAS_COST = builder.comment("Source gas consumed per boost in units; also the gas buffer capacity.")
                .worldRestart().defineInRange("costUnits", defaults.gas().cost(), 1L, Long.MAX_VALUE);
        GAS_BOOST_INTERVAL = positiveInt(builder, "boostIntervalTicks", defaults.gas().intervalTicks(),
                "Ticks between growth boosts paid with Source gas.");
        GAS_GROWTH_MIN = positiveInt(builder, "growthMinTicks", defaults.gasGrowth().minTicks(),
                "Inclusive minimum growth ticks per gas boost; must form a positive ordered range.");
        GAS_GROWTH_MAX = positiveInt(builder, "growthMaxTicks", defaults.gasGrowth().maxTicks(),
                "Inclusive maximum growth ticks per gas boost; must form a positive ordered range.");
        builder.pop();

        SPEC = builder.build();
    }

    private SourcePotsConfig() {
    }

    public static SourceSettings snapshot() {
        return new SourceSettings(JAR_SOURCE_COST.get(), JAR_PAYMENT_INTERVAL.get(), JAR_POWERED_DURATION.get(),
                JAR_BOOST_INTERVAL.get(), JAR_SEARCH_RANGE.get(),
                new SourceSettings.ModeSettings(LIQUID_COST.get(), LIQUID_BOOST_INTERVAL.get()),
                new SourceSettings.ModeSettings(GAS_COST.get(), GAS_BOOST_INTERVAL.get()),
                new SourceSettings.GrowthSettings(JAR_GROWTH_MIN.get(), JAR_GROWTH_MAX.get()),
                new SourceSettings.GrowthSettings(LIQUID_GROWTH_MIN.get(), LIQUID_GROWTH_MAX.get()),
                new SourceSettings.GrowthSettings(GAS_GROWTH_MIN.get(), GAS_GROWTH_MAX.get()));
    }

    private static ModConfigSpec.IntValue positiveInt(ModConfigSpec.Builder builder, String key, int defaultValue,
                                                     String description) {
        return builder.comment(description).worldRestart().defineInRange(key, defaultValue, 1, Integer.MAX_VALUE);
    }
}
