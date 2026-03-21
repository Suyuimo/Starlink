package de.weinschenk.starlink.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Visueller Filter-Modus für Orbit- und Receiver-Brille.
 * Gespeichert als NBT auf dem ItemStack.
 *
 * ALL         → alle Satelliten anzeigen
 * PUBLIC_ONLY → nur öffentliche anzeigen
 * PRIVATE_ONLY→ nur private anzeigen (alle, ohne PIN-Prüfung – rein visuell)
 */
public enum GlassesFilterMode {
    ALL, PUBLIC_ONLY, PRIVATE_ONLY;

    private static final String NBT_KEY = "starlink:filter";

    public GlassesFilterMode next() {
        return values()[(ordinal() + 1) % 3];
    }

    public String displayName() {
        return switch (this) {
            case ALL          -> "Alle";
            case PUBLIC_ONLY  -> "Öffentlich";
            case PRIVATE_ONLY -> "Privat";
        };
    }

    public static GlassesFilterMode get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return ALL;
        int o = tag.getInt(NBT_KEY);
        return (o >= 0 && o < 3) ? values()[o] : ALL;
    }

    public static void set(ItemStack stack, GlassesFilterMode mode) {
        stack.getOrCreateTag().putInt(NBT_KEY, mode.ordinal());
    }
}
