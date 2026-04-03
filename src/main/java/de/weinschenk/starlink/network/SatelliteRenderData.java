package de.weinschenk.starlink.network;

import de.weinschenk.starlink.entity.SatelliteType;

/**
 * Lightweight satellite data for client-side rendering.
 * Satellites are a shared medium — no privacy filtering at satellite level.
 */
public record SatelliteRenderData(double x, double z, double angle, int orbitId,
                                   SatelliteType satType) {}
