package de.weinschenk.starlink.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class WirelessItemMenu extends AbstractContainerMenu {

    // [0]=energy/1000  [1]=maxEnergy/1000  [2]=satCount  [3]=isReceiver  [4]=isLinked  [5]=isPrivate
    public static final int DATA_COUNT = 6;
    private static final int HANDLER_SLOTS = 9;
    private final ContainerData data;

    private final BlockPos blockPos;
    private final String initialChannel;

    // Server-side constructor
    public WirelessItemMenu(MenuType<?> type, int id, Inventory inv, IItemHandler handler,
                             ContainerData data) {
        this(type, id, inv, handler, data, BlockPos.ZERO, "");
    }

    public WirelessItemMenu(MenuType<?> type, int id, Inventory inv, IItemHandler handler,
                             ContainerData data, BlockPos blockPos, String initialChannel) {
        super(type, id);
        checkContainerDataCount(data, DATA_COUNT);
        this.data           = data;
        this.blockPos       = blockPos;
        this.initialChannel = initialChannel;
        // 3x3 handler slots
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                addSlot(new SlotItemHandler(handler, col + row * 3, 8 + col * 18, 17 + row * 18));
        // Player inventory
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public static WirelessItemMenu forTransmitter(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String ch    = buf.readUtf(64);
        return new WirelessItemMenu(ModMenuTypes.ITEM_TRANSMITTER_MENU.get(), id, inv,
                new ItemStackHandler(HANDLER_SLOTS), new SimpleContainerData(DATA_COUNT), pos, ch);
    }

    public static WirelessItemMenu forReceiver(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String ch    = buf.readUtf(64);
        return new WirelessItemMenu(ModMenuTypes.ITEM_RECEIVER_MENU.get(), id, inv,
                new ItemStackHandler(HANDLER_SLOTS), new SimpleContainerData(DATA_COUNT), pos, ch);
    }

    public int getEnergy()            { return data.get(0) * 1000; }
    public int getMaxEnergy()         { return data.get(1) * 1000; }
    public int getSatCount()          { return data.get(2); }
    public boolean isReceiver()       { return data.get(3) == 1; }
    public boolean isLinked()         { return data.get(4) == 1; }
    public boolean isPrivate()        { return data.get(5) == 1; }
    public BlockPos getBlockPos()     { return blockPos; }
    public String getInitialChannel() { return initialChannel; }

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index < HANDLER_SLOTS) {
            if (!moveItemStackTo(stack, HANDLER_SLOTS, HANDLER_SLOTS + 36, true))
                return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, HANDLER_SLOTS, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }
}
