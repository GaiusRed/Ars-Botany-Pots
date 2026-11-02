package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.List;

/** Changes only isolated GameTest values, including all six caches, and restores them on close. */
final class GrowthConfigFixture implements AutoCloseable {
    private final List<ModConfigSpec.IntValue> values = new ArrayList<>();
    private final List<Integer> previous = new ArrayList<>();

    GrowthConfigFixture(int minimum, int maximum) {
        this(minimum, maximum, minimum, maximum, minimum, maximum);
    }

    GrowthConfigFixture(int jarMin, int jarMax, int liquidMin, int liquidMax, int gasMin, int gasMax) {
        int[] replacements = {jarMin, jarMax, liquidMin, liquidMax, gasMin, gasMax};
        for (String mode : List.of("jar", "liquid", "gas")) {
            for (String bound : List.of("Min", "Max")) {
                ModConfigSpec.IntValue value = SourcePotsConfig.SPEC.getValues().get(mode + ".growth" + bound + "Ticks");
                previous.add(value.get());
                values.add(value);
            }
        }
        for (int i = 0; i < values.size(); i++) set(i, replacements[i]);
    }

    public void close() {
        for (int i = 0; i < values.size(); i++) set(i, previous.get(i));
    }

    private void set(int index, int value) {
        values.get(index).set(value);
        values.get(index).clearCache();
    }
}
