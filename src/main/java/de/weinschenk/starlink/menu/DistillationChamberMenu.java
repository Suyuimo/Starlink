package de.weinschenk.starlink.menu;

import de.weinschenk.starlink.block.DistillationChamberBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DistillationChamberMenu extends AbstractContainerMenu {

    // ContainerData indices
    public static final int DATA_PROGRESS      = 0;
    public static final int DATA_MAX_PROGRESS  = 1;
    public static final int DATA_BURN_TIME     = 2;
    public static final int DATA_BURN_DURATION = 3;
    public static final int DATA_COUNT         = 4;

    private final DistillationChamberBlockEntity blockEntity;
    private final ContainerData data;

    // Geöffnet vom Server mit echter BE-Referenz
    public DistillationChamberMenu(int containerId, Inventory playerInventory, DistillationChamberBlockEntity be) {
        super(ModMenuTypes.DISTILLATION_CHAMBER.get(), containerId);
        this.blockEntity = be;

        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case DATA_PROGRESS      -> be.progress;
                    case DATA_MAX_PROGRESS  -> DistillationChamberBlockEntity.MAX_PROGRESS;
                    case DATA_BURN_TIME     -> be.burnTime;
                    case DATA_BURN_DURATION -> be.burnDuration;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case DATA_PROGRESS      -> be.progress = value;
                    case DATA_BURN_TIME     -> be.burnTime = value;
                    case DATA_BURN_DURATION -> be.burnDuration = value;
                }
            }
            @Override public int getCount() { return DATA_COUNT; }
        };

        addMachineSlots();
        addPlayerInventory(playerInventory);
        addDataSlots(this.data);
    }

    // Geöffnet vom Client (BE wird per BlockPos nachgeschlagen)
    public DistillationChamberMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                (DistillationChamberBlockEntity) playerInventory.player.level()
                        .getBlockEntity(buf.readBlockPos()));
    }

    private void addMachineSlots() {
        // Input: Raw Petroleum (oben links)
        addSlot(new Slot(blockEntity.items, DistillationChamberBlockEntity.SLOT_INPUT, 56, 17));
        // Fuel: Kohle (unten links)
        addSlot(new Slot(blockEntity.items, DistillationChamberBlockEntity.SLOT_FUEL, 56, 53));
        // Output: Rocket Fuel (rechts)
        addSlot(new Slot(blockEntity.items, DistillationChamberBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // Inventar-Reihen
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // Shift-Klick Logik
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < DistillationChamberBlockEntity.SLOT_COUNT) {
                // Aus Maschine → ins Spieler-Inventar
                if (!moveItemStackTo(stack, DistillationChamberBlockEntity.SLOT_COUNT, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Aus Spieler-Inventar → in passenden Maschinen-Slot
                if (!moveItemStackTo(stack, DistillationChamberBlockEntity.SLOT_INPUT, DistillationChamberBlockEntity.SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.items != null;
    }

    // Getter für den Screen
    public int getProgress()     { return data.get(DATA_PROGRESS); }
    public int getMaxProgress()  { return data.get(DATA_MAX_PROGRESS); }
    public int getBurnTime()     { return data.get(DATA_BURN_TIME); }
    public int getBurnDuration() { return data.get(DATA_BURN_DURATION); }

    public boolean isActive() { return getBurnTime() > 0; }
    public int getProgressScaled(int pixels) {
        return getMaxProgress() == 0 ? 0 : getProgress() * pixels / getMaxProgress();
    }
    public int getBurnScaled(int pixels) {
        return getBurnDuration() == 0 ? 0 : getBurnTime() * pixels / getBurnDuration();
    }
}
