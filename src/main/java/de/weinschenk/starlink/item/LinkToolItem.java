package de.weinschenk.starlink.item;

import de.weinschenk.starlink.api.IStarlinkEndpoint;
import de.weinschenk.starlink.block.wireless.IWirelessReceiver;
import de.weinschenk.starlink.block.wireless.TieredWirelessBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LinkToolItem extends Item {

    public LinkToolItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /** Called from TieredWirelessBlock.use() */
    public InteractionResult handleLink(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);

        // ── Resolve receiver interface (native or API) ────────────────────────
        IWirelessReceiver nativeReceiver = be instanceof IWirelessReceiver r ? r : null;
        IStarlinkEndpoint apiEndpoint    = be == null ? null :
                be.getCapability(IStarlinkEndpoint.CAPABILITY).orElse(null);
        IStarlinkEndpoint apiReceiver    = (apiEndpoint != null && apiEndpoint.canReceive()) ? apiEndpoint : null;

        if (nativeReceiver != null || apiReceiver != null) {
            if (!stack.hasTag() || !stack.getTag().contains("TxX")) {
                player.sendSystemMessage(Component.literal("[Link] Zuerst einen Transmitter markieren."));
                return InteractionResult.FAIL;
            }
            CompoundTag tag = stack.getTag();
            BlockPos txPos = new BlockPos(tag.getInt("TxX"), tag.getInt("TxY"), tag.getInt("TxZ"));

            // Channel check — works for both native and API endpoints
            String rxChannel = be instanceof TieredWirelessBlockEntity rxWl ? rxWl.getChannel()
                    : (apiReceiver != null ? apiReceiver.getChannel() : "");
            BlockEntity txBe = level.getBlockEntity(txPos);
            String txChannel = txBe instanceof TieredWirelessBlockEntity txWl ? txWl.getChannel()
                    : (txBe != null ? txBe.getCapability(IStarlinkEndpoint.CAPABILITY)
                            .map(IStarlinkEndpoint::getChannel).orElse("") : "");
            if (!txChannel.equals(rxChannel)) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c[Link] Kanal stimmt nicht überein! TX: \""
                        + txChannel + "\"  RX: \"" + rxChannel + "\""));
                return InteractionResult.FAIL;
            }

            if (nativeReceiver != null) nativeReceiver.setLinkedTransmitter(txPos);
            if (apiReceiver    != null) apiReceiver.setLinkedTransmitter(txPos);
            player.sendSystemMessage(Component.literal("[Link] Empfänger verknüpft mit " + txPos));
            return InteractionResult.SUCCESS;
        }

        // ── Transmitter — native or API ───────────────────────────────────────
        boolean isNativeTx = be instanceof TieredWirelessBlockEntity;
        boolean isApiTx    = apiEndpoint != null && apiEndpoint.canTransmit();
        if (isNativeTx || isApiTx) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt("TxX", pos.getX());
            tag.putInt("TxY", pos.getY());
            tag.putInt("TxZ", pos.getZ());
            player.sendSystemMessage(Component.literal("[Link] Transmitter gespeichert: " + pos));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
