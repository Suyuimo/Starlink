package de.weinschenk.starlink.block;

import de.weinschenk.starlink.item.ModItems;
import de.weinschenk.starlink.menu.DistillationChamberMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DistillationChamberBlockEntity extends BlockEntity implements MenuProvider {

    // Slots: [0] Raw Petroleum Input, [1] Kohle/Brennstoff, [2] Fuel Kanister Output
    public static final int SLOT_INPUT    = 0;
    public static final int SLOT_FUEL     = 1;
    public static final int SLOT_OUTPUT   = 2;
    public static final int SLOT_COUNT    = 3;

    // Ticks für einen Verarbeitungsvorgang (200 = 10 Sekunden)
    public static final int MAX_PROGRESS  = 200;
    // Brenndauer von Kohle in Ticks
    private static final int COAL_BURN_TIME = 400;

    public final SimpleContainer items = new SimpleContainer(SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            DistillationChamberBlockEntity.this.setChanged();
        }
    };

    // Fortschritt des aktuellen Verarbeitungsvorgangs (0..MAX_PROGRESS)
    public int progress = 0;
    // Verbleibende Brennticks aus dem aktuellen Brennstoff-Item
    public int burnTime = 0;
    // Gesamtbrenndauer des letzten Brennstoffs (für Fortschrittsbalken)
    public int burnDuration = 0;

    public DistillationChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISTILLATION_CHAMBER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DistillationChamberBlockEntity be) {
        boolean wasActive = be.burnTime > 0;

        // Brennstoff verbrauchen
        if (be.burnTime > 0) {
            be.burnTime--;
        }

        boolean canProcess = be.canProcess();

        // Neuen Brennstoff anzünden wenn nötig und vorhanden
        if (be.burnTime == 0 && canProcess) {
            ItemStack fuelSlot = be.items.getItem(SLOT_FUEL);
            if (isFuel(fuelSlot)) {
                be.burnDuration = COAL_BURN_TIME;
                be.burnTime = COAL_BURN_TIME;
                fuelSlot.shrink(1);
                be.setChanged();
            }
        }

        // Verarbeitung vorantreiben
        if (be.burnTime > 0 && canProcess) {
            be.progress++;
            if (be.progress >= MAX_PROGRESS) {
                be.processItem();
                be.progress = 0;
            }
            be.setChanged();
        } else if (!canProcess) {
            be.progress = 0;
        }

        // Block-State aktualisieren (aktiv/inaktiv für visuelle Rückmeldung)
        boolean isActive = be.burnTime > 0;
        if (wasActive != isActive) {
            level.setBlock(pos, state.setValue(DistillationChamberBlock.ACTIVE, isActive), 3);
        }
    }

    /**
     * Prüft ob ein Verarbeitungsvorgang möglich ist:
     * Input vorhanden, Output-Slot hat Platz.
     */
    private boolean canProcess() {
        ItemStack input = items.getItem(SLOT_INPUT);
        if (input.getItem() != ModItems.ROCKET_FUEL_RAW.get() || input.isEmpty()) return false;

        ItemStack output = items.getItem(SLOT_OUTPUT);
        if (output.isEmpty()) return true;
        if (output.getItem() != ModItems.ROCKET_FUEL.get()) return false;
        return output.getCount() < output.getMaxStackSize();
    }

    /**
     * Führt einen Verarbeitungsvorgang durch: konsumiert Input, produziert Output.
     */
    private void processItem() {
        items.getItem(SLOT_INPUT).shrink(1);

        ItemStack output = items.getItem(SLOT_OUTPUT);
        if (output.isEmpty()) {
            items.setItem(SLOT_OUTPUT, new ItemStack(ModItems.ROCKET_FUEL.get()));
        } else {
            output.grow(1);
        }
        setChanged();
    }

    private static boolean isFuel(ItemStack stack) {
        // Kohle und Holzkohle als Brennstoff
        return stack.getItem() == Items.COAL || stack.getItem() == Items.CHARCOAL;
    }

    public boolean isActive() { return burnTime > 0; }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.starlink.distillation_chamber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DistillationChamberMenu(containerId, inventory, this);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        for (int i = 0; i < SLOT_COUNT; i++) {
            tag.put("Slot" + i, items.getItem(i).save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("Progress");
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.setItem(i, ItemStack.of(tag.getCompound("Slot" + i)));
        }
    }
}
