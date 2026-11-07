package io.github.gaiusred.sourcepots.smoke;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlock;
import io.github.gaiusred.sourcepots.block.ArsBotanyPotBlockEntity;
import io.github.gaiusred.sourcepots.registry.ModContent;
import io.github.gaiusred.sourcepots.source.SourceMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native runtime checks whose compile and core runtime classpaths contain no optional API. */
@Mod("sourcepots_smoke")
@GameTestHolder("sourcepots_smoke")
@PrefixGameTestTemplate(false)
public final class OptionalRuntimeSmokeTests {
    private static final BlockPos CENTER = new BlockPos(5, 2, 5);

    public OptionalRuntimeSmokeTests() {
        NeoForge.EVENT_BUS.addListener(OptionalRuntimeSmokeTests::prepareTemplate);
    }

    @GameTest(template = "empty")
    public static void actualOptionalClasspathMatchesProfile(GameTestHelper h) {
        boolean fluid = fluidProfile();
        boolean mekanism = "mekanism".equals(System.getProperty("sourcepots.smokeProfile"));
        h.assertTrue(ModList.get().isLoaded("mekanism") == mekanism, "Mekanism presence must match actual profile");
        h.assertTrue(ModList.get().isLoaded("sauce") == fluid, "Sauce presence must match actual profile");
        h.assertTrue(!ModList.get().isLoaded("starbunclemania"), "Smoke runtime must omit StarbuncleMania");
        h.assertTrue(!ModList.get().isLoaded("ars_elemental"), "Smoke runtime must omit Ars Elemental");
        assertClassPresence(h, "mekanism.api.chemical.IChemicalHandler", mekanism);
        assertClassPresence(h, "com.alexthw.sauce.Sauce", fluid);
        h.assertTrue(BuiltInRegistries.FLUID.containsKey(ResourceLocation.parse("sauce:source_fluid")) == fluid,
                "Actual Source fluid registry must match profile");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void registeredPotGrowsAndJarPowerHarvests(GameTestHelper h) {
        var pot = pot(h);
        h.setBlock(CENTER.below(), Blocks.CHEST);
        h.setBlock(CENTER.above(2), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ars_nouveau:source_jar")));
        var jar = (SourceJarTile) h.getBlockEntity(CENTER.above(2));
        jar.setSource(2500);
        h.assertTrue(pot.sourceAccess().canPayJar(), "Ars Jar source must be available without optional mods");
        tick(h, pot);
        h.assertTrue(jar.getSource() == 0, "First payment must consume exactly 2500 Source");
        for (int i = 1; i < 600; i++) {
            jar.setSource(2500);
            tick(h, pot);
        }
        var chest = (ChestBlockEntity) h.getBlockEntity(CENTER.below());
        h.assertTrue(!chest.isEmpty(), "Real registered pot must harvest wheat into its chest with Jar power");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unpoweredPotTicksWithoutOptionalApis(GameTestHelper h) {
        var pot = pot(h);
        float before = pot.growthTime();
        for (int i = 0; i < 20; i++) tick(h, pot);
        h.assertTrue(pot.growthTime() > before, "Unpowered real pot must continue normal crop growth");
        h.assertTrue(!(pot.sourceAccess().fundedSideMask(SourceMode.GAS) != 0), "Missing Source gas provider must disable gas payments");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void buffersSurviveLoadWithMissingProviders(GameTestHelper h) {
        var pot = pot(h);
        var saved = pot.saveWithoutMetadata(h.getLevel().registryAccess());
        var power = saved.getCompound("sourcepots_v2");
        var buffers=new CompoundTag();
        int index=1;
        for(Direction side:Direction.Plane.HORIZONTAL) {
            var face=new CompoundTag();
            face.putLong("liquid",index*100);
            face.putLong("gas",index*10);
            buffers.put(side.getName(),face);
            index++;
        }
        power.put("buffers",buffers);
        var restored = new ArsBotanyPotBlockEntity(pot.getBlockPos(), pot.getBlockState());
        restored.loadAdditional(saved, h.getLevel().registryAccess());
        h.assertTrue(restored.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("sourcepots_v2").equals(power),
                "Saving before level attachment preserves directional data");
        restored.setLevel(h.getLevel());
        index=1;
        for(Direction side:Direction.Plane.HORIZONTAL) {
            h.assertTrue(restored.sourceAccess().storedLiquid(side)==index*100 && restored.sourceAccess().storedGas(side)==index*10,
                    "Each directional quantity survives missing providers");
            index++;
        }
        h.assertTrue(restored.sourceAccess().fundedSideMask(SourceMode.GAS)==0 && restored.sourceAccess().fundedSideMask(SourceMode.LIQUID)==0,
                "Dormant or partial quantities cannot fund growth");
        h.assertTrue(restored.saveWithoutMetadata(h.getLevel().registryAccess()).getCompound("sourcepots_v2").equals(power),
                "Saving after level attachment preserves all quantities");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void optionalCapabilitiesFollowActualResources(GameTestHelper h) throws ReflectiveOperationException {
        var pot = pot(h);
        var access = pot.sourceAccess();
        h.assertTrue(access.liquidAvailable() == fluidProfile(), "Liquid availability must follow Source fluid registration");
        h.assertTrue(!access.gasStorage().available(), "Mekanism alone must not supply Source gas");
        var fluidHandler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pot.getBlockPos(), Direction.NORTH);
        h.assertTrue((fluidHandler != null) == fluidProfile(), "Native fluid capability must follow its provider");
        if ("mekanism".equals(System.getProperty("sourcepots.smokeProfile"))) {
            var capabilities = Class.forName("mekanism.common.capabilities.Capabilities");
            var chemical = capabilities.getField("CHEMICAL").get(null);
            @SuppressWarnings("unchecked")
            var blockCapability = (BlockCapability<Object, Direction>) chemical.getClass().getMethod("block").invoke(chemical);
            h.assertTrue(h.getLevel().getCapability(blockCapability, pot.getBlockPos(), Direction.NORTH) == null,
                    "Native chemical capability must stay absent with Mekanism but no Source gas");
        }
        for(Direction side:Direction.values()) {
            var view=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pot.getBlockPos(),side);
            h.assertTrue((view!=null)==(fluidProfile() && side.getAxis().isHorizontal()),"Fluid capability follows horizontal provider availability");
        }
        h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pot.getBlockPos(),null)==null,"Unsided Source absent");
        if (fluidProfile()) {
            var source = BuiltInRegistries.FLUID.get(ResourceLocation.parse("sauce:source_fluid"));
            h.assertTrue(fluidHandler.fill(new FluidStack(source, 999), FluidAction.EXECUTE) == 999,
                    "Source fluid must fill without StarbuncleMania or Mekanism");
            h.assertTrue(!(access.fundedSideMask(SourceMode.LIQUID) != 0), "Default liquid cost is 1000 mB");
            h.assertTrue(fluidHandler.fill(new FluidStack(source, 1), FluidAction.EXECUTE) == 1,
                    "Final mB must fill the default liquid buffer");
            h.assertTrue((access.fundedSideMask(SourceMode.LIQUID) != 0) && access.pay(SourceMode.LIQUID, 1 << Direction.NORTH.get2DDataValue()),
                    "Exactly 1000 mB must pay for a liquid boost");
            h.assertTrue(access.storedLiquid(Direction.NORTH) == 0, "A liquid payment must consume exactly 1000 mB");
            fluidHandler.fill(new FluidStack(source, 1000), FluidAction.EXECUTE);
            float before = pot.growthTime();
            tick(h, pot);
            h.assertTrue(pot.growthTime() - before >= 240 && access.storedLiquid(Direction.NORTH) == 0,
                    "A real Source-fluid payment must power the pot ticker without Mekanism or StarbuncleMania");
        }
        h.succeed();
    }

    private static boolean fluidProfile() {
        return "fluid".equals(System.getProperty("sourcepots.smokeProfile"));
    }

    private static void assertClassPresence(GameTestHelper h, String name, boolean expected) {
        boolean present;
        try {
            Class.forName(name, false, OptionalRuntimeSmokeTests.class.getClassLoader());
            present = true;
        } catch (ClassNotFoundException e) {
            present = false;
        }
        h.assertTrue(present == expected, "Actual classloader presence must match profile: " + name);
    }

    private static ArsBotanyPotBlockEntity pot(GameTestHelper h) {
        h.setBlock(CENTER, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("sourcepots:terracotta_hopper_botany_pot")));
        var pot = (ArsBotanyPotBlockEntity) h.getBlockEntity(CENTER);
        pot.setSoilItem(new ItemStack(Items.DIRT));
        pot.setSeed(new ItemStack(Items.WHEAT_SEEDS));
        pot.updateGrowthTime(0);
        return pot;
    }

    private static void tick(GameTestHelper h, ArsBotanyPotBlockEntity pot) {
        var block = (ArsBotanyPotBlock) pot.getBlockState().getBlock();
        var ticker = block.getTicker(h.getLevel(), pot.getBlockState(), ModContent.POT_ENTITY.get());
        h.assertTrue(ticker != null, "Registered pot must receive its real ticker");
        ticker.tick(h.getLevel(), pot.getBlockPos(), pot.getBlockState(), pot);
    }

    private static void prepareTemplate(ServerStartingEvent event) {
        CompoundTag data = new CompoundTag();
        ListTag size = new ListTag();
        size.add(IntTag.valueOf(12)); size.add(IntTag.valueOf(8)); size.add(IntTag.valueOf(12));
        data.put("size", size);
        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
        data.put("palette", palette);
        data.put("blocks", new ListTag());
        data.put("entities", new ListTag());
        event.getServer().overworld().getStructureManager()
                .getOrCreate(ResourceLocation.parse("sourcepots_smoke:empty"))
                .load(BuiltInRegistries.BLOCK.asLookup(), data);
    }
}
