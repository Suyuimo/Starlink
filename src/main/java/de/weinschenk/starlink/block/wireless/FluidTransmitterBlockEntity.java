package de.weinschenk.starlink.block.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FluidTransmitterBlockEntity extends TieredWirelessBlockEntity {

    public FluidTank tank;
    private LazyOptional<FluidTank> fluidCap;

    public FluidTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModWirelessBlockEntities.FLUID_TRANSMITTER.get(), pos, state);
        initTank();
        initEnergy();
    }

    private void initTank() {
        tank = new FluidTank(WirelessTiers.fluidBuffer(getTier())) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        fluidCap = LazyOptional.of(() -> tank);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidTransmitterBlockEntity be) {
        be.refreshSatelliteCount(level, pos);
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;

        int cost = WirelessTiers.fluidEnergyCostPerTick(be.getTier(), be.getCachedSatCount(), be.tank.getFluidAmount());
        be.consumeEnergy(cost);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        fluidCap.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Fluid", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        initTank();
        if (tag.contains("Fluid")) {
            tank.readFromNBT(tag.getCompound("Fluid"));
        }
    }
}
