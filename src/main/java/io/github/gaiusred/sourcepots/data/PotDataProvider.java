package io.github.gaiusred.sourcepots.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.gaiusred.sourcepots.registry.PotVariants;
import io.github.gaiusred.sourcepots.registry.PotVariants.PotVariant;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** All distributable pot resources share the registration inventory in PotVariants. */
public final class PotDataProvider implements DataProvider {
    private final Path output;

    public PotDataProvider(PackOutput output) {
        this.output = output.getOutputFolder();
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient() || event.includeServer(),
                new PotDataProvider(event.getGenerator().getPackOutput()));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var writes = new ArrayList<CompletableFuture<?>>();
        var language = new JsonObject();
        language.addProperty("itemGroup.sourcepots.ars_botany_pots", "Ars Botany Pots");
        for (var variant : PotVariants.ALL) {
            String path = variant.basePath();
            String id = "sourcepots:" + path;
            write(cache, writes, "data/sourcepots/recipe/" + path + ".json", recipe(variant));
            write(cache, writes, "data/sourcepots/loot_table/blocks/" + path + ".json", loot(id));
            write(cache, writes, "assets/sourcepots/blockstates/" + path + ".json",
                    object("variants", object("", object("model", "sourcepots:block/" + path))));
            write(cache, writes, "assets/sourcepots/models/block/" + path + ".json", model(variant));
            write(cache, writes, "assets/sourcepots/models/item/" + path + ".json",
                    object("parent", "sourcepots:block/" + path));
            language.addProperty("block.sourcepots." + path, "Ars " + title(path));
        }
        write(cache, writes, "assets/sourcepots/lang/en_us.json", language);
        write(cache, writes, "assets/sourcepots/models/block/template/gold_rim.json", goldRim());
        write(cache, writes, "data/botanypots/tags/block/botany_pots.json",
                matchingTag("block", ResourceLocation.parse("botanypots:botany_pots")));
        write(cache, writes, "data/botanypots/tags/item/botany_pots.json",
                matchingTag("item", ResourceLocation.parse("botanypots:botany_pots")));
        write(cache, writes, "data/minecraft/tags/block/mineable/pickaxe.json",
                matchingTag("block", ResourceLocation.parse("minecraft:mineable/pickaxe")));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private static JsonObject matchingTag(String kind, ResourceLocation originalTag) {
        var members = originalTagMembers(kind, originalTag);
        var mapped = new JsonArray();
        for (var variant : PotVariants.ALL) {
            if (members.contains(variant.originalItem())) mapped.add("sourcepots:" + variant.basePath());
        }
        return object("replace", false, "values", mapped);
    }

    private static Set<ResourceLocation> originalTagMembers(String kind, ResourceLocation tag) {
        // Read the pinned original jar directly: the merged resource manager also sees our generated overrides.
        var file = ModList.get().getModFileById("botanypots").getFile();
        var path = file.findResource("data", tag.getNamespace(), "tags", kind, tag.getPath() + ".json");
        var members = new HashSet<ResourceLocation>();
        try (var reader = Files.newBufferedReader(path)) {
            for (var value : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values")) {
                String id = value.getAsString();
                if (id.startsWith("#")) members.addAll(originalTagMembers(kind, ResourceLocation.parse(id.substring(1))));
                else members.add(ResourceLocation.parse(id));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read pinned Botany Pots " + kind + " tag " + tag, e);
        }
        return members;
    }

    private void write(CachedOutput cache, ArrayList<CompletableFuture<?>> writes, String path, JsonObject json) {
        writes.add(DataProvider.saveStable(cache, json, output.resolve(path)));
    }

    private static JsonObject recipe(PotVariant variant) {
        return object("type", "minecraft:crafting_shaped", "category", "misc",
                "pattern", array("A A", "BCB", "BBB"),
                "key", object("A", object("item", "minecraft:gold_nugget"),
                        "B", object("tag", "c:planks/archwood"),
                        "C", object("item", variant.originalItem().toString())),
                "result", object("id", "sourcepots:" + variant.basePath(), "count", 1));
    }

    private static JsonObject loot(String id) {
        return object("type", "minecraft:block", "pools", array(object("rolls", 1,
                "conditions", array(object("condition", "minecraft:survives_explosion")),
                "entries", array(object("type", "minecraft:item", "name", id)))),
                "random_sequence", id);
    }

    private static JsonObject model(PotVariant variant) {
        return object("parent", "minecraft:block/block", "loader", "neoforge:composite",
                "textures", object("particle", variant.material().toString()),
                "children", object("body", object("parent", "botanypots:block/" + variant.basePath()),
                        "rim", object("parent", "sourcepots:block/template/gold_rim")),
                "item_render_order", array("body", "rim"));
    }

    private static JsonObject goldRim() {
        // Authored cap strips. Y=8.125 clears the original outer top (8) and inner lip (8.1).
        return object("parent", "minecraft:block/block", "render_type", "minecraft:cutout",
                "textures", object("gold", "minecraft:block/gold_block", "particle", "minecraft:block/gold_block"),
                "elements", array(strip(2, 2, 14, 3), strip(2, 13, 14, 14),
                        strip(2, 3, 3, 13), strip(13, 3, 14, 13)));
    }

    private static JsonObject strip(int x0, int z0, int x1, int z1) {
        return object("from", array(x0, 8.125, z0), "to", array(x1, 8.125, z1),
                "faces", object("up", object("texture", "#gold", "uv", array(x0, z0, x1, z1))));
    }

    private static String title(String path) {
        return Arrays.stream(path.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static JsonObject object(Object... pairs) {
        var json = new JsonObject();
        for (int i = 0; i < pairs.length; i += 2) json.add((String)pairs[i], value(pairs[i + 1]));
        return json;
    }

    private static JsonArray array(Object... values) {
        var json = new JsonArray();
        for (var value : values) json.add(value(value));
        return json;
    }

    private static JsonElement value(Object value) {
        if (value instanceof JsonElement json) return json;
        if (value instanceof Number number) return new com.google.gson.JsonPrimitive(number);
        if (value instanceof Boolean bool) return new com.google.gson.JsonPrimitive(bool);
        return new com.google.gson.JsonPrimitive((String)value);
    }

    @Override public String getName() { return "Source Pots variant resources"; }
}

