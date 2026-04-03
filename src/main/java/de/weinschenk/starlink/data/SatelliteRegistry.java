package de.weinschenk.starlink.data;

import de.weinschenk.starlink.entity.SatelliteEntity;
import de.weinschenk.starlink.entity.SatelliteType;
import de.weinschenk.starlink.network.SatelliteRenderData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Persistent satellite registry.
 * Satellites are a shared medium — no privacy/filtering at satellite level.
 */
public class SatelliteRegistry extends SavedData {

    public static final String NAME = "starlink_satellites";

    private final Map<UUID, SatEntry> satellites = new HashMap<>();
    private final Map<Integer, Double> orbitVelocityFactors = new HashMap<>();
    private static final Random RNG = new Random();

    public double getOrbitVelocityFactor(int orbitId) {
        return orbitVelocityFactors.computeIfAbsent(orbitId, id -> {
            double result = 1.0;
            for (int attempts = 0; attempts < 100; attempts++) {
                double raw = RNG.nextDouble() * 1.5 + 0.5;
                final double c = RNG.nextBoolean() ? raw : -raw;
                if (orbitVelocityFactors.values().stream().noneMatch(v -> Math.abs(v - c) < 0.15)) {
                    result = c;
                    break;
                }
            }
            setDirty();
            return result;
        });
    }

    public record SatEntry(int orbitId, double velocityFactor, double angle0, long startTick,
                           SatelliteType satType, long lifetimeTicks) {

        public double currentAngle(long tick) {
            return angle0 + (tick - startTick) * SatelliteEntity.ANGULAR_VELOCITY * velocityFactor;
        }

        public double currentX(long tick) {
            return SatelliteEntity.ORBIT_RADIUS * Math.cos(currentAngle(tick));
        }

        public double currentZ(long tick) {
            return SatelliteEntity.ORBIT_RADIUS * Math.sin(currentAngle(tick));
        }

        /** True when this satellite has exceeded its lifetime and should be removed. */
        public boolean isExpired(long currentTick) {
            return currentTick - startTick >= lifetimeTicks;
        }

        /** Remaining ticks until crash. Negative means already expired. */
        public long remainingTicks(long currentTick) {
            return startTick + lifetimeTicks - currentTick;
        }
    }

    // -------------------------------------------------------------------------

