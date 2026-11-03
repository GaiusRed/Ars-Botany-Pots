package io.github.gaiusred.sourcepots.block;

import io.github.gaiusred.sourcepots.registry.ModContent;
import net.darkhax.botanypots.common.impl.block.BotanyPotBlock;
import net.darkhax.botanypots.common.impl.block.PotType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class ArsBotanyPotBlock extends BotanyPotBlock {
    public ArsBotanyPotBlock(MapColor mapColor) { super(mapColor, PotType.HOPPER); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArsBotanyPotBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModContent.POT_ENTITY.get(), ArsBotanyPotBlockEntity::tickPot);
    }
}
