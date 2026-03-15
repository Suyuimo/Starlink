package de.weinschenk.starlink.network;

import de.weinschenk.starlink.client.audio.RadioStreamPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client: Stoppt den Radio-Stream beim empfangenden Client.
 */
public class StopStreamPacket {

    public static void encode(StopStreamPacket packet, FriendlyByteBuf buf) {}

    public static StopStreamPacket decode(FriendlyByteBuf buf) {
        return new StopStreamPacket();
    }

    public static void handle(StopStreamPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> RadioStreamPlayer.INSTANCE.stop())
        );
        ctx.get().setPacketHandled(true);
    }
}
