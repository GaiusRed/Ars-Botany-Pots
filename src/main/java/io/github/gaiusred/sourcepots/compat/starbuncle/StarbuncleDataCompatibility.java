package io.github.gaiusred.sourcepots.compat.starbuncle;

import alexthw.starbunclemania.datagen.Setup;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Datagen-only bootstrap, invoked only when Starbunclemania is installed. */
public final class StarbuncleDataCompatibility {
    public static void register(IEventBus modBus) {
        modBus.addListener(StarbuncleDataCompatibility::beforeBlockRegistration);
    }

    private static void beforeBlockRegistration(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK)) return;
        // All subscribers are injected before registry initialization. NeoForge sends
        // GatherDataEvent even to mods excluded by --mod.
        ModList.get().getModContainerById("starbunclemania").ifPresent(container -> {
            container.getEventBus().unregister(Setup.class);
            LogUtils.getLogger().info("Source Pots datagen: excluded Starbunclemania's incompatible datagen Setup listener");
        });
    }

    private StarbuncleDataCompatibility() {}
}
