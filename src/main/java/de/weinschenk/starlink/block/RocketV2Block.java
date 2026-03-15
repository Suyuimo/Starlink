package de.weinschenk.starlink.block;

import de.weinschenk.starlink.menu.RocketV2Menu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class RocketV2Block extends BaseEntityBlock {

    public RocketV2Block(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RocketV2BlockEntity(pos, state);
    }

    /** Kann nur auf einem Launch Controller V2 platziert werden. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof LaunchControllerV2Block;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                 Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide && !state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RocketV2BlockEntity rbe) {
                NetworkHooks.openScreen(sp,
                        new SimpleMenuProvider(
                                (id, inv, p) -> new RocketV2Menu(id, inv, rbe),
                                Component.translatable("block.starlink.rocket_v2")
                        ),
                        buf -> buf.writeBlockPos(pos)
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Satelliten beim Abbau droppen. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RocketV2BlockEntity rbe) {
                for (int i = 0; i < RocketV2BlockEntity.SATELLITE_SLOTS; i++) {
                    ItemStack stack = rbe.getInventory().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
