package io.github.gaiusred.sourcepots;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHooks;
import io.github.gaiusred.sourcepots.gametest.GrowthTestEnvironment;
import io.github.gaiusred.sourcepots.gametest.ResourceTestEnvironment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import io.github.gaiusred.sourcepots.registry.ModContent;

@Mod(SourcePots.MOD_ID)
public final class SourcePots {
    public static final String MOD_ID = "sourcepots";

    public SourcePots(IEventBus modBus, ModContainer container) {
        ModContent.register(modBus);
        modBus.addListener(ModContent::registerCapabilities);
        modBus.addListener(io.github.gaiusred.sourcepots.data.PotDataProvider::gatherData);
        io.github.gaiusred.sourcepots.data.PotDataCompatibility.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SourcePotsConfig.SPEC);
        if (GameTestHooks.isGametestEnabled()) {
            NeoForge.EVENT_BUS.addListener(GrowthTestEnvironment::prepareTemplate);
            modBus.addListener(ResourceTestEnvironment::registerCapabilities);
        }
    }
}
