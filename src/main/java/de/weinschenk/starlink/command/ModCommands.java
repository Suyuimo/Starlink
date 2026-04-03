package de.weinschenk.starlink.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.block.LaunchControllerBlockEntity;
import de.weinschenk.starlink.block.LaunchControllerV2BlockEntity;
import de.weinschenk.starlink.data.SatelliteRegistry;
import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.entity.ModEntities;
import de.weinschenk.starlink.entity.SatelliteEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Starlink.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("starlink")
                .then(Commands.literal("tp")
                    .then(Commands.literal("orbit")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> teleportToOrbit(ctx.getSource()))))
                .then(Commands.literal("satellites")
                    .then(Commands.literal("killall")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> killAllSatellites(ctx.getSource())))
                    .then(Commands.literal("kill")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("orbit")
                            .then(Commands.argument("orbitId", IntegerArgumentType.integer(0, 63))
                                .executes(ctx -> killOrbit(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "orbitId")))
                                .then(Commands.literal("nearest")
                                    .executes(ctx -> killNearestInOrbit(ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "orbitId"))))))
                        .then(Commands.literal("nearest")
                            .executes(ctx -> killNearest(ctx.getSource()))))
                    .then(Commands.literal("spawn")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 2000))
                            .executes(ctx -> spawnSatellites(ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "count"), 0, 0))
                            .then(Commands.argument("offset", IntegerArgumentType.integer(0))
                                .executes(ctx -> spawnSatellites(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "count"),
                                        IntegerArgumentType.getInteger(ctx, "offset"), 0))
                                .then(Commands.argument("orbit", IntegerArgumentType.integer(0, 63))
                                    .executes(ctx -> spawnSatellites(ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "count"),
                                            IntegerArgumentType.getInteger(ctx, "offset"),
                                            IntegerArgumentType.getInteger(ctx, "orbit")))))))
                    .then(Commands.literal("fill")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 2000))
                            .executes(ctx -> fillOrbit(ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "count"), 0))
                            .then(Commands.argument("orbit", IntegerArgumentType.integer(0, 63))
                                .executes(ctx -> fillOrbit(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "count"),
                                        IntegerArgumentType.getInteger(ctx, "orbit")))))))
                .then(Commands.literal("controller")
                    .then(Commands.literal("reset")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> resetControllers(ctx.getSource()))))
        );
    }

    /** Teleportiert den Spieler an den entsprechenden Punkt auf dem Orbit-Ring (gleicher Winkel, Radius 10.000). */
    private static int teleportToOrbit(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Nur für Spieler verfügbar."));
            return 0;
        }

        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        // Winkel aus der aktuellen Spielerposition berechnen
        double angle = Math.atan2(player.getZ(), player.getX());
        double orbitX = SatelliteEntity.ORBIT_RADIUS * Math.cos(angle);
        double orbitZ = SatelliteEntity.ORBIT_RADIUS * Math.sin(angle);

        player.teleportTo(orbitLevel,
                orbitX,
                SatelliteEntity.ORBIT_HEIGHT + 20,
                orbitZ,
                player.getYRot(),
                player.getXRot());

        source.sendSuccess(() -> Component.literal("Willkommen im Orbit!"), false);
        return 1;
    }

    /** Abstand in Blöcken zwischen zwei aufeinanderfolgenden Spawn-Satelliten. */
    private static final double SATELLITE_SPACING = 50.0;

    /**
     * Spawnt {@code count} Satelliten ab dem Winkel der Spielerposition,
     * jeweils mit {@code SATELLITE_SPACING} Bogenabstand.
     * {@code offsetBlocks} verschiebt den Startpunkt zusätzlich entlang des Orbits.
     */
    private static int spawnSatellites(CommandSourceStack source, int count, int offsetBlocks, int orbitId) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        Vec3 pos = source.getPosition();
        double baseAngle = Math.atan2(pos.z, pos.x)
                + offsetBlocks / SatelliteEntity.ORBIT_RADIUS
                + orbitId * (2 * Math.PI / 64.0);

        double angularSpacing = SATELLITE_SPACING / SatelliteEntity.ORBIT_RADIUS;

        SatelliteRegistry registry = SatelliteRegistry.get(source.getServer());
        long startTick = source.getServer().overworld().getGameTime();
        int spawned = 0;

        double r = SatelliteEntity.ORBIT_RADIUS;
        for (int i = 0; i < count; i++) {
            double angle  = baseAngle + i * angularSpacing;
            double spawnX = r * Math.cos(angle);
            double spawnZ = r * Math.sin(angle);

            SatelliteEntity satellite = ModEntities.SATELLITE.get().create(orbitLevel);
            if (satellite != null) {
                satellite.setAngle(angle);
                satellite.setOrbitId(orbitId);
                satellite.setPos(spawnX, SatelliteEntity.ORBIT_HEIGHT, spawnZ);
                orbitLevel.addFreshEntity(satellite);
                registry.register(satellite.getUUID(), orbitId, angle, startTick);
                satellite.setVelocityFactor(registry.getOrbitVelocityFactor(orbitId));
                spawned++;
            }
        }

        int finalSpawned = spawned;
        source.sendSuccess(() -> Component.literal(
                "§6[Starlink] §a" + finalSpawned + " Satellit(en) gespawnt §7(Orbit " + orbitId + " / R=" + (int)r + " Blöcke)"
        ), true);
        return finalSpawned;
    }

    /**
     * Verteilt {@code count} Satelliten gleichmäßig über den gesamten Orbit-Ring (2π).
     */
    private static int fillOrbit(CommandSourceStack source, int count, int orbitId) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        // Jeder Orbit bekommt einen festen Versatz (45° pro Orbit), damit sie sofort getrennt sichtbar sind
        double orbitOffset    = orbitId * (2 * Math.PI / 64.0);
        double angularSpacing = (2 * Math.PI) / count;

        SatelliteRegistry registry = SatelliteRegistry.get(source.getServer());
        long startTick = source.getServer().overworld().getGameTime();
        int spawned = 0;

        double rf = SatelliteEntity.ORBIT_RADIUS;
        for (int i = 0; i < count; i++) {
            double angle  = orbitOffset + i * angularSpacing;
            double spawnX = rf * Math.cos(angle);
            double spawnZ = rf * Math.sin(angle);

            SatelliteEntity satellite = ModEntities.SATELLITE.get().create(orbitLevel);
            if (satellite != null) {
                satellite.setAngle(angle);
                satellite.setOrbitId(orbitId);
                satellite.setPos(spawnX, SatelliteEntity.ORBIT_HEIGHT, spawnZ);
                orbitLevel.addFreshEntity(satellite);
                registry.register(satellite.getUUID(), orbitId, angle, startTick);
                satellite.setVelocityFactor(registry.getOrbitVelocityFactor(orbitId));
                spawned++;
            }
        }

        int finalSpawned = spawned;
        int spacingBlocks = (int)(angularSpacing * SatelliteEntity.ORBIT_RADIUS);
        source.sendSuccess(() -> Component.literal(
                "§6[Starlink] §a" + finalSpawned + " Satellit(en) gleichmäßig verteilt §7(Orbit " + orbitId + ", Abstand ~" + spacingBlocks + " Blöcke)"
        ), true);
        return finalSpawned;
    }

    /** Entfernt alle Satelliten eines bestimmten Orbits. */
    private static int killOrbit(CommandSourceStack source, int orbitId) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        SatelliteRegistry registry = SatelliteRegistry.get(source.getServer());
        // Betroffene UUIDs vor dem Löschen merken, um Entities zu discarden
        List<UUID> toKill = new ArrayList<>(registry.getEntries().entrySet().stream()
                .filter(e -> e.getValue().orbitId() == orbitId)
                .map(Map.Entry::getKey)
                .toList());

        int removed = registry.unregisterOrbit(orbitId);

        // Geladene Entities discarden
        for (Entity entity : orbitLevel.getAllEntities()) {
            if (entity instanceof SatelliteEntity sat && toKill.contains(sat.getUUID())) {
                sat.setActive(false);
                sat.discard();
            }
        }

        source.sendSuccess(() -> Component.literal(
                "§6[Starlink] §a" + removed + " Satellit(en) in Orbit §e" + orbitId + " §aentfernt."
        ), true);
        return removed;
    }

    /** Entfernt den nächsten Satelliten in einem bestimmten Orbit. */
    private static int killNearestInOrbit(CommandSourceStack source, int orbitId) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        SatelliteRegistry registry = SatelliteRegistry.get(source.getServer());
        Vec3 pos = source.getPosition();
        long tick = source.getServer().overworld().getGameTime();
        var nearest = registry.getNearestInOrbit(orbitId, pos.x, pos.z, tick);

        if (nearest.isEmpty()) {
            source.sendFailure(Component.literal("Kein Satellit in Orbit " + orbitId + " gefunden."));
            return 0;
        }

        UUID uuid = nearest.get();
        registry.unregister(uuid);

        for (Entity entity : orbitLevel.getAllEntities()) {
            if (entity instanceof SatelliteEntity sat && sat.getUUID().equals(uuid)) {
                sat.setActive(false);
                sat.discard();
            }
        }

        source.sendSuccess(() -> Component.literal(
                "§6[Starlink] §aSatellit §7(" + uuid.toString().substring(0, 8) + "...) §ain Orbit §e" + orbitId + " §aentfernt."
        ), true);
        return 1;
    }

    /** Entfernt den Satelliten, der dem Spieler (auf der X/Z-Ebene) am nächsten ist. */
    private static int killNearest(CommandSourceStack source) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        SatelliteRegistry registry = SatelliteRegistry.get(source.getServer());
        if (registry.total() == 0) {
            source.sendFailure(Component.literal("Keine Satelliten im Orbit."));
            return 0;
        }

        Vec3 pos = source.getPosition();
        long tick = source.getServer().overworld().getGameTime();
        var nearest = registry.getNearestTo(pos.x, pos.z, tick);

        if (nearest.isEmpty()) {
            source.sendFailure(Component.literal("Kein Satellit gefunden."));
            return 0;
        }

        UUID uuid = nearest.get();
        registry.unregister(uuid);

        for (Entity entity : orbitLevel.getAllEntities()) {
            if (entity instanceof SatelliteEntity sat && sat.getUUID().equals(uuid)) {
                sat.setActive(false);
                sat.discard();
            }
        }

        source.sendSuccess(() -> Component.literal(
                "§6[Starlink] §aSatellit §7(" + uuid.toString().substring(0, 8) + "...) §aentfernt."
        ), true);
        return 1;
    }

    /**
     * Killt alle Satelliten — auch in ungeladenen Chunks.
     */
    private static int killAllSatellites(CommandSourceStack source) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        // Alle geladenen Satellite-Entities entfernen
        List<SatelliteEntity> toKill = new ArrayList<>();
        for (Entity entity : orbitLevel.getAllEntities()) {
            if (entity instanceof SatelliteEntity sat) toKill.add(sat);
        }
        toKill.forEach(sat -> { sat.setActive(false); sat.discard(); });

        // Registry leeren
        SatelliteRegistry.get(source.getServer()).clear();

        // Alle force-geladenen Chunks in der Orbit-Dimension freigeben
        int releasedChunks = 0;
        ForcedChunksSavedData forced = orbitLevel.getDataStorage()
                .get(ForcedChunksSavedData::load, "chunks");
        if (forced != null) {
            List<ChunkPos> toRelease = new ArrayList<>();
            for (long packed : forced.getChunks()) toRelease.add(new ChunkPos(packed));
            for (ChunkPos cp : toRelease) orbitLevel.setChunkForced(cp.x, cp.z, false);
            releasedChunks = toRelease.size();
        }

        int killed = toKill.size();
        int released = releasedChunks;
        source.sendSuccess(() -> Component.literal(
                killed + " Satellit(en) entfernt, " + released + " force-loaded Chunk(s) freigegeben."
        ), true);
        return killed;
    }

    /**
     * Setzt das 'launching'-Flag aller LaunchControllerBlockEntities zurück.
     */
    private static int resetControllers(CommandSourceStack source) {
        int count = 0;
        for (LaunchControllerBlockEntity ctrl : new ArrayList<>(LaunchControllerBlockEntity.ALL_LOADED)) {
            if (ctrl.isLaunching()) {
                ctrl.onRocketDeparted();
                count++;
            }
        }
        for (LaunchControllerV2BlockEntity ctrl : new ArrayList<>(LaunchControllerV2BlockEntity.ALL_LOADED)) {
            if (ctrl.isLaunching()) {
                ctrl.onRocketDeparted();
                count++;
            }
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.literal(finalCount + " Controller zurückgesetzt."), true);
        return finalCount;
    }
}
