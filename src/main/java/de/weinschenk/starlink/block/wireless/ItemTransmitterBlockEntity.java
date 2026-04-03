package de.weinschenk.starlink.block.wireless;

import de.weinschenk.starlink.menu.ModMenuTypes;
import de.weinschenk.starlink.menu.WirelessItemMenu;
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
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemTransmitterBlockEntity extends TieredWirelessBlockEntity {

    public final ItemStackHandler handler = new ItemStackHandler(9);
    private final LazyOptional<ItemStackHandler> itemCap = LazyOptional.of(() -> handler);

    public ItemTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModWirelessBlockEntities.ITEM_TRANSMITTER.get(), pos, state);
        initEnergy();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ItemTransmitterBlockEntity be) {
        be.refreshSatelliteCount(level, pos);
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;

        int items = 0;
        for (int i = 0; i < be.handler.getSlots(); i++) items += be.handler.getStackInSlot(i).getCount();
        int cost = WirelessTiers.itemEnergyCostPerTick(be.getTier(), be.getItemSatCount(), items);
        be.consumeEnergy(cost);
    }

    @Override
    public void openGui(ServerPlayer player) {
        ItemTransmitterBlockEntity self = this;
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (id, inv, p) -> new WirelessItemMenu(ModMenuTypes.ITEM_TRANSMITTER_MENU.get(), id, inv,
                self.handler,
                new ContainerData() {
                    @Override public int get(int i) {
                        return switch (i) {
                            case 0 -> self.getEnergyStored() / 1000;
                            case 1 -> self.getMaxEnergy() / 1000;
                            case 2 -> self.getItemSatCount();
                            case 5 -> self.isPrivate() ? 1 : 0;
                            default -> 0;
                        };
                    }
                    @Override public void set(int i, int v) {}
                    @Override public int getCount() { return WirelessItemMenu.DATA_COUNT; }
                },
                self.getBlockPos(), self.getChannel()),
            Component.translatable("block.starlink.item_transmitter")),
        buf -> { buf.writeBlockPos(self.getBlockPos()); buf.writeUtf(self.getChannel(), 64); });
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override public void setRemoved() { super.setRemoved(); itemCap.invalidate(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", handler.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) handler.deserializeNBT(tag.getCompound("Items"));
    }
}
