package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.List;

/** Changes only isolated GameTest values, including all tier caches, and restores them on close. */
final class GrowthConfigFixture implements AutoCloseable {
    private final List<ModConfigSpec.IntValue> values = new ArrayList<>();
    private final List<Integer> previous = new ArrayList<>();

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
