package de.weinschenk.starlink.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InfiniteEnergyCellBlockEntity extends BlockEntity {

    /** Pro Tick an jeden Nachbarn gepushte Energie. */
    private static final int PUSH_PER_TICK = 500_000;

    /** Capability: liefert immer Integer.MAX_VALUE, extractable, nicht receivable. */
    private final IEnergyStorage infiniteStorage = new IEnergyStorage() {
        @Override public int receiveEnergy(int max, boolean sim) { return 0; }
        @Override public int extractEnergy(int max, boolean sim) { return max; }
        @Override public int getEnergyStored()    { return Integer.MAX_VALUE; }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract()  { return true; }
        @Override public boolean canReceive()  { return false; }
    };

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> infiniteStorage);

    public InfiniteEnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFINITE_ENERGY_CELL.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, InfiniteEnergyCellBlockEntity be) {
        if (level.isClientSide) return;

        // Energie aktiv in alle 6 Nachbar-Blöcke pushen
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;

            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                if (storage.canReceive()) {
                    storage.receiveEnergy(PUSH_PER_TICK, false);
                }
            });
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }
}
