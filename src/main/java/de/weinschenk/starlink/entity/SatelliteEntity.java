package de.weinschenk.starlink.entity;

import de.weinschenk.starlink.data.SatelliteRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.UUID;
import java.util.List;

public class SatelliteEntity extends Entity {

    public static final double ORBIT_HEIGHT       = 100.0;
    public static final double SPEED              = 10.0;
    public static final double ORBIT_RADIUS       = 10_000.0;
    /** Winkelgeschwindigkeit in Radiant pro Tick (Basis, ohne velocityFactor). */
    public static final double ANGULAR_VELOCITY = SPEED / ORBIT_RADIUS;

    private static final double COLLISION_RADIUS = 15.0;
    private static final int    COLLISION_CHECK_INTERVAL = 100;

    private static final EntityDataAccessor<Float>   DATA_HEALTH =
            SynchedEntityData.defineId(SatelliteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(SatelliteEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float MAX_HEALTH = 20.0f;

    private boolean crashPending = false;

    /** Aktueller Orbit-Winkel in Radiant. */
    private double angle = 0.0;

    /** Orbit-Index (0–7), bestimmt Farbgebung und Gruppenzugehörigkeit. */
    private int orbitId = 0;

    /**
     * Multiplikator für die Winkelgeschwindigkeit dieses Orbits.
     * Positiv = Uhrzeigersinn, negativ = Gegenuhrzeigersinn.
     * Wird aus der SatelliteRegistry geladen, nicht persistiert (kommt immer frisch aus Registry).
     */
    private double velocityFactor = 1.0;

    /** Registry-UUID dieser Entity (kann von der Entity-UUID abweichen). */
    private UUID registryUUID = null;

    public void setRegistryUUID(UUID id) { this.registryUUID = id; }
    public UUID getRegistryUUID()        { return registryUUID != null ? registryUUID : getUUID(); }

    public void   setAngle(double angle)          { this.angle = angle; }
    public double getAngle()                      { return angle; }
    public void   setOrbitId(int id)              { this.orbitId = id; }
    public int    getOrbitId()                    { return orbitId; }
    public void   setVelocityFactor(double f)     { this.velocityFactor = f; }
    public double getVelocityFactor()             { return velocityFactor; }

    public SatelliteEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEALTH, MAX_HEALTH);
        this.entityData.define(DATA_ACTIVE, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isActive()) return;

        ServerLevel serverLevel = (ServerLevel) level();

        // Kreisförmige Orbitalbewegung
        angle += ANGULAR_VELOCITY * velocityFactor;
        double newX = ORBIT_RADIUS * Math.cos(angle);
        double newZ = ORBIT_RADIUS * Math.sin(angle);
        setPos(newX, ORBIT_HEIGHT, newZ);

        // Kollision prüfen (erst nach 20 Ticks Spawn-Schutz)
        if (tickCount >= 20 && tickCount % COLLISION_CHECK_INTERVAL == 0) {
            checkCollisions(serverLevel);
        }
    }

    // -------------------------------------------------------------------------
    // Kollisionserkennung
    // -------------------------------------------------------------------------

    private void checkCollisions(ServerLevel level) {
        AABB searchBox = new AABB(
                getX() - COLLISION_RADIUS, ORBIT_HEIGHT - COLLISION_RADIUS, getZ() - COLLISION_RADIUS,
                getX() + COLLISION_RADIUS, ORBIT_HEIGHT + COLLISION_RADIUS, getZ() + COLLISION_RADIUS
        );

        List<SatelliteEntity> nearby = level.getEntitiesOfClass(
                SatelliteEntity.class, searchBox,
                other -> other != this && other.isActive()
        );

        for (SatelliteEntity other : nearby) {
            if (distanceTo(other) <= COLLISION_RADIUS) {
                triggerCollision(other, level);
                break;
            }
        }
    }

    private void triggerCollision(SatelliteEntity other, ServerLevel level) {
        if (crashPending || other.crashPending) return;
        crashPending = true;
        other.crashPending = true;

        spawnCrashParticles(level, getX(), ORBIT_HEIGHT, getZ());
        spawnCrashParticles(level, other.getX(), ORBIT_HEIGHT, other.getZ());

        crash();
        other.crash();
    }

    public void crash() {
        setActive(false);
        if (!level().isClientSide) {
            SatelliteRegistry.get(((ServerLevel) level()).getServer()).unregister(getRegistryUUID());
        }
        discard();
    }

    private void spawnCrashParticles(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FIREWORK,          x, y, z, 40, 5.0, 2.0, 5.0, 0.1);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,       x, y, z, 30, 3.0, 1.0, 3.0, 0.05);
    }

    @Override
    public void remove(RemovalReason reason) {
        // Nur bei KILLED aus der Registry entfernen, nicht bei DISCARDED (= normales Despawn)
        if (reason == RemovalReason.KILLED && !level().isClientSide) {
            SatelliteRegistry.get(((ServerLevel) level()).getServer()).unregister(getRegistryUUID());
        }
        super.remove(reason);
    }

    // -------------------------------------------------------------------------
    // Schaden
    // -------------------------------------------------------------------------

    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        float newHealth = getHealth() - amount;
        if (newHealth <= 0) { crash(); return true; }
        setHealth(newHealth);
        return true;
    }

    // -------------------------------------------------------------------------
    // Getter / Setter
    // -------------------------------------------------------------------------

    public float   getHealth()          { return entityData.get(DATA_HEALTH); }
    public void    setHealth(float h)   { entityData.set(DATA_HEALTH, Math.max(0, Math.min(MAX_HEALTH, h))); }
    public boolean isActive()           { return entityData.get(DATA_ACTIVE); }
    public void    setActive(boolean a) { entityData.set(DATA_ACTIVE, a); }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setHealth(tag.getFloat("Health"));
        setActive(tag.getBoolean("Active"));
        angle   = tag.getDouble("Angle");
        orbitId = tag.contains("OrbitId") ? tag.getInt("OrbitId") : 0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health",   getHealth());
        tag.putBoolean("Active", isActive());
        tag.putDouble("Angle",   angle);
        tag.putInt("OrbitId",    orbitId);
    }

    @Override public boolean isPickable()    { return true; }
    @Override public boolean isPushable()    { return false; }
    @Override public boolean shouldBeSaved() { return false; }
}
