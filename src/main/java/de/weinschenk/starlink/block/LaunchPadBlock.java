package de.weinschenk.starlink.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Dekorativer Block der das 3x3-Fundament der Startbasis bildet.
 * Hat keine eigene Logik — der LaunchControllerBlock prüft ob alle
 * 8 Nachbarblöcke diesen Typ haben.
 */
public class LaunchPadBlock extends Block {

    public LaunchPadBlock(BlockBehaviour.Properties props) {
        super(props);
    }
}
