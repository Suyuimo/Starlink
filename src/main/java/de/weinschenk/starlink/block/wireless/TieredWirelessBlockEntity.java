package de.weinschenk.starlink.block.wireless;

import de.weinschenk.starlink.data.SatelliteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TieredWirelessBlockEntity extends BlockEntity {

    private static final double SATELLITE_RANGE = 100.0;

    protected int cachedSatCount = 0;
    private long lastSatRefreshTick = -1L;

    // ---- Optional RF energy storage (Item/Fluid types only) -----------------

    private class RFStore extends EnergyStorage {
        RFStore(int cap) { super(cap, cap, 0); }
        boolean consume(int amount) {
            if (energy < amount) return false;
            energy -= amount;
            setChanged();
            return true;
        }
        void setDirect(int value) {
            energy = Math.max(0, Math.min(capacity, value));
        }
    }

    private RFStore energyStore = null;
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.empty();

    public TieredWirelessBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Call in subclass constructor to enable RF energy storage. */
    protected void initEnergy() {
        energyStore = new RFStore(WirelessTiers.energyBuffer(getTier()));
        energyCap = LazyOptional.of(() -> energyStore);
    }

    /**
     * Tries to consume {@code amount} RF. Returns true on success (or if this block
     * has no energy requirement). Returns false if insufficient energy.
     */
    public boolean consumeEnergy(int amount) {
        return energyStore == null || energyStore.consume(amount);
    }

    public int getEnergyStored() {
        return energyStore == null ? 0 : energyStore.getEnergyStored();
    }

    public int getMaxEnergy() {
        return energyStore == null ? 0 : energyStore.getMaxEnergyStored();
    }

    public int getCachedSatCount() {
        return cachedSatCount;
    }

    public int getTier() {
        if (getBlockState().getBlock() instanceof TieredWirelessBlock twb) {
            return twb.getTier();
        }
        return 1;
    }

    public void refreshSatelliteCount(Level level, BlockPos pos) {
        long currentTick = level.getGameTime();
        if (currentTick - lastSatRefreshTick < 20) return;
        lastSatRefreshTick = currentTick;

        if (!(level instanceof ServerLevel serverLevel)) return;

        cachedSatCount = SatelliteRegistry.get(serverLevel.getServer())
                .countNear(pos.getX(), pos.getZ(), SATELLITE_RANGE, serverLevel.getGameTime());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && energyStore != null) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        energyCap.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SatCache", cachedSatCount);
        if (energyStore != null) tag.putInt("RFEnergy", energyStore.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cachedSatCount = tag.getInt("SatCache");
        if (energyStore != null && tag.contains("RFEnergy")) energyStore.setDirect(tag.getInt("RFEnergy"));
    }
}
