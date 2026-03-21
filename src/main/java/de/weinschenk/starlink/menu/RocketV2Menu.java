package de.weinschenk.starlink.menu;

import de.weinschenk.starlink.block.ModBlockEntities;
import de.weinschenk.starlink.block.RocketV2BlockEntity;
import de.weinschenk.starlink.item.SatelliteItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class RocketV2Menu extends AbstractContainerMenu {

    public static final int DATA_SAT_COUNT = 0;
    public static final int DATA_COUNT     = 1;

    // Layout: Spieler-Inventar um 22px nach unten verschoben für die Config-Zeile
    public static final int PLAYER_INV_START_Y = 124;

    final RocketV2BlockEntity blockEntity;
    private final ContainerData data;

    /** Server-seitiger Konstruktor. */
    public RocketV2Menu(int containerId, Inventory playerInventory, RocketV2BlockEntity be) {
        super(ModMenuTypes.ROCKET_V2.get(), containerId);
        this.blockEntity = be;

        this.data = new ContainerData() {
            @Override public int get(int index) { return index == DATA_SAT_COUNT ? be.getSatelliteCount() : 0; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };

        // 20 Satelliten-Slots: 5 Spalten × 4 Reihen, ab x=8, y=18
        IItemHandler inv = be.getInventory();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 5; col++) {
                addSlot(new SlotItemHandler(inv, row * 5 + col, 8 + col * 18, 18 + row * 18));
            }
        }

        addPlayerInventory(playerInventory, 8, PLAYER_INV_START_Y);
        addDataSlots(this.data);
    }

    /** Client-seitiger Konstruktor. */
    public RocketV2Menu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                (RocketV2BlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    // -------------------------------------------------------------------------
    // Per-Slot Privacy-Toggle (Client sendet handleInventoryButtonClick → Server ruft hier auf)
    // -------------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= RocketV2BlockEntity.SATELLITE_SLOTS) return false;

        ItemStack stack = blockEntity.getInventory().getStackInSlot(id);
        if (stack.isEmpty() || !(stack.getItem() instanceof SatelliteItem)) return false;

        boolean nowPrivate = !SatelliteItem.isPrivate(stack);
        ItemStack updated = stack.copy();
        SatelliteItem.setPrivate(updated, nowPrivate);
        if (!nowPrivate) SatelliteItem.setPin(updated, ""); // PIN löschen wenn wieder öffentlich
        blockEntity.getInventory().setStackInSlot(id, updated);
        return true;
    }

    // -------------------------------------------------------------------------
    // Zugriff für SetSatellitePinPacket
    // -------------------------------------------------------------------------

    public ItemStack getSlotItem(int slotIndex) {
        return blockEntity.getInventory().getStackInSlot(slotIndex);
    }

    public void setSlotItem(int slotIndex, ItemStack stack) {
        blockEntity.getInventory().setStackInSlot(slotIndex, stack);
    }

    // -------------------------------------------------------------------------

    public int getSatelliteCount() { return data.get(DATA_SAT_COUNT); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved();
    }

    private void addPlayerInventory(Inventory inv, int startX, int startY) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, startX + col * 18, startY + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, startX + col * 18, startY + 58));
    }
}
