package de.weinschenk.starlink.network;

/**
 * Leichtgewichtige Satelliten-Daten für die Client-seitige Darstellung.
 * Enthält nur was der Renderer braucht.
 */
public record SatelliteRenderData(double x, double z, int direction, boolean axisX) {}
