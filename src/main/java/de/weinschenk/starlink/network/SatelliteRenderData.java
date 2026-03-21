package de.weinschenk.starlink.network;

/**
 * Leichtgewichtige Satelliten-Daten für die Client-seitige Darstellung.
 * angle    = aktueller Winkel in Radiant
 * isPrivate= ob der Satellit privat ist
 * pin      = zugehöriger PIN (leer bei öffentlichen Satelliten)
 */
public record SatelliteRenderData(double x, double z, double angle, int orbitId,
                                   boolean isPrivate, String pin) {}
