package de.weinschenk.starlink.client.tracking;

import de.weinschenk.starlink.network.SatelliteRenderData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-seitiger Datenspeicher für Satellitenpositionen.
 * Wird vom SatelliteDataPacket aktualisiert.
 */
public class SatelliteTrackingClient {

    private static volatile List<SatelliteRenderData> satellites = Collections.emptyList();

    public static void update(List<SatelliteRenderData> data) {
        satellites = new ArrayList<>(data);
    }

    public static List<SatelliteRenderData> getSatellites() {
        return satellites;
    }

    public static void clear() {
        satellites = Collections.emptyList();
    }
}
