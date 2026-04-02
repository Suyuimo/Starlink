package de.weinschenk.starlink.client;

import de.weinschenk.starlink.client.renderer.RocketRenderer;
import de.weinschenk.starlink.client.renderer.RocketV2Renderer;
import de.weinschenk.starlink.client.renderer.SatelliteRenderer;
import de.weinschenk.starlink.client.screen.DistillationChamberScreen;
import de.weinschenk.starlink.client.screen.LaunchControllerScreen;
import de.weinschenk.starlink.client.screen.LaunchControllerV2Screen;
import de.weinschenk.starlink.client.screen.RocketV2Screen;
import de.weinschenk.starlink.entity.ModEntities;
import de.weinschenk.starlink.menu.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static de.weinschenk.starlink.Starlink.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SATELLITE.get(), SatelliteRenderer::new);
        event.registerEntityRenderer(ModEntities.ROCKET.get(),    RocketRenderer::new);
        event.registerEntityRenderer(ModEntities.ROCKET_V2.get(), RocketV2Renderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.DISTILLATION_CHAMBER.get(), DistillationChamberScreen::new);
            MenuScreens.register(ModMenuTypes.LAUNCH_CONTROLLER.get(),    LaunchControllerScreen::new);
            MenuScreens.register(ModMenuTypes.ROCKET_V2.get(),            RocketV2Screen::new);
            MenuScreens.register(ModMenuTypes.LAUNCH_CONTROLLER_V2.get(), LaunchControllerV2Screen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SatelliteRenderer.LAYER_LOCATION, SatelliteRenderer::createBodyLayer);
        event.registerLayerDefinition(RocketRenderer.LAYER_LOCATION, RocketRenderer::createBodyLayer);
    }
}
