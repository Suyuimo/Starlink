package de.weinschenk.starlink;

import com.mojang.logging.LogUtils;
import de.weinschenk.starlink.block.ModBlockEntities;
import de.weinschenk.starlink.block.ModBlocks;
import de.weinschenk.starlink.compat.CCPeripheralCompat;
import de.weinschenk.starlink.block.wireless.ModWirelessBlockEntities;
import de.weinschenk.starlink.block.wireless.ModWirelessBlocks;
import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.entity.ModEntities;
import de.weinschenk.starlink.item.ModItems;
import de.weinschenk.starlink.menu.ModMenuTypes;
import de.weinschenk.starlink.network.ModNetwork;
import de.weinschenk.starlink.recipe.ModRecipeTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Starlink.MODID)
public class Starlink {

    public static final String MODID = "starlink";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> STARLINK_TAB = CREATIVE_TABS.register("starlink_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.ROCKET_FUEL.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.starlink"))
                    .displayItems((params, output) -> {
                        // Items
                        output.accept(ModItems.ROCKET_FUEL.get());
                        output.accept(ModItems.ROCKET_FUEL_RAW.get());
                        output.accept(ModItems.ORBIT_GLASSES.get());
                        output.accept(ModItems.RECEIVER_GLASSES.get());
                        // Blöcke
                        output.accept(ModBlocks.PETROLEUM_SHALE_ORE.get());
                        output.accept(ModBlocks.DISTILLATION_CHAMBER.get());
                        output.accept(ModBlocks.RECEIVER.get());
                        output.accept(ModBlocks.LAUNCH_PAD.get());
                        output.accept(ModBlocks.LAUNCH_CONTROLLER.get());
                        // V2 Rocket System – 5 Satelliten-Varianten
                        output.accept(ModItems.SATELLITE_BASIC.get());
                        output.accept(ModItems.SATELLITE_ENERGY.get());
                        output.accept(ModItems.SATELLITE_FLUID.get());
                        output.accept(ModItems.SATELLITE_ITEM.get());
                        output.accept(ModItems.SATELLITE_ADVANCED.get());
                        output.accept(ModBlocks.SATELLITE_WORKBENCH.get());
                        output.accept(ModBlocks.ROCKET_WORKBENCH.get());
                        output.accept(ModBlocks.ROCKET_V2.get());
                        output.accept(ModBlocks.LAUNCH_CONTROLLER_V2.get());
                        output.accept(ModBlocks.SATELLITE_INTERFACE.get());
                        output.accept(ModBlocks.INFINITE_ENERGY_CELL.get());
                        // Wireless blocks
                        output.accept(ModItems.LINK_TOOL.get());
                        for (var b : ModWirelessBlocks.ENERGY_TRANSMITTERS)  output.accept(b.get());
                        for (var b : ModWirelessBlocks.ENERGY_RECEIVERS)     output.accept(b.get());
                        for (var b : ModWirelessBlocks.ITEM_TRANSMITTERS)    output.accept(b.get());
                        for (var b : ModWirelessBlocks.ITEM_RECEIVERS)       output.accept(b.get());
                        for (var b : ModWirelessBlocks.FLUID_TRANSMITTERS)   output.accept(b.get());
                        for (var b : ModWirelessBlocks.FLUID_RECEIVERS)      output.accept(b.get());
                    })
                    .build());

    public Starlink() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModWirelessBlocks.BLOCKS.register(modEventBus);
        ModWirelessBlocks.ITEMS.register(modEventBus);
        ModWirelessBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
        ModRecipeTypes.RECIPE_SERIALIZERS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModNetwork.register();
        if (ModList.get().isLoaded("computercraft")) {
            CCPeripheralCompat.register();
        }
        LOGGER.info("Starlink mod loaded. Orbit dimension key: {}", ModDimensions.ORBIT_LEVEL_KEY.location());
    }
}
