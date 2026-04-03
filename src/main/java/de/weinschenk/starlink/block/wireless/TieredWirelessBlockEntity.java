package de.weinschenk.starlink.block.wireless;

import de.weinschenk.starlink.data.SatelliteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    protected int cachedSatCount    = 0;  // total (unweighted) – used for radio signal
    protected int cachedEnergySats  = 0;  // weighted for RF energy transfer
    protected int cachedFluidSats   = 0;  // weighted for fluid transfer
    protected int cachedItemSats    = 0;  // weighted for item transfer
    private long lastSatRefreshTick = -1L;

    /** Kanalname: leer = öffentlich, nicht-leer = privat (nur gleicher Kanal darf verbinden/übertragen) */
    private String channel = "";

    public String getChannel() { return channel; }
    public void setChannel(String ch) { this.channel = ch == null ? "" : ch; setChanged(); }
    public boolean isPrivate() { return !channel.isEmpty(); }

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

    public int getCachedSatCount()   { return cachedSatCount; }
    public int getEnergySatCount()   { return cachedEnergySats; }
    public int getFluidSatCount()    { return cachedFluidSats; }
    public int getItemSatCount()     { return cachedItemSats; }

    /** Override in each subclass to open the block's GUI for this player. */
    public void openGui(ServerPlayer player) { }

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

        SatelliteRegistry reg = SatelliteRegistry.get(serverLevel.getServer());
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        long tick = serverLevel.getGameTime();

        cachedSatCount   = reg.countNear(x, y, z, tick);
        cachedEnergySats = reg.countNearEnergy(x, y, z, tick);
        cachedFluidSats  = reg.countNearFluid(x, y, z, tick);
        cachedItemSats   = reg.countNearItem(x, y, z, tick);
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
        tag.putString("Channel", channel);
        if (energyStore != null) tag.putInt("RFEnergy", energyStore.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cachedSatCount = tag.getInt("SatCache");
        channel = tag.contains("Channel") ? tag.getString("Channel") : "";
        if (energyStore != null && tag.contains("RFEnergy")) energyStore.setDirect(tag.getInt("RFEnergy"));
    }
}
