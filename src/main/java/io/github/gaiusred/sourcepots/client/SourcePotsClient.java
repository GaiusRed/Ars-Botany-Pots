package io.github.gaiusred.sourcepots.client;

import io.github.gaiusred.sourcepots.SourcePots;
import io.github.gaiusred.sourcepots.registry.ModContent;
import net.darkhax.botanypots.common.impl.block.BotanyPotRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SourcePots.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SourcePotsClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModContent.POT_ENTITY.get(), BotanyPotRenderer::new);
    }
    private SourcePotsClient() {}
}
