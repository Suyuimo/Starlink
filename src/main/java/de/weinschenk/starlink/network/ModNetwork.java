package de.weinschenk.starlink.network;

import de.weinschenk.starlink.Starlink;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Starlink.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, StartStreamPacket.class,
                StartStreamPacket::encode,
                StartStreamPacket::decode,
                StartStreamPacket::handle);

        CHANNEL.registerMessage(id++, StopStreamPacket.class,
                StopStreamPacket::encode,
                StopStreamPacket::decode,
                StopStreamPacket::handle);

        CHANNEL.registerMessage(id++, SatelliteDataPacket.class,
                SatelliteDataPacket::encode,
                SatelliteDataPacket::decode,
                SatelliteDataPacket::handle);

        CHANNEL.registerMessage(id++, SetWirelessChannelPacket.class,
                SetWirelessChannelPacket::encode,
                SetWirelessChannelPacket::decode,
                SetWirelessChannelPacket::handle);
    }
}
