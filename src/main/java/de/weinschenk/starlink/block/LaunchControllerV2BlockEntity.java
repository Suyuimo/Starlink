package de.weinschenk.starlink.block;

import com.mojang.logging.LogUtils;
import de.weinschenk.starlink.entity.ModEntities;
import de.weinschenk.starlink.entity.RocketV2Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class LaunchControllerV2BlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int MAX_ENERGY  = 10_000_000;  // 10 Mio FE
    public static final int LAUNCH_COST =  5_000_000;  //  5 Mio FE
    public static final int MAX_RECEIVE =    200_000;  // 200k FE/tick

    private static final int VALIDATE_INTERVAL = 40;

    // V2: 8 LaunchPads als Ring eine Ebene UNTER dem Controller (Y-1).
    // Mitte (0,-1,0) bleibt frei → Energiekabel von unten anbindbar.
    private static final BlockPos[] PAD_OFFSETS = {
            new BlockPos(-1,-1,-1), new BlockPos(0,-1,-1), new BlockPos(1,-1,-1),
            new BlockPos(-1,-1, 0),                        new BlockPos(1,-1, 0),
            new BlockPos(-1,-1, 1), new BlockPos(0,-1, 1), new BlockPos(1,-1, 1)
    };

    public static final Set<LaunchControllerV2BlockEntity> ALL_LOADED =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static final int MAX_ORBIT_ID = 63;

    private boolean launching = false;
    private int     orbitId   = 0;

    // ---- Energiespeicher -------------------------------------------------------

    private class InternalEnergyStorage extends EnergyStorage {
        InternalEnergyStorage() {
            super(MAX_ENERGY, MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) setChanged();
            return received;
        }

        public void consume(int amount) {
            this.energy = Math.max(0, this.energy - amount);
            setChanged();
        }

        public void setEnergyDirect(int value) {
            this.energy = Math.max(0, Math.min(capacity, value));
        }
    }

    private final InternalEnergyStorage energyStorage = new InternalEnergyStorage();
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyStorage);

    // ---------------------------------------------------------------------------

    public LaunchControllerV2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_CONTROLLER_V2.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ALL_LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ALL_LOADED.remove(this);
        energyCap.invalidate();
    }

    // ---------------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchControllerV2BlockEntity be) {
        if (level.getGameTime() % VALIDATE_INTERVAL != 0) return;

        boolean valid = be.isMultiblockValid(level, pos);
        if (valid != state.getValue(LaunchControllerV2Block.READY)) {
            level.setBlock(pos, state.setValue(LaunchControllerV2Block.READY, valid), 3);
        }
    }

    // ---------------------------------------------------------------------------

    public LaunchResult tryLaunch(Level level, BlockPos pos, Player player) {
        if (launching)                              return LaunchResult.ALREADY_LAUNCHING;
        if (!isMultiblockValid(level, pos))         return LaunchResult.INVALID_MULTIBLOCK;
        if (energyStorage.getEnergyStored() < LAUNCH_COST) return LaunchResult.NOT_ENOUGH_ENERGY;

        BlockPos rocketPos = pos.above();
        BlockEntity rocketBe = level.getBlockEntity(rocketPos);
        if (!(rocketBe instanceof RocketV2BlockEntity rbe)) return LaunchResult.NO_ROCKET;

        int satCount = rbe.getSatelliteCount();
        if (satCount == 0) return LaunchResult.NO_SATELLITES;

        // Satelliten verbrauchen, Energie abziehen, Raketenblock entfernen
        var configs = rbe.consumeAllSatellitesWithConfig();
        energyStorage.consume(LAUNCH_COST);
        launching = true;
        setChanged();

        level.removeBlock(rocketPos, false);

        // RocketV2Entity spawnen
        RocketV2Entity rocket = new RocketV2Entity(ModEntities.ROCKET_V2.get(), level);
        rocket.setControllerPos(pos);
        rocket.setSatelliteConfigs(configs);
        rocket.setOrbitId(orbitId);
        if (player != null) rocket.setLaunchingPlayer(player.getUUID());
        rocket.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        level.addFreshEntity(rocket);

        LOGGER.info("[Starlink] RocketV2 launched with {} satellites, orbit={}", configs.size(), orbitId);
        return LaunchResult.SUCCESS;
    }

    public void onRocketDeparted() {
        launching = false;
        setChanged();
    }

    private boolean isMultiblockValid(Level level, BlockPos pos) {
        for (BlockPos offset : PAD_OFFSETS) {
            if (!level.getBlockState(pos.offset(offset)).is(ModBlocks.LAUNCH_PAD.get())) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------------------
    // Getters / Setters
    // ---------------------------------------------------------------------------

    public int  getEnergyStored()       { return energyStorage.getEnergyStored(); }
    public int  getEnergyStoredKilo()   { return energyStorage.getEnergyStored() / 1000; }
    public boolean isLaunching()        { return launching; }
    public int  getOrbitId()            { return orbitId; }
    public void setOrbitId(int id)      { orbitId = Math.max(0, Math.min(MAX_ORBIT_ID, id)); setChanged(); }

    // ContainerData client-sync only
    public void setEnergyKiloSync(int kiloFe) { energyStorage.setEnergyDirect(kiloFe * 1000); }

    public int getSatelliteCountAbove() {
        Level l = getLevel();
        if (l == null) return 0;
        BlockEntity be = l.getBlockEntity(getBlockPos().above());
        return be instanceof RocketV2BlockEntity rbe ? rbe.getSatelliteCount() : 0;
    }

    // ---------------------------------------------------------------------------
    // NBT
    // ---------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy",    energyStorage.getEnergyStored());
        tag.putBoolean("Launching", launching);
        tag.putInt("OrbitId",   orbitId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergyDirect(tag.getInt("Energy"));
        launching = tag.getBoolean("Launching");
        orbitId   = tag.contains("OrbitId") ? tag.getInt("OrbitId") : 0;
    }

    // ---------------------------------------------------------------------------
    // Capability
    // ---------------------------------------------------------------------------

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    // ---------------------------------------------------------------------------

    public enum LaunchResult {
        SUCCESS            ("RocketV2 gestartet! T-minus..."),
        ALREADY_LAUNCHING  ("Eine Rakete ist bereits unterwegs."),
        INVALID_MULTIBLOCK ("Startbasis unvollständig! 3x3 LaunchPad-Blöcke benötigt."),
        NOT_ENOUGH_ENERGY  ("Nicht genug Energie! Benötigt: 5.000.000 FE."),
        NO_ROCKET          ("Keine Rakete V2 auf dem Controller."),
        NO_SATELLITES      ("Keine Satelliten in der Rakete.");

        private final String message;
        LaunchResult(String msg) { message = msg; }
        public String message()  { return message; }
    }
}
