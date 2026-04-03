package de.weinschenk.starlink.block.wireless;

import de.weinschenk.starlink.menu.ModMenuTypes;
import de.weinschenk.starlink.menu.WirelessEnergyMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyReceiverBlockEntity extends TieredWirelessBlockEntity implements IWirelessReceiver {

    static class InternalEnergyStorage extends EnergyStorage {
        InternalEnergyStorage(int capacity) { super(capacity); }
        void setEnergy(int e) { energy = Math.min(Math.max(0, e), capacity); }
    }

    private InternalEnergyStorage storage;

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate)  { return storage.extractEnergy(maxExtract, simulate); }
        @Override public int getEnergyStored()    { return storage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return storage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    });

    // Mekanism Joule buffer (double, separate from Forge RF storage)
    private double mekanismBuffer = 0.0;
    private LazyOptional<?> mekanismCap = LazyOptional.empty();

    public double getMekanismBuffer()          { return mekanismBuffer; }
    public void   setMekanismBuffer(double v)  { mekanismBuffer = Math.max(0, v); setChanged(); }
    public void   addMekanismBuffer(double v)  { setMekanismBuffer(mekanismBuffer + v); }

    @Nullable
    private BlockPos linkedTransmitterPos = null;

    public EnergyReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModWirelessBlockEntities.ENERGY_RECEIVER.get(), pos, state);
        storage = new InternalEnergyStorage(WirelessTiers.energyBuffer(getTier()));
        if (ModList.get().isLoaded("mekanism")) {
            mekanismCap = MekanismEnergyCompat.lazyReceiver(this);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyReceiverBlockEntity be) {
        be.refreshSatelliteCount(level, pos);
        if (be.linkedTransmitterPos == null) return;

        BlockEntity rawBe = level.getBlockEntity(be.linkedTransmitterPos);
        if (!(rawBe instanceof EnergyTransmitterBlockEntity tx)) return;
        if (!be.getChannel().equals(tx.getChannel())) return;

        int effectiveSats = Math.min(be.getEnergySatCount(), tx.getEnergySatCount());
        if (effectiveSats <= 0) return;

        int rawSats = Math.min(be.getCachedSatCount(), tx.getCachedSatCount());
        long bw = WirelessTiers.energyBandwidth(be.getTier(), effectiveSats, rawSats);

        // ── Forge Energy (RF) transfer ────────────────────────────────────────
        int toMove = (int) Math.min(bw, Integer.MAX_VALUE);
        int space = be.storage.getMaxEnergyStored() - be.storage.getEnergyStored();
        int moved = tx.extractEnergyDirect(Math.min(toMove, space), false);
        if (moved > 0) {
            be.storage.setEnergy(be.storage.getEnergyStored() + moved);
            be.setChanged();
        }

        // ── Mekanism (Joules) transfer — unbounded double ─────────────────────
        if (ModList.get().isLoaded("mekanism") && tx.getMekanismBuffer() > 0) {
            MekanismEnergyCompat.transferEnergy(tx, be);
        }
    }

    public int getStoredEnergy()    { return storage.getEnergyStored(); }
    public int getStoredEnergyMax() { return storage.getMaxEnergyStored(); }

    @Override
    public void openGui(ServerPlayer player) {
        EnergyReceiverBlockEntity self = this;
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (id, inv, p) -> new WirelessEnergyMenu(ModMenuTypes.ENERGY_RECEIVER_MENU.get(), id, inv,
                new ContainerData() {
                    @Override public int get(int i) {
                        return switch (i) {
                            case 0 -> self.getStoredEnergy() / 1000;
                            case 1 -> self.getStoredEnergyMax() / 1000;
                            case 2 -> self.getEnergySatCount();
                            case 3 -> 1; // is receiver
                            case 4 -> self.linkedTransmitterPos != null ? 1 : 0;
                            case 5 -> self.isPrivate() ? 1 : 0;
                            default -> 0;
                        };
                    }
                    @Override public void set(int i, int v) {}
                    @Override public int getCount() { return WirelessEnergyMenu.DATA_COUNT; }
                },
                self.getBlockPos(), self.getChannel()),
            Component.translatable("block.starlink.energy_receiver")),
        buf -> { buf.writeBlockPos(self.getBlockPos()); buf.writeUtf(self.getChannel(), 64); });
    }

    @Override public void setLinkedTransmitter(BlockPos pos) { this.linkedTransmitterPos = pos; setChanged(); }
    @Override @Nullable public BlockPos getLinkedTransmitter() { return linkedTransmitterPos; }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (ModList.get().isLoaded("mekanism") && cap == MekanismEnergyCompat.CAPABILITY)
            return MekanismEnergyCompat.castLazy(mekanismCap.cast(), cap);
        return super.getCapability(cap, side);
    }

    @Override public void setRemoved() { super.setRemoved(); energyCap.invalidate(); mekanismCap.invalidate(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", storage.getEnergyStored());
        tag.putDouble("MekBuffer", mekanismBuffer);
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
        mekanismBuffer = tag.getDouble("MekBuffer");
        if (tag.contains("LinkedTxX")) {
            linkedTransmitterPos = new BlockPos(tag.getInt("LinkedTxX"), tag.getInt("LinkedTxY"), tag.getInt("LinkedTxZ"));
        }
    }
}
