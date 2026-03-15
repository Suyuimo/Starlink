package de.weinschenk.starlink.block;

import de.weinschenk.starlink.item.ModItems;
import de.weinschenk.starlink.menu.LaunchControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class LaunchControllerBlock extends BaseEntityBlock {

    // true wenn der Multiblock gültig ist und gestartet werden kann
    public static final BooleanProperty READY = BooleanProperty.create("ready");

    public LaunchControllerBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(READY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(READY);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LaunchControllerBlockEntity controller)) return InteractionResult.FAIL;

        ItemStack held = player.getItemInHand(hand);

        // Fuel auftanken
        if (held.getItem() == ModItems.ROCKET_FUEL.get()) {
            int added = controller.addFuel(held.getCount());
            held.shrink(added);
            player.sendSystemMessage(Component.literal(
                    "Treibstoff: " + controller.getFuel() + "/" + LaunchControllerBlockEntity.FUEL_REQUIRED
            ));
            return InteractionResult.CONSUME;
        }

        // GUI öffnen (leere Hand oder sonstiges Item)
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new LaunchControllerMenu(id, inv, controller),
                    Component.literal("Startkontrolle")
            );
            NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.LAUNCH_CONTROLLER.get(),
                LaunchControllerBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
