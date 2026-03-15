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
 * Persistent satellite registry.
 * Stores start position + start tick per satellite and computes current position
 * mathematically — no entity ticking required.
 */
public class SatelliteRegistry extends SavedData {

    public static final String NAME = "starlink_satellites";

    private static final double ORBIT_LENGTH = SatelliteEntity.HALF_ORBIT * 2.0; // 60000

    private final Map<UUID, SatEntry> satellites = new HashMap<>();

    /**
     * @param startX    X position at registration time
     * @param startZ    Z position at registration time
     * @param startTick Server game tick at registration time
     * @param direction +1 or -1
     * @param axisX     true = moves along X, false = moves along Z
     */
    public record SatEntry(double startX, double startZ, long startTick,
                           int direction, boolean axisX) {

        public double currentX(long tick) {
            if (!axisX) return startX;
            return wrapOrbit(startX + (tick - startTick) * SatelliteEntity.SPEED * direction);
        }

        public double currentZ(long tick) {
            if (axisX) return startZ;
            return wrapOrbit(startZ + (tick - startTick) * SatelliteEntity.SPEED * direction);
        }

        private static double wrapOrbit(double v) {
            // wrap into [-HALF_ORBIT, +HALF_ORBIT)
            double h = SatelliteEntity.HALF_ORBIT;
            v = ((v + h) % ORBIT_LENGTH + ORBIT_LENGTH) % ORBIT_LENGTH - h;
            return v;
        }
    }

    // -------------------------------------------------------------------------

    public static SatelliteRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(SatelliteRegistry::load, SatelliteRegistry::new, NAME);
    }

    // -------------------------------------------------------------------------

    public void register(UUID id, double startX, double startZ,
                         long startTick, int direction, boolean axisX) {
        satellites.put(id, new SatEntry(startX, startZ, startTick, direction, axisX));
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

    public int countNear(double posX, double posZ, double range, long currentTick) {
        int count = 0;
        for (SatEntry e : satellites.values()) {
            if (Math.abs(e.currentX(currentTick) - posX) <= range
                    && Math.abs(e.currentZ(currentTick) - posZ) <= range) {
                count++;
            }
        }
        return count;
    }

    public List<SatelliteRenderData> getAllRenderData(long currentTick) {
        List<SatelliteRenderData> list = new ArrayList<>(satellites.size());
        for (SatEntry e : satellites.values()) {
            list.add(new SatelliteRenderData(
                    e.currentX(currentTick), e.currentZ(currentTick),
                    e.direction(), e.axisX()));
        }
        return list;
    }

    public int total() {
        return satellites.size();
    }

    /** Read-only view of all entries for entity management. */
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
            reg.satellites.put(s.getUUID("Id"), new SatEntry(
                    s.getDouble("X"), s.getDouble("Z"),
                    s.getLong("Tick"),
                    s.getInt("Dir"),
                    !s.contains("AxisX") || s.getBoolean("AxisX")));
        }
        return reg;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        satellites.forEach((id, e) -> {
            CompoundTag s = new CompoundTag();
            s.putUUID("Id", id);
            s.putDouble("X", e.startX());
            s.putDouble("Z", e.startZ());
            s.putLong("Tick", e.startTick());
            s.putInt("Dir", e.direction());
            s.putBoolean("AxisX", e.axisX());
            list.add(s);
        });
        tag.put("Sats", list);
        return tag;
    }
}
