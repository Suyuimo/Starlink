package de.weinschenk.starlink.block;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class PetroleumShaleOreBlock extends DropExperienceBlock {

    public PetroleumShaleOreBlock(BlockBehaviour.Properties props) {
        super(props, UniformInt.of(1, 3));
    }
}
