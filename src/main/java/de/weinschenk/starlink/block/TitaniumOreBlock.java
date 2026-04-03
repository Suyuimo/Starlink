package de.weinschenk.starlink.block;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class TitaniumOreBlock extends DropExperienceBlock {

    public TitaniumOreBlock(BlockBehaviour.Properties props) {
        super(props, UniformInt.of(2, 5));
    }
}