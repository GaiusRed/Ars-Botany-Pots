package io.github.gaiusred.sourcepots.source;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;

public final class JarSourceAccess {
    public boolean canPay(ServerLevel level, BlockPos pos, SourceSettings settings) {
        return SourceUtil.hasSourceNearby(pos, level, settings.jarSearchRange(), Math.toIntExact(settings.jarSourceCost()));
    }
    public boolean pay(ServerLevel level, BlockPos pos, SourceSettings settings) {
        return SourceUtil.takeSourceMultipleWithParticles(pos, level, settings.jarSearchRange(), Math.toIntExact(settings.jarSourceCost())) != null;
    }
}
