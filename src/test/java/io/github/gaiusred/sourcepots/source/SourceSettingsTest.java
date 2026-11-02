package io.github.gaiusred.sourcepots.source;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceSettingsTest {
    @Test
    void defaultsMatchSpec() {
        SourceSettings settings = SourceSettings.defaults();

        assertAll(
                () -> assertEquals(600, settings.jarGrowth().minTicks()),
                () -> assertEquals(800, settings.jarGrowth().maxTicks()),
                () -> assertEquals(new SourceSettings.GrowthSettings(500, 700), settings.growth(SourceMode.LIQUID)),
                () -> assertEquals(new SourceSettings.GrowthSettings(400, 600), settings.growth(SourceMode.GAS)),
                () -> assertEquals(2500, settings.jarSourceCost()),
                () -> assertEquals(80, settings.jarPaymentIntervalTicks()),
                () -> assertEquals(100, settings.jarPoweredDurationTicks()),
                () -> assertEquals(10, settings.jarBoostIntervalTicks()),
                () -> assertEquals(3, settings.jarSearchRange()),
                () -> assertEquals(500, settings.liquid().cost()),
                () -> assertEquals(5, settings.liquid().intervalTicks()),
                () -> assertEquals(250, settings.gas().cost()),
                () -> assertEquals(1, settings.gas().intervalTicks())
        );
    }

    @Test
    void rejectsNonPositiveValues() {
        for (int invalid : new int[]{0, -1}) {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> settings(invalid, 80, 100, 20, 3, 250, 10, 125, 5)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> settings(2000, invalid, 100, 20, 3, 250, 10, 125, 5)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> settings(2000, 80, invalid, 20, 3, 250, 10, 125, 5)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> settings(2000, 80, 100, invalid, 3, 250, 10, 125, 5)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> settings(2000, 80, 100, 20, invalid, 250, 10, 125, 5)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> settings(2000, 80, 100, 20, 3, invalid, 10, 125, 5)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> settings(2000, 80, 100, 20, 3, 250, invalid, 125, 5)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> settings(2000, 80, 100, 20, 3, 250, 10, invalid, 5)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> settings(2000, 80, 100, 20, 3, 250, 10, 125, invalid))
            );
        }
    }

    @Test
    void rejectsUnsafeRangeAndFluidCapacity() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> settings(2000, 80, 100, 20, 17, 250, 10, 125, 5)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> settings(2000, 80, 100, 20, 3, (long) Integer.MAX_VALUE + 1, 10, 125, 5)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> settings((long) Integer.MAX_VALUE + 1, 80, 100, 20, 3, 250, 10, 125, 5))
        );
    }

    @Test
    void acceptsMaximumSupportedValues() {
        SourceSettings settings = settings(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, 16, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE);

        assertAll(
                () -> assertEquals(Integer.MAX_VALUE, settings.jarSourceCost()),
                () -> assertEquals(16, settings.jarSearchRange()),
                () -> assertEquals(Integer.MAX_VALUE, settings.liquid().cost()),
                () -> assertEquals(Long.MAX_VALUE, settings.gas().cost())
        );
    }

    @Test
    void growthRangeAcceptsEqualCustomAndMaximumBounds() {
        assertAll(
                () -> assertEquals(7, new SourceSettings.GrowthSettings(7, 7).minTicks()),
                () -> assertEquals(19, new SourceSettings.GrowthSettings(3, 19).maxTicks()),
                () -> assertEquals(Integer.MAX_VALUE,
                        new SourceSettings.GrowthSettings(1, Integer.MAX_VALUE).maxTicks())
        );
    }

    @Test
    void growthRangeRejectsNonpositiveAndReversedBounds() {
        for (int[] bounds : new int[][]{{0, 1}, {-1, 1}, {1, 0}, {1, -1}, {20, 19}}) {
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> new SourceSettings.GrowthSettings(bounds[0], bounds[1]));
            org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains("growth"));
        }
    }

    private static SourceSettings settings(long jarCost, int paymentInterval, int duration, int boostInterval,
                                           int range, long liquidCost, int liquidInterval, long gasCost, int gasInterval) {
        return new SourceSettings(jarCost, paymentInterval, duration, boostInterval, range,
                new SourceSettings.ModeSettings(liquidCost, liquidInterval),
                new SourceSettings.ModeSettings(gasCost, gasInterval), SourceSettings.defaults().jarGrowth(), SourceSettings.defaults().liquidGrowth(), SourceSettings.defaults().gasGrowth());
    }
}
