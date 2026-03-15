package de.weinschenk.starlink.menu;

import de.weinschenk.starlink.block.LaunchControllerV2BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LaunchControllerV2Menu extends AbstractContainerMenu {

    // ContainerData indices (synced short → max 32767)
    // Energy / 1000 → max 10000 für 10 Mio FE (passt in short)
    public static final int DATA_ENERGY_KILO = 0;   // energy / 1000
    public static final int DATA_SAT_COUNT   = 1;   // Satelliten in Rakete oben
    public static final int DATA_AXIS_X      = 2;   // 1 = X-Achse, 0 = Z-Achse
    public static final int DATA_COUNT       = 3;

    // clickMenuButton IDs
    public static final int BTN_TOGGLE_AXIS = 0;
    public static final int BTN_LAUNCH      = 1;

    private final LaunchControllerV2BlockEntity blockEntity;
    private final ContainerData data;

    /** Server-seitiger Konstruktor. */
    public LaunchControllerV2Menu(int containerId, Inventory playerInventory, LaunchControllerV2BlockEntity be) {
        super(ModMenuTypes.LAUNCH_CONTROLLER_V2.get(), containerId);
        this.blockEntity = be;

        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case DATA_ENERGY_KILO -> be.getEnergyStoredKilo();
                    case DATA_SAT_COUNT   -> be.getSatelliteCountAbove();
                    case DATA_AXIS_X      -> be.isOrbitAxisX() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                if (index == DATA_ENERGY_KILO) be.setEnergyKiloSync(value);
                if (index == DATA_AXIS_X)      be.setOrbitAxisX(value != 0);
            }
            @Override public int getCount() { return DATA_COUNT; }
        };

        addPlayerInventory(playerInventory);
        addDataSlots(this.data);
    }

    /** Client-seitiger Konstruktor. */
    public LaunchControllerV2Menu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                (LaunchControllerV2BlockEntity) playerInventory.player.level()
                        .getBlockEntity(buf.readBlockPos()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_TOGGLE_AXIS) {
            blockEntity.setOrbitAxisX(!blockEntity.isOrbitAxisX());
            return true;
        }
        if (id == BTN_LAUNCH) {
            Level level = blockEntity.getLevel();
            if (level != null) {
                LaunchControllerV2BlockEntity.LaunchResult result =
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

    public int     getEnergyKilo()  { return data.get(DATA_ENERGY_KILO); }
    public int     getSatCount()    { return data.get(DATA_SAT_COUNT); }
    public boolean isAxisX()        { return data.get(DATA_AXIS_X) != 0; }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 152));
    }
}
