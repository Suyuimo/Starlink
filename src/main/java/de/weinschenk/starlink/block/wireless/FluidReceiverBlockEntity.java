package de.weinschenk.starlink.block.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FluidReceiverBlockEntity extends TieredWirelessBlockEntity implements IWirelessReceiver {

    public FluidTank tank;
    private LazyOptional<FluidTank> fluidCap;

    @Nullable
    private BlockPos linkedTransmitterPos = null;

    public FluidReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModWirelessBlockEntities.FLUID_RECEIVER.get(), pos, state);
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

    public static void tick(Level level, BlockPos pos, BlockState state, FluidReceiverBlockEntity be) {
        be.refreshSatelliteCount(level, pos);

        if (be.linkedTransmitterPos == null) return;

        BlockEntity rawBe = level.getBlockEntity(be.linkedTransmitterPos);
        if (!(rawBe instanceof FluidTransmitterBlockEntity tx)) return;

        int recSats = be.getCachedSatCount();
        int txSats = tx.getCachedSatCount();
        int effectiveSats = Math.min(recSats, txSats);

        if (effectiveSats <= 0) return;

        // Energy check: both transmitter and receiver must have enough RF
        int recCost = WirelessTiers.fluidEnergyCostPerTick(be.getTier(), recSats, be.tank.getFluidAmount());
        int txCost  = WirelessTiers.fluidEnergyCostPerTick(tx.getTier(), txSats,  tx.tank.getFluidAmount());
        if (!be.consumeEnergy(recCost) || !tx.consumeEnergy(txCost)) return;

        int bw = WirelessTiers.fluidBandwidth(be.getTier(), effectiveSats);
        FluidStack toTransfer = tx.tank.drain(bw, IFluidHandler.FluidAction.SIMULATE);
        if (!toTransfer.isEmpty()) {
            int filled = be.tank.fill(toTransfer, IFluidHandler.FluidAction.SIMULATE);
            if (filled > 0) {
                FluidStack actualDrain = tx.tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                be.tank.fill(actualDrain, IFluidHandler.FluidAction.EXECUTE);
                be.setChanged();
            }
        }
    }

    @Override
    public void setLinkedTransmitter(BlockPos pos) {
        this.linkedTransmitterPos = pos;
        setChanged();
    }

    @Override
    @Nullable
    public BlockPos getLinkedTransmitter() {
        return linkedTransmitterPos;
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
        if (linkedTransmitterPos != null) {
            tag.putInt("LinkedTxX", linkedTransmitterPos.getX());
            tag.putInt("LinkedTxY", linkedTransmitterPos.getY());
            tag.putInt("LinkedTxZ", linkedTransmitterPos.getZ());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        initTank();
        if (tag.contains("Fluid")) {
            tank.readFromNBT(tag.getCompound("Fluid"));
        }
        if (tag.contains("LinkedTxX")) {
            linkedTransmitterPos = new BlockPos(
                    tag.getInt("LinkedTxX"),
                    tag.getInt("LinkedTxY"),
                    tag.getInt("LinkedTxZ")
            );
        }
    }
}
