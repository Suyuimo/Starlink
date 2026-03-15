package de.weinschenk.starlink.server;

import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.entity.SatelliteEntity;
import de.weinschenk.starlink.item.OrbitGlassesItem;
import de.weinschenk.starlink.item.ReceiverGlassesItem;
import de.weinschenk.starlink.network.ModNetwork;
import de.weinschenk.starlink.network.SatelliteDataPacket;
import de.weinschenk.starlink.network.SatelliteRenderData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;

@Mod.EventBusSubscriber(modid = Starlink.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Alle 20 Ticks (1 Sekunde) Satellitendaten verschicken
    private static final int UPDATE_INTERVAL = 20;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (server.getTickCount() % UPDATE_INTERVAL != 0) return;

        // Alle Spieler die eine Orbit- oder Empfänger-Brille tragen
        List<ServerPlayer> glassesWearers = server.getPlayerList().getPlayers().stream()
                .filter(p -> {
                    var item = p.getItemBySlot(EquipmentSlot.HEAD).getItem();
                    return item instanceof OrbitGlassesItem || item instanceof ReceiverGlassesItem;
                })
                .toList();

        // Debug: alle 5 Sekunden loggen was der Server sieht
        if (server.getTickCount() % (UPDATE_INTERVAL * 5) == 0) {
            int total = server.getPlayerList().getPlayers().size();
            int withGlasses = glassesWearers.size();
            LOGGER.debug("[Starlink] Spieler gesamt: {}, davon mit Brille: {}", total, withGlasses);
        }

        if (glassesWearers.isEmpty()) return;

        // Satelliten aus der Orbit-Dimension holen
        ServerLevel orbitLevel = server.getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            LOGGER.warn("[Starlink] Orbit-Dimension nicht geladen! Satellitendaten können nicht gesendet werden.");
            return;
        }

        List<SatelliteRenderData> satelliteData = orbitLevel
                .getEntitiesOfClass(SatelliteEntity.class, AABB.ofSize(
                        net.minecraft.world.phys.Vec3.ZERO, 60_000_000, 1000, 60_000_000),
                        SatelliteEntity::isActive)
                .stream()
                .map(s -> new SatelliteRenderData(s.getX(), s.getZ(), s.getOrbitDirection(), s.isAxisX()))
                .toList();

        LOGGER.info("[Starlink] Orbit-Dimension geladen. Satelliten gefunden: {}", satelliteData.size());

        SatelliteDataPacket packet = new SatelliteDataPacket(satelliteData);

        for (ServerPlayer player : glassesWearers) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    packet
            );
        }
    }
}
