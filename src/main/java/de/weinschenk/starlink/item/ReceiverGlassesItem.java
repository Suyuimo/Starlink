package de.weinschenk.starlink.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ReceiverGlassesItem extends Item implements Equipable {

    public ReceiverGlassesItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public EquipmentSlot getEquipmentSlot()             { return EquipmentSlot.HEAD; }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack s)  { return EquipmentSlot.HEAD; }

    /**
     * Normaler Rechtsklick: Brille anlegen.
     * Schleichen + Rechtsklick: Aktuelle Signalstärke anzeigen (Anzahl Satelliten im 20°-Kegel).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            // Signalstatus wird client-seitig aus den gecachten Daten berechnet
            return InteractionResultHolder.success(stack);
        }
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }
}
