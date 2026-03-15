package de.weinschenk.starlink.block;

import de.weinschenk.starlink.Config;
import de.weinschenk.starlink.data.SatelliteRegistry;
import de.weinschenk.starlink.network.ModNetwork;
import de.weinschenk.starlink.network.StartStreamPacket;
import de.weinschenk.starlink.network.StopStreamPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ReceiverBlockEntity extends BlockEntity {

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int SKY_CHECK_HEIGHT = 100;
    private static final double SATELLITE_RANGE = 100.0;
    private static final double LISTENER_RANGE = 32.0;
    private static final double DROPOUT_CHANCE = 0.03;

    // UUIDs der Spieler, die gerade den Stream hören
    private final Set<UUID> activeListeners = new HashSet<>();

    public ReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECEIVER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReceiverBlockEntity be) {
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) return;

        boolean signalActive = be.checkSignal((ServerLevel) level, pos);

        // Block-State aktualisieren wenn sich das Signal geändert hat
        if (signalActive != state.getValue(ReceiverBlock.ACTIVE)) {
            level.setBlock(pos, state.setValue(ReceiverBlock.ACTIVE, signalActive), 3);
        }

        // Spieler-Tracking: Wer ist gerade in Reichweite?
        be.updateListeners((ServerLevel) level, pos, signalActive);
    }

    /**
     * Vergleicht die aktuelle Spielerliste mit der vorherigen.
     * - Neue Spieler in Reichweite + Signal aktiv  → StartStreamPacket
     * - Spieler außer Reichweite oder Signal weg   → StopStreamPacket
     */
    private void updateListeners(ServerLevel level, BlockPos pos, boolean signalActive) {
        Set<UUID> nowInRange = getPlayersInRange(level, pos);

        // Spieler die neu in Reichweite sind und Signal aktiv → Stream starten
        if (signalActive) {
            for (UUID uuid : nowInRange) {
                if (!activeListeners.contains(uuid)) {
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                    if (player != null) {
                        sendStart(player);
                    }
                }
            }
        }

        // Spieler die nicht mehr in Reichweite sind oder kein Signal → Stream stoppen
        for (UUID uuid : activeListeners) {
            if (!nowInRange.contains(uuid) || !signalActive) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                if (player != null) {
                    sendStop(player);
                }
            }
        }

        // Listener-Set aktualisieren
        activeListeners.clear();
        if (signalActive) {
            activeListeners.addAll(nowInRange);
        }
    }

    private Set<UUID> getPlayersInRange(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(
                pos.getX() - LISTENER_RANGE, pos.getY() - LISTENER_RANGE, pos.getZ() - LISTENER_RANGE,
                pos.getX() + LISTENER_RANGE, pos.getY() + LISTENER_RANGE, pos.getZ() + LISTENER_RANGE
        );
        Set<UUID> result = new HashSet<>();
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            result.add(player.getUUID());
        }
        return result;
    }

    private void sendStart(ServerPlayer player) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StartStreamPacket(Config.streamUrl)
        );
    }

    private void sendStop(ServerPlayer player) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StopStreamPacket()
        );
    }

    /**
     * Wenn der Block abgebaut wird alle aktiven Listener stoppen.
     */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) {
            for (UUID uuid : activeListeners) {
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
                if (player != null) sendStop(player);
            }
            activeListeners.clear();
        }
    }

    // -------------------------------------------------------------------------
    // Signal-Checks
    // -------------------------------------------------------------------------

    private boolean checkSignal(ServerLevel level, BlockPos pos) {
        if (!hasClearSky(level, pos)) return false;
        if (!hasSatelliteInRange(level, pos)) return false;
        if (Math.random() < DROPOUT_CHANCE) return false;
        return true;
    }

    private boolean hasClearSky(Level level, BlockPos pos) {
        for (int i = 1; i <= SKY_CHECK_HEIGHT; i++) {
            if (!level.getBlockState(pos.above(i)).isAir()) return false;
        }
        return true;
    }

    private boolean hasSatelliteInRange(ServerLevel level, BlockPos pos) {
        return SatelliteRegistry.get(level.getServer())
                .countNear(pos.getX(), pos.getZ(), SATELLITE_RANGE, level.getGameTime()) > 0;
    }

    public boolean isReceiving() {
        return getBlockState().getValue(ReceiverBlock.ACTIVE);
    }
}
