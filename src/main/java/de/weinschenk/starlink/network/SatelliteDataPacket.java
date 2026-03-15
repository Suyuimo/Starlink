package de.weinschenk.starlink.network;

import de.weinschenk.starlink.client.tracking.SatelliteTrackingClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server → Client: Aktuelle Positionen aller Satelliten im kreisförmigen Orbit.
 * Wird nur an Spieler gesendet die die OrbitBrille tragen.
 */
public class SatelliteDataPacket {

    private final List<SatelliteRenderData> satellites;

    public SatelliteDataPacket(List<SatelliteRenderData> satellites) {
        this.satellites = satellites;
    }

    public static void encode(SatelliteDataPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.satellites.size());
        for (SatelliteRenderData data : packet.satellites) {
            buf.writeDouble(data.x());
            buf.writeDouble(data.z());
            buf.writeDouble(data.angle());
        }
    }

    public static SatelliteDataPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<SatelliteRenderData> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new SatelliteRenderData(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        return new SatelliteDataPacket(list);
    }

    public static void handle(SatelliteDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> SatelliteTrackingClient.update(packet.satellites))
        );
        ctx.get().setPacketHandled(true);
    }
}
