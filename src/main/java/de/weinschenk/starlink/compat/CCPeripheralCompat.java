package de.weinschenk.starlink.compat;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import de.weinschenk.starlink.block.SatelliteInterfaceBlock;
import de.weinschenk.starlink.block.SatelliteInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Isolates all CC:Tweaked API references so the mod loads cleanly
 * when CC:Tweaked is not installed.
 *
 * Call {@link #register()} during FMLCommonSetupEvent ONLY when CC:Tweaked is loaded.
 */
public final class CCPeripheralCompat {

    private CCPeripheralCompat() {}

    public static void register() {
        ForgeComputerCraftAPI.registerPeripheralProvider(CCPeripheralCompat::getPeripheral);
    }

    private static LazyOptional<IPeripheral> getPeripheral(Level level, BlockPos pos, Direction side) {
        if (!(level.getBlockState(pos).getBlock() instanceof SatelliteInterfaceBlock)) {
            return LazyOptional.empty();
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SatelliteInterfaceBlockEntity sibe)) {
            return LazyOptional.empty();
        }
        return LazyOptional.of(() -> new SatelliteInterfacePeripheral(sibe));
    }

    // -------------------------------------------------------------------------

    public static class SatelliteInterfacePeripheral implements IPeripheral {

        private final SatelliteInterfaceBlockEntity be;

        SatelliteInterfacePeripheral(SatelliteInterfaceBlockEntity be) {
            this.be = be;
        }

        @Override
        public String getType() {
            return "starlink_satellite_interface";
        }

        @Override
        public boolean equals(IPeripheral other) {
            return other instanceof SatelliteInterfacePeripheral sip && sip.be == this.be;
        }

        /** Returns the total number of satellites currently visible overhead. */
        @LuaFunction
        public int getSatelliteCount() {
            return be.getCachedSatCount();
        }

        /** Returns the effective satellite count for RF energy transfer. */
        @LuaFunction
        public int getEnergySatelliteCount() {
            return be.getCachedEnergySats();
        }

        /** Returns the effective satellite count for fluid transfer. */
        @LuaFunction
        public int getFluidSatelliteCount() {
            return be.getCachedFluidSats();
        }

        /** Returns the effective satellite count for item transfer. */
        @LuaFunction
        public int getItemSatelliteCount() {
            return be.getCachedItemSats();
        }

        /** Returns true if at least one satellite is currently overhead. */
        @LuaFunction
        public boolean hasCoverage() {
            return be.getCachedSatCount() > 0;
        }
    }
}
