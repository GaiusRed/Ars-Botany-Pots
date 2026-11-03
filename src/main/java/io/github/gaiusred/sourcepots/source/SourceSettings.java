package io.github.gaiusred.sourcepots.source;

import java.util.Objects;

public record SourceSettings(
        long jarSourceCost,
        int jarPaymentIntervalTicks,
        int jarPoweredDurationTicks,
        int jarBoostIntervalTicks,
        int jarSearchRange,
        ModeSettings liquid,
        ModeSettings gas,
        GrowthSettings jarGrowth,
        GrowthSettings liquidGrowth,
        GrowthSettings gasGrowth
) {
    /** Bounds the cubic Jar search to a safe, configurable radius. */
    public static final int MAX_JAR_SEARCH_RANGE = 16;

    public SourceSettings {
        requirePositiveBounded(jarSourceCost, Integer.MAX_VALUE, "jarSourceCost");
        requirePositive(jarPaymentIntervalTicks, "jarPaymentIntervalTicks");
        requirePositive(jarPoweredDurationTicks, "jarPoweredDurationTicks");
        requirePositive(jarBoostIntervalTicks, "jarBoostIntervalTicks");
        requirePositiveBounded(jarSearchRange, MAX_JAR_SEARCH_RANGE, "jarSearchRange");
        Objects.requireNonNull(liquid, "liquid");
        Objects.requireNonNull(gas, "gas");
        Objects.requireNonNull(jarGrowth, "jarGrowth");
        Objects.requireNonNull(liquidGrowth, "liquidGrowth");
        Objects.requireNonNull(gasGrowth, "gasGrowth");
        requirePositiveBounded(liquid.cost(), Integer.MAX_VALUE, "liquid.cost");
    }

    public static SourceSettings defaults() {
        return new SourceSettings(2500, 80, 100, 10, 3, new ModeSettings(500, 5), new ModeSettings(250, 1),
                new GrowthSettings(600, 800), new GrowthSettings(500, 700), new GrowthSettings(400, 600));
    }

    public GrowthSettings growth(SourceMode mode) {
        return switch (mode) {
            case JAR -> jarGrowth;
            case LIQUID -> liquidGrowth;
            case GAS -> gasGrowth;
        };
    }

    private static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requirePositiveBounded(long value, long maximum, String name) {
        requirePositive(value, name);
        if (value > maximum) {
            throw new IllegalArgumentException(name + " must be at most " + maximum);
        }
    }

    public record GrowthSettings(int minTicks, int maxTicks) {
        public GrowthSettings {
            requirePositive(minTicks, "growthMinTicks");
            requirePositive(maxTicks, "growthMaxTicks");
            if (minTicks > maxTicks) {
                throw new IllegalArgumentException("growthMinTicks must be at most growthMaxTicks");
            }
        }
    }

    public record ModeSettings(long cost, int intervalTicks) {
        public ModeSettings {
            requirePositive(cost, "cost");
            requirePositive(intervalTicks, "intervalTicks");
        }
    }
}
