package io.github.gaiusred.sourcepots.registry;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PotVariantsTest {
    @Test void variantsExactlyMatchPinnedBlockstatesModelsAndTextures() throws Exception {
        Path root = Path.of("upstream/BotanyPots/common/src/main/resources/assets/botanypots");
        var states = inventory(root.resolve("blockstates"));
        var models = inventory(root.resolve("models/block"));
        assertEquals(61, states.size());
        assertEquals(states, models);
        var actual = new TreeSet<String>();
        for (var variant : PotVariants.ALL) {
            assertTrue(actual.add(variant.basePath()), "Duplicate variant " + variant.basePath());
            assertEquals("botanypots:" + variant.basePath(), variant.originalItem().toString());
            var json = JsonParser.parseString(Files.readString(root.resolve("models/block/" + variant.basePath() + ".json"))).getAsJsonObject();
            var textures = json.getAsJsonObject("textures");
            assertEquals(textures.get("material").getAsString(), variant.material().toString());
        }
        assertEquals(states, actual, "Every pinned hopper pot must have exactly one upgrade");
    }
    @Test void inventoryIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> PotVariants.ALL.clear());
    }
    private static TreeSet<String> inventory(Path dir) throws Exception {
        try (var files = Files.list(dir)) {
            return files.map(path -> path.getFileName().toString()).filter(name -> name.endsWith("_hopper_botany_pot.json"))
                    .map(name -> name.substring(0, name.length() - 5)).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        }
    }
}
