package de.weinschenk.starlink.api;

import de.weinschenk.starlink.data.SatelliteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Public API for querying Starlink satellite coverage at any world position.
 *
 * <p>All methods are safe to call from any server-side tick. They return 0 / false
 * on the client side or when the server is not yet available.</p>
 *
 * <h3>Example usage</h3>
 * <pre>{@code
 * // In your block entity tick (server-side only):
 * int sats = StarlinkAPI.getSatelliteCount(level, getBlockPos());
 * long rfPerTick = sats * MY_RF_PER_SATELLITE;
 * }</pre>
 *
 * <h3>Satellite types and multipliers</h3>
 * <p>The weighted counts (energy / fluid / item) reflect the satellite's
 * specialisation — e.g. an ENERGY satellite contributes 4.0 to
 * {@link #getEnergySatelliteCount} but only 0.5 to the others.</p>
 */
public final class StarlinkAPI {

    private StarlinkAPI() {}

    // ── Raw count ─────────────────────────────────────────────────────────────

    /**
     * Returns the total number of satellites currently in the ±20° cone above
     * the given position (unweighted — each satellite counts as 1 regardless of type).
     */
    public static int getSatelliteCount(Level level, BlockPos pos) {
        SatelliteRegistry reg = getRegistry(level);
        if (reg == null) return 0;
        return reg.countNear(pos.getX(), pos.getY(), pos.getZ(), level.getGameTime());
    }

    /** Returns {@code true} if at least one satellite is currently overhead. */
    public static boolean hasCoverage(Level level, BlockPos pos) {
        return getSatelliteCount(level, pos) > 0;
    }

    // ── Weighted counts (respects satellite type specialisation) ──────────────

    /**
     * Returns the effective satellite count for RF energy transfer.
     * ENERGY satellites contribute 4.0 each; others contribute less.
     */
    public static int getEnergySatelliteCount(Level level, BlockPos pos) {
        SatelliteRegistry reg = getRegistry(level);
        if (reg == null) return 0;
        return reg.countNearEnergy(pos.getX(), pos.getY(), pos.getZ(), level.getGameTime());
    }

    /**
     * Returns the effective satellite count for fluid transfer.
     * FLUID satellites contribute 4.0 each; others contribute less.
     */
    public static int getFluidSatelliteCount(Level level, BlockPos pos) {
        SatelliteRegistry reg = getRegistry(level);
        if (reg == null) return 0;
        return reg.countNearFluid(pos.getX(), pos.getY(), pos.getZ(), level.getGameTime());
    }

    /**
     * Returns the effective satellite count for item transfer.
     * ITEM satellites contribute 4.0 each; others contribute less.
     */
    public static int getItemSatelliteCount(Level level, BlockPos pos) {
        SatelliteRegistry reg = getRegistry(level);
        if (reg == null) return 0;
        return reg.countNearItem(pos.getX(), pos.getY(), pos.getZ(), level.getGameTime());
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static SatelliteRegistry getRegistry(Level level) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return null;
        return SatelliteRegistry.get(serverLevel.getServer());
    }
}
