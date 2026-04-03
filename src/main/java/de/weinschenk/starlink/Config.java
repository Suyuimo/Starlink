package de.weinschenk.starlink;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Starlink.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> STREAM_URL = BUILDER
            .comment("URL of the radio stream to play when a receiver has satellite signal.",
                     "Supported formats: OGG, WAV, AIFF, AU.")
            .define("streamUrl", "http://localhost:8000/stream");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String streamUrl;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        streamUrl = STREAM_URL.get();
    }
}
