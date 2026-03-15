package de.weinschenk.starlink.menu;

import de.weinschenk.starlink.block.ModBlockEntities;
import de.weinschenk.starlink.block.RocketV2BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class RocketV2Menu extends AbstractContainerMenu {

    public static final int DATA_SAT_COUNT = 0;
    public static final int DATA_COUNT     = 1;

    private final RocketV2BlockEntity blockEntity;
    private final ContainerData        data;

    /** Server-seitiger Konstruktor. */
    public RocketV2Menu(int containerId, Inventory playerInventory, RocketV2BlockEntity be) {
        super(ModMenuTypes.ROCKET_V2.get(), containerId);
        this.blockEntity = be;

        this.data = new ContainerData() {
            @Override public int get(int index) {
                return index == DATA_SAT_COUNT ? be.getSatelliteCount() : 0;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };

        // 20 Satelliten-Slots: 4 Spalten × 5 Reihen, ab x=26, y=18
        IItemHandler inv = be.getInventory();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new SlotItemHandler(inv, row * 4 + col, 26 + col * 18, 18 + row * 18));
            }
        }

        addPlayerInventory(playerInventory, 8, 118);
        addDataSlots(this.data);
    }

    /** Client-seitiger Konstruktor (liest BlockPos aus dem Buffer). */
    public RocketV2Menu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                (RocketV2BlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

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
