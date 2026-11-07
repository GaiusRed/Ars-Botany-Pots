package io.github.gaiusred.sourcepots.source;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceSettingsTest {
    @Test void defaultsMatchAllNineTiers() {
        var settings = SourceSettings.defaults();
        assertEquals(2500, settings.jarSourceCost());
        assertEquals(80, settings.jarPaymentIntervalTicks());
        assertEquals(100, settings.jarPoweredDurationTicks());
        assertEquals(10, settings.jarBoostIntervalTicks());
        assertEquals(3, settings.jarSearchRange());
        assertEquals(new SourceSettings.GrowthSettings(15, 15), settings.tier(SourceMode.JAR, 0).growth());
        assertEquals(1000, settings.liquid().cost());
        assertEquals(1000, settings.liquid().capacity());
        assertEquals(60, settings.gas().cost());
        assertEquals(60, settings.gas().capacity());
        int[] liquidGrowth = {240, 390, 541, 691};
        int[] gasGrowth = {541, 751, 931, 1141};
        for (int n = 1; n <= 4; n++) {
            assertEquals(new SourceSettings.TierSettings(60, new SourceSettings.GrowthSettings(liquidGrowth[n-1], liquidGrowth[n-1])), settings.tier(SourceMode.LIQUID, n));
            assertEquals(new SourceSettings.TierSettings(60, new SourceSettings.GrowthSettings(gasGrowth[n-1], gasGrowth[n-1])), settings.tier(SourceMode.GAS, n));
        }
        assertEquals(16, settings.maxHarvestsPerTick());
    }

    @Test void defaultCostsMatchMeasuredGeneratorBudgets() {
        var settings = SourceSettings.defaults();
        double liquidPerTick = (double) settings.liquid().cost() / settings.liquid().tier(1).intervalTicks();
        double gasPerTick = (double) settings.gas().cost() / settings.gas().tier(1).intervalTicks();
        assertEquals(1.5, 25.0 / liquidPerTick, 1e-9);
        assertEquals(1.0, 1.0 / gasPerTick, 1e-9);
        assertEquals(375000, settings.jarSourceCost() * 12000 / settings.jarPaymentIntervalTicks());
    }

    @Test void validatesCostsCapacitiesTierCountsAndRanges() {
        var tiers = SourceSettings.defaults().gas().tiers();
        assertEquals(1500, new SourceSettings.ModeSettings(500, 1500, tiers).capacity());
        assertEquals(Long.MAX_VALUE, new SourceSettings.ModeSettings(Long.MAX_VALUE, Long.MAX_VALUE, tiers).cost());
        for (long invalid : new long[]{0, -1}) {
            assertThrows(IllegalArgumentException.class, () -> new SourceSettings.ModeSettings(invalid, 500, tiers));
            assertThrows(IllegalArgumentException.class, () -> new SourceSettings.ModeSettings(1, invalid, tiers));
        }
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings.ModeSettings(501, 500, tiers));
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings.ModeSettings(1, 1, tiers.subList(0, 3)));
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings.ModeSettings(1, 1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings.ModeSettings(1, 1, java.util.Collections.nCopies(5, tiers.getFirst())));
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings.TierSettings(0, tiers.getFirst().growth()));
        for (int[] bounds : new int[][]{{0, 1}, {-1, 1}, {1, 0}, {20, 19}})
            assertThrows(IllegalArgumentException.class, () -> new SourceSettings.GrowthSettings(bounds[0], bounds[1]));
        assertEquals(Integer.MAX_VALUE, new SourceSettings.GrowthSettings(1, Integer.MAX_VALUE).maxTicks());
        for (int n : new int[]{0, 5}) assertThrows(IllegalArgumentException.class, () -> SourceSettings.defaults().gas().tier(n));
        assertThrows(IllegalArgumentException.class, () -> SourceSettings.defaults().tier(SourceMode.JAR, 1));
        var mutable = new ArrayList<>(tiers);
        var mode = new SourceSettings.ModeSettings(1, 2, mutable);
        mutable.clear();
        assertEquals(4, mode.tiers().size());
        assertThrows(UnsupportedOperationException.class, () -> mode.tiers().clear());
    }

    @Test void validatesSnapshotLimits() {
        for (int n : new int[]{1, 256}) assertEquals(n, snapshot(SourceSettings.defaults().liquid(), n).maxHarvestsPerTick());
        for (int n : new int[]{0, 257}) assertThrows(IllegalArgumentException.class, () -> snapshot(SourceSettings.defaults().liquid(), n));
        var gas = SourceSettings.defaults().gas();
        assertThrows(IllegalArgumentException.class, () -> snapshot(new SourceSettings.ModeSettings(1, (long)Integer.MAX_VALUE+1, gas.tiers()), 16));
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings(1,1,1,1,17,gas,gas,gas.tier(1).growth(),16));
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings(0,1,1,1,1,gas,gas,gas.tier(1).growth(),16));
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings(1,0,1,1,1,gas,gas,gas.tier(1).growth(),16));
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings(1,1,0,1,1,gas,gas,gas.tier(1).growth(),16));
        assertThrows(IllegalArgumentException.class, () -> new SourceSettings(1,1,1,0,1,gas,gas,gas.tier(1).growth(),16));
    }
    private SourceSettings snapshot(SourceSettings.ModeSettings liquid, int limit) {
        var d = SourceSettings.defaults();
        return new SourceSettings(d.jarSourceCost(),80,100,10,3,liquid,d.gas(),d.jarGrowth(),limit);
    }
}
