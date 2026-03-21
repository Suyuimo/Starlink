package de.weinschenk.starlink.menu;

import de.weinschenk.starlink.block.ReceiverBlockEntity;
import de.weinschenk.starlink.data.SignalFilterMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ReceiverMenu extends AbstractContainerMenu {

    public static final int DATA_MODE  = 0;
    public static final int DATA_COUNT = 1;

    private final ReceiverBlockEntity blockEntity;
    private final BlockPos            blockPos;
    private       String              clientPin;   // auf dem Client initial gesetzt, dann lokal gehalten

    private final ContainerData data;

    /** Server-seitiger Konstruktor. */
    public ReceiverMenu(int containerId, Inventory playerInventory, ReceiverBlockEntity be) {
        super(ModMenuTypes.RECEIVER.get(), containerId);
        this.blockEntity = be;
        this.blockPos    = be.getBlockPos();
        this.clientPin   = be.getRequiredPin();

        this.data = new ContainerData() {
            @Override public int get(int i)          { return be.getMode().ordinal(); }
            @Override public void set(int i, int v)  { /* nur Lesen */ }
            @Override public int getCount()          { return DATA_COUNT; }
        };
        addDataSlots(this.data);
    }

    /** Client-seitiger Konstruktor (liest BlockPos + Initialzustand aus dem Buffer). */
    public ReceiverMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenuTypes.RECEIVER.get(), containerId);
        BlockPos pos = buf.readBlockPos();
        this.blockPos  = pos;
        this.clientPin = buf.readUtf(64);

        // Server-Modus als Startwert lesen, ContainerData überschreibt danach automatisch
        int modeOrd = buf.readInt();
        this.data = new ContainerData() {
            private int mode = modeOrd;
            @Override public int get(int i)         { return mode; }
            @Override public void set(int i, int v) { if (i == 0) mode = v; }
            @Override public int getCount()         { return DATA_COUNT; }
        };
        addDataSlots(this.data);

        // BE auf dem Client abrufen (kann null sein – kein Problem für Darstellung)
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        this.blockEntity = be instanceof ReceiverBlockEntity rbe ? rbe : null;
    }

    // -------------------------------------------------------------------------

    public SignalFilterMode getCurrentMode() {
        int ord = data.get(DATA_MODE);
        return (ord >= 0 && ord < SignalFilterMode.values().length)
                ? SignalFilterMode.values()[ord]
                : SignalFilterMode.ALL;
    }

    public String getClientPin()               { return clientPin; }
    public void   setClientPin(String pin)     { this.clientPin = pin; }
    public BlockPos getBlockPos()              { return blockPos; }

    /** Server-seitig: Modus per Button-Klick setzen. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) return false;
        if (id >= 0 && id < SignalFilterMode.values().length) {
            blockEntity.setMode(SignalFilterMode.values()[id]);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || !blockEntity.isRemoved();
    }
}
