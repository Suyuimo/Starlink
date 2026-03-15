package de.weinschenk.starlink.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SatelliteWorkbenchBlock extends Block {

    public SatelliteWorkbenchBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("block.starlink.satellite_workbench")
            ));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
