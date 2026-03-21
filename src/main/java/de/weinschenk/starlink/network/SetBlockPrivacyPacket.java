package de.weinschenk.starlink.network;

import de.weinschenk.starlink.block.ReceiverBlockEntity;
import de.weinschenk.starlink.data.SignalFilterMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → Server: Setzt Privacy-Modus und PIN für einen ReceiverBlock.
 */
public class SetBlockPrivacyPacket {

    private final BlockPos pos;
    private final int      mode;
    private final String   pin;

    public SetBlockPrivacyPacket(BlockPos pos, int mode, String pin) {
        this.pos  = pos;
        this.mode = mode;
        this.pin  = pin;
    }

    public static void encode(SetBlockPrivacyPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeInt(p.mode);
        buf.writeUtf(p.pin, 64);
    }

    public static SetBlockPrivacyPacket decode(FriendlyByteBuf buf) {
        return new SetBlockPrivacyPacket(buf.readBlockPos(), buf.readInt(), buf.readUtf(64));
    }

    public static void handle(SetBlockPrivacyPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level() instanceof ServerLevel sl)) return;

            // Reichweiten-Check
            if (player.distanceToSqr(Vec3.atCenterOf(packet.pos)) > 64 * 64) return;

            BlockEntity be = sl.getBlockEntity(packet.pos);
            if (!(be instanceof ReceiverBlockEntity rbe)) return;

            int modeIdx = Math.max(0, Math.min(SignalFilterMode.values().length - 1, packet.mode));
            rbe.setMode(SignalFilterMode.values()[modeIdx]);
            rbe.setRequiredPin(packet.pin);
        });
        ctx.get().setPacketHandled(true);
    }
}
