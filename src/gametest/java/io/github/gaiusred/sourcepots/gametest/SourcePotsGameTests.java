package io.github.gaiusred.sourcepots.gametest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Development-only bootstrap; ordinary runs load only the production mod. */
@Mod("sourcepots_gametest")
public final class SourcePotsGameTests {
    public SourcePotsGameTests(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(GrowthConfigFixture::installRegressionProfile);
        NeoForge.EVENT_BUS.addListener(GrowthTestEnvironment::prepareTemplate);
        modBus.addListener(ResourceTestEnvironment::registerCapabilities);
    }
}
