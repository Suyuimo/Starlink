package de.weinschenk.starlink.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class ReceiverMenu extends AbstractContainerMenu {

    // [0]=isActive  [1]=satCount
    public static final int DATA_COUNT = 2;

    private final ContainerData data;

    public ReceiverMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenuTypes.RECEIVER_MENU.get(), id);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        addDataSlots(data);
    }

    // Client-side constructor
    public ReceiverMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, new SimpleContainerData(DATA_COUNT));
    }

    public boolean isActive()   { return data.get(0) == 1; }
    public int getSatCount()    { return data.get(1); }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
