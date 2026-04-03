package de.weinschenk.starlink.entity;

/**
 * The five satellite generations, each optimised for a different transfer type.
 * Multipliers are applied to the effective satellite count seen by wireless blocks:
 *   effectiveCount = sum over overhead sats of (type.mult for that transfer category)
 *
 * Basic    – balanced starter satellite
 * Energy   – specialised for RF energy transfer
 * Fluid    – specialised for fluid (liquid/gas) transfer
 * Item     – specialised for item (cargo) transfer
 * Advanced – high-bandwidth for all categories, rare end-game satellite
 */
public enum SatelliteType {

    //                                                                baseLifeDays
    BASIC(   0, "basic",    1.0, 1.0, 1.0, 5),   // 5 MC-Tage  (~1:40h real)
    ENERGY(  1, "energy",   4.0, 0.5, 0.5, 3),   // 3 MC-Tage  — stromhungrig
    FLUID(   2, "fluid",    0.5, 4.0, 0.5, 4),   // 4 MC-Tage
    ITEM(    3, "item",     0.5, 0.5, 4.0, 4),   // 4 MC-Tage
    ADVANCED(4, "advanced", 3.0, 3.0, 3.0, 7);   // 7 MC-Tage  — hochwertige Bauweise

    /** Numeric ID stored in NBT and synched data. */
    public final int id;
    /** Registry key suffix used for item / texture names. */
    public final String key;
    /** Effective-count multiplier for RF energy bandwidth. */
    public final double energyMult;
    /** Effective-count multiplier for fluid bandwidth. */
    public final double fluidMult;
    /** Effective-count multiplier for item bandwidth. */
    public final double itemMult;
    /** Base lifetime in Minecraft days (1 day = 24 000 ticks). Actual lifetime has ±20 % random variation. */
    public final int baseLifeDays;

    SatelliteType(int id, String key, double energyMult, double fluidMult, double itemMult, int baseLifeDays) {
        this.id           = id;
        this.key          = key;
        this.energyMult   = energyMult;
        this.fluidMult    = fluidMult;
        this.itemMult     = itemMult;
        this.baseLifeDays = baseLifeDays;
    }

    public static SatelliteType byId(int id) {
        for (SatelliteType t : values()) {
            if (t.id == id) return t;
        }
        return BASIC;
    }
}
