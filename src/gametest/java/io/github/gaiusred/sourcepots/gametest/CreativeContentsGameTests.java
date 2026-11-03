package io.github.gaiusred.sourcepots.gametest;

import io.github.gaiusred.sourcepots.SourcePots;
import io.github.gaiusred.sourcepots.registry.PotVariants;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class CreativeContentsGameTests {
    private static final ResourceKey<CreativeModeTab> BOTANY_POTS = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, ResourceLocation.parse("botanypots:tab"));

    private static final ResourceLocation ARS_TAB = ResourceLocation.parse("sourcepots:ars_botany_pots");

    @GameTest(template="empty", batch="creative-contents")
    public static void ownTabContainsEveryMappedPotAndNoUpstreamPots(GameTestHelper h) {
        var tab = ownTab(h);
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(), false,
                h.getLevel().registryAccess()));
        assertMappedPots(h, tab.getDisplayItems(), "Ars Botany Pots tab");
        h.assertTrue(tab.getIconItem().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("sourcepots:terracotta_hopper_botany_pot"))),
                "Own category must use the Ars terracotta pot icon");
        h.assertTrue(tab.getDisplayName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents title
                        && title.getKey().equals("itemGroup.sourcepots.ars_botany_pots"),
                "Own category must have a translated title");
        h.succeed();
    }

    @GameTest(template="empty", batch="creative-contents")
    public static void ownTabSearchContainsEveryMappedPot(GameTestHelper h) {
        var tab = ownTab(h);
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(), false,
                h.getLevel().registryAccess()));
        assertMappedPots(h, tab.getSearchTabDisplayItems(), "Creative search");
        h.succeed();
    }

    @GameTest(template="empty", batch="creative-contents")
    public static void originalBotanyPotsTabKeepsExistingEntriesWithoutArsPots(GameTestHelper h) {
        var event = populate(h, BOTANY_POTS);
        h.assertTrue(event.getParentEntries().size() == 1 && event.getSearchEntries().size() == 1,
                "Source pots must stop appending to the original Botany Pots category and preserve its entries");
        h.assertTrue(event.getParentEntries().first().is(Items.DIAMOND) && event.getSearchEntries().first().is(Items.DIAMOND),
                "The original category's existing entries must remain unchanged");
        h.succeed();
    }

    private static CreativeModeTab ownTab(GameTestHelper h) {
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(ARS_TAB);
        h.assertTrue(tab.isPresent(), "Own Ars Botany Pots Creative category must be registered");
        return tab.orElseThrow();
    }

    @GameTest(template="empty", batch="creative-contents")
    public static void unrelatedTabContentsStayUntouched(GameTestHelper h) {
        var event = populate(h, CreativeModeTabs.BUILDING_BLOCKS);
        h.assertTrue(event.getParentEntries().size() == 1 && event.getParentEntries().first().is(Items.DIAMOND),
                "Unrelated parent tab must keep only its existing entry");
        h.assertTrue(event.getSearchEntries().size() == 1 && event.getSearchEntries().first().is(Items.DIAMOND),
                "Unrelated search entries must remain unchanged");
        h.succeed();
    }

    private static BuildCreativeModeTabContentsEvent populate(GameTestHelper h, ResourceKey<CreativeModeTab> key) {
        var parent = new InsertableLinkedOpenCustomHashSet<ItemStack>(ItemStackLinkedSet.TYPE_AND_TAG);
        var search = new InsertableLinkedOpenCustomHashSet<ItemStack>(ItemStackLinkedSet.TYPE_AND_TAG);
        parent.add(new ItemStack(Items.DIAMOND));
        search.add(new ItemStack(Items.DIAMOND));
        var parameters = new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(), false,
                h.getLevel().registryAccess());
        var event = new BuildCreativeModeTabContentsEvent(BuiltInRegistries.CREATIVE_MODE_TAB.get(key), key,
                parameters, parent, search);
        // Exercise this mod's installed listener without invoking other mods' client-only handlers on a server.
        ModList.get().getModContainerById(SourcePots.MOD_ID).orElseThrow().getEventBus().post(event);
        return event;
    }

    private static void assertMappedPots(GameTestHelper h, java.util.Collection<ItemStack> entries, String location) {
        var actual = entries.stream().filter(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("sourcepots"))
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem())).collect(Collectors.toSet());
        var expected = PotVariants.ALL.stream().map(variant -> ResourceLocation.fromNamespaceAndPath("sourcepots", variant.basePath()))
                .collect(Collectors.toSet());
        h.assertTrue(actual.size() == 61, location + " needs all 61 Ars pots; found " + actual.size());
        h.assertTrue(actual.equals(expected), location + " must contain the exact mapped Ars pot items");
        h.assertTrue(entries.size() == 61,
                location + " must contain only the mapped Ars pots");
        h.assertTrue(entries.stream().allMatch(stack -> stack.getCount() == 1), location + " must contain single-item stacks");
    }

    private CreativeContentsGameTests() {}
}
