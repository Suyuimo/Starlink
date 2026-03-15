package de.weinschenk.starlink.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class OrbitGlassesItem extends Item implements Equipable {

    public OrbitGlassesItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /** Vanilla-Interface: ermöglicht Rechtsklick-Equipping in den Helm-Slot. */
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    /** Forge-Extension: weitere Systeme (Dispenser, etc.) erkennen den Slot. */
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }
}
