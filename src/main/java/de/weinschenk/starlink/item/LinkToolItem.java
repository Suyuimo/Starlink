package de.weinschenk.starlink.item;

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

        if (be instanceof IWirelessReceiver receiver) {
            // Check if we have a stored transmitter
            if (!stack.hasTag() || !stack.getTag().contains("TxX")) {
                player.sendSystemMessage(Component.literal("[Link] Zuerst einen Transmitter markieren."));
                return InteractionResult.FAIL;
            }
            CompoundTag tag = stack.getTag();
            BlockPos txPos = new BlockPos(tag.getInt("TxX"), tag.getInt("TxY"), tag.getInt("TxZ"));
            receiver.setLinkedTransmitter(txPos);
            player.sendSystemMessage(Component.literal("[Link] Empfänger verknüpft mit " + txPos));
            return InteractionResult.SUCCESS;
        }

        if (be instanceof TieredWirelessBlockEntity) {
            // It's a transmitter (or unrecognized wireless BE) — store pos
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
