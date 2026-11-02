package io.github.gaiusred.sourcepots.registry;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.resources.ResourceLocation;

public final class PotVariants {
    public record PotVariant(String basePath, ResourceLocation originalItem, ResourceLocation material, ResourceLocation materialTop) {}
    public static final List<PotVariant> ALL = variants();
    private static List<PotVariant> variants() {
        var materials = new ArrayList<String>();
        materials.add("terracotta");
        for (String color : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            for (String suffix : List.of("_terracotta", "_glazed_terracotta", "_concrete")) materials.add(color + suffix);
        }
        materials.addAll(List.of("bricks", "stone_bricks", "mossy_stone_bricks", "deepslate_bricks", "tuff_bricks", "mud_bricks",
                "prismarine_bricks", "nether_bricks", "red_nether_bricks", "polished_blackstone_bricks", "end_stone_bricks", "quartz_bricks"));
        return materials.stream().map(base -> new PotVariant(base + "_hopper_botany_pot",
                        ResourceLocation.fromNamespaceAndPath("botanypots", base + "_hopper_botany_pot"),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "block/" + base),
                        ResourceLocation.fromNamespaceAndPath("botanypots", "block/" + base + "_pot_top")))
                .sorted(Comparator.comparing(PotVariant::basePath)).toList();
    }
    private PotVariants() {}
}
