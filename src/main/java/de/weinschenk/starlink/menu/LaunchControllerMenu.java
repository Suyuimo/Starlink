package de.weinschenk.starlink.menu;

import de.weinschenk.starlink.block.LaunchControllerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LaunchControllerMenu extends AbstractContainerMenu {

    // ContainerData indices (synced server → client)
    public static final int DATA_FUEL    = 0;
    public static final int DATA_AXIS_X  = 1;  // 1 = X-Achse, 0 = Z-Achse
    public static final int DATA_COUNT   = 2;

    // clickMenuButton IDs
    public static final int BTN_TOGGLE_AXIS = 0;
    public static final int BTN_LAUNCH      = 1;

    private final LaunchControllerBlockEntity blockEntity;
    private final ContainerData data;

    /** Server-seitiger Konstruktor (mit echter BE-Referenz). */
    public LaunchControllerMenu(int containerId, Inventory playerInventory, LaunchControllerBlockEntity be) {
        super(ModMenuTypes.LAUNCH_CONTROLLER.get(), containerId);
        this.blockEntity = be;

        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case DATA_FUEL   -> be.getFuel();
                    case DATA_AXIS_X -> be.isOrbitAxisX() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                if (index == DATA_FUEL)   be.setFuelSync(value);
                if (index == DATA_AXIS_X) be.setOrbitAxisX(value != 0);
            }
            @Override public int getCount() { return DATA_COUNT; }
        };

        addPlayerInventory(playerInventory);
        addDataSlots(this.data);
    }

    /** Client-seitiger Konstruktor (BE wird anhand der BlockPos nachgeschlagen). */
    public LaunchControllerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                (LaunchControllerBlockEntity) playerInventory.player.level()
                        .getBlockEntity(buf.readBlockPos()));
    }

    /**
     * Schaltflächen-Handler (läuft server-seitig).
     * Button 0: Orbit-Achse umschalten (X ↔ Z)
     * Button 1: Rakete starten + GUI schließen
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_TOGGLE_AXIS) {
            blockEntity.setOrbitAxisX(!blockEntity.isOrbitAxisX());
            return true;
        }
        if (id == BTN_LAUNCH) {
            Level level = blockEntity.getLevel();
            if (level != null) {
                LaunchControllerBlockEntity.LaunchResult result =
                        blockEntity.tryLaunch(level, blockEntity.getBlockPos(), player);
                player.sendSystemMessage(Component.literal(result.message()));
            }
            player.closeContainer();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }

    // ── Spieler-Inventar-Slots (nur damit das Inventar im GUI sichtbar ist) ──

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 152));
    }

    // ── Getter für Screen ──────────────────────────────────────────────────

    public int  getFuel()    { return data.get(DATA_FUEL); }
    public boolean isAxisX() { return data.get(DATA_AXIS_X) != 0; }
}
