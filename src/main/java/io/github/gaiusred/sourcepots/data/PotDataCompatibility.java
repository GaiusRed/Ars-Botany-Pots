package io.github.gaiusred.sourcepots.data;

import alexthw.starbunclemania.datagen.Setup;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Workaround for the pinned dependency's obsolete Patchouli datagen superclass. */
public final class PotDataCompatibility {
    public static void register(IEventBus modBus) {
        if (Boolean.getBoolean("sourcepots.datagen") && DatagenModLoader.isRunningDataGen()) {
            modBus.addListener(PotDataCompatibility::beforeBlockRegistration);
        }
    }

    private static void beforeBlockRegistration(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK)) return;
        // All automatic subscribers are injected before registry initialization. NeoForge
        // dispatches GatherDataEvent to every mod, including generators excluded by --mod.
        ModList.get().getModContainerById("starbunclemania").orElseThrow()
                .getEventBus().unregister(Setup.class);
        LogUtils.getLogger().info("Source Pots datagen: excluded Starbunclemania's incompatible datagen Setup listener");
    }

    private PotDataCompatibility() {}
}
