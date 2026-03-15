package de.weinschenk.starlink.dimension;

/**
 * Definiert die festen Satelliten-Routen in der Orbit-Dimension.
 *
 * Routen sind Breitenparallelen (konstantes Z) mit wechselnder Flugrichtung:
 *   Route  0:  Z =     0,  Richtung +X
 *   Route  1:  Z =   500,  Richtung -X
 *   Route  2:  Z =  -500,  Richtung +X
 *   Route  3:  Z =  1000,  Richtung -X
 *   Route  4:  Z = -1000,  Richtung +X
 *   ...
 *
 * Abstand zwischen Routen: ROUTE_SPACING Blöcke in Z.
 * Richtung wechselt mit jeder Route (gerade Index = +X, ungerade = -X).
 */
public class OrbitRoutes {

    // Abstand zwischen zwei Routen in Z-Blöcken
    public static final int ROUTE_SPACING = 200;

    // Maximale Anzahl Routen in jede Richtung (±)
    private static final int MAX_ROUTES = 20;

    /**
     * Gibt die Z-Koordinate der nächstgelegenen Route zurück.
     * Der Satellit wird beim Spawn auf diese Z eingerastet.
     */
    public static double snapToNearestRouteZ(double spawnZ) {
        int routeIndex = (int) Math.round(spawnZ / ROUTE_SPACING);
        routeIndex = Math.max(-MAX_ROUTES, Math.min(MAX_ROUTES, routeIndex));
        return routeIndex * ROUTE_SPACING;
    }

    /**
     * Gibt die Flugrichtung (+1 oder -1) für eine Route anhand ihrer Z-Koordinate zurück.
     * Gerade Routen fliegen in +X, ungerade in -X.
     */
    public static int directionForRouteZ(double routeZ) {
        int routeIndex = (int) Math.round(routeZ / ROUTE_SPACING);
        // Negativen Index normalisieren damit Math.abs(index) % 2 korrekt ist
        return Math.abs(routeIndex) % 2 == 0 ? 1 : -1;
    }

    // ── Z-Achsen-Routen (konstantes X, Satellit fliegt in ±Z) ─────────────────

    /** Gibt die X-Koordinate der nächstgelegenen Z-Achsen-Route zurück. */
    public static double snapToNearestRouteX(double spawnX) {
        int routeIndex = (int) Math.round(spawnX / ROUTE_SPACING);
        routeIndex = Math.max(-MAX_ROUTES, Math.min(MAX_ROUTES, routeIndex));
        return routeIndex * ROUTE_SPACING;
    }

    /** Flugrichtung für Z-Achsen-Route (gerade Index = +Z, ungerade = -Z). */
    public static int directionForRouteX(double routeX) {
        int routeIndex = (int) Math.round(routeX / ROUTE_SPACING);
        return Math.abs(routeIndex) % 2 == 0 ? 1 : -1;
    }

    /**
     * Gibt alle aktiven Routen-Z-Koordinaten zurück (für Debug/Visualisierung).
     */
    public static int[] getAllRouteZValues(int count) {
        int[] routes = new int[count * 2 + 1];
        for (int i = -count; i <= count; i++) {
            routes[i + count] = i * ROUTE_SPACING;
        }
        return routes;
    }
}
