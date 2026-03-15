package de.weinschenk.starlink.entity;

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

import java.util.List;

public class SatelliteEntity extends Entity {

    public static final double ORBIT_HEIGHT = 100.0;
    public static final double SPEED = 0.5;

    /** Halber Orbit-Umfang. Satellit fliegt von -HALF_ORBIT bis +HALF_ORBIT, dann Wrap. */
    public static final double HALF_ORBIT = 30_000.0;

    private static final double COLLISION_RADIUS = 15.0;
    private static final int    COLLISION_CHECK_INTERVAL = 10;

    private static final EntityDataAccessor<Float>   DATA_HEALTH    =
            SynchedEntityData.defineId(SatelliteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE    =
            SynchedEntityData.defineId(SatelliteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DIRECTION =
            SynchedEntityData.defineId(SatelliteEntity.class, EntityDataSerializers.INT);
    /** true = fliegt entlang X-Achse, false = entlang Z-Achse */
    private static final EntityDataAccessor<Boolean> DATA_AXIS_X    =
            SynchedEntityData.defineId(SatelliteEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float MAX_HEALTH = 20.0f;

    // Chunk-Force-Loading: aktueller Chunk + Lookahead
    private int lastForcedChunkX = Integer.MIN_VALUE;
    private int lastForcedChunkZ = Integer.MIN_VALUE;
    private int lookaheadChunkX  = Integer.MIN_VALUE;
    private int lookaheadChunkZ  = Integer.MIN_VALUE;

    private boolean crashPending = false;

    public SatelliteEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEALTH,    MAX_HEALTH);
        this.entityData.define(DATA_ACTIVE,    true);
        this.entityData.define(DATA_DIRECTION, 1);
        this.entityData.define(DATA_AXIS_X,    true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isActive()) return;

        ServerLevel serverLevel = (ServerLevel) level();

        forceLoadCurrentChunk(serverLevel);

        // Bewegen entlang der gewählten Achse + Orbit-Loop (Wrap bei ±HALF_ORBIT)
        if (isAxisX()) {
            double newX = getX() + SPEED * getOrbitDirection();
            if (newX >  HALF_ORBIT) newX = -HALF_ORBIT;
            if (newX < -HALF_ORBIT) newX =  HALF_ORBIT;
            setPos(newX, ORBIT_HEIGHT, getZ());
        } else {
            double newZ = getZ() + SPEED * getOrbitDirection();
            if (newZ >  HALF_ORBIT) newZ = -HALF_ORBIT;
            if (newZ < -HALF_ORBIT) newZ =  HALF_ORBIT;
            setPos(getX(), ORBIT_HEIGHT, newZ);
        }

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
        releaseChunk();
        discard();
    }

    private void spawnCrashParticles(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FIREWORK,          x, y, z, 40, 5.0, 2.0, 5.0, 0.1);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,       x, y, z, 30, 3.0, 1.0, 3.0, 0.05);
    }

    // -------------------------------------------------------------------------
    // Chunk Force-Loading (current + lookahead in Flugrichtung)
    // -------------------------------------------------------------------------

    private void forceLoadCurrentChunk(ServerLevel level) {
        int chunkX = (int) Math.floor(getX() / 16.0);
        int chunkZ = (int) Math.floor(getZ() / 16.0);

        // Lookahead: nächster Chunk in Flugrichtung
        int aheadX = chunkX + (isAxisX() ? getOrbitDirection() : 0);
        int aheadZ = chunkZ + (isAxisX() ? 0 : getOrbitDirection());

        level.setChunkForced(chunkX, chunkZ, true);
        level.setChunkForced(aheadX, aheadZ, true);

        // Alten Chunk freigeben wenn nicht mehr benötigt
        if (lastForcedChunkX != Integer.MIN_VALUE
                && (lastForcedChunkX != chunkX || lastForcedChunkZ != chunkZ)
                && (lastForcedChunkX != aheadX  || lastForcedChunkZ != aheadZ)) {
            level.setChunkForced(lastForcedChunkX, lastForcedChunkZ, false);
        }
        if (lookaheadChunkX != Integer.MIN_VALUE
                && (lookaheadChunkX != chunkX || lookaheadChunkZ != chunkZ)
                && (lookaheadChunkX != aheadX  || lookaheadChunkZ != aheadZ)) {
            level.setChunkForced(lookaheadChunkX, lookaheadChunkZ, false);
        }

        lastForcedChunkX = chunkX;
        lastForcedChunkZ = chunkZ;
        lookaheadChunkX  = aheadX;
        lookaheadChunkZ  = aheadZ;
    }

    private void releaseChunk() {
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        if (lastForcedChunkX != Integer.MIN_VALUE) {
            sl.setChunkForced(lastForcedChunkX, lastForcedChunkZ, false);
            lastForcedChunkX = Integer.MIN_VALUE;
            lastForcedChunkZ = Integer.MIN_VALUE;
        }
        if (lookaheadChunkX != Integer.MIN_VALUE) {
            sl.setChunkForced(lookaheadChunkX, lookaheadChunkZ, false);
            lookaheadChunkX = Integer.MIN_VALUE;
            lookaheadChunkZ = Integer.MIN_VALUE;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        // Chunks nur freigeben wenn der Satellit permanent entfernt wird (Crash, Discard).
        // Beim normalen Entladen (Shutdown, Chunk-Unload) die Force-Loading NICHT aufheben,
        // sonst ist der Chunk beim nächsten Serverstart nicht mehr geladen → Satellit verschwindet.
        if (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED) {
            releaseChunk();
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

    public float   getHealth()              { return entityData.get(DATA_HEALTH); }
    public void    setHealth(float h)       { entityData.set(DATA_HEALTH, Math.max(0, Math.min(MAX_HEALTH, h))); }
    public boolean isActive()               { return entityData.get(DATA_ACTIVE); }
    public void    setActive(boolean a)     { entityData.set(DATA_ACTIVE, a); }
    public int     getOrbitDirection()      { return entityData.get(DATA_DIRECTION); }
    public void    setOrbitDirection(int d) { entityData.set(DATA_DIRECTION, d > 0 ? 1 : -1); }
    public boolean isAxisX()               { return entityData.get(DATA_AXIS_X); }
    public void    setAxisX(boolean axisX) { entityData.set(DATA_AXIS_X, axisX); }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setHealth(tag.getFloat("Health"));
        setActive(tag.getBoolean("Active"));
        setOrbitDirection(tag.getInt("Direction"));
        setAxisX(!tag.contains("AxisX") || tag.getBoolean("AxisX")); // default: X-Achse
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health",     getHealth());
        tag.putBoolean("Active",   isActive());
        tag.putInt("Direction",    getOrbitDirection());
        tag.putBoolean("AxisX",    isAxisX());
    }

    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
}
