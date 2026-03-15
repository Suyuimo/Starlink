package de.weinschenk.starlink.command;

import com.mojang.brigadier.CommandDispatcher;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.block.LaunchControllerBlockEntity;
import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.entity.SatelliteEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
                        .executes(ctx -> killAllSatellites(ctx.getSource()))))
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

    // Bereich der force-geladen wird um Satelliten in ungeladenen Chunks zu finden
    private static final int KILL_CHUNK_RADIUS = 50;

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

        // Phase 1: Bereich force-loaden damit Entities aktiv werden
        for (int cx = -KILL_CHUNK_RADIUS; cx <= KILL_CHUNK_RADIUS; cx++) {
            for (int cz = -KILL_CHUNK_RADIUS; cz <= KILL_CHUNK_RADIUS; cz++) {
                orbitLevel.setChunkForced(cx, cz, true);
            }
        }

        // Phase 2: Nächsten Tick abwarten, dann killen & Chunks freigeben
        source.getServer().execute(() -> {
            List<SatelliteEntity> toKill = new ArrayList<>();
            for (Entity entity : orbitLevel.getAllEntities()) {
                if (entity instanceof SatelliteEntity sat) {
                    toKill.add(sat);
                }
            }
            toKill.forEach(sat -> {
                sat.setActive(false);
                sat.discard();
            });
            // Forced-Chunks wieder freigeben
            for (int cx = -KILL_CHUNK_RADIUS; cx <= KILL_CHUNK_RADIUS; cx++) {
                for (int cz = -KILL_CHUNK_RADIUS; cz <= KILL_CHUNK_RADIUS; cz++) {
                    orbitLevel.setChunkForced(cx, cz, false);
                }
            }
            source.sendSuccess(() -> Component.literal(toKill.size() + " Satellit(en) entfernt."), true);
        });

        source.sendSuccess(() -> Component.literal("Bereinigung läuft..."), false);
        return 1;
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
