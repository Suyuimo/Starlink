package de.weinschenk.starlink.block.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class ItemReceiverBlock extends TieredWirelessBlock {

    public ItemReceiverBlock(int tier, BlockBehaviour.Properties props) {
        super(tier, props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemReceiverBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return createTickerHelper(type, ModWirelessBlockEntities.ITEM_RECEIVER.get(),
                ItemReceiverBlockEntity::tick);
    }
}
