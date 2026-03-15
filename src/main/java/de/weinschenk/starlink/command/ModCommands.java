package de.weinschenk.starlink.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.block.LaunchControllerBlockEntity;
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
                                    IntegerArgumentType.getInteger(ctx, "count"), 0))
                            .then(Commands.argument("offset", IntegerArgumentType.integer(0))
                                .executes(ctx -> spawnSatellites(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "count"),
                                        IntegerArgumentType.getInteger(ctx, "offset"))))))
                    .then(Commands.literal("fill")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 2000))
                            .executes(ctx -> fillOrbit(ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "count"))))))
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
    private static int spawnSatellites(CommandSourceStack source, int count, int offsetBlocks) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        Vec3 pos = source.getPosition();
        double baseAngle = Math.atan2(pos.z, pos.x)
                + offsetBlocks / SatelliteEntity.ORBIT_RADIUS;

        double angularSpacing = SATELLITE_SPACING / SatelliteEntity.ORBIT_RADIUS;

        SatelliteRegistry registry = SatelliteRegistry.get(source.getServer());
        long startTick = source.getServer().overworld().getGameTime();
        int spawned = 0;

        for (int i = 0; i < count; i++) {
            double angle  = baseAngle + i * angularSpacing;
            double spawnX = SatelliteEntity.ORBIT_RADIUS * Math.cos(angle);
            double spawnZ = SatelliteEntity.ORBIT_RADIUS * Math.sin(angle);

            SatelliteEntity satellite = ModEntities.SATELLITE.get().create(orbitLevel);
            if (satellite != null) {
                satellite.setAngle(angle);
                satellite.setPos(spawnX, SatelliteEntity.ORBIT_HEIGHT, spawnZ);
                orbitLevel.addFreshEntity(satellite);
                registry.register(satellite.getUUID(), angle, startTick);
                spawned++;
            }
        }

        int finalSpawned = spawned;
        source.sendSuccess(() -> Component.literal(
                "§6[Starlink] §a" + finalSpawned + " Satellit(en) gespawnt §7(Offset " + offsetBlocks + " Blöcke)"
        ), true);
        return finalSpawned;
    }

    /**
     * Verteilt {@code count} Satelliten gleichmäßig über den gesamten Orbit-Ring (2π).
     */
    private static int fillOrbit(CommandSourceStack source, int count) {
        ServerLevel orbitLevel = source.getServer().getLevel(ModDimensions.ORBIT_LEVEL_KEY);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Orbit-Dimension nicht gefunden!"));
            return 0;
        }

        double angularSpacing = (2 * Math.PI) / count;

        SatelliteRegistry registry = SatelliteRegistry.get(source.getServer());
        long startTick = source.getServer().overworld().getGameTime();
        int spawned = 0;

        for (int i = 0; i < count; i++) {
            double angle  = i * angularSpacing;
            double spawnX = SatelliteEntity.ORBIT_RADIUS * Math.cos(angle);
            double spawnZ = SatelliteEntity.ORBIT_RADIUS * Math.sin(angle);

            SatelliteEntity satellite = ModEntities.SATELLITE.get().create(orbitLevel);
            if (satellite != null) {
                satellite.setAngle(angle);
                satellite.setPos(spawnX, SatelliteEntity.ORBIT_HEIGHT, spawnZ);
                orbitLevel.addFreshEntity(satellite);
                registry.register(satellite.getUUID(), angle, startTick);
                spawned++;
            }
        }

        int finalSpawned = spawned;
        int spacingBlocks = (int)(angularSpacing * SatelliteEntity.ORBIT_RADIUS);
        source.sendSuccess(() -> Component.literal(
                "§6[Starlink] §a" + finalSpawned + " Satellit(en) gleichmäßig verteilt §7(Abstand ~" + spacingBlocks + " Blöcke)"
        ), true);
        return finalSpawned;
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
        int finalCount = count;
        source.sendSuccess(() -> Component.literal(finalCount + " Controller zurückgesetzt."), true);
        return finalCount;
    }
}
