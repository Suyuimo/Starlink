package de.weinschenk.starlink.block.wireless;

import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.entity.SatelliteEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public abstract class TieredWirelessBlockEntity extends BlockEntity {

    private static final double SATELLITE_RANGE = 100.0;

    protected int cachedSatCount = 0;
    private long lastSatRefreshTick = -1L;

    public TieredWirelessBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getCachedSatCount() {
        return cachedSatCount;
    }

    public int getTier() {
        if (getBlockState().getBlock() instanceof TieredWirelessBlock twb) {
            return twb.getTier();
        }
        return 1;
    }

    public void refreshSatelliteCount(Level level, BlockPos pos) {
        long currentTick = level.getGameTime();
        if (currentTick - lastSatRefreshTick < 20) return;
        lastSatRefreshTick = currentTick;

        if (!(level instanceof ServerLevel serverLevel)) return;

        ServerLevel orbitLevel = serverLevel.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            cachedSatCount = 0;
            return;
        }

        AABB searchBox = new AABB(
                pos.getX() - SATELLITE_RANGE, -2048, pos.getZ() - SATELLITE_RANGE,
                pos.getX() + SATELLITE_RANGE,  2048, pos.getZ() + SATELLITE_RANGE
        );

        List<SatelliteEntity> satellites = orbitLevel.getEntitiesOfClass(
                SatelliteEntity.class, searchBox, SatelliteEntity::isActive);

        cachedSatCount = satellites.size();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SatCache", cachedSatCount);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cachedSatCount = tag.getInt("SatCache");
    }
}
