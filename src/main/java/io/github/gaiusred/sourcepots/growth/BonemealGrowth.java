package io.github.gaiusred.sourcepots.growth;

import com.mojang.logging.LogUtils;
import io.github.gaiusred.sourcepots.source.SourceSettings;
import net.darkhax.bookshelf.common.api.function.ReloadableCache;
import net.darkhax.botanypots.common.api.context.BotanyPotContext;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.fertilizer.Fertilizer;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.darkhax.botanypots.common.impl.data.recipe.fertilizer.BasicFertilizer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/** Uses current bonemeal eligibility and configured Source growth, without player interaction effects. */
public final class BonemealGrowth {
    private static final Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation MISSING = ResourceLocation.fromNamespaceAndPath("sourcepots", "missing_bonemeal");
    // Only diagnostic identities are retained. Bookshelf resets this set on server reload.
    private final ReloadableCache<Set<ResourceLocation>> reported = ReloadableCache.of(level -> new HashSet<>());

    public int sampleGrowth(SourceSettings.GrowthSettings growth, RandomSource random) {
        // Positive minimum bounds keep this inclusive width within Integer.MAX_VALUE.
        return growth.minTicks() + random.nextInt(growth.maxTicks() - growth.minTicks() + 1);
    }

    public boolean isEligible(BotanyPotBlockEntity pot, ServerLevel level) {
        var context = pot.getRecipeContext();
        var soil = pot.getOrInvalidateSoil();
        var crop = pot.getOrInvalidateCrop();
        if (soil == null || crop == null || !crop.isGrowthSustained(context, level)
                || !crop.canHarvest(context, level) || !canAdvance(pot)) {
            return false;
        }
        var lookup = new BonemealContext(context, new ItemStack(Items.BONE_MEAL));
        var holder = Fertilizer.getFertilizer(level, lookup, lookup.getInteractionItem());
        if (holder == null) {
            report(level, MISSING, "No active bonemeal fertilizer recipe");
            return false;
        }
        if (!(holder.value() instanceof BasicFertilizer fertilizer)) {
            report(level, holder.id(), "Unsupported bonemeal fertilizer recipe");
            return false;
        }
        if (!fertilizer.matches(lookup, level)) {
            return false;
        }
        int amount = fertilizer.getProperties().growthAmount().getAmount(lookup, level);
        if (amount <= 0) {
            report(level, holder.id(), "Bonemeal fertilizer has nonpositive growth");
            return false;
        }
        return true;
    }

    /** Caller must validate current-tick recipe eligibility before applying retained or purchased credit. */
    public double applyEligibleCredit(BotanyPotBlockEntity pot, double credit) {
        if (!(pot.getLevel() instanceof ServerLevel) || !Double.isFinite(credit)
                || credit <= 0 || !canAdvance(pot)) {
            return credit;
        }
        double progress = pot.growthTime();
        int required = pot.getRequiredGrowthTicks();
        double needed = required - progress;
        if (credit >= needed) {
            // Maturity saturates at the effective requirement, even if its float marker rounds up.
            float mature = required;
            if (mature < (double) required) mature = Math.nextUp(mature);
            pot.updateGrowthTime(mature);
            return credit - needed;
        }
        float target = (float) (progress + credit);
        // Partial growth must never round above its paid budget. Keep unrepresentable credit.
        if (target - progress > credit) target = Math.nextDown(target);
        if (target <= progress) return credit;
        pot.updateGrowthTime(target);
        return credit - (target - progress);
    }

    private static boolean canAdvance(BotanyPotBlockEntity pot) {
        int required = pot.getRequiredGrowthTicks();
        return required > 0 && Float.isFinite(pot.growthTime()) && pot.growthTime() < (double) required;
    }

    private void report(ServerLevel level, ResourceLocation identity, String reason) {
        if (reported.apply(level).add(identity)) {
            LOG.warn("{}: {}. Source growth is disabled for this recipe.", reason, identity);
        }
    }

    private record BonemealContext(BotanyPotContext pot, ItemStack interactionItem) implements BotanyPotContext {
        public ItemStack getItem(int slot) { return pot.getItem(slot); }
        public int size() { return pot.size(); }
        public ItemStack getSoilItem() { return pot.getSoilItem(); }
        public ItemStack getSeedItem() { return pot.getSeedItem(); }
        public ItemStack getHarvestItem() { return pot.getHarvestItem(); }
        public LootParams createLootParams(BlockState state) { return pot.createLootParams(state); }
        public void runFunction(ResourceLocation id) { pot.runFunction(id); }
        public Player getPlayer() { return pot.getPlayer(); }
        public ItemStack getInteractionItem() { return interactionItem; }
        public int getRequiredGrowthTicks() { return pot.getRequiredGrowthTicks(); }
        public boolean isServerThread() { return pot.isServerThread(); }
        public Crop getCrop() { return pot.getCrop(); }
        public Soil getSoil() { return pot.getSoil(); }
    }
}
