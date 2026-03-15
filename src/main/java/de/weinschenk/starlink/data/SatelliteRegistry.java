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
     * @param angle0    Winkel (Radiant) zum Registrierungszeitpunkt
     * @param startTick Server-Gametick zum Registrierungszeitpunkt
     */
    public record SatEntry(double angle0, long startTick) {

        public double currentAngle(long tick) {
            return angle0 + (tick - startTick) * SatelliteEntity.ANGULAR_VELOCITY;
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

    public void register(UUID id, double angle0, long startTick) {
        satellites.put(id, new SatEntry(angle0, startTick));
        setDirty();
    }

    public void unregister(UUID id) {
        if (satellites.remove(id) != null) setDirty();
    }

    public void clear() {
        if (!satellites.isEmpty()) {
            satellites.clear();
            setDirty();
        }
    }

    /**
     * Zählt Satelliten die innerhalb eines 20°-Zenith-Kegels senkrecht über dem Block stehen.
     * Bedingung: horizontaler Abstand ≤ (ORBIT_HEIGHT - posY) * tan(20°)
     */
    private static final double MAX_H_DIST_FACTOR = Math.tan(Math.toRadians(20.0));

    public int countNear(double posX, double posY, double posZ, long currentTick) {
        double vDist = de.weinschenk.starlink.entity.SatelliteEntity.ORBIT_HEIGHT - posY;
        if (vDist <= 0) return 0;
        double maxHDist = vDist * MAX_H_DIST_FACTOR;
        int count = 0;
        for (SatEntry e : satellites.values()) {
            double dx = e.currentX(currentTick) - posX;
            double dz = e.currentZ(currentTick) - posZ;
            if (dx * dx + dz * dz <= maxHDist * maxHDist) count++;
        }
        return count;
    }

    public List<SatelliteRenderData> getAllRenderData(long currentTick) {
        List<SatelliteRenderData> list = new ArrayList<>(satellites.size());
        for (SatEntry e : satellites.values()) {
            list.add(new SatelliteRenderData(
                    e.currentX(currentTick),
                    e.currentZ(currentTick),
                    e.currentAngle(currentTick)));
        }
        return list;
    }

    public int total() {
        return satellites.size();
    }

    /** Read-only view für das Entity-Management. */
    public Map<UUID, SatEntry> getEntries() {
        return java.util.Collections.unmodifiableMap(satellites);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    public static SatelliteRegistry load(CompoundTag tag) {
        SatelliteRegistry reg = new SatelliteRegistry();
        ListTag list = tag.getList("Sats", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag s = list.getCompound(i);
            reg.satellites.put(s.getUUID("Id"),
                    new SatEntry(s.getDouble("Angle"), s.getLong("Tick")));
        }
        return reg;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        satellites.forEach((id, e) -> {
            CompoundTag s = new CompoundTag();
            s.putUUID("Id",       id);
            s.putDouble("Angle",  e.angle0());
            s.putLong("Tick",     e.startTick());
            list.add(s);
        });
        tag.put("Sats", list);
        return tag;
    }
}
