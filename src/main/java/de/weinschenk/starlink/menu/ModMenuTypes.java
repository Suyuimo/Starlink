package de.weinschenk.starlink.menu;

import de.weinschenk.starlink.Starlink;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Starlink.MODID);

    public static final RegistryObject<MenuType<DistillationChamberMenu>> DISTILLATION_CHAMBER =
            MENU_TYPES.register("distillation_chamber",
                    () -> IForgeMenuType.create(DistillationChamberMenu::new));

    public static final RegistryObject<MenuType<LaunchControllerMenu>> LAUNCH_CONTROLLER =
            MENU_TYPES.register("launch_controller",
                    () -> IForgeMenuType.create(LaunchControllerMenu::new));

    public static final RegistryObject<MenuType<RocketV2Menu>> ROCKET_V2 =
            MENU_TYPES.register("rocket_v2",
                    () -> IForgeMenuType.create(RocketV2Menu::new));

    public static final RegistryObject<MenuType<LaunchControllerV2Menu>> LAUNCH_CONTROLLER_V2 =
            MENU_TYPES.register("launch_controller_v2",
                    () -> IForgeMenuType.create(LaunchControllerV2Menu::new));

    public static final RegistryObject<MenuType<SatelliteWorkbenchMenu>> SATELLITE_WORKBENCH =
            MENU_TYPES.register("satellite_workbench",
                    () -> IForgeMenuType.create((id, inv, data) -> new SatelliteWorkbenchMenu(id, inv)));

    public static final RegistryObject<MenuType<RocketWorkbenchMenu>> ROCKET_WORKBENCH =
            MENU_TYPES.register("rocket_workbench",
                    () -> IForgeMenuType.create((id, inv, data) -> new RocketWorkbenchMenu(id, inv)));

    public static final RegistryObject<MenuType<WirelessEnergyMenu>> ENERGY_TRANSMITTER_MENU =
            MENU_TYPES.register("energy_transmitter",
                    () -> IForgeMenuType.create(WirelessEnergyMenu::forTransmitter));

    public static final RegistryObject<MenuType<WirelessEnergyMenu>> ENERGY_RECEIVER_MENU =
            MENU_TYPES.register("energy_receiver",
                    () -> IForgeMenuType.create(WirelessEnergyMenu::forReceiver));

    public static final RegistryObject<MenuType<WirelessItemMenu>> ITEM_TRANSMITTER_MENU =
            MENU_TYPES.register("item_transmitter",
                    () -> IForgeMenuType.create(WirelessItemMenu::forTransmitter));

    public static final RegistryObject<MenuType<WirelessItemMenu>> ITEM_RECEIVER_MENU =
            MENU_TYPES.register("item_receiver",
                    () -> IForgeMenuType.create(WirelessItemMenu::forReceiver));

    public static final RegistryObject<MenuType<WirelessFluidMenu>> FLUID_TRANSMITTER_MENU =
            MENU_TYPES.register("fluid_transmitter",
                    () -> IForgeMenuType.create(WirelessFluidMenu::forTransmitter));

    public static final RegistryObject<MenuType<WirelessFluidMenu>> FLUID_RECEIVER_MENU =
            MENU_TYPES.register("fluid_receiver",
                    () -> IForgeMenuType.create(WirelessFluidMenu::forReceiver));

    public static final RegistryObject<MenuType<ReceiverMenu>> RECEIVER_MENU =
            MENU_TYPES.register("receiver",
                    () -> IForgeMenuType.create(ReceiverMenu::new));
}
