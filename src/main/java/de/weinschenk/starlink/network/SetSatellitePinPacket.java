package de.weinschenk.starlink.network;

import de.weinschenk.starlink.item.SatelliteItem;
import de.weinschenk.starlink.menu.RocketV2Menu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → Server: Setzt den PIN für einen Satelliten-Slot im RocketV2-Menü.
 */
public class SetSatellitePinPacket {

    private final int    slotIndex;
    private final String pin;

    public SetSatellitePinPacket(int slotIndex, String pin) {
        this.slotIndex = slotIndex;
        this.pin       = pin;
    }

    public static void encode(SetSatellitePinPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.slotIndex);
        buf.writeUtf(p.pin, 64);
    }

    public static SetSatellitePinPacket decode(FriendlyByteBuf buf) {
        return new SetSatellitePinPacket(buf.readVarInt(), buf.readUtf(64));
    }

    public static void handle(SetSatellitePinPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof RocketV2Menu menu)) return;
            if (packet.slotIndex < 0 || packet.slotIndex >= 20) return;

            ItemStack stack = menu.getSlotItem(packet.slotIndex);
            if (stack.isEmpty() || !SatelliteItem.isPrivate(stack)) return;

            ItemStack updated = stack.copy();
            SatelliteItem.setPin(updated, packet.pin.trim());
            menu.setSlotItem(packet.slotIndex, updated);
        });
        ctx.get().setPacketHandled(true);
    }
}