    public static SatelliteRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(SatelliteRegistry::load, SatelliteRegistry::new, NAME);
    }

    // -------------------------------------------------------------------------

    public void register(UUID id, int orbitId, double angle0, long startTick) {
        register(id, orbitId, angle0, startTick, SatelliteType.BASIC);
    }

    public void register(UUID id, int orbitId, double angle0, long startTick, SatelliteType satType) {
        double factor   = getOrbitVelocityFactor(orbitId);
        long   lifetime = computeLifetime(satType);
        satellites.put(id, new SatEntry(orbitId, factor, angle0, startTick, satType, lifetime));
        setDirty();
    }

    /** Lifetime = base days ± 20 % random variation, in ticks. */
    long computeLifetime(SatelliteType satType) {
        long base   = satType.baseLifeDays * 24_000L;
        double roll = 0.8 + RNG.nextDouble() * 0.4; // 0.80 – 1.20
        return (long)(base * roll);
    }

    /**
     * Removes all satellites whose lifetime has elapsed.
     * Returns the number removed.
     */
    public int removeExpired(long currentTick) {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, SatEntry> e : satellites.entrySet()) {
            if (e.getValue().isExpired(currentTick)) expired.add(e.getKey());
        }
        if (expired.isEmpty()) return 0;
        expired.forEach(satellites::remove);
        setDirty();
        return expired.size();
    }

    public void unregister(UUID id) {
        if (satellites.remove(id) != null) setDirty();
    }

    public int unregisterOrbit(int orbitId) {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, SatEntry> entry : satellites.entrySet()) {
            if (entry.getValue().orbitId() == orbitId) toRemove.add(entry.getKey());
        }
        toRemove.forEach(satellites::remove);
        if (!toRemove.isEmpty()) setDirty();
        return toRemove.size();
    }

    public Optional<UUID> getNearestInOrbit(int orbitId, double x, double z, long tick) {
        UUID nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<UUID, SatEntry> entry : satellites.entrySet()) {
            if (entry.getValue().orbitId() != orbitId) continue;
            SatEntry e = entry.getValue();
            double dx = e.currentX(tick) - x;
            double dz = e.currentZ(tick) - z;
            double dist = dx * dx + dz * dz;
            if (dist < minDist) { minDist = dist; nearest = entry.getKey(); }
        }
        return Optional.ofNullable(nearest);
    }

    public Optional<UUID> getNearestTo(double x, double z, long tick) {
        UUID nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<UUID, SatEntry> entry : satellites.entrySet()) {
            SatEntry e = entry.getValue();
            double dx = e.currentX(tick) - x;
            double dz = e.currentZ(tick) - z;
            double dist = dx * dx + dz * dz;
            if (dist < minDist) { minDist = dist; nearest = entry.getKey(); }
        }
        return Optional.ofNullable(nearest);
    }

    public void clear() {
        if (!satellites.isEmpty() || !orbitVelocityFactors.isEmpty()) {
            satellites.clear();
            orbitVelocityFactors.clear();
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // Satellite queries
    // -------------------------------------------------------------------------

    private static final double CONE_HALF_ANGLE_RAD = Math.toRadians(20.0);

    /** Counts all satellites within the ±20° cone above the position. */
    public int countNear(double posX, double posY, double posZ, long currentTick) {
        if (satellites.isEmpty()) return 0;
        double playerAngle = Math.atan2(posZ, posX);
        int count = 0;
        for (SatEntry e : satellites.values()) {
            double da = e.currentAngle(currentTick) - playerAngle;
            da -= 2 * Math.PI * Math.floor((da + Math.PI) / (2 * Math.PI));
            if (Math.abs(da) <= CONE_HALF_ANGLE_RAD) count++;
        }
        return count;
    }

    /**
     * Returns the effective satellite count weighted by the given transfer-type multipliers.
     * e.g. one ENERGY satellite contributes 4.0 toward energy, 0.5 toward fluid/item.
     */
    public int countNearWeighted(double posX, double posY, double posZ, long currentTick,
                                  double energyWeight, double fluidWeight, double itemWeight) {
        if (satellites.isEmpty()) return 0;
        double playerAngle = Math.atan2(posZ, posX);
        double total = 0.0;
        for (SatEntry e : satellites.values()) {
            double da = e.currentAngle(currentTick) - playerAngle;
            da -= 2 * Math.PI * Math.floor((da + Math.PI) / (2 * Math.PI));
            if (Math.abs(da) > CONE_HALF_ANGLE_RAD) continue;
            total += e.satType().energyMult * energyWeight
                   + e.satType().fluidMult  * fluidWeight
                   + e.satType().itemMult   * itemWeight;
        }
        return (int) Math.round(total);
    }

    public int countNearEnergy(double posX, double posY, double posZ, long tick) {
        return countNearWeighted(posX, posY, posZ, tick, 1.0, 0.0, 0.0);
    }

    public int countNearFluid(double posX, double posY, double posZ, long tick) {
        return countNearWeighted(posX, posY, posZ, tick, 0.0, 1.0, 0.0);
    }

    public int countNearItem(double posX, double posY, double posZ, long tick) {
        return countNearWeighted(posX, posY, posZ, tick, 0.0, 0.0, 1.0);
    }

    public List<SatelliteRenderData> getAllRenderData(long currentTick) {
        List<SatelliteRenderData> list = new ArrayList<>(satellites.size());
        for (SatEntry e : satellites.values()) {
            list.add(new SatelliteRenderData(
                    e.currentX(currentTick),
                    e.currentZ(currentTick),
                    e.currentAngle(currentTick),
                    e.orbitId(),
                    e.satType()));
        }
        return list;
    }

    public int total() { return satellites.size(); }

    public Map<UUID, SatEntry> getEntries() {
        return java.util.Collections.unmodifiableMap(satellites);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    public static SatelliteRegistry load(CompoundTag tag) {
        SatelliteRegistry reg = new SatelliteRegistry();

        ListTag factors = tag.getList("OrbitFactors", Tag.TAG_COMPOUND);
        for (int i = 0; i < factors.size(); i++) {
            CompoundTag f = factors.getCompound(i);
            reg.orbitVelocityFactors.put(f.getInt("OrbitId"), f.getDouble("Factor"));
        }

        ListTag list = tag.getList("Sats", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag s = list.getCompound(i);
            int orbitId = s.contains("Orbit") ? s.getInt("Orbit") : 0;
            double factor = s.contains("VFactor")
                    ? s.getDouble("VFactor")
                    : reg.getOrbitVelocityFactor(orbitId);
            SatelliteType satType = s.contains("SatType")
                    ? SatelliteType.byId(s.getInt("SatType")) : SatelliteType.BASIC;
            long lifetime = s.contains("Lifetime")
                    ? s.getLong("Lifetime")
                    : reg.computeLifetime(satType); // assign lifetime to legacy entries
            reg.satellites.put(s.getUUID("Id"),
                    new SatEntry(orbitId, factor, s.getDouble("Angle"), s.getLong("Tick"), satType, lifetime));
        }
        return reg;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag factors = new ListTag();
        orbitVelocityFactors.forEach((orbitId, factor) -> {
            CompoundTag f = new CompoundTag();
            f.putInt("OrbitId", orbitId);
            f.putDouble("Factor", factor);
            factors.add(f);
        });
        tag.put("OrbitFactors", factors);

        ListTag list = new ListTag();
        satellites.forEach((id, e) -> {
            CompoundTag s = new CompoundTag();
            s.putUUID("Id",         id);
            s.putInt("Orbit",       e.orbitId());
            s.putDouble("VFactor",  e.velocityFactor());
            s.putDouble("Angle",    e.angle0());
            s.putLong("Tick",       e.startTick());
            s.putInt("SatType",     e.satType().id);
            s.putLong("Lifetime",   e.lifetimeTicks());
            list.add(s);
        });
        tag.put("Sats", list);
        return tag;
    }
}
