package de.weinschenk.starlink.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class WirelessFluidMenu extends AbstractContainerMenu {

    // [0]=fluidAmount/100  [1]=fluidCapacity/100
    // [2]=energy/1000  [3]=maxEnergy/1000  [4]=satCount
    // [5]=isReceiver  [6]=isLinked  [7]=isPrivate
    public static final int DATA_COUNT = 8;
    private final ContainerData data;

    private final BlockPos blockPos;
    private final String initialChannel;

    // Server-side constructor
    public WirelessFluidMenu(MenuType<?> type, int id, Inventory inv, ContainerData data) {
        this(type, id, inv, data, BlockPos.ZERO, "");
    }

    public WirelessFluidMenu(MenuType<?> type, int id, Inventory inv, ContainerData data,
                              BlockPos blockPos, String initialChannel) {
        super(type, id);
        checkContainerDataCount(data, DATA_COUNT);
        this.data           = data;
        this.blockPos       = blockPos;
        this.initialChannel = initialChannel;
        addDataSlots(data);
    }

    public static WirelessFluidMenu forTransmitter(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String ch    = buf.readUtf(64);
        return new WirelessFluidMenu(ModMenuTypes.FLUID_TRANSMITTER_MENU.get(), id, inv,
                new SimpleContainerData(DATA_COUNT), pos, ch);
    }

    public static WirelessFluidMenu forReceiver(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String ch    = buf.readUtf(64);
        return new WirelessFluidMenu(ModMenuTypes.FLUID_RECEIVER_MENU.get(), id, inv,
                new SimpleContainerData(DATA_COUNT), pos, ch);
    }

    public int getFluidAmount()       { return data.get(0) * 100; }
    public int getFluidCapacity()     { return data.get(1) * 100; }
    public int getEnergy()            { return data.get(2) * 1000; }
    public int getMaxEnergy()         { return data.get(3) * 1000; }
    public int getSatCount()          { return data.get(4); }
    public boolean isReceiver()       { return data.get(5) == 1; }
    public boolean isLinked()         { return data.get(6) == 1; }
    public boolean isPrivate()        { return data.get(7) == 1; }
    public BlockPos getBlockPos()     { return blockPos; }
    public String getInitialChannel() { return initialChannel; }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
