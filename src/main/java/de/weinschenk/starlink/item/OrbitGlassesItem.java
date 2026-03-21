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

public class OrbitGlassesItem extends Item implements Equipable {

    public OrbitGlassesItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public EquipmentSlot getEquipmentSlot()             { return EquipmentSlot.HEAD; }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack s)  { return EquipmentSlot.HEAD; }

    /**
     * Normaler Rechtsklick: Brille anlegen.
     * Schleichen + Rechtsklick: Filter-Modus wechseln (Alle → Öffentlich → Privat → Alle …).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                GlassesFilterMode next = GlassesFilterMode.get(stack).next();
                GlassesFilterMode.set(stack, next);
                player.displayClientMessage(
                        Component.literal("§6[Starlink] §fFilter: §b" + next.displayName()), true);
            }
            return InteractionResultHolder.success(stack);
        }
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }
}
