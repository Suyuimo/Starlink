package de.weinschenk.starlink.block.wireless;

import de.weinschenk.starlink.menu.ModMenuTypes;
import de.weinschenk.starlink.menu.WirelessFluidMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.network.NetworkHooks;

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
            @Override protected void onContentsChanged() { setChanged(); }
        };
        fluidCap = LazyOptional.of(() -> tank);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidTransmitterBlockEntity be) {
        be.refreshSatelliteCount(level, pos);
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;
        int cost = WirelessTiers.fluidEnergyCostPerTick(be.getTier(), be.getFluidSatCount(), be.tank.getFluidAmount());
        be.consumeEnergy(cost);
    }

    @Override
    public void openGui(ServerPlayer player) {
        FluidTransmitterBlockEntity self = this;
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (id, inv, p) -> new WirelessFluidMenu(ModMenuTypes.FLUID_TRANSMITTER_MENU.get(), id, inv,
                new ContainerData() {
                    @Override public int get(int i) {
                        return switch (i) {
                            case 0 -> self.tank.getFluidAmount() / 100;
                            case 1 -> self.tank.getCapacity() / 100;
                            case 2 -> self.getEnergyStored() / 1000;
                            case 3 -> self.getMaxEnergy() / 1000;
                            case 4 -> self.getFluidSatCount();
                            case 7 -> self.isPrivate() ? 1 : 0;
                            default -> 0;
                        };
                    }
                    @Override public void set(int i, int v) {}
                    @Override public int getCount() { return WirelessFluidMenu.DATA_COUNT; }
                },
                self.getBlockPos(), self.getChannel()),
            Component.translatable("block.starlink.fluid_transmitter")),
        buf -> { buf.writeBlockPos(self.getBlockPos()); buf.writeUtf(self.getChannel(), 64); });
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(cap, side);
    }

    @Override public void setRemoved() { super.setRemoved(); fluidCap.invalidate(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Fluid", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        initTank();
        if (tag.contains("Fluid")) tank.readFromNBT(tag.getCompound("Fluid"));
    }
}
