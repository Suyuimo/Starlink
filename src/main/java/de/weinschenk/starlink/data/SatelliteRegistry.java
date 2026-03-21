package de.weinschenk.starlink.data;

import de.weinschenk.starlink.entity.SatelliteEntity;
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
import java.util.Random;
import java.util.UUID;

/**
 * Persistentes Satelliten-Register.
 * Speichert Startwinkel + Starttick und berechnet die aktuelle Position mathematisch.
 * Alle Satelliten umkreisen den Weltmittelpunkt auf einem Kreis mit Radius ORBIT_RADIUS.
 */
public class SatelliteRegistry extends SavedData {

    public static final String NAME = "starlink_satellites";

    private final Map<UUID, SatEntry> satellites = new HashMap<>();

    /**
     * Zufällige, eindeutige Winkelgeschwindigkeits-Faktoren pro Orbit-Index.
     */
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
                           boolean isPrivate, String pin) {

        public double currentAngle(long tick) {
            return angle0 + (tick - startTick) * SatelliteEntity.ANGULAR_VELOCITY * velocityFactor;
        }

        public double currentX(long tick) {
            return SatelliteEntity.ORBIT_RADIUS * Math.cos(currentAngle(tick));
        }

        public double currentZ(long tick) {
            return SatelliteEntity.ORBIT_RADIUS * Math.sin(currentAngle(tick));
        }
    }

    // -------------------------------------------------------------------------

    public static SatelliteRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(SatelliteRegistry::load, SatelliteRegistry::new, NAME);
    }

    // -------------------------------------------------------------------------

    /** Registriert einen öffentlichen Satelliten. */
    public void register(UUID id, int orbitId, double angle0, long startTick) {
        register(id, orbitId, angle0, startTick, false, "");
    }

    /** Registriert einen Satelliten mit Privacy-Einstellung. */
    public void register(UUID id, int orbitId, double angle0, long startTick,
                         boolean isPrivate, String pin) {
        double factor = getOrbitVelocityFactor(orbitId);
        satellites.put(id, new SatEntry(orbitId, factor, angle0, startTick, isPrivate, pin));
        setDirty();
    }

    public void unregister(UUID id) {
        if (satellites.remove(id) != null) setDirty();
    }

    public void clear() {
        if (!satellites.isEmpty() || !orbitVelocityFactors.isEmpty()) {
            satellites.clear();
            orbitVelocityFactors.clear();
            setDirty();
        }
    }

    // -------------------------------------------------------------------------
    // Satelliten-Abfragen
    // -------------------------------------------------------------------------

    private static final double CONE_HALF_ANGLE_RAD = Math.toRadians(20.0);

    /**
     * Zählt alle Satelliten im ±20°-Bogen um die Spieler-Richtung (ungefiltert).
     */
    public int countNear(double posX, double posY, double posZ, long currentTick) {
        return countNearFiltered(posX, posY, posZ, currentTick, SignalFilterMode.ALL, "");
    }

    /**
     * Zählt Satelliten mit Privacy-Filter.
     * ALL         → alle
     * PUBLIC_ONLY → nur öffentliche
     * PRIVATE_ONLY→ nur private, deren PIN mit {@code pin} übereinstimmt
     */
    public int countNearFiltered(double posX, double posY, double posZ, long currentTick,
                                  SignalFilterMode mode, String pin) {
        if (satellites.isEmpty()) return 0;
        double playerAngle = Math.atan2(posZ, posX);
        int count = 0;
        for (SatEntry e : satellites.values()) {
            boolean include = switch (mode) {
                case ALL          -> true;
                case PUBLIC_ONLY  -> !e.isPrivate();
                case PRIVATE_ONLY -> e.isPrivate() && e.pin().equals(pin);
            };
            if (!include) continue;
            double da = e.currentAngle(currentTick) - playerAngle;
            da -= 2 * Math.PI * Math.floor((da + Math.PI) / (2 * Math.PI));
            if (Math.abs(da) <= CONE_HALF_ANGLE_RAD) count++;
        }
        return count;
    }

    public List<SatelliteRenderData> getAllRenderData(long currentTick) {
        List<SatelliteRenderData> list = new ArrayList<>(satellites.size());
        for (SatEntry e : satellites.values()) {
            list.add(new SatelliteRenderData(
                    e.currentX(currentTick),
                    e.currentZ(currentTick),
                    e.currentAngle(currentTick),
                    e.orbitId(),
                    e.isPrivate(),
                    e.pin()));
        }
        return list;
    }

    public int total() {
        return satellites.size();
    }

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
            boolean isPrivate = s.contains("Private") && s.getBoolean("Private");
            String pin = s.contains("Pin") ? s.getString("Pin") : "";
            reg.satellites.put(s.getUUID("Id"),
                    new SatEntry(orbitId, factor, s.getDouble("Angle"), s.getLong("Tick"),
                            isPrivate, pin));
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
            s.putUUID("Id",        id);
            s.putInt("Orbit",      e.orbitId());
            s.putDouble("VFactor", e.velocityFactor());
            s.putDouble("Angle",   e.angle0());
            s.putLong("Tick",      e.startTick());
            s.putBoolean("Private", e.isPrivate());
            s.putString("Pin",     e.pin());
            list.add(s);
        });
        tag.put("Sats", list);
        return tag;
    }
}
