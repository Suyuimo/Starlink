package de.weinschenk.starlink.block.wireless;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public interface IWirelessReceiver {

    void setLinkedTransmitter(BlockPos pos);

    @Nullable
    BlockPos getLinkedTransmitter();
}
