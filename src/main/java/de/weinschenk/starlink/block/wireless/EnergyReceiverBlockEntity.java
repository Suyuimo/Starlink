package de.weinschenk.starlink.block.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyReceiverBlockEntity extends TieredWirelessBlockEntity implements IWirelessReceiver {

    static class InternalEnergyStorage extends EnergyStorage {
        InternalEnergyStorage(int capacity) {
            super(capacity);
        }

        void setEnergy(int e) {
            energy = Math.min(Math.max(0, e), capacity);
        }
    }

    private InternalEnergyStorage storage;

    // External capability: allows extract but blocks receive (receiver is read-only externally)
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0; // blocked externally
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return storage.extractEnergy(maxExtract, simulate);
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
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    });

    @Nullable
    private BlockPos linkedTransmitterPos = null;

    public EnergyReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModWirelessBlockEntities.ENERGY_RECEIVER.get(), pos, state);
        storage = new InternalEnergyStorage(WirelessTiers.energyBuffer(getTier()));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyReceiverBlockEntity be) {
        be.refreshSatelliteCount(level, pos);

        if (be.linkedTransmitterPos == null) return;

        BlockEntity rawBe = level.getBlockEntity(be.linkedTransmitterPos);
        if (!(rawBe instanceof EnergyTransmitterBlockEntity tx)) return;

        int recSats = be.getCachedSatCount();
        int txSats = tx.getCachedSatCount();
        int effectiveSats = Math.min(recSats, txSats);

        if (effectiveSats <= 0) return;

        long bw = WirelessTiers.energyBandwidth(be.getTier(), effectiveSats);
        int toMove = (int) Math.min(bw, Integer.MAX_VALUE);
        int space = be.storage.getMaxEnergyStored() - be.storage.getEnergyStored();
        int moved = tx.extractEnergyDirect(Math.min(toMove, space), false);

        if (moved > 0) {
            be.storage.setEnergy(be.storage.getEnergyStored() + moved);
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
        if (linkedTransmitterPos != null) {
            tag.putInt("LinkedTxX", linkedTransmitterPos.getX());
            tag.putInt("LinkedTxY", linkedTransmitterPos.getY());
            tag.putInt("LinkedTxZ", linkedTransmitterPos.getZ());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storage = new InternalEnergyStorage(WirelessTiers.energyBuffer(getTier()));
        storage.setEnergy(tag.getInt("Energy"));
        if (tag.contains("LinkedTxX")) {
            linkedTransmitterPos = new BlockPos(
                    tag.getInt("LinkedTxX"),
                    tag.getInt("LinkedTxY"),
                    tag.getInt("LinkedTxZ")
            );
        }
    }
}
