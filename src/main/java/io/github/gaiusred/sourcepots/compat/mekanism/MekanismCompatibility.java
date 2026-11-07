package io.github.gaiusred.sourcepots.compat.mekanism;

import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity;
import io.github.gaiusred.sourcepots.registry.ModContent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Explicit bootstrap only; never an automatically loaded event subscriber. */
public final class MekanismCompatibility {
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), ModContent.POT_ENTITY.get(),
                (pot, side) -> side != null && side.getAxis().isHorizontal() && pot instanceof ArsBotanyPotBlockEntity ars && ars.sourceAccess() != null
                        && ars.sourceAccess().gasStorage() instanceof MekanismSourceStorage storage ? storage.handler(side) : null);
    }
    private MekanismCompatibility() {}
}
