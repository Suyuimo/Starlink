package de.weinschenk.starlink.api;

import de.weinschenk.starlink.Starlink;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Starlink.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StarlinkCapabilities {

    private StarlinkCapabilities() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IStarlinkEndpoint.class);
    }
}
