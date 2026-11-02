package io.github.gaiusred.sourcepots.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/** Development-only fixtures, installed before GameTest structure preparation. */
public final class GrowthTestEnvironment {
    private GrowthTestEnvironment() {}

    public static void prepareTemplate(ServerStartingEvent event) {
        createTemplate(event.getServer().overworld());
    }

    static void createTemplate(net.minecraft.server.level.ServerLevel level) {
        CompoundTag data = new CompoundTag();
        ListTag size = new ListTag();
        size.add(IntTag.valueOf(12));
        size.add(IntTag.valueOf(8));
        size.add(IntTag.valueOf(12));
        data.put("size", size);
        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
        data.put("palette", palette);
        data.put("blocks", new ListTag());
        data.put("entities", new ListTag());
        level.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sourcepots", "empty"))
                .load(BuiltInRegistries.BLOCK.asLookup(), data);
    }
}
