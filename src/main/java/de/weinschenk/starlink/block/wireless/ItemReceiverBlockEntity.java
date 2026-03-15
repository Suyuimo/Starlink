package de.weinschenk.starlink.block.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemReceiverBlockEntity extends TieredWirelessBlockEntity implements IWirelessReceiver {

    public final ItemStackHandler handler = new ItemStackHandler(9);
    private final LazyOptional<ItemStackHandler> itemCap = LazyOptional.of(() -> handler);

    @Nullable
    private BlockPos linkedTransmitterPos = null;

    public ItemReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModWirelessBlockEntities.ITEM_RECEIVER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ItemReceiverBlockEntity be) {
        be.refreshSatelliteCount(level, pos);

        if (level.getGameTime() % 20 != 0) return;
        if (be.linkedTransmitterPos == null) return;

        BlockEntity rawBe = level.getBlockEntity(be.linkedTransmitterPos);
        if (!(rawBe instanceof ItemTransmitterBlockEntity tx)) return;

        int recSats = be.getCachedSatCount();
        int txSats = tx.getCachedSatCount();
        int effectiveSats = Math.min(recSats, txSats);

        if (effectiveSats <= 0) return;

        int bandwidthItems = WirelessTiers.itemsBandwidth(be.getTier(), effectiveSats);
        int remaining = bandwidthItems;

        for (int slot = 0; slot < tx.handler.getSlots() && remaining > 0; slot++) {
            ItemStack extracted = tx.handler.extractItem(slot, remaining, true);
            if (extracted.isEmpty()) continue;
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(be.handler, extracted, false);
            int moved = extracted.getCount() - leftover.getCount();
            tx.handler.extractItem(slot, moved, false);
            remaining -= moved;
        }

        if (remaining < bandwidthItems) {
            be.setChanged();
        }
    }

    @Override
    public void setLinkedTransmitter(BlockPos pos) {
        this.linkedTransmitterPos = pos;
        setChanged();
    }

    @Override
    @Nullable
    public BlockPos getLinkedTransmitter() {
        return linkedTransmitterPos;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        itemCap.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", handler.serializeNBT());
        if (linkedTransmitterPos != null) {
            tag.putInt("LinkedTxX", linkedTransmitterPos.getX());
            tag.putInt("LinkedTxY", linkedTransmitterPos.getY());
            tag.putInt("LinkedTxZ", linkedTransmitterPos.getZ());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) {
            handler.deserializeNBT(tag.getCompound("Items"));
        }
        if (tag.contains("LinkedTxX")) {
            linkedTransmitterPos = new BlockPos(
                    tag.getInt("LinkedTxX"),
                    tag.getInt("LinkedTxY"),
                    tag.getInt("LinkedTxZ")
            );
        }
    }
}
