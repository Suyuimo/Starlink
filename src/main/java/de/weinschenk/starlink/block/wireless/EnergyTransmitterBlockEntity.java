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

    // Mekanism Joule buffer (double, separate from Forge RF storage)
    private double mekanismBuffer = 0.0;
    private LazyOptional<?> mekanismCap = LazyOptional.empty();

    public double getMekanismBuffer()          { return mekanismBuffer; }
    public void   setMekanismBuffer(double v)  { mekanismBuffer = Math.max(0, v); setChanged(); }
    public void   addMekanismBuffer(double v)  { setMekanismBuffer(mekanismBuffer + v); }

    // External capability: allows receive but blocks extract (transmitter is write-only externally)
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return storage.receiveEnergy(maxReceive, simulate); }
        @Override public int extractEnergy(int maxExtract, boolean simulate)  { return 0; }
        @Override public int getEnergyStored()    { return storage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return storage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    });

    public EnergyTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModWirelessBlockEntities.ENERGY_TRANSMITTER.get(), pos, state);
        storage = new InternalEnergyStorage(WirelessTiers.energyBuffer(getTier()));
        if (ModList.get().isLoaded("mekanism")) {
            mekanismCap = MekanismEnergyCompat.lazyTransmitter(this);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyTransmitterBlockEntity be) {
        be.refreshSatelliteCount(level, pos);
        if (be.storage.getMaxEnergyStored() != WirelessTiers.energyBuffer(be.getTier())) {
            int stored = be.storage.getEnergyStored();
            be.storage = new InternalEnergyStorage(WirelessTiers.energyBuffer(be.getTier()));
            be.storage.setEnergy(stored);
        }
    }

    public int extractEnergyDirect(int amount, boolean simulate) {
        return storage.extractEnergy(amount, simulate);
    }

    public int getStoredEnergy()    { return storage.getEnergyStored(); }
    public int getStoredEnergyMax() { return storage.getMaxEnergyStored(); }

    @Override
    public void openGui(ServerPlayer player) {
        EnergyTransmitterBlockEntity self = this;
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (id, inv, p) -> new WirelessEnergyMenu(ModMenuTypes.ENERGY_TRANSMITTER_MENU.get(), id, inv,
                new ContainerData() {
                    @Override public int get(int i) {
                        return switch (i) {
                            case 0 -> self.getStoredEnergy() / 1000;
                            case 1 -> self.getStoredEnergyMax() / 1000;
                            case 2 -> self.getEnergySatCount();
                            case 5 -> self.isPrivate() ? 1 : 0;
                            default -> 0;
                        };
                    }
                    @Override public void set(int i, int v) {}
                    @Override public int getCount() { return WirelessEnergyMenu.DATA_COUNT; }
                },
                self.getBlockPos(), self.getChannel()),
            Component.translatable("block.starlink.energy_transmitter")),
        buf -> { buf.writeBlockPos(self.getBlockPos()); buf.writeUtf(self.getChannel(), 64); });
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (ModList.get().isLoaded("mekanism") && cap == MekanismEnergyCompat.CAPABILITY)
            return MekanismEnergyCompat.castLazy(mekanismCap.cast(), cap);
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        energyCap.invalidate();
        mekanismCap.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", storage.getEnergyStored());
        tag.putDouble("MekBuffer", mekanismBuffer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storage = new InternalEnergyStorage(WirelessTiers.energyBuffer(getTier()));
        storage.setEnergy(tag.getInt("Energy"));
        mekanismBuffer = tag.getDouble("MekBuffer");
    }
}
