package io.github.gaiusred.sourcepots.data;

import io.github.gaiusred.sourcepots.compat.starbuncle.StarbuncleDataCompatibility;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

/** Workaround for the pinned dependency's obsolete Patchouli datagen superclass. */
public final class PotDataCompatibility {
    public static void register(IEventBus modBus) {
        if (Boolean.getBoolean("sourcepots.datagen") && DatagenModLoader.isRunningDataGen()
                && ModList.get().isLoaded("starbunclemania")) {
            StarbuncleDataCompatibility.register(modBus);
        }
    }

    private PotDataCompatibility() {}
}
