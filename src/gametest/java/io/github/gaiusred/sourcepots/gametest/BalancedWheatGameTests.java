package io.github.gaiusred.sourcepots.gametest;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import io.github.gaiusred.sourcepots.compat.mekanism.MekanismSourceStorage;
import io.github.gaiusred.sourcepots.config.SourcePotsConfig;
import io.github.gaiusred.sourcepots.source.SourceMode;
import io.github.gaiusred.sourcepots.source.SourceSettings;
import mekanism.api.Action;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Measures ten simulated minutes through the real ticker, crop recipe and harvest implementation. */
@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class BalancedWheatGameTests {
    private static final BlockPos CENTER = new BlockPos(5, 2, 5);

    @GameTest(template = "empty", batch = "balanced-wheat", timeoutTicks = 400)
    public static void nineDefaultTiersMeetWheatAndConsumptionTargets(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(SourceSettings.defaults())) {
            h.assertTrue(SourcePotsConfig.snapshot().equals(SourceSettings.defaults()), "All nine tiers use production defaults");
            measure(h, SourceMode.JAR, 0, 25);
            int[] liquid = {50, 75, 100, 125};
            int[] gas = {100, 135, 165, 200};
            for (int sides = 1; sides <= 4; sides++) {
                measure(h, SourceMode.LIQUID, sides, liquid[sides - 1]);
                measure(h, SourceMode.GAS, sides, gas[sides - 1]);
            }
        }
        h.succeed();
    }

    private static void measure(GameTestHelper h, SourceMode mode, int sides, int target) {
        h.setBlock(CENTER, Blocks.AIR);
        h.setBlock(CENTER.above(2), Blocks.AIR);
        var pot = ArsPotGameTests.pot(h, CENTER);
        SourceJarTile jar = null;
        if (mode == SourceMode.JAR) {
            h.setBlock(CENTER.above(2), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ars_nouveau:source_jar")));
            jar = (SourceJarTile) h.getBlockEntity(CENTER.above(2));
        }
        var settings = SourceSettings.defaults();
        long accepted = 0;
        long jarConsumed = 0;
        int wheat = 0;
        for (int tick = 0; tick < 12000; tick++) {
            if (jar != null) jar.setSource(2500);
            int face = 0;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (face++ >= sides) break;
                if (mode == SourceMode.LIQUID) {
                    accepted += pot.sourceAccess().fluidHandler(side).fill(SourceAccessGameTests.fluid(1000), FluidAction.EXECUTE);
                } else {
                    var handler = ((MekanismSourceStorage) pot.sourceAccess().gasStorage()).handler(side);
                    accepted += 60 - handler.insertChemical(SourceAccessGameTests.gas(60), Action.EXECUTE).getAmount();
                }
            }
            ArsPotGameTests.tick(h, pot);
            if (jar != null) jarConsumed += 2500 - jar.getSource();
            for (int slot : BotanyPotBlockEntity.STORAGE_SLOTS) {
                if (pot.getItem(slot).is(Items.WHEAT)) wheat += pot.getItem(slot).getCount();
                pot.setItem(slot, ItemStack.EMPTY);
            }
        }
        String label = mode + " / " + sides + " faces";
        h.assertTrue(Math.abs(wheat - target) <= 1, label + " expected about " + target + " wheat, got " + wheat);
        if (mode == SourceMode.JAR) {
            h.assertTrue(jarConsumed == 375000, "Jar retains its existing payment budget");
        } else {
            long stored = 0;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                stored += mode == SourceMode.LIQUID ? pot.sourceAccess().storedLiquid(side) : pot.sourceAccess().storedGas(side);
            }
            long cost = mode == SourceMode.LIQUID ? settings.liquid().cost() : settings.gas().cost();
            h.assertTrue(accepted - stored == 200 * sides * cost, label + " must pay exactly 200 times per face");
        }
        org.slf4j.LoggerFactory.getLogger(BalancedWheatGameTests.class)
                .info("Balanced wheat: {}, wheat/12000ticks={}, consumed={}", label, wheat,
                        mode == SourceMode.JAR ? jarConsumed : accepted - remaining(pot, mode));
    }

    private static long remaining(io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity pot, SourceMode mode) {
        long result = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            result += mode == SourceMode.LIQUID ? pot.sourceAccess().storedLiquid(side) : pot.sourceAccess().storedGas(side);
        }
        return result;
    }
}
