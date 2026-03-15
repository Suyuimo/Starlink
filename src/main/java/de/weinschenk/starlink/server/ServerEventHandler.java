package de.weinschenk.starlink.server;

import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.data.SatelliteRegistry;
import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.entity.ModEntities;
import de.weinschenk.starlink.entity.SatelliteEntity;
import de.weinschenk.starlink.item.OrbitGlassesItem;
import de.weinschenk.starlink.item.ReceiverGlassesItem;
import de.weinschenk.starlink.network.ModNetwork;
import de.weinschenk.starlink.network.SatelliteDataPacket;
import de.weinschenk.starlink.network.SatelliteRenderData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Starlink.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int GLASSES_UPDATE_INTERVAL = 20;

    /** Radius around an orbit-dimension player within which satellite entities are spawned.
     *  Must stay within server simulation distance (~160 blocks = 10 chunks) so that
     *  addFreshEntity() succeeds (chunk must be loaded). */
    private static final double SPAWN_RADIUS   = 150.0;
    private static final double DESPAWN_RADIUS = 250.0;

    /** registry UUID → active SatelliteEntity */
    private static final Map<UUID, SatelliteEntity> managedEntities = new HashMap<>();

    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Dynamische Orbit-Entities (jede Sekunde)
        long gameTime = server.overworld().getGameTime();

        if (gameTime % 20 == 0) {
            updateOrbitEntities(server, gameTime);
        }

        // Brillen-Daten senden
        if (gameTime % GLASSES_UPDATE_INTERVAL != 0) return;

        List<ServerPlayer> glassesWearers = server.getPlayerList().getPlayers().stream()
                .filter(p -> {
                    var item = p.getItemBySlot(EquipmentSlot.HEAD).getItem();
                    return item instanceof OrbitGlassesItem || item instanceof ReceiverGlassesItem;
                })
                .toList();

        if (glassesWearers.isEmpty()) return;

        List<SatelliteRenderData> satelliteData =
                SatelliteRegistry.get(server).getAllRenderData(gameTime);

        SatelliteDataPacket packet = new SatelliteDataPacket(satelliteData);
        for (ServerPlayer player : glassesWearers) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        managedEntities.clear();
    }

    // -------------------------------------------------------------------------

    private static void updateOrbitEntities(MinecraftServer server, long gameTime) {
        ServerLevel orbitLevel = server.getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) return;

        List<ServerPlayer> orbitPlayers = server.getPlayerList().getPlayers().stream()
                .filter(p -> p.level().dimension().equals(ModDimensions.ORBIT_LEVEL_KEY))
                .toList();

        // Entities entfernen die zu weit weg sind oder deren Chunk entladen wurde
        managedEntities.entrySet().removeIf(entry -> {
            SatelliteEntity entity = entry.getValue();
            if (entity.isRemoved()) return true;

            boolean tooFar = orbitPlayers.isEmpty() || orbitPlayers.stream().allMatch(p ->
                    Math.abs(entity.getX() - p.getX()) > DESPAWN_RADIUS &&
                    Math.abs(entity.getZ() - p.getZ()) > DESPAWN_RADIUS
            );
            if (tooFar) {
                entity.discard();
                return true;
            }
            return false;
        });

        if (orbitPlayers.isEmpty()) return;

        // Neue Entities für nahe Registry-Einträge spawnen
        SatelliteRegistry registry = SatelliteRegistry.get(server);
        long currentTick = gameTime;

        for (Map.Entry<UUID, SatelliteRegistry.SatEntry> entry : registry.getEntries().entrySet()) {
            UUID id = entry.getKey();

            // Schon aktiv?
            SatelliteEntity existing = managedEntities.get(id);
            if (existing != null && !existing.isRemoved()) continue;

            SatelliteRegistry.SatEntry sat = entry.getValue();
            double cx = sat.currentX(currentTick);
            double cz = sat.currentZ(currentTick);

            boolean nearPlayer = orbitPlayers.stream().anyMatch(p ->
                    Math.abs(cx - p.getX()) <= SPAWN_RADIUS &&
                    Math.abs(cz - p.getZ()) <= SPAWN_RADIUS
            );
            if (!nearPlayer) continue;

            SatelliteEntity entity = ModEntities.SATELLITE.get().create(orbitLevel);
            if (entity == null) continue;

            entity.setRegistryUUID(id); // für crash/kill → Registry-Austrag
            entity.setPos(cx, SatelliteEntity.ORBIT_HEIGHT, cz);
            entity.setOrbitDirection(sat.direction());
            entity.setAxisX(sat.axisX());
            // Nur in Map eintragen wenn Spawn erfolgreich war (Chunk muss geladen sein)
            if (orbitLevel.addFreshEntity(entity)) {
                managedEntities.put(id, entity);
            }
        }
    }
}
