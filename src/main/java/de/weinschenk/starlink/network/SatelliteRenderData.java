package de.weinschenk.starlink.network;

/**
 * Leichtgewichtige Satelliten-Daten für die Client-seitige Darstellung.
 * angle = aktueller Winkel in Radiant (für Richtungspfeil im Overlay).
 */
public record SatelliteRenderData(double x, double z, double angle) {}
