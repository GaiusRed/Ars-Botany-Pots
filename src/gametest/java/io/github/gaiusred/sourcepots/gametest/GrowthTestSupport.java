package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import io.github.gaiusred.sourcepots.growth.BonemealGrowth;
import io.github.gaiusred.sourcepots.source.SourceMode;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.server.level.ServerLevel;

/** Test orchestration for standalone growth-adapter assertions. */
final class GrowthTestSupport {
    private GrowthTestSupport() {}

    static int growthAmount(BonemealGrowth growth, BotanyPotBlockEntity pot, ServerLevel level, SourceMode mode) {
        return growth.isEligible(pot, level)
                ? growth.sampleGrowth(SourcePotsConfig.snapshot().growth(mode), level.random) : 0;
    }

    static boolean apply(BonemealGrowth growth, BotanyPotBlockEntity pot, int amount) {
        return amount > 0 && pot.getLevel() instanceof ServerLevel server && growth.isEligible(pot, server)
                && growth.applyEligibleCredit(pot, amount) < amount;
    }
}
