package de.weinschenk.starlink.network;

import de.weinschenk.starlink.client.audio.RadioStreamPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client: Startet den Radio-Stream beim empfangenden Client.
 * Enthält die Stream-URL, damit der Client sich direkt verbinden kann.
 */
public class StartStreamPacket {

    private final String streamUrl;

    public StartStreamPacket(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public static void encode(StartStreamPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.streamUrl);
    }

    public static StartStreamPacket decode(FriendlyByteBuf buf) {
        return new StartStreamPacket(buf.readUtf());
    }

    public static void handle(StartStreamPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> RadioStreamPlayer.INSTANCE.start(packet.streamUrl))
        );
        ctx.get().setPacketHandled(true);
    }
}
