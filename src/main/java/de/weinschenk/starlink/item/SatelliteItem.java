package de.weinschenk.starlink.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SatelliteItem extends Item {

    public static final String TAG_PRIVATE = "starlink:private";
    public static final String TAG_PIN     = "starlink:pin";

    public SatelliteItem() {
        super(new Item.Properties().stacksTo(16));
    }

    // -------------------------------------------------------------------------
    // NBT-Helfer (statisch, damit RocketV2BlockEntity / RocketV2Menu sie nutzen können)
    // -------------------------------------------------------------------------

    public static boolean isPrivate(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_PRIVATE);
    }

    public static String getPin(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains(TAG_PIN)) ? tag.getString(TAG_PIN) : "";
    }

    public static void setPrivate(ItemStack stack, boolean priv) {
        stack.getOrCreateTag().putBoolean(TAG_PRIVATE, priv);
    }

    public static void setPin(ItemStack stack, String pin) {
        stack.getOrCreateTag().putString(TAG_PIN, pin);
    }
}
