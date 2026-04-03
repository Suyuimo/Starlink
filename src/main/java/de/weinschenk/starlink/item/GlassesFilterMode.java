package de.weinschenk.starlink.item;

import net.minecraft.world.item.ItemStack;

/**
 * Visueller Filter-Modus für Orbit-Brille.
 * Satelliten sind ein geteiltes Medium — nur ALL ist sinnvoll.
 */
public enum GlassesFilterMode {
    ALL;

    public GlassesFilterMode next() { return ALL; }

    public String displayName() { return "Alle"; }

    public static GlassesFilterMode get(ItemStack stack) { return ALL; }

    public static void set(ItemStack stack, GlassesFilterMode mode) {}
}
