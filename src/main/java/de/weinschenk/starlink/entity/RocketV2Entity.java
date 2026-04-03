package de.weinschenk.starlink.entity;

import com.mojang.logging.LogUtils;
import de.weinschenk.starlink.block.LaunchControllerV2BlockEntity;
import de.weinschenk.starlink.block.RocketV2BlockEntity;
import de.weinschenk.starlink.data.SatelliteRegistry;
import de.weinschenk.starlink.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RocketV2Entity extends Entity {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double RISE_SPEED         = 0.5;
    private static final double LAUNCH_THRESHOLD_Y = 319.0;
    private static final int    PARTICLE_INTERVAL  = 2;

    /** Bogenlängenabstand in Blöcken zwischen zwei Satelliten beim Deployment */
    private static final double SATELLITE_SPACING = 50.0;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(RocketV2Entity.class, EntityDataSerializers.INT);

    private BlockPos controllerPos         = BlockPos.ZERO;
    private List<RocketV2BlockEntity.SatelliteConfig> satelliteConfigs = new ArrayList<>();
    private UUID     launchingPlayerUuid   = null;
    private int      orbitId               = 0;

    // Für NBT-Kompatibilität: alte Felder
    private int      legacySatelliteCount  = 0;

    public enum Phase { IGNITION, RISING, DEPARTED }

    public RocketV2Entity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_PHASE, Phase.IGNITION.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        Phase phase = getPhase();

        switch (phase) {
            case IGNITION -> {
                if (tickCount == 1) {
                    int count = satelliteConfigs.isEmpty() ? legacySatelliteCount : satelliteConfigs.size();
                    LOGGER.info("[Starlink] RocketV2 gezündet bei Y={}", (int) getY());
                    sendChatToPlayer("§6[Starlink V2] §fRakete gezündet! T-minus... §7(" + count + " Satelliten)");
                }
                if (tickCount >= 40) setPhase(Phase.RISING);
            }
            case RISING -> {
                this.setPos(getX(), getY() + RISE_SPEED, getZ());

                if (tickCount % PARTICLE_INTERVAL == 0) spawnThrustParticles((ServerLevel) level());

                if (tickCount % 40 == 0) {
                    int currentY = (int) getY();
                    int percent  = (int) ((currentY / LAUNCH_THRESHOLD_Y) * 100);
                    LOGGER.info("[Starlink] RocketV2 steigt... Y={}", currentY);
                    sendChatToPlayer("§6[Starlink V2] §fHöhe: " + currentY + " / " + (int) LAUNCH_THRESHOLD_Y + " §7(" + percent + "%)");
                }

                if (getY() >= LAUNCH_THRESHOLD_Y) deployToOrbit();
            }
            case DEPARTED -> this.discard();
        }
    }

    // -------------------------------------------------------------------------

    private void spawnThrustParticles(ServerLevel level) {
        double x = getX(), y = getY() - 0.5, z = getZ();
        level.sendParticles(ParticleTypes.FLAME,       x, y,       z, 8,  0.15, 0.0, 0.15, 0.05);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y - 0.5, z, 12, 0.3,  0.1, 0.3,  0.02);
        level.sendParticles(ParticleTypes.LAVA,        x, y,       z, 3,  0.1,  0.0, 0.1,  0.0);
    }

    // -------------------------------------------------------------------------

    private void deployToOrbit() {
        ServerLevel orbitLevel = ((ServerLevel) level()).getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            LOGGER.error("[Starlink] Orbit-Dimension nicht gefunden!");
            finishLaunch();
            return;
        }

        double baseAngle      = Math.atan2(getZ(), getX());
        double r              = SatelliteEntity.ORBIT_RADIUS;
        double angularSpacing = SATELLITE_SPACING / r;
        long   startTick      = ((ServerLevel) level()).getServer().overworld().getGameTime();

        SatelliteRegistry registry = SatelliteRegistry.get(((ServerLevel) level()).getServer());

        int total = satelliteConfigs.isEmpty() ? legacySatelliteCount : satelliteConfigs.size();
        int spawned = 0;

        for (int i = 0; i < total; i++) {
            SatelliteType satType = !satelliteConfigs.isEmpty()
                    ? satelliteConfigs.get(i).satType() : SatelliteType.BASIC;

            double angle  = baseAngle + i * angularSpacing;
            double spawnX = r * Math.cos(angle);
            double spawnZ = r * Math.sin(angle);

            SatelliteEntity satellite = ModEntities.SATELLITE.get().create(orbitLevel);
            if (satellite != null) {
                satellite.setAngle(angle);
                satellite.setOrbitId(orbitId);
                satellite.setSatelliteType(satType);
                satellite.setPos(spawnX, SatelliteEntity.ORBIT_HEIGHT, spawnZ);
                orbitLevel.addFreshEntity(satellite);
                registry.register(satellite.getUUID(), orbitId, angle, startTick, satType);
                satellite.setVelocityFactor(registry.getOrbitVelocityFactor(orbitId));
                spawned++;
            }
        }

        LOGGER.info("[Starlink] RocketV2: {} Satelliten deployed (50-Block-Abstand auf Orbit-Ring)", spawned);
        sendChatToPlayer("§6[Starlink V2] §a" + spawned + " Satelliten deployed! §7(Abstand " + (int) SATELLITE_SPACING + " Blöcke)");

        finishLaunch();
    }

    private void finishLaunch() {
        BlockEntity be = level().getBlockEntity(controllerPos);
        if (be instanceof LaunchControllerV2BlockEntity ctrl) ctrl.onRocketDeparted();
        setPhase(Phase.DEPARTED);
        this.discard();
    }

    // -------------------------------------------------------------------------
    // Chat
    // -------------------------------------------------------------------------

    private void sendChatToPlayer(String message) {
        if (launchingPlayerUuid == null || !(level() instanceof ServerLevel sl)) return;
        ServerPlayer player = sl.getServer().getPlayerList().getPlayer(launchingPlayerUuid);
        if (player != null) player.sendSystemMessage(Component.literal(message));
    }

    // -------------------------------------------------------------------------
    // Getter / Setter
    // -------------------------------------------------------------------------

    public Phase getPhase() {
        return Phase.values()[this.entityData.get(DATA_PHASE)];
    }

    public void setPhase(Phase phase) {
        this.entityData.set(DATA_PHASE, phase.ordinal());
    }

    public void setControllerPos(BlockPos pos)                          { this.controllerPos = pos; }
    public void setSatelliteConfigs(List<RocketV2BlockEntity.SatelliteConfig> configs) {
        this.satelliteConfigs = configs;
        this.legacySatelliteCount = configs.size();
    }
    public void setSatelliteCount(int count)                            { this.legacySatelliteCount = count; }
    public void setLaunchingPlayer(UUID uuid)                           { this.launchingPlayerUuid = uuid; }
    public void setOrbitId(int id)                                      { this.orbitId = id; }

    // -------------------------------------------------------------------------
    // remove() override
    // -------------------------------------------------------------------------

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            BlockEntity be = level().getBlockEntity(controllerPos);
            if (be instanceof LaunchControllerV2BlockEntity ctrl) ctrl.onRocketDeparted();
        }
        super.remove(reason);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        controllerPos = new BlockPos(tag.getInt("CtrlX"), tag.getInt("CtrlY"), tag.getInt("CtrlZ"));
        orbitId = tag.contains("OrbitId") ? tag.getInt("OrbitId") : 0;
        setPhase(Phase.values()[tag.getInt("Phase")]);
        if (tag.hasUUID("PlayerUUID")) launchingPlayerUuid = tag.getUUID("PlayerUUID");

        satelliteConfigs = new ArrayList<>();
        if (tag.contains("SatConfigs", Tag.TAG_LIST)) {
            ListTag list = tag.getList("SatConfigs", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                SatelliteType satType = SatelliteType.byId(entry.getInt("SatType"));
                satelliteConfigs.add(new RocketV2BlockEntity.SatelliteConfig(satType));
            }
        }
        legacySatelliteCount = satelliteConfigs.isEmpty() ? tag.getInt("SatCount") : satelliteConfigs.size();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CtrlX",    controllerPos.getX());
        tag.putInt("CtrlY",    controllerPos.getY());
        tag.putInt("CtrlZ",    controllerPos.getZ());
        tag.putInt("OrbitId",  orbitId);
        tag.putInt("Phase",    getPhase().ordinal());
        if (launchingPlayerUuid != null) tag.putUUID("PlayerUUID", launchingPlayerUuid);

        ListTag list = new ListTag();
        for (RocketV2BlockEntity.SatelliteConfig cfg : satelliteConfigs) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("SatType", cfg.satType().id);
            list.add(entry);
        }
        tag.put("SatConfigs", list);
        // Legacy-Feld für Abwärtskompatibilität
        tag.putInt("SatCount", satelliteConfigs.size());
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
}
