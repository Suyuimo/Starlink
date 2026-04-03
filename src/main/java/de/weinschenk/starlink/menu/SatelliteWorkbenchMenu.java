package de.weinschenk.starlink.menu;

import de.weinschenk.starlink.block.ModBlocks;
import de.weinschenk.starlink.recipe.ModRecipeTypes;
import de.weinschenk.starlink.recipe.SatelliteCraftingRecipe;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class SatelliteWorkbenchMenu extends AbstractContainerMenu {

    private static final int RESULT_SLOT      = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int CRAFT_SLOT_END   = 10;
    private static final int INV_SLOT_START   = 10;
    private static final int HOTBAR_START     = 37;
    private static final int HOTBAR_END       = 46;

    private final CraftingContainer craftSlots;
    private final ResultContainer   resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public SatelliteWorkbenchMenu(int containerId, Inventory inv) {
        this(containerId, inv, ContainerLevelAccess.NULL);
    }

    public SatelliteWorkbenchMenu(int containerId, Inventory inv, ContainerLevelAccess access) {
        super(ModMenuTypes.SATELLITE_WORKBENCH.get(), containerId);
        this.access = access;
        this.player = inv.player;
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);

        this.addSlot(new ResultSlot(inv.player, craftSlots, resultSlots, 0, 124, 35));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                this.addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, 17 + row * 18));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    @Override
    public void slotsChanged(Container container) {
        this.access.execute((level, pos) -> updateResult(level));
    }

    private void updateResult(Level level) {
        if (level.isClientSide) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;

        Optional<SatelliteCraftingRecipe> optional = level.getServer().getRecipeManager()
                .getRecipeFor(ModRecipeTypes.SATELLITE_CRAFTING.get(), craftSlots, level);

        if (optional.isPresent()) {
            SatelliteCraftingRecipe recipe = optional.get();
            if (resultSlots.setRecipeUsed(level, serverPlayer, recipe)) {
                result = recipe.assemble(craftSlots, level.registryAccess());
            }
        }

        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), 0, result));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> clearContainer(player, craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SATELLITE_WORKBENCH.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stackInSlot = slot.getItem();
        result = stackInSlot.copy();

        if (index == RESULT_SLOT) {
            this.access.execute((level, pos) -> stackInSlot.getItem().onCraftedBy(stackInSlot, level, player));
            if (!moveItemStackTo(stackInSlot, INV_SLOT_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stackInSlot, result);
        } else if (index >= INV_SLOT_START) {
            if (!moveItemStackTo(stackInSlot, CRAFT_SLOT_START, CRAFT_SLOT_END, false)) {
                if (index < HOTBAR_START) {
                    if (!moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
                } else {
                    if (!moveItemStackTo(stackInSlot, INV_SLOT_START, HOTBAR_START, false)) return ItemStack.EMPTY;
                }
            }
        } else {
            if (!moveItemStackTo(stackInSlot, INV_SLOT_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stackInSlot.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stackInSlot);
        if (index == RESULT_SLOT) player.drop(stackInSlot, false);
        return result;
    }
}
