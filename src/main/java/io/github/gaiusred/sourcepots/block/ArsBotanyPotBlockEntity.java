package io.github.gaiusred.sourcepots.block;

import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import io.github.gaiusred.sourcepots.registry.ModContent;
import io.github.gaiusred.sourcepots.source.PotSourceAccess;
import io.github.gaiusred.sourcepots.source.SourceState;
import io.github.gaiusred.sourcepots.source.SourceScheduler;
import io.github.gaiusred.sourcepots.growth.BonemealGrowth;
import com.mojang.logging.LogUtils;
import net.darkhax.bookshelf.common.api.data.enchantment.EnchantmentLevel;
import net.darkhax.bookshelf.common.api.service.Services;
import net.darkhax.botanypots.common.impl.BotanyPotsMod;
import net.darkhax.botanypots.common.impl.Helpers;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public final class ArsBotanyPotBlockEntity extends BotanyPotBlockEntity {
    private static final BonemealGrowth GROWTH = new BonemealGrowth();
    private static final SourceScheduler SCHEDULER = new SourceScheduler();
    // Before a top-up: credit < required - progress <= MAX_VALUE + 1 (native progress starts at -1).
    // One purchase adds at most MAX_VALUE; keep that bounded envelope across save/load.
    private static final double MAX_GROWTH_CREDIT = 2d * Integer.MAX_VALUE + 1;
    private final SourceState sourceState = new SourceState();
    private PotSourceAccess source;
    private CompoundTag pendingPower = new CompoundTag();
    private boolean inSourceTick;
    private boolean tickEligible;
    private boolean tickingNative;
    private boolean nativeMature;
    private boolean harvestLimitReached;
    // Unspent paid growth includes residuals too small to advance native float progress.
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
        if (source.restoreBuffers(pendingPower)) {
            // Once per restoration, never per tick or capability query.
            LogUtils.getLogger().warn("Clamped saved Source buffers at {} to configured capacities (liquid {}, gas {})",
                    worldPosition, source.fluidHandler(net.minecraft.core.Direction.NORTH).getTankCapacity(0), source.gasCapacity());
        }
        pendingPower = new CompoundTag();
    }

    public static void tickPot(Level level, BlockPos pos, BlockState state, BotanyPotBlockEntity pot) {
        if (pot.isRemoved() || pot.getLevel() == null) return;
        if (level instanceof ServerLevel server && pot instanceof ArsBotanyPotBlockEntity ars) {
            ars.harvestLimitReached = false;
            ars.nativeMature = false;
            ars.tickingNative = true;
            try {
                BotanyPotBlockEntity.tickPot(level, pos, state, pot);
            } finally {
                ars.tickingNative = false;
            }
            // Pinned native maturity always calls updateComparatorLevel(15); a harvest then resets to -1.
            // Starting progress is insufficient: crop.onTick can mature a crop starting at -1.
            boolean harvested = ars.nativeMature && ars.growthTime() == -1;
            if (ars.sourceAssisted && harvested) {
                ars.growCooldown.setTicks(0);
                ars.sourceAssisted = false;
            }
            ars.tickSource(server, harvested ? 1 : 0);
        } else {
            BotanyPotBlockEntity.tickPot(level, pos, state, pot);
        }
    }

    @Override
    public void updateComparatorLevel(int newLevel) {
        if (tickingNative && newLevel == 15) nativeMature = true;
        super.updateComparatorLevel(newLevel);
    }

    private boolean canBuyGrowth(ServerLevel server) {
        return !harvestLimitReached && hasStorageRoom() && GROWTH.isEligible(this, server)
                && growthCredit < (double) getRequiredGrowthTicks() - growthTime();
    }

    private void tickSource(ServerLevel server, int nativeHarvests) {
        var settings = SourcePotsConfig.snapshot();
        int remainingHarvests = settings.maxHarvestsPerTick() - nativeHarvests;
        harvestLimitReached = remainingHarvests == 0;
        // Validate once for the current cycle, including a purchased boost; revalidate after each harvest.
        tickEligible = !isRemoved() && !harvestLimitReached && hasStorageRoom() && GROWTH.isEligible(this, server);
        remainingHarvests -= spendGrowthCredit(server, remainingHarvests);
        // Spending can leave a sub-float-step residual; a scheduled top-up must add to it.
        tickEligible &= !harvestLimitReached && growthCredit < (double) getRequiredGrowthTicks() - growthTime();
        inSourceTick = true;
        try {
            var access = sourceAccess();
            if (tickEligible) access.pullAdjacent();
            var boost = SCHEDULER.tick(sourceState, settings, tickEligible, access);
            if (boost != null) {
                growthCredit += GROWTH.sampleGrowth(boost.tier().growth(), server.random);
                spendGrowthCredit(server, remainingHarvests);
            }
            // Timers advance even on inactive crops and must be saved on unload.
            setChanged();
        } finally {
            inSourceTick = false;
        }
    }

    private int spendGrowthCredit(ServerLevel server, int remainingHarvests) {
        int harvested = 0;
        while (harvested < remainingHarvests && growthCredit > 0 && tickEligible && !isRemoved()) {
            double remaining = GROWTH.applyEligibleCredit(this, growthCredit);
            if (remaining == growthCredit) break;
            sourceAssisted = true;
            growthCredit = remaining;
            if (growthTime() < (double) getRequiredGrowthTicks()) break;
            if (!harvestSourceCycle(server)) {
                tickEligible = false;
                break;
            }
            harvested++;
            harvestLimitReached = harvested == remainingHarvests;
            tickEligible = !harvestLimitReached && !isRemoved() && hasStorageRoom() && GROWTH.isEligible(this, server);
        }
        return harvested;
    }

    /** Only the pinned native hopper harvest branch; tick hooks, natural growth and export stay in tickPot. */
    private boolean harvestSourceCycle(ServerLevel server) {
        var context = getRecipeContext();
        var soil = getOrInvalidateSoil();
        var crop = getOrInvalidateCrop();
        int required = getRequiredGrowthTicks();
        if (isRemoved() || !isHopper() || soil == null || crop == null || required <= 0
                || growthTime() < (double) required || !hasStorageRoom()
                || !crop.isGrowthSustained(context, server) || !crop.canHarvest(context, server)) return false;

        updateComparatorLevel(15);
        int rolls = Helpers.getLootRolls(context, server, crop, soil);
        for (int roll = 0; roll < rolls; roll++) {
            crop.onHarvest(context, server, stack -> Services.GAMEPLAY.addItem(stack, getItems(), STORAGE_SLOTS));
        }
        if (BotanyPotsMod.CONFIG.get().gameplay.damage_harvest_tool
                && EnchantmentLevel.FIRST.get(Helpers.NEGATE_HARVEST_DAMAGE_TAG, getHarvestItem()) <= 0) {
            getHarvestItem().hurtAndBreak(1, server, null, stack -> {});
        }
        server.gameEvent(GameEvent.BLOCK_CHANGE, worldPosition, GameEvent.Context.of(getBlockState()));
        growthTime.reset();
        growCooldown.setTicks(0);
        sourceAssisted = false;
        return true;
    }

    private boolean hasStorageRoom() {
        for (int slot : STORAGE_SLOTS) {
            var stack = getItem(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return false;
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
        pendingPower = tag.getCompound("sourcepots_v2").copy();
        double savedCredit = pendingPower.getDouble("growth_credit");
        growthCredit = Double.isFinite(savedCredit) ? Math.clamp(savedCredit, 0, MAX_GROWTH_CREDIT) : 0;
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
            access.saveBuffers(power);
        }
        power.putDouble("growth_credit", growthCredit);
        power.putBoolean("source_assisted", sourceAssisted);
        power.putLong("jar_remaining", sourceState.jarRemainingTicks());
        power.putLong("jar_retry", sourceState.jarRetryCooldownTicks());
        power.putLong("boost_cooldown", sourceState.boostCooldownTicks());
        tag.put("sourcepots_v2", power);
    }

    private static long savedLong(CompoundTag tag, String key) {
        return tag.get(key) instanceof NumericTag number ? number.getAsLong() : 0;
    }
}
