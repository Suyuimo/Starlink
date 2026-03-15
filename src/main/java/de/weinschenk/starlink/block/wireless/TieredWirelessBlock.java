package de.weinschenk.starlink.block.wireless;

import de.weinschenk.starlink.item.LinkToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public abstract class TieredWirelessBlock extends BaseEntityBlock {

    private final int tier;

    public TieredWirelessBlock(int tier, BlockBehaviour.Properties props) {
        super(props);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof LinkToolItem lt) {
            return lt.handleLink(stack, level, pos, player);
        }

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            int satCount = 0;
            if (be instanceof TieredWirelessBlockEntity twbe) {
                satCount = twbe.getCachedSatCount();
            }
            String msg = "[Starlink] T" + tier + " | Satelliten: " + satCount;
            player.sendSystemMessage(Component.literal(msg));

            if (be instanceof IWirelessReceiver receiver) {
                BlockPos linked = receiver.getLinkedTransmitter();
                if (linked != null) {
                    player.sendSystemMessage(Component.literal("[Starlink] Verknüpft mit: " + linked));
                } else {
                    player.sendSystemMessage(Component.literal("[Starlink] Kein Transmitter verknüpft."));
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
