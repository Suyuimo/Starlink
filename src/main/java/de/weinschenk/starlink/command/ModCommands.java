package de.weinschenk.starlink.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.block.LaunchControllerBlockEntity;
import de.weinschenk.starlink.data.SatelliteRegistry;
import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.dimension.OrbitRoutes;
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
                    .then(Commands.literal("spawn")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 2000))
                            .executes(ctx -> spawnSatellites(ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "count"), true, 0))
                            .then(Commands.argument("axisX", BoolArgumentType.bool())
                                .executes(ctx -> spawnSatellites(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "count"),
                                        BoolArgumentType.getBool(ctx, "axisX"), 0))
                                .then(Commands.argument("offset", IntegerArgumentType.integer(0))
                                    .executes(ctx -> spawnSatellites(ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "count"),
                                            BoolArgumentType.getBool(ctx, "axisX"),
                                            IntegerArgumentType.getInteger(ctx, "offset")))))))
                    .then(Commands.literal("fill")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 2000))
                            .executes(ctx -> fillOrbit(ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "count"), true))
                            .then(Commands.argument("axisX", BoolArgumentType.bool())
                                .executes(ctx -> fillOrbit(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "count"),
                                        BoolArgumentType.getBool(ctx, "axisX")))))))
                .then(Commands.literal("controller")
                    .then(Commands.literal("reset")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> resetControllers(ctx.getSource()))))
        );
    }

    /** Teleportiert den Spieler in die Orbit-Dimension (gleiche X/Z, Y=120). */
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

        player.teleportTo(orbitLevel,
                player.getX(),
                SatelliteEntity.ORBIT_HEIGHT + 20,
                player.getZ(),
                player.getYRot(),
                player.getXRot());

        source.sendSuccess(() -> Component.literal("Willkommen im Orbit!"), false);
        return 1;
    }

    private static final double SATELLITE_SPACING = 50.0;

    /**
     * Spawnt {@code count} Satelliten direkt in der Orbit-Dimension.
     * Startposition: X/Z des ausführenden Spielers (oder 0/0 bei Console).
     * Abstand und Route identisch zu RocketV2Entity.deployToOrbit().
     */
    private static int spawnSatellites(CommandSourceStack source, int count, boolean axisX, int offsetBlocks) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        Vec3 pos = source.getPosition();
        double originX = pos.x;
        double originZ = pos.z;

        double routeZ, routeX;
        int direction;
        if (axisX) {
            routeZ    = OrbitRoutes.snapToNearestRouteZ(originZ);
            routeX    = originX + offsetBlocks;
            direction = OrbitRoutes.directionForRouteZ(routeZ);
        } else {
            routeX    = OrbitRoutes.snapToNearestRouteX(originX);
            routeZ    = originZ + offsetBlocks;
            direction = OrbitRoutes.directionForRouteX(routeX);
        }

        int spawned = doSpawnBatch(orbitLevel, source, count, axisX, routeX, routeZ, direction);

        int finalSpawned = spawned;
        String axis = axisX ? "X" : "Z";
        source.sendSuccess(() -> Component.literal(
                "§6[Starlink] §a" + finalSpawned + " Satellit(en) gespawnt §7(Achse " + axis
                + ", Route " + (axisX ? (int) routeZ : (int) routeX)
                + ", Offset " + offsetBlocks + ", Richtung " + direction + ")"
        ), true);
        return finalSpawned;
    }

    /**
     * Verteilt {@code count} Satelliten gleichmäßig über die gesamte Umlaufbahn (60.000 Blöcke).
     * Egal wie viele: der Abstand wird automatisch berechnet.
     */
    private static int fillOrbit(CommandSourceStack source, int count, boolean axisX) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        Vec3 pos = source.getPosition();
        double originX = pos.x;
        double originZ = pos.z;

        double routeZ, routeX;
        int direction;
        if (axisX) {
            routeZ    = OrbitRoutes.snapToNearestRouteZ(originZ);
            routeX    = originX;
            direction = OrbitRoutes.directionForRouteZ(routeZ);
        } else {
            routeX    = OrbitRoutes.snapToNearestRouteX(originX);
            routeZ    = originZ;
            direction = OrbitRoutes.directionForRouteX(routeX);
        }

        double orbitLength = SatelliteEntity.HALF_ORBIT * 2.0; // 60.000
        double spacing = orbitLength / count;

        SatelliteRegistry registry = SatelliteRegistry.get(source.getServer());
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            // Gleichmäßig über [-HALF_ORBIT, +HALF_ORBIT] verteilen (nie außerhalb des Wrap-Bereichs)
            double offset = -SatelliteEntity.HALF_ORBIT + spacing * i;
            double spawnX = axisX ? routeX + offset : routeX;
            double spawnZ = axisX ? routeZ           : routeZ + offset;

            SatelliteEntity satellite = ModEntities.SATELLITE.get().create(orbitLevel);
            if (satellite != null) {
                satellite.setPos(spawnX, SatelliteEntity.ORBIT_HEIGHT, spawnZ);
                satellite.setOrbitDirection(direction);
                satellite.setAxisX(axisX);
                orbitLevel.addFreshEntity(satellite);
                registry.register(satellite.getUUID(), spawnX, spawnZ, source.getServer().overworld().getGameTime(), direction, axisX);
                spawned++;
            }
        }

        int finalSpawned = spawned;
        double finalSpacing = spacing;
        String axis = axisX ? "X" : "Z";
        source.sendSuccess(() -> Component.literal(
                "§6[Starlink] §a" + finalSpawned + " Satellit(en) gleichmäßig verteilt §7(Achse " + axis
                + ", Abstand " + (int) finalSpacing + " Blöcke)"
        ), true);
        return finalSpawned;
    }

    private static int doSpawnBatch(ServerLevel orbitLevel, CommandSourceStack source,
                                    int count, boolean axisX,
                                    double routeX, double routeZ, int direction) {
        SatelliteRegistry registry = SatelliteRegistry.get(source.getServer());
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            double spawnX = axisX ? routeX + (SATELLITE_SPACING * i * direction) : routeX;
            double spawnZ = axisX ? routeZ                                        : routeZ + (SATELLITE_SPACING * i * direction);

            SatelliteEntity satellite = ModEntities.SATELLITE.get().create(orbitLevel);
            if (satellite != null) {
                satellite.setPos(spawnX, SatelliteEntity.ORBIT_HEIGHT, spawnZ);
                satellite.setOrbitDirection(direction);
                satellite.setAxisX(axisX);
                orbitLevel.addFreshEntity(satellite);
                registry.register(satellite.getUUID(), spawnX, spawnZ, source.getServer().overworld().getGameTime(), direction, axisX);
                spawned++;
            }
        }
        return spawned;
    }

    /**
     * Killt alle Satelliten — auch in ungeladenen Chunks.
     * Phase 1: Chunks force-loaden.
     * Phase 2: Einen Tick später Entities killen + Chunks wieder freigeben.
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
     * Setzt das 'launching'-Flag aller LaunchControllerBlockEntities in allen geladenen Dimensionen zurück.
     * Nützlich wenn eine Rakete ohne ordentliches Ende entfernt wurde (z.B. per /kill @e oder Server-Crash).
     */
    private static int resetControllers(CommandSourceStack source) {
        int count = 0;
        for (LaunchControllerBlockEntity ctrl : new ArrayList<>(LaunchControllerBlockEntity.ALL_LOADED)) {
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
