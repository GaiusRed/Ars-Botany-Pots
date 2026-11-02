package io.github.gaiusred.sourcepots.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity;
import io.github.gaiusred.sourcepots.registry.PotVariants;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

@GameTestHolder("sourcepots")
@PrefixGameTestTemplate(false)
public final class PotDataGameTests {
    @GameTest(template="empty", batch="pot-data")
    public static void everyVariantPreservesOriginalMapColor(GameTestHelper h) {
        for (var variant : PotVariants.ALL) {
            var original = BuiltInRegistries.BLOCK.getOptional(variant.originalItem()).orElseThrow();
            var upgraded = BuiltInRegistries.BLOCK.getOptional(id(variant.basePath())).orElseThrow();
            h.assertTrue(upgraded.defaultMapColor() == original.defaultMapColor(),
                    "Map color must match the original hopper: " + variant.basePath());
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="pot-data")
    public static void everyVariantCraftsOnlyFromItsMatchingHopper(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        long count = recipes.getRecipes().stream().filter(r -> r.id().getNamespace().equals("sourcepots")).count();
        h.assertTrue(count == PotVariants.ALL.size(), "Each mapped variant needs exactly one upgrade recipe; found " + count);
        var planks = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse("c:planks/archwood"))).orElseThrow();
        h.assertTrue(planks.size() > 0, "Archwood tag fixture must contain real planks");
        for (var variant : PotVariants.ALL) {
            var id = id(variant.basePath());
            var holder = recipes.byKey(id);
            h.assertTrue(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "Missing shaped upgrade: " + id);
            var recipe = (ShapedRecipe)holder.orElseThrow().value();
            var json = resource(h, "data/sourcepots/recipe/" + variant.basePath() + ".json");
            h.assertTrue(json.getAsJsonArray("pattern").toString().equals("[\"A A\",\"BCB\",\"BBB\"]"), "Upgrade shape must preserve the empty top middle: " + id);
            var key = json.getAsJsonObject("key");
            h.assertTrue(key.getAsJsonObject("A").get("item").getAsString().equals("minecraft:gold_nugget")
                    && key.getAsJsonObject("B").get("tag").getAsString().equals("c:planks/archwood")
                    && key.getAsJsonObject("C").get("item").getAsString().equals(variant.originalItem().toString()), "Upgrade ingredients must keep the exact hopper identity: " + id);
            for (var plank : planks) {
                var input = input(new ItemStack(BuiltInRegistries.ITEM.get(variant.originalItem())), new ItemStack(plank));
                h.assertTrue(recipe.matches(input, h.getLevel()), "Archwood plank must craft its matching upgrade: " + id);
                var output = recipe.assemble(input, h.getLevel().registryAccess());
                h.assertTrue(output.is(BuiltInRegistries.ITEM.get(id)) && output.getCount() == 1, "Upgrade must return one matching Ars pot: " + id);
            }
            var plank = new ItemStack(planks.iterator().next());
            for (var candidate : PotVariants.ALL) {
                boolean matches = recipe.matches(input(new ItemStack(BuiltInRegistries.ITEM.get(candidate.originalItem())), plank), h.getLevel());
                h.assertTrue(matches == candidate.equals(variant), "Other hopper materials cannot substitute: " + id);
                var normal = ResourceLocation.fromNamespaceAndPath("botanypots", candidate.basePath().replace("_hopper_botany_pot", "_botany_pot"));
                h.assertTrue(!recipe.matches(input(new ItemStack(BuiltInRegistries.ITEM.get(normal)), plank), h.getLevel()), "Normal pots cannot upgrade: " + normal);
            }
            h.assertTrue(!recipe.matches(input(new ItemStack(BuiltInRegistries.ITEM.get(variant.originalItem())), new ItemStack(Items.OAK_PLANKS)), h.getLevel()), "Ordinary planks cannot replace archwood");
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="pot-data")
    public static void everyVariantKeepsUnrotatedBodyAndRimInBothModels(GameTestHelper h) {
        for (var variant : PotVariants.ALL) {
            String path = variant.basePath();
            var model = resource(h, "assets/sourcepots/models/block/" + path + ".json");
            h.assertTrue(model.get("loader").getAsString().equals("neoforge:composite"), "Body and rim must both render: " + path);
            var children = model.getAsJsonObject("children");
            h.assertTrue(children.size() == 2 && children.getAsJsonObject("body").size() == 1
                    && children.getAsJsonObject("body").get("parent").getAsString().equals("botanypots:block/" + path)
                    && children.getAsJsonObject("rim").size() == 1
                    && children.getAsJsonObject("rim").get("parent").getAsString().equals("sourcepots:block/template/gold_rim"), "Reuse exact upstream body without texture or rotation overrides, with shared rim: " + path);
            h.assertTrue(model.getAsJsonArray("item_render_order").toString().equals("[\"body\",\"rim\"]"), "Inventory must include both model passes");
            var variants = resource(h, "assets/sourcepots/blockstates/" + path + ".json").getAsJsonObject("variants");
            var state = variants.getAsJsonObject("");
            h.assertTrue(variants.size() == 1 && state.size() == 1 && state.get("model").getAsString().equals("sourcepots:block/" + path), "No facing rotations may change the original body: " + path);
            h.assertTrue(resource(h, "assets/sourcepots/models/item/" + path + ".json").get("parent").getAsString().equals("sourcepots:block/" + path), "Item must inherit composite geometry: " + path);
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="pot-data")
    public static void representativeBodiesPreserveMaterialAndTopReferences(GameTestHelper h) {
        for (String base : List.of("terracotta", "black_concrete", "white_glazed_terracotta")) {
            String path = base + "_hopper_botany_pot";
            var body = resource(h, "assets/sourcepots/models/block/" + path + ".json").getAsJsonObject("children").getAsJsonObject("body");
            String parent = body.get("parent").getAsString().substring("botanypots:".length());
            var upstream = resource(h, "assets/botanypots/models/" + parent + ".json");
            h.assertTrue(upstream.get("parent").getAsString().equals("botanypots:block/template/hopper_pot"), "Original hopper silhouette must remain");
            var textures = upstream.getAsJsonObject("textures");
            h.assertTrue(textures.get("material").getAsString().equals("minecraft:block/" + base)
                    && textures.get("material_top").getAsString().equals("botanypots:block/" + base + "_pot_top"), "Body must retain its original side and top textures: " + base);
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="pot-data")
    public static void goldRimLeavesOpeningClearAboveExistingLip(GameTestHelper h) {
        var rim = resource(h, "assets/sourcepots/models/block/template/gold_rim.json");
        h.assertTrue(rim.getAsJsonObject("textures").get("gold").getAsString().equals("minecraft:block/gold_block"), "Gold rim needs a real gold texture");
        var elements = rim.getAsJsonArray("elements");
        h.assertTrue(elements.size() == 4, "Four strips must leave the center open");
        double area = 0;
        for (var value : elements) {
            var element = value.getAsJsonObject(); var from = element.getAsJsonArray("from"); var to = element.getAsJsonArray("to");
            double x0=from.get(0).getAsDouble(), z0=from.get(2).getAsDouble(), x1=to.get(0).getAsDouble(), z1=to.get(2).getAsDouble();
            h.assertTrue(!element.has("rotation") && from.get(1).getAsDouble() == 8.125 && to.get(1).getAsDouble() == 8.125, "Rim must clear both original surfaces at 8 and 8.1");
            h.assertTrue(x0 >= 2 && z0 >= 2 && x1 <= 14 && z1 <= 14 && (x1 <= 3 || x0 >= 13 || z1 <= 3 || z0 >= 13), "Gold must never cover the 10 by 10 opening");
            var faces = element.getAsJsonObject("faces");
            h.assertTrue(faces.size() == 1 && faces.getAsJsonObject("up").get("texture").getAsString().equals("#gold"), "Cap must have an upward gold face");
            area += (x1-x0)*(z1-z0);
        }
        for (int i=0; i<elements.size(); i++) for (int j=i+1; j<elements.size(); j++) {
            var first=elements.get(i).getAsJsonObject(); var second=elements.get(j).getAsJsonObject();
            var f0=first.getAsJsonArray("from"); var f1=first.getAsJsonArray("to");
            var s0=second.getAsJsonArray("from"); var s1=second.getAsJsonArray("to");
            double width=Math.min(f1.get(0).getAsDouble(),s1.get(0).getAsDouble())-Math.max(f0.get(0).getAsDouble(),s0.get(0).getAsDouble());
            double depth=Math.min(f1.get(2).getAsDouble(),s1.get(2).getAsDouble())-Math.max(f0.get(2).getAsDouble(),s0.get(2).getAsDouble());
            h.assertTrue(width<=0 || depth<=0,"Rim strips must not overlap each other");
        }
        h.assertTrue(area == 44, "Rim must cover precisely the 12-square outer ring around the 10-square opening");
        h.succeed();
    }

    @GameTest(template="empty", batch="pot-data")
    public static void everyVariantMirrorsOriginalMiningAndPotTags(GameTestHelper h) {
        var potBlocks=TagKey.create(Registries.BLOCK,ResourceLocation.parse("botanypots:botany_pots"));
        var potItems=TagKey.create(Registries.ITEM,ResourceLocation.parse("botanypots:botany_pots"));
        for(var variant:PotVariants.ALL) {
            var original=BuiltInRegistries.BLOCK.get(variant.originalItem());
            var upgraded=BuiltInRegistries.BLOCK.get(id(variant.basePath()));
            h.assertTrue(upgraded.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)==original.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),"Pickaxe membership must match original: "+variant.basePath());
            h.assertTrue(upgraded.defaultBlockState().is(potBlocks)==original.defaultBlockState().is(potBlocks),"Block pot tag must match original: "+variant.basePath());
            h.assertTrue(new ItemStack(upgraded.asItem()).is(potItems)==new ItemStack(original.asItem()).is(potItems),"Item pot tag must match original: "+variant.basePath());
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="pot-data")
    public static void allVariantsHaveNamesAndSelfLoot(GameTestHelper h) {
        var names = resource(h, "assets/sourcepots/lang/en_us.json");
        for (var variant : PotVariants.ALL) {
            var block = BuiltInRegistries.BLOCK.get(id(variant.basePath()));
            h.assertTrue(names.has(block.getDescriptionId()) && names.get(block.getDescriptionId()).getAsString().startsWith("Ars "), "Every variant needs a readable Ars name");
            var drops = Block.getDrops(block.defaultBlockState(), h.getLevel(), h.absolutePos(new BlockPos(5,2,5)), null);
            h.assertTrue(drops.size() == 1 && drops.getFirst().is(block.asItem()) && drops.getFirst().getCount() == 1, "Loot must return exactly one matching pot: " + variant.basePath());
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="pot-data")
    public static void breakingPaidPotDropsMatchingItemAndPlacementStartsEmpty(GameTestHelper h) {
        try (var ignored = new GrowthConfigFixture(100, 100)) {
            var pos = new BlockPos(5, 2, 5);
            var block = BuiltInRegistries.BLOCK.get(id("black_concrete_hopper_botany_pot"));
            h.setBlock(pos, block);
            var pot = (ArsBotanyPotBlockEntity) h.getBlockEntity(pos);
            pot.setSoilItem(new ItemStack(Items.DIRT));
            pot.setSeed(new ItemStack(Items.WHEAT_SEEDS));
            h.setBlock(pos.above(2), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ars_nouveau:source_jar")));
            var jar = (SourceJarTile) h.getBlockEntity(pos.above(2));
            jar.setSource(2500);
            ArsBotanyPotBlockEntity.tickPot(h.getLevel(), pot.getBlockPos(), pot.getBlockState(), pot);
            var liquid = new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse("sauce:source_fluid")), 250);
            pot.sourceAccess().fluidHandler().fill(liquid, FluidAction.EXECUTE);
            var sourceGas = MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.parse("starbunclemania:source_gas")).orElseThrow();
            var gas = new ChemicalStack(sourceGas, 100);
            pot.sourceAccess().chemicalHandler().insertChemical(gas, Action.EXECUTE);
            var power = pot.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("sourcepots");
            h.assertTrue(power.getLong("jar_remaining") == 100 && power.getLong("liquid") == 250 && power.getLong("gas") == 100,
                    "Fixture must have paid time and both filled buffers");
            h.getLevel().destroyBlock(h.absolutePos(pos), true);
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(1));
            var self = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(block.asItem())).toList();
            h.assertTrue(self.size() == 1 && self.getFirst().getCount() == 1,
                    "Breaking paid pot must drop exactly one matching self item");
            var stack = self.getFirst().copy();
            h.assertTrue(!stack.has(DataComponents.BLOCK_ENTITY_DATA) && !stack.has(DataComponents.CUSTOM_DATA),
                    "Self item must carry no stored power data");
            var target = pos.offset(3, 0, 0);
            h.setBlock(target.below(), Blocks.STONE);
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(target.below())), Direction.UP, h.absolutePos(target.below()), false);
            var placement = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            var result = ((BlockItem) stack.getItem()).place(placement);
            h.assertTrue(result.consumesAction() && h.getBlockEntity(target) instanceof ArsBotanyPotBlockEntity,
                    "Dropped item must place its matching pot");
            var fresh = (ArsBotanyPotBlockEntity) h.getBlockEntity(target);
            var empty = fresh.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("sourcepots");
            for (String key : List.of("liquid", "gas", "jar_remaining", "jar_retry", "boost_cooldown")) {
                h.assertTrue(empty.getLong(key) == 0, "Replacement must start without power: " + key);
            }
            h.succeed();
        
        }
    }

    private static CraftingInput input(ItemStack pot, ItemStack plank) {
        return CraftingInput.of(3,3,List.of(new ItemStack(Items.GOLD_NUGGET),ItemStack.EMPTY,new ItemStack(Items.GOLD_NUGGET),plank.copy(),pot,plank.copy(),plank.copy(),plank.copy(),plank.copy()));
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("sourcepots",path); }
    private static JsonObject resource(GameTestHelper h,String path) {
        var stream=PotDataGameTests.class.getClassLoader().getResourceAsStream(path);
        h.assertTrue(stream!=null,"Missing generated resource: "+path);
        try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
        catch(java.io.IOException e) { throw new IllegalStateException(e); }
    }
    private PotDataGameTests() {}
}


