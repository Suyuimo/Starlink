package de.weinschenk.starlink.block.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyTransmitterBlockEntity extends TieredWirelessBlockEntity {

    static class InternalEnergyStorage extends EnergyStorage {
        InternalEnergyStorage(int capacity) {
            super(capacity);
        }

        void setEnergy(int e) {
            energy = Math.min(Math.max(0, e), capacity);
        }
    }

    private InternalEnergyStorage storage;

    // External capability: allows receive but blocks extract (transmitter is write-only externally)
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return storage.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // blocked externally
        }

        @Override
        public int getEnergyStored() {
            return storage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    });

    public EnergyTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModWirelessBlockEntities.ENERGY_TRANSMITTER.get(), pos, state);
        storage = new InternalEnergyStorage(WirelessTiers.energyBuffer(getTier()));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyTransmitterBlockEntity be) {
        be.refreshSatelliteCount(level, pos);
        // Ensure storage is initialized with correct tier capacity
        if (be.storage.getMaxEnergyStored() != WirelessTiers.energyBuffer(be.getTier())) {
            int stored = be.storage.getEnergyStored();
            be.storage = new InternalEnergyStorage(WirelessTiers.energyBuffer(be.getTier()));
            be.storage.setEnergy(stored);
        }
    }

    /**
     * Directly extracts energy bypassing the external capability restriction.
     */
    public int extractEnergyDirect(int amount, boolean simulate) {
        return storage.extractEnergy(amount, simulate);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
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
        tag.putInt("Energy", storage.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storage = new InternalEnergyStorage(WirelessTiers.energyBuffer(getTier()));
        storage.setEnergy(tag.getInt("Energy"));
    }
}
