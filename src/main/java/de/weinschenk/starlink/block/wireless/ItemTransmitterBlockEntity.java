package de.weinschenk.starlink.block.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemTransmitterBlockEntity extends TieredWirelessBlockEntity {

    public final ItemStackHandler handler = new ItemStackHandler(9);
    private final LazyOptional<ItemStackHandler> itemCap = LazyOptional.of(() -> handler);

    public ItemTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModWirelessBlockEntities.ITEM_TRANSMITTER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ItemTransmitterBlockEntity be) {
        be.refreshSatelliteCount(level, pos);
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) {
            handler.deserializeNBT(tag.getCompound("Items"));
        }
    }
}
