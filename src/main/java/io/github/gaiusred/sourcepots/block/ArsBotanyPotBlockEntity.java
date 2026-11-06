package io.github.gaiusred.sourcepots.block;

import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import io.github.gaiusred.sourcepots.registry.ModContent;
import io.github.gaiusred.sourcepots.source.PotSourceAccess;
import io.github.gaiusred.sourcepots.source.SourceState;
import io.github.gaiusred.sourcepots.source.SourceScheduler;
import io.github.gaiusred.sourcepots.growth.BonemealGrowth;
import com.mojang.logging.LogUtils;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ArsBotanyPotBlockEntity extends BotanyPotBlockEntity {
    private static final BonemealGrowth GROWTH = new BonemealGrowth();
    private static final SourceScheduler SCHEDULER = new SourceScheduler();
    private final SourceState sourceState = new SourceState();
    private PotSourceAccess source;
    private CompoundTag pendingPower = new CompoundTag();
    private boolean inSourceTick;
    private boolean tickEligible;
    // At most the unspent portion of one paid boost; double preserves float growth fractions.
    private double growthCredit;
    private boolean sourceAssisted;

    public ArsBotanyPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.POT_ENTITY, pos, state);
    }

    public PotSourceAccess sourceAccess() {
        if (source == null && level instanceof ServerLevel server) {
            source = new PotSourceAccess(server, worldPosition, SourcePotsConfig.snapshot(),
                    () -> !isRemoved() && (inSourceTick ? tickEligible : canBuyGrowth(server)), this::setChanged);
            restorePendingBuffers();
        }
        return source;
    }

    private void restorePendingBuffers() {
        if (source.restoreBuffers(savedLong(pendingPower, "liquid"), savedLong(pendingPower, "gas"))) {
            // Once per restoration, never per tick or capability query.
            LogUtils.getLogger().warn("Clamped saved Source buffers at {} to configured capacities (liquid {}, gas {})",
                    worldPosition, source.fluidHandler().getTankCapacity(0), source.gasCapacity());
        }
        pendingPower = new CompoundTag();
    }

    public static void tickPot(Level level, BlockPos pos, BlockState state, BotanyPotBlockEntity pot) {
        if (pot.isRemoved() || pot.getLevel() == null) return;
        float before = pot.growthTime();
        BotanyPotBlockEntity.tickPot(level, pos, state, pot);
        if (level instanceof ServerLevel server && pot instanceof ArsBotanyPotBlockEntity ars) {
            // Native hopper harvest resets growth directly, whereas crop/soil changes call reset().
            if (ars.sourceAssisted && before >= 0 && ars.growthTime() == -1) {
                ars.growCooldown.setTicks(0);
                ars.sourceAssisted = false;
            }
            ars.tickSource(server);
        }
    }

    private boolean canBuyGrowth(ServerLevel server) {
        return GROWTH.isEligible(this, server)
                && growthCredit < (double) getRequiredGrowthTicks() - growthTime();
    }

    private void applyRetainedGrowth() {
        double remaining = GROWTH.applyEligibleCredit(this, growthCredit);
        if (remaining < growthCredit) sourceAssisted = true;
        growthCredit = remaining;
    }

    private void tickSource(ServerLevel server) {
        // Validate recipe growth once before payment; random recipe amounts must not reroll after charging.
        var settings = SourcePotsConfig.snapshot();
        boolean eligible = GROWTH.isEligible(this, server);
        if (eligible) applyRetainedGrowth();
        tickEligible = eligible && growthCredit < (double) getRequiredGrowthTicks() - growthTime();
        inSourceTick = true;
        try {
            var access = sourceAccess();
            access.pullAdjacent();
            var authorizedMode = SCHEDULER.tick(sourceState, settings, tickEligible, access);
            if (authorizedMode != null) {
                // Retained credit is spent first. A payment can only occur after it is exhausted.
                growthCredit = GROWTH.sampleGrowth(settings.growth(authorizedMode), server.random);
                applyRetainedGrowth();
            }
            // Timers advance even on inactive crops and must be saved on unload.
            setChanged();
        } finally {
            inSourceTick = false;
        }
    }

    @Override
    public void reset() {
        super.reset();
        growthCredit = 0;
        sourceAssisted = false;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pendingPower = tag.getCompound("sourcepots").copy();
        double savedCredit = pendingPower.getDouble("growth_credit");
        growthCredit = Double.isFinite(savedCredit) ? Math.clamp(savedCredit, 0, Integer.MAX_VALUE) : 0;
        sourceAssisted = pendingPower.getBoolean("source_assisted");
        sourceState.restore(savedLong(pendingPower, "jar_remaining"), savedLong(pendingPower, "jar_retry"),
                savedLong(pendingPower, "boost_cooldown"));
        // Keep capability views held by neighboring machines attached to the restored storage.
        if (source != null) restorePendingBuffers();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var access = sourceAccess();
        var power = access == null ? pendingPower.copy() : new CompoundTag();
        if (access != null) {
            power.putLong("liquid", access.storedLiquid());
            power.putLong("gas", access.storedGas());
        }
        power.putDouble("growth_credit", growthCredit);
        power.putBoolean("source_assisted", sourceAssisted);
        power.putLong("jar_remaining", sourceState.jarRemainingTicks());
        power.putLong("jar_retry", sourceState.jarRetryCooldownTicks());
        power.putLong("boost_cooldown", sourceState.boostCooldownTicks());
        tag.put("sourcepots", power);
    }

    private static long savedLong(CompoundTag tag, String key) {
        return tag.get(key) instanceof NumericTag number ? number.getAsLong() : 0;
    }
}
