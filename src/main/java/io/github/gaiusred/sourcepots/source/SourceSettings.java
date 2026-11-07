package io.github.gaiusred.sourcepots.source;

import java.util.Objects;
import java.util.List;
import java.util.stream.IntStream;

public record SourceSettings(
        long jarSourceCost,
        int jarPaymentIntervalTicks,
        int jarPoweredDurationTicks,
        int jarBoostIntervalTicks,
        int jarSearchRange,
        ModeSettings liquid,
        ModeSettings gas,
        GrowthSettings jarGrowth,
        int maxHarvestsPerTick
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
        requirePositiveBounded(maxHarvestsPerTick, 256, "maxHarvestsPerTick");
        requirePositiveBounded(liquid.capacity(), Integer.MAX_VALUE, "liquid.capacity");
    }

    public static SourceSettings defaults() {
        return new SourceSettings(2500, 80, 100, 10, 3,
                new ModeSettings(500, 500, defaultTiers(5, 500, 700)),
                new ModeSettings(250, 250, defaultTiers(1, 400, 600)), new GrowthSettings(600, 800), 16);
    }

    private static List<TierSettings> defaultTiers(int interval, int min, int max) {
        return IntStream.rangeClosed(1, 4)
                .mapToObj(n -> new TierSettings(interval, new GrowthSettings(min * n, max * n))).toList();
    }

    public TierSettings tier(SourceMode mode, int fundedSides) {
        return switch (mode) {
            case JAR -> {
                if (fundedSides != 0) throw new IllegalArgumentException("Jar has no funded sides");
                yield new TierSettings(jarBoostIntervalTicks, jarGrowth);
            }
            case LIQUID -> liquid.tier(fundedSides);
            case GAS -> gas.tier(fundedSides);
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

    public record TierSettings(int intervalTicks, GrowthSettings growth) {
        public TierSettings {
            requirePositive(intervalTicks, "intervalTicks");
            Objects.requireNonNull(growth, "growth");
        }
    }

    public record ModeSettings(long cost, long capacity, List<TierSettings> tiers) {
        public ModeSettings {
            requirePositive(cost, "cost");
            requirePositive(capacity, "capacity");
            if (capacity < cost) throw new IllegalArgumentException("capacity must cover cost");
            tiers = List.copyOf(tiers);
            if (tiers.size() != 4) throw new IllegalArgumentException("Exactly four tiers are required");
        }

        public TierSettings tier(int fundedSides) {
            if (fundedSides < 1 || fundedSides > 4) throw new IllegalArgumentException("fundedSides must be 1 through 4");
            return tiers.get(fundedSides - 1);
        }
    }
}
