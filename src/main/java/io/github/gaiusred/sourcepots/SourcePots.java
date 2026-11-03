package io.github.gaiusred.sourcepots;

import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import io.github.gaiusred.sourcepots.data.PotDataCompatibility;
import io.github.gaiusred.sourcepots.data.PotDataProvider;
import io.github.gaiusred.sourcepots.registry.ModContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(SourcePots.MOD_ID)
public final class SourcePots {
    public static final String MOD_ID = "sourcepots";

    public SourcePots(IEventBus modBus, ModContainer container) {
        ModContent.register(modBus);
        modBus.addListener(ModContent::registerCapabilities);
        modBus.addListener(PotDataProvider::gatherData);
        PotDataCompatibility.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SourcePotsConfig.SPEC);
    }
}
