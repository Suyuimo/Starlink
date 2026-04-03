package de.weinschenk.starlink.network;

import de.weinschenk.starlink.api.IStarlinkEndpoint;
import de.weinschenk.starlink.block.wireless.TieredWirelessBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetWirelessChannelPacket {

    private final BlockPos pos;
    private final String channel;

    public SetWirelessChannelPacket(BlockPos pos, String channel) {
        this.pos     = pos;
        this.channel = channel;
    }

    public static void encode(SetWirelessChannelPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeUtf(p.channel, 64);
    }

    public static SetWirelessChannelPacket decode(FriendlyByteBuf buf) {
        return new SetWirelessChannelPacket(buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(SetWirelessChannelPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            BlockEntity be = player.level().getBlockEntity(p.pos);
            if (be instanceof TieredWirelessBlockEntity wb) {
                wb.setChannel(p.channel);
            } else if (be != null) {
                be.getCapability(IStarlinkEndpoint.CAPABILITY)
                        .ifPresent(ep -> ep.setChannel(p.channel));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
