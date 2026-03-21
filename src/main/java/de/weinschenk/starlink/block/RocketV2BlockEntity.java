package de.weinschenk.starlink.block;

import de.weinschenk.starlink.item.SatelliteItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RocketV2BlockEntity extends BlockEntity {

    public static final int SATELLITE_SLOTS = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(SATELLITE_SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof SatelliteItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> inventory);

    public RocketV2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROCKET_V2.get(), pos, state);
    }

    public int getSatelliteCount() {
        int count = 0;
        for (int i = 0; i < SATELLITE_SLOTS; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) count++;
        }
        return count;
    }

    /** Leert das Inventar und gibt die Anzahl der verbrauchten Satelliten zurück. */
    public int consumeAllSatellites() {
        int count = 0;
        for (int i = 0; i < SATELLITE_SLOTS; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
                count++;
            }
        }
        setChanged();
        return count;
    }

    /**
     * Leert das Inventar und gibt pro Satellit seine Privacy-Konfiguration zurück.
     * Wird von RocketV2Entity.deployToOrbit() verwendet.
     */
    public List<SatelliteConfig> consumeAllSatellitesWithConfig() {
        List<SatelliteConfig> configs = new ArrayList<>();
        for (int i = 0; i < SATELLITE_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                configs.add(new SatelliteConfig(
                        SatelliteItem.isPrivate(stack),
                        SatelliteItem.getPin(stack)));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        setChanged();
        return configs;
    }

    public record SatelliteConfig(boolean isPrivate, String pin) {}

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }
}
