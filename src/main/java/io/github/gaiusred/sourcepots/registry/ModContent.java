package io.github.gaiusred.sourcepots.registry;

import io.github.gaiusred.sourcepots.SourcePots;
import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlock;
import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

public final class ModContent {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SourcePots.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, SourcePots.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, SourcePots.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SourcePots.MOD_ID);
    public static final Map<ResourceLocation, DeferredHolder<Block, ArsBotanyPotBlock>> POTS;
    static {
        var pots = new LinkedHashMap<ResourceLocation, DeferredHolder<Block, ArsBotanyPotBlock>>();
        for (var variant : PotVariants.ALL) {
            var block = BLOCKS.register(variant.basePath(), () -> new ArsBotanyPotBlock(
                    BuiltInRegistries.BLOCK.getOptional(variant.originalItem()).orElseThrow().defaultMapColor()));
            ITEMS.register(variant.basePath(), () -> new BlockItem(block.get(), new Item.Properties()));
            pots.put(variant.originalItem(), block);
        }
        POTS = java.util.Collections.unmodifiableMap(pots);
    }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BotanyPotBlockEntity>> POT_ENTITY = TYPES.register("ars_botany_pot",
            () -> BlockEntityType.Builder.<BotanyPotBlockEntity>of(ArsBotanyPotBlockEntity::new,
                    POTS.values().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARS_BOTANY_POTS_TAB = TABS.register("ars_botany_pots",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sourcepots.ars_botany_pots"))
                    .icon(() -> new ItemStack(POTS.get(ResourceLocation.parse("botanypots:terracotta_hopper_botany_pot")).get()))
                    .displayItems((parameters, output) -> POTS.values().forEach(pot -> output.accept(pot.get())))
                    .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
        TABS.register(bus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, POT_ENTITY.get(),
                (pot, side) -> side == Direction.DOWN ? new SidedInvWrapper(pot, Direction.DOWN) : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, POT_ENTITY.get(),
                (pot, side) -> side != null && pot instanceof ArsBotanyPotBlockEntity ars && ars.sourceAccess() != null
                        ? ars.sourceAccess().fluidHandler() : null);
        event.registerBlockEntity(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), POT_ENTITY.get(),
                (pot, side) -> side != null && pot instanceof ArsBotanyPotBlockEntity ars && ars.sourceAccess() != null
                        ? ars.sourceAccess().chemicalHandler() : null);
    }
    private ModContent() {}
}
