package de.weinschenk.starlink.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.weinschenk.starlink.client.tracking.SatelliteTrackingClient;
import de.weinschenk.starlink.dimension.OrbitRoutes;
import de.weinschenk.starlink.item.OrbitGlassesItem;
import de.weinschenk.starlink.item.ReceiverGlassesItem;
import de.weinschenk.starlink.network.SatelliteRenderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

import static de.weinschenk.starlink.Starlink.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class OrbitOverlayRenderer {

    // Wie weit die Routenlinie links/rechts gezeichnet wird (Blöcke)
    private static final float ROUTE_LINE_HALF_LENGTH = 800f;

    // Höhe über dem Spieler-Auge wo die Linien gezeichnet werden
    private static final float RENDER_HEIGHT_OFFSET = 80f;

    // Größe des Satelliten-Markers (Kreuz-Armlänge in Blöcken)
    private static final float MARKER_SIZE = 12f;

    // Empfänger-Reichweite (muss mit ReceiverBlockEntity.SATELLITE_RANGE übereinstimmen)
    private static final double RECEIVER_RANGE = 100.0;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Welche Brille wird getragen?
        var headItem = player.getItemBySlot(EquipmentSlot.HEAD).getItem();
        boolean isOrbitGlasses    = headItem instanceof OrbitGlassesItem;
        boolean isReceiverGlasses = headItem instanceof ReceiverGlassesItem;
        if (!isOrbitGlasses && !isReceiverGlasses) return;

        List<SatelliteRenderData> allSatellites = SatelliteTrackingClient.getSatellites();
        if (allSatellites.isEmpty()) return;

        // Empfänger-Brille: nur Satelliten in ±RECEIVER_RANGE filtern
        List<SatelliteRenderData> satellites = isReceiverGlasses
                ? allSatellites.stream()
                        .filter(s -> Math.abs(s.x() - player.getX()) <= RECEIVER_RANGE
                                  && Math.abs(s.z() - player.getZ()) <= RECEIVER_RANGE)
                        .toList()
                : allSatellites;

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double renderY = cam.y + RENDER_HEIGHT_OFFSET;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        // Verschieben auf Welt-Koordinaten (PoseStack ist kamera-relativ)
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        for (SatelliteRenderData sat : satellites) {
            int[] color = routeColor(sat.axisX() ? sat.z() : sat.x());
            int r = color[0], g = color[1], b = color[2];

            float satX  = (float) sat.x();
            float satZ  = (float) sat.z();
            float ry    = (float) renderY;
            float camXf = (float) cam.x;
            float camZf = (float) cam.z;
            int   dir   = sat.direction();

            // ── Routenlinie ──────────────────────────────────────────────────
            if (sat.axisX()) {
                // X-Achsen-Satellit: Linie entlang X, konstantes Z
                drawDashedLineX(lines, matrix, normal,
                        camXf - ROUTE_LINE_HALF_LENGTH, ry, satZ,
                        camXf + ROUTE_LINE_HALF_LENGTH, ry, satZ,
                        r, g, b, 160, dir);
            } else {
                // Z-Achsen-Satellit: Linie entlang Z, konstantes X
                drawDashedLineZ(lines, matrix, normal,
                        satX, ry, camZf - ROUTE_LINE_HALF_LENGTH,
                        satX, ry, camZf + ROUTE_LINE_HALF_LENGTH,
                        r, g, b, 160, dir);
            }

            // ── Satellit-Marker (Kreuz) ───────────────────────────────────────
            drawLine(lines, matrix, normal,
                    satX - MARKER_SIZE, ry, satZ,
                    satX + MARKER_SIZE, ry, satZ,
                    255, 255, 255, 255, 1, 0, 0);
            drawLine(lines, matrix, normal,
                    satX, ry, satZ - MARKER_SIZE,
                    satX, ry, satZ + MARKER_SIZE,
                    255, 255, 255, 255, 0, 0, 1);

            // Senkrechte Projektion nach unten
            drawLine(lines, matrix, normal,
                    satX, ry, satZ,
                    satX, (float)(cam.y - 20), satZ,
                    r, g, b, 60, 0, -1, 0);

            // ── Richtungspfeil ────────────────────────────────────────────────
            if (sat.axisX()) {
                // Pfeil entlang X
                float tip  = satX + (dir * (MARKER_SIZE + 8));
                float base = satX + (dir * (MARKER_SIZE + 2));
                drawLine(lines, matrix, normal, base, ry, satZ - 4, tip, ry, satZ, r, g, b, 220, dir, 0, 0);
                drawLine(lines, matrix, normal, base, ry, satZ + 4, tip, ry, satZ, r, g, b, 220, dir, 0, 0);
            } else {
                // Pfeil entlang Z
                float tip  = satZ + (dir * (MARKER_SIZE + 8));
                float base = satZ + (dir * (MARKER_SIZE + 2));
                drawLine(lines, matrix, normal, satX - 4, ry, base, satX, ry, tip, r, g, b, 220, 0, 0, dir);
                drawLine(lines, matrix, normal, satX + 4, ry, base, satX, ry, tip, r, g, b, 220, 0, 0, dir);
            }
        }

        // Empfänger-Brille: Reichweite-Rahmen um den Spieler zeichnen
        if (isReceiverGlasses) {
            float ry  = (float)(cam.y + RENDER_HEIGHT_OFFSET);
            float cx  = (float) cam.x;
            float cz  = (float) cam.z;
            float r   = (float) RECEIVER_RANGE;
            // Grüner Rahmen: 200×200 Block Quadrat
            drawLine(lines, matrix, normal, cx - r, ry, cz - r, cx + r, ry, cz - r, 0, 255, 80, 180, 1,0,0);
            drawLine(lines, matrix, normal, cx + r, ry, cz - r, cx + r, ry, cz + r, 0, 255, 80, 180, 0,0,1);
            drawLine(lines, matrix, normal, cx + r, ry, cz + r, cx - r, ry, cz + r, 0, 255, 80, 180, -1,0,0);
            drawLine(lines, matrix, normal, cx - r, ry, cz + r, cx - r, ry, cz - r, 0, 255, 80, 180, 0,0,-1);
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /** HUD-Anzeige: Anzahl der verfolgten Satelliten in der oberen linken Ecke. */
    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        var headItem = player.getItemBySlot(EquipmentSlot.HEAD).getItem();
        boolean isOrbit    = headItem instanceof OrbitGlassesItem;
        boolean isReceiver = headItem instanceof ReceiverGlassesItem;
        if (!isOrbit && !isReceiver) return;

        List<SatelliteRenderData> allSats = SatelliteTrackingClient.getSatellites();

        String text;
        if (isOrbit) {
            text = allSats.isEmpty()
                    ? "\u00a7cOrbit Glasses \u00a77\u2014 Keine Satelliten"
                    : "\u00a7aOrbit Glasses \u00a77\u2014 " + allSats.size() + " Satellit" + (allSats.size() == 1 ? "" : "en");
        } else {
            long inRange = allSats.stream()
                    .filter(s -> Math.abs(s.x() - player.getX()) <= RECEIVER_RANGE
                              && Math.abs(s.z() - player.getZ()) <= RECEIVER_RANGE)
                    .count();
            text = inRange == 0
                    ? "\u00a7cReceiver Glasses \u00a77\u2014 Kein Signal"
                    : "\u00a7aReceiver Glasses \u00a77\u2014 " + inRange + " Satellit" + (inRange == 1 ? "" : "en") + " in Reichweite";
        }

        GuiGraphics gui = event.getGuiGraphics();
        gui.drawString(mc.font, text, 4, 4, 0xFFFFFF, true);
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private static void drawLine(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  int r, int g, int b, int a,
                                  float nx, float ny, float nz) {
        vc.vertex(m, x1, y1, z1).color(r, g, b, a).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, a).normal(n, nx, ny, nz).endVertex();
    }

    /** Gestrichelte Linie entlang der X-Achse (konstantes Z). */
    private static void drawDashedLineX(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                         float x1, float y, float z,
                                         float x2, float y2, float z2,
                                         int r, int g, int b, int a, int direction) {
        float dashLen = 20f, step = 30f;
        float nx = direction > 0 ? 1f : -1f;
        for (float cursor = x1; cursor < x2; cursor += step) {
            float end = Math.min(cursor + dashLen, x2);
            vc.vertex(m, cursor, y, z).color(r, g, b, a).normal(n, nx, 0, 0).endVertex();
            vc.vertex(m, end,    y, z).color(r, g, b, a).normal(n, nx, 0, 0).endVertex();
        }
    }

    /** Gestrichelte Linie entlang der Z-Achse (konstantes X). */
    private static void drawDashedLineZ(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                         float x, float y, float z1,
                                         float x2, float y2, float z2,
                                         int r, int g, int b, int a, int direction) {
        float dashLen = 20f, step = 30f;
        float nz = direction > 0 ? 1f : -1f;
        for (float cursor = z1; cursor < z2; cursor += step) {
            float end = Math.min(cursor + dashLen, z2);
            vc.vertex(m, x, y, cursor).color(r, g, b, a).normal(n, 0, 0, nz).endVertex();
            vc.vertex(m, x, y, end   ).color(r, g, b, a).normal(n, 0, 0, nz).endVertex();
        }
    }

    /**
     * Gibt eine Farbe für eine Route zurück basierend auf ihrem Z-Index.
     * Verschiedene Routen bekommen verschiedene Farben damit sie unterscheidbar sind.
     */
    private static int[] routeColor(double routeZ) {
        int index = (int) Math.round(routeZ / OrbitRoutes.ROUTE_SPACING);
        // Farben im Hue-Kreis verteilt
        int hueStep = 60;
        float hue = ((Math.abs(index) * hueStep) % 360) / 360f;

        // Einfache HSV→RGB Konvertierung (S=1, V=1)
        float h6 = hue * 6f;
        int   hi = (int) h6;
        float f  = h6 - hi;
        float q  = 1 - f;
        float[] rgb = switch (hi % 6) {
            case 0 -> new float[]{1, f, 0};
            case 1 -> new float[]{q, 1, 0};
            case 2 -> new float[]{0, 1, f};
            case 3 -> new float[]{0, q, 1};
            case 4 -> new float[]{f, 0, 1};
            default -> new float[]{1, 0, q};
        };
        return new int[]{(int)(rgb[0]*255), (int)(rgb[1]*255), (int)(rgb[2]*255)};
    }
}
