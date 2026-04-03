package de.weinschenk.starlink.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class WirelessEnergyMenu extends AbstractContainerMenu {

    // [0]=energy/1000  [1]=maxEnergy/1000  [2]=satCount  [3]=isReceiver  [4]=isLinked  [5]=isPrivate
    public static final int DATA_COUNT = 6;
    private final ContainerData data;

    private final BlockPos blockPos;
    private final String initialChannel;

    // Server-side constructor
    public WirelessEnergyMenu(MenuType<?> type, int id, Inventory inv, ContainerData data) {
        this(type, id, inv, data, BlockPos.ZERO, "");
    }

    public WirelessEnergyMenu(MenuType<?> type, int id, Inventory inv, ContainerData data,
                               BlockPos blockPos, String initialChannel) {
        super(type, id);
        checkContainerDataCount(data, DATA_COUNT);
        this.data           = data;
        this.blockPos       = blockPos;
        this.initialChannel = initialChannel;
        addDataSlots(data);
    }

    public static WirelessEnergyMenu forTransmitter(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String ch    = buf.readUtf(64);
        return new WirelessEnergyMenu(ModMenuTypes.ENERGY_TRANSMITTER_MENU.get(), id, inv,
                new SimpleContainerData(DATA_COUNT), pos, ch);
    }

    public static WirelessEnergyMenu forReceiver(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String ch    = buf.readUtf(64);
        return new WirelessEnergyMenu(ModMenuTypes.ENERGY_RECEIVER_MENU.get(), id, inv,
                new SimpleContainerData(DATA_COUNT), pos, ch);
    }

    public int getEnergy()         { return data.get(0) * 1000; }
    public int getMaxEnergy()      { return data.get(1) * 1000; }
    public int getSatCount()       { return data.get(2); }
    public boolean isReceiver()    { return data.get(3) == 1; }
    public boolean isLinked()      { return data.get(4) == 1; }
    public boolean isPrivate()     { return data.get(5) == 1; }
    public BlockPos getBlockPos()  { return blockPos; }
    public String getInitialChannel() { return initialChannel; }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
