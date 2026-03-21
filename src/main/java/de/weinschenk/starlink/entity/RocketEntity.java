package de.weinschenk.starlink.entity;

import com.mojang.logging.LogUtils;
import de.weinschenk.starlink.block.LaunchControllerBlockEntity;
import de.weinschenk.starlink.data.SatelliteRegistry;
import de.weinschenk.starlink.dimension.ModDimensions;
import org.slf4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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

import java.util.UUID;
import java.util.List;

public class RocketEntity extends Entity {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double RISE_SPEED = 0.5;
    private static final double LAUNCH_THRESHOLD_Y = 319.0;
    private static final int PARTICLE_INTERVAL = 2;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);

    private BlockPos controllerPos = BlockPos.ZERO;
    private UUID     launchingPlayerUuid = null;
    private int      orbitId = 0;

    public enum Phase { IGNITION, RISING, DEPARTED }

    public RocketEntity(EntityType<?> type, Level level) {
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
                    LOGGER.info("[Starlink] Rakete gezündet bei Y={}", (int) getY());
                    sendChatToPlayer("§6[Starlink] §fRakete gezündet! T-minus...");
                }
                if (tickCount >= 40) {
                    LOGGER.info("[Starlink] Rakete startet RISING-Phase");
                    setPhase(Phase.RISING);
                }
            }
            case RISING -> {
                this.setPos(getX(), getY() + RISE_SPEED, getZ());

                if (tickCount % PARTICLE_INTERVAL == 0) {
                    spawnThrustParticles((ServerLevel) level());
                }

                if (tickCount % 40 == 0) {
                    int currentY = (int) getY();
                    int percent = (int) ((currentY / LAUNCH_THRESHOLD_Y) * 100);
                    LOGGER.info("[Starlink] Rakete steigt... Y={} (Ziel: {})", currentY, (int) LAUNCH_THRESHOLD_Y);
                    sendChatToPlayer("§6[Starlink] §fHöhe: " + currentY + " / " + (int) LAUNCH_THRESHOLD_Y + " §7(" + percent + "%)");
                }

                if (getY() >= LAUNCH_THRESHOLD_Y) {
                    teleportToOrbit();
                }
            }
            case DEPARTED -> this.discard();
        }
    }

    private void spawnThrustParticles(ServerLevel level) {
        double x = getX();
        double y = getY() - 0.5;
        double z = getZ();
        level.sendParticles(ParticleTypes.FLAME,       x, y,       z, 8,  0.15, 0.0, 0.15, 0.05);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y - 0.5, z, 12, 0.3,  0.1, 0.3,  0.02);
        level.sendParticles(ParticleTypes.LAVA,        x, y,       z, 3,  0.1,  0.0, 0.1,  0.0);
    }

    /**
     * Berechnet den Orbit-Winkel aus der aktuellen X/Z-Position der Rakete,
     * platziert den Satelliten auf dem Orbit-Ring und registriert ihn.
     */
    private void teleportToOrbit() {
        ServerLevel orbitLevel = ((ServerLevel) level()).getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            LOGGER.error("[Starlink] Orbit-Dimension nicht gefunden! Satellit kann nicht gespawnt werden.");
            return;
        }

        // Winkel aus Starposition der Rakete → Punkt auf dem Orbit-Ring
        double angle  = Math.atan2(getZ(), getX());
        double r      = SatelliteEntity.ORBIT_RADIUS;
        double spawnX = r * Math.cos(angle);
        double spawnZ = r * Math.sin(angle);
        long   startTick = ((ServerLevel) level()).getServer().overworld().getGameTime();

        SatelliteEntity satellite = ModEntities.SATELLITE.get().create(orbitLevel);
        if (satellite != null) {
            satellite.setAngle(angle);
            satellite.setOrbitId(orbitId);
            satellite.setPos(spawnX, SatelliteEntity.ORBIT_HEIGHT, spawnZ);
            orbitLevel.addFreshEntity(satellite);
            SatelliteRegistry reg = SatelliteRegistry.get(((ServerLevel) level()).getServer());
            reg.register(satellite.getUUID(), orbitId, angle, startTick);
            satellite.setVelocityFactor(reg.getOrbitVelocityFactor(orbitId));
            LOGGER.info("[Starlink] Satellit gespawnt im Orbit: Winkel={}, X={}, Z={}",
                    String.format("%.4f", angle), (int) spawnX, (int) spawnZ);
        } else {
            LOGGER.error("[Starlink] Satellit-Entity konnte nicht erstellt werden!");
        }

        // Controller benachrichtigen
        BlockEntity be = level().getBlockEntity(controllerPos);
        if (be instanceof LaunchControllerBlockEntity controller) {
            controller.onRocketDeparted();
        }

        sendChatToPlayer("§6[Starlink] §aOrbit erreicht! §fSatellit deployed.");
        setPhase(Phase.DEPARTED);
        this.discard();
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

    public void setControllerPos(BlockPos pos)  { this.controllerPos = pos; }
    public void setLaunchingPlayer(UUID uuid)   { this.launchingPlayerUuid = uuid; }
    public void setOrbitId(int id)              { this.orbitId = id; }

    private void sendChatToPlayer(String message) {
        if (launchingPlayerUuid == null || !(level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(launchingPlayerUuid);
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        controllerPos = new BlockPos(tag.getInt("CtrlX"), tag.getInt("CtrlY"), tag.getInt("CtrlZ"));
        setPhase(Phase.values()[tag.getInt("Phase")]);
        orbitId = tag.contains("OrbitId") ? tag.getInt("OrbitId") : 0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CtrlX",   controllerPos.getX());
        tag.putInt("CtrlY",   controllerPos.getY());
        tag.putInt("CtrlZ",   controllerPos.getZ());
        tag.putInt("Phase",   getPhase().ordinal());
        tag.putInt("OrbitId", orbitId);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            BlockEntity be = level().getBlockEntity(controllerPos);
            if (be instanceof LaunchControllerBlockEntity controller) {
                controller.onRocketDeparted();
            }
        }
        super.remove(reason);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
}
