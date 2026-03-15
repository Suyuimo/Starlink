package de.weinschenk.starlink.entity;

import com.mojang.logging.LogUtils;
import de.weinschenk.starlink.block.LaunchControllerBlockEntity;
import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.dimension.OrbitRoutes;
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

    // Blöcke pro Tick die die Rakete steigt
    private static final double RISE_SPEED = 0.5;

    // Ab welcher Y-Koordinate in den Orbit teleportiert wird (Build-Limit 1.20.1)
    private static final double LAUNCH_THRESHOLD_Y = 319.0;

    // Partikel-Abstand in Ticks
    private static final int PARTICLE_INTERVAL = 2;

    // Synced: Phasen-Status für den Client (für visuelle Effekte)
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);

    // Ursprüngliche Position des Controllers — um nach dem Start zu benachrichtigen
    private BlockPos controllerPos = BlockPos.ZERO;

    // UUID des Spielers der die Rakete gestartet hat (für Chat-Nachrichten)
    private UUID launchingPlayerUuid = null;

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
                // Kurze Zündverzögerung (2 Sekunden = 40 Ticks)
                if (tickCount >= 40) {
                    LOGGER.info("[Starlink] Rakete startet RISING-Phase");
                    setPhase(Phase.RISING);
                }
            }
            case RISING -> {
                // Steigen
                this.setPos(getX(), getY() + RISE_SPEED, getZ());

                // Partikel: Rauch + Flammen unterhalb der Rakete
                if (tickCount % PARTICLE_INTERVAL == 0) {
                    spawnThrustParticles((ServerLevel) level());
                }

                // Fortschritt alle 2 Sekunden im Chat und Log
                if (tickCount % 40 == 0) {
                    int currentY = (int) getY();
                    int percent = (int) ((currentY / LAUNCH_THRESHOLD_Y) * 100);
                    LOGGER.info("[Starlink] Rakete steigt... Y={} (Ziel: {})", currentY, (int) LAUNCH_THRESHOLD_Y);
                    sendChatToPlayer("§6[Starlink] §fHöhe: " + currentY + " / " + (int) LAUNCH_THRESHOLD_Y + " §7(" + percent + "%)");
                }

                // Orbit erreicht?
                if (getY() >= LAUNCH_THRESHOLD_Y) {
                    teleportToOrbit();
                }
            }
            case DEPARTED -> {
                // Sollte nicht mehr existieren, aber zur Sicherheit
                this.discard();
            }
        }
    }

    /**
     * Spawnt Schub-Partikel unterhalb der Rakete.
     * Flammen + schwarzer Rauch für realistischen Raketeneffekt.
     */
    private void spawnThrustParticles(ServerLevel level) {
        double x = getX();
        double y = getY() - 0.5;
        double z = getZ();

        // Flammen-Kern
        level.sendParticles(ParticleTypes.FLAME,
                x, y, z,
                8,                  // Anzahl
                0.15, 0.0, 0.15,   // Spread X/Y/Z
                0.05);              // Geschwindigkeit

        // Großer Rauch
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                x, y - 0.5, z,
                12,
                0.3, 0.1, 0.3,
                0.02);

        // Glühende Funken
        level.sendParticles(ParticleTypes.LAVA,
                x, y, z,
                3,
                0.1, 0.0, 0.1,
                0.0);
    }

    /**
     * Teleportiert die Rakete in die Orbit-Dimension und spawnt dort einen Satelliten.
     * Benachrichtigt danach den LaunchController.
     */
    private void teleportToOrbit() {
        ServerLevel orbitLevel = ((ServerLevel) level()).getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            LOGGER.error("[Starlink] Orbit-Dimension nicht gefunden! Satellit kann nicht gespawnt werden.");
            return;
        }

        // Achse aus dem LaunchController lesen
        boolean axisX = true;
        BlockEntity ctrlBe = level().getBlockEntity(controllerPos);
        if (ctrlBe instanceof LaunchControllerBlockEntity ctrl) {
            axisX = ctrl.isOrbitAxisX();
        }

        // Route anhand der gewählten Achse bestimmen
        double routeZ, routeX;
        int    direction;
        if (axisX) {
            routeZ    = OrbitRoutes.snapToNearestRouteZ(getZ());
            routeX    = getX();
            direction = OrbitRoutes.directionForRouteZ(routeZ);
        } else {
            routeX    = OrbitRoutes.snapToNearestRouteX(getX());
            routeZ    = getZ();
            direction = OrbitRoutes.directionForRouteX(routeX);
        }

        // Spawn-Chunk vorab force-loaden, sonst landet der Satellit in einem
        // ungeladenen Chunk und tick() läuft nie → getEntitiesOfClass findet ihn nicht
        int spawnChunkX = (int) Math.floor(routeX / 16.0);
        int spawnChunkZ = (int) Math.floor(routeZ / 16.0);
        orbitLevel.setChunkForced(spawnChunkX, spawnChunkZ, true);

        // Satelliten in der Orbit-Dimension spawnen
        SatelliteEntity satellite = ModEntities.SATELLITE.get().create(orbitLevel);
        if (satellite != null) {
            satellite.setPos(routeX, SatelliteEntity.ORBIT_HEIGHT, routeZ);
            satellite.setOrbitDirection(direction);
            satellite.setAxisX(axisX);
            orbitLevel.addFreshEntity(satellite);
            LOGGER.info("[Starlink] Satellit gespawnt in Orbit-Dimension: X={}, Z={}, Richtung={}", getX(), routeZ, direction);
        } else {
            LOGGER.error("[Starlink] Satellit-Entity konnte nicht erstellt werden!");
        }

        // Controller über den abgeschlossenen Start informieren
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

    public void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
    }

    public void setLaunchingPlayer(UUID uuid) {
        this.launchingPlayerUuid = uuid;
    }

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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CtrlX", controllerPos.getX());
        tag.putInt("CtrlY", controllerPos.getY());
        tag.putInt("CtrlZ", controllerPos.getZ());
        tag.putInt("Phase", getPhase().ordinal());
    }

    /**
     * Stellt sicher, dass der Controller-Block immer benachrichtigt wird,
     * egal wie die Rakete entfernt wird (killed, discarded, Crash…).
     */
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

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }
}
