package de.weinschenk.starlink.block.wireless;

public final class WirelessTiers {

    public static final int T8_SAT_REQUIRED = 20;

    private static final long[] ENERGY_PER_SAT = {
            64, 256, 1_024, 4_096, 16_384, 65_536, 262_144, 262_144
    };
    private static final int[] ITEMS_PER_SAT = {
            1, 4, 16, 64, 256, 1_024, 4_096, 4_096
    };
    private static final int[] FLUID_PER_SAT = {
            100, 400, 1_600, 6_400, 25_600, 102_400, 409_600, 409_600
    };

    private WirelessTiers() {}

    /**
     * Returns energy bandwidth (RF/t) for the given tier (1-8) and satellite count.
     */
    public static long energyBandwidth(int tier, int satCount) {
        if (satCount <= 0) return 0L;
        int idx = Math.min(tier, 8) - 1;
        if (tier == 8 && satCount >= T8_SAT_REQUIRED) {
            return Long.MAX_VALUE / 2;
        }
        return ENERGY_PER_SAT[idx] * (long) satCount;
    }

    /**
     * Returns item bandwidth (items/s) for the given tier (1-8) and satellite count.
     */
    public static int itemsBandwidth(int tier, int satCount) {
        if (satCount <= 0) return 0;
        int idx = Math.min(tier, 8) - 1;
        if (tier == 8 && satCount >= T8_SAT_REQUIRED) {
            return Integer.MAX_VALUE;
        }
        long bw = (long) ITEMS_PER_SAT[idx] * satCount;
        return (int) Math.min(bw, Integer.MAX_VALUE);
    }

    /**
     * Returns fluid bandwidth (mB/t) for the given tier (1-8) and satellite count.
     */
    public static int fluidBandwidth(int tier, int satCount) {
        if (satCount <= 0) return 0;
        int idx = Math.min(tier, 8) - 1;
        if (tier == 8 && satCount >= T8_SAT_REQUIRED) {
            return Integer.MAX_VALUE;
        }
        long bw = (long) FLUID_PER_SAT[idx] * satCount;
        return (int) Math.min(bw, Integer.MAX_VALUE);
    }

    /**
     * Returns energy buffer capacity (RF) for the given tier (1-8).
     */
    public static int energyBuffer(int tier) {
        return Math.min(tier, 8) * 4_000_000;
    }

    /**
     * Returns fluid buffer capacity (mB) for the given tier (1-8).
     */
    public static int fluidBuffer(int tier) {
        return Math.min(tier, 8) * 40_000;
    }
}
