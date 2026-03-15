package de.weinschenk.starlink.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Empfänger-Brille: zeigt nur Satelliten die sich gerade in Empfänger-Reichweite
 * (±100 Blöcke X/Z vom Spieler) befinden — exakt wie der ReceiverBlock prüft.
 */
public class ReceiverGlassesItem extends Item implements Equipable {

    public ReceiverGlassesItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() { return EquipmentSlot.HEAD; }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) { return EquipmentSlot.HEAD; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }
}
