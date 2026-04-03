package de.weinschenk.starlink.block;

import de.weinschenk.starlink.data.SatelliteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SatelliteInterfaceBlockEntity extends BlockEntity {

    private int cachedSatCount   = 0;
    private int cachedEnergySats = 0;
    private int cachedFluidSats  = 0;
    private int cachedItemSats   = 0;
    private long lastRefreshTick = -1L;

    public SatelliteInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SATELLITE_INTERFACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SatelliteInterfaceBlockEntity be) {
        if (level.isClientSide) return;
        long currentTick = level.getGameTime();
        if (currentTick - be.lastRefreshTick < 20) return;
        be.lastRefreshTick = currentTick;

        if (!(level instanceof ServerLevel serverLevel)) return;
        SatelliteRegistry reg = SatelliteRegistry.get(serverLevel.getServer());
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();

        be.cachedSatCount   = reg.countNear(x, y, z, currentTick);
        be.cachedEnergySats = reg.countNearEnergy(x, y, z, currentTick);
        be.cachedFluidSats  = reg.countNearFluid(x, y, z, currentTick);
        be.cachedItemSats   = reg.countNearItem(x, y, z, currentTick);
    }

    public int getCachedSatCount()   { return cachedSatCount; }
    public int getCachedEnergySats() { return cachedEnergySats; }
    public int getCachedFluidSats()  { return cachedFluidSats; }
    public int getCachedItemSats()   { return cachedItemSats; }
}
