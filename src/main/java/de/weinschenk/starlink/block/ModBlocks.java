package de.weinschenk.starlink.block;

import de.weinschenk.starlink.Starlink;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Starlink.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Starlink.MODID);

    public static final RegistryObject<Block> RECEIVER = BLOCKS.register("receiver",
            () -> new ReceiverBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Item> RECEIVER_ITEM = ITEMS.register("receiver",
            () -> new BlockItem(RECEIVER.get(), new Item.Properties()));

    public static final RegistryObject<Block> LAUNCH_PAD = BLOCKS.register("launch_pad",
            () -> new LaunchPadBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(5.0f, 10.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> LAUNCH_PAD_ITEM = ITEMS.register("launch_pad",
            () -> new BlockItem(LAUNCH_PAD.get(), new Item.Properties()));

    public static final RegistryObject<Block> LAUNCH_CONTROLLER = BLOCKS.register("launch_controller",
            () -> new LaunchControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(5.0f, 10.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Item> LAUNCH_CONTROLLER_ITEM = ITEMS.register("launch_controller",
            () -> new BlockItem(LAUNCH_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<Block> PETROLEUM_SHALE_ORE = BLOCKS.register("petroleum_shale_ore",
            () -> new PetroleumShaleOreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> PETROLEUM_SHALE_ORE_ITEM = ITEMS.register("petroleum_shale_ore",
            () -> new BlockItem(PETROLEUM_SHALE_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Block> TITANIUM_ORE = BLOCKS.register("titanium_ore",
            () -> new TitaniumOreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(4.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> TITANIUM_ORE_ITEM = ITEMS.register("titanium_ore",
            () -> new BlockItem(TITANIUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Block> DEEPSLATE_TITANIUM_ORE = BLOCKS.register("deepslate_titanium_ore",
            () -> new TitaniumOreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(5.0f, 4.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> DEEPSLATE_TITANIUM_ORE_ITEM = ITEMS.register("deepslate_titanium_ore",
            () -> new BlockItem(DEEPSLATE_TITANIUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Block> DISTILLATION_CHAMBER = BLOCKS.register("distillation_chamber",
            () -> new DistillationChamberBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f, 8.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Item> DISTILLATION_CHAMBER_ITEM = ITEMS.register("distillation_chamber",
            () -> new BlockItem(DISTILLATION_CHAMBER.get(), new Item.Properties()));

    // ── V2 Rocket System ────────────────────────────────────────────────────

    public static final RegistryObject<Block> SATELLITE_WORKBENCH = BLOCKS.register("satellite_workbench",
            () -> new SatelliteWorkbenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> SATELLITE_WORKBENCH_ITEM = ITEMS.register("satellite_workbench",
            () -> new BlockItem(SATELLITE_WORKBENCH.get(), new Item.Properties()));

    public static final RegistryObject<Block> ROCKET_WORKBENCH = BLOCKS.register("rocket_workbench",
            () -> new RocketWorkbenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> ROCKET_WORKBENCH_ITEM = ITEMS.register("rocket_workbench",
            () -> new BlockItem(ROCKET_WORKBENCH.get(), new Item.Properties()));

    public static final RegistryObject<Block> ROCKET_V2 = BLOCKS.register("rocket_v2",
            () -> new RocketV2Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(4.0f, 8.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Item> ROCKET_V2_ITEM = ITEMS.register("rocket_v2",
            () -> new BlockItem(ROCKET_V2.get(), new Item.Properties()));

    public static final RegistryObject<Block> LAUNCH_CONTROLLER_V2 = BLOCKS.register("launch_controller_v2",
            () -> new LaunchControllerV2Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(5.0f, 10.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Item> LAUNCH_CONTROLLER_V2_ITEM = ITEMS.register("launch_controller_v2",
            () -> new BlockItem(LAUNCH_CONTROLLER_V2.get(), new Item.Properties()));

    // ── CC:Tweaked Integration ────────────────────────────────────────────────

    public static final RegistryObject<Block> SATELLITE_INTERFACE = BLOCKS.register("satellite_interface",
            () -> new SatelliteInterfaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Item> SATELLITE_INTERFACE_ITEM = ITEMS.register("satellite_interface",
            () -> new BlockItem(SATELLITE_INTERFACE.get(), new Item.Properties()));

    // ── Debug / Test ─────────────────────────────────────────────────────────

    public static final RegistryObject<Block> INFINITE_ENERGY_CELL = BLOCKS.register("infinite_energy_cell",
            () -> new InfiniteEnergyCellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.0f, 1.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 7)));

    public static final RegistryObject<Item> INFINITE_ENERGY_CELL_ITEM = ITEMS.register("infinite_energy_cell",
            () -> new BlockItem(INFINITE_ENERGY_CELL.get(), new Item.Properties()));
}
