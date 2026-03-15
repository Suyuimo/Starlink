package de.weinschenk.starlink.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.weinschenk.starlink.client.tracking.SatelliteTrackingClient;
import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.entity.SatelliteEntity;
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

    private static final float  DISPLAY_RADIUS   = 400f;
    private static final float  MARKER_SIZE       = 10f;
    private static final float  ARROW_LEN         = 20f;
    private static final int    RING_SEGMENTS     = 128;
    private static final int    CONE_SEGMENTS     = 64;
    private static final double CONE_HALF_ANGLE   = Math.toRadians(20.0);

    /** Eine volle Umdrehung der Orbitalebene dauert ~10 Minuten (12 000 Ticks). */
    private static final double INCLINATION_PERIOD = 12_000.0;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        var headItem = player.getItemBySlot(EquipmentSlot.HEAD).getItem();
        boolean isOrbitGlasses    = headItem instanceof OrbitGlassesItem;
        boolean isReceiverGlasses = headItem instanceof ReceiverGlassesItem;
        if (!isOrbitGlasses && !isReceiverGlasses) return;

        if (player.level().dimension().equals(ModDimensions.ORBIT_LEVEL_KEY)) return;

        List<SatelliteRenderData> satellites = SatelliteTrackingClient.getSatellites();
        if (satellites.isEmpty()) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        float cx = (float) cam.x;
        float cy = (float) cam.y;
        float cz = (float) cam.z;

        // Inclination dreht sich langsam – eine volle Umdrehung alle ~10 Minuten
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        double inclination = (gameTime * Math.PI * 2.0 / INCLINATION_PERIOD) % (Math.PI * 2.0);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // Orbitale Ebene: u=(1,0,0), v=(0, cos i, sin i)
        // Punkt auf Ring: P(a) = center + R*(cos(a)*u + sin(a)*v)
        //   px = cx + R*cos(a)
        //   py = cy + R*sin(a)*cos(i)
        //   pz = cz + R*sin(a)*sin(i)
        // Normale der Orbitebene: u × v = (0, -sin i, cos i)
        float normY = (float) -Math.sin(inclination);
        float normZ = (float)  Math.cos(inclination);

        // ── Orbit-Ring (geneigt) ──────────────────────────────────────────────
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double a1 = 2 * Math.PI * i       / RING_SEGMENTS;
            double a2 = 2 * Math.PI * (i + 1) / RING_SEGMENTS;
            float rx1 = cx + DISPLAY_RADIUS * (float) Math.cos(a1);
            float ry1 = cy + DISPLAY_RADIUS * (float)(Math.sin(a1) * Math.cos(inclination));
            float rz1 = cz + DISPLAY_RADIUS * (float)(Math.sin(a1) * Math.sin(inclination));
            float rx2 = cx + DISPLAY_RADIUS * (float) Math.cos(a2);
            float ry2 = cy + DISPLAY_RADIUS * (float)(Math.sin(a2) * Math.cos(inclination));
            float rz2 = cz + DISPLAY_RADIUS * (float)(Math.sin(a2) * Math.sin(inclination));
            drawLine(lines, matrix, normal, rx1, ry1, rz1, rx2, ry2, rz2, 100, 180, 255, 160, 0, normY, normZ);
        }

        // ── Satelliten-Marker ─────────────────────────────────────────────────
        for (SatelliteRenderData sat : satellites) {
            double angle = sat.angle();
            float sx = cx + DISPLAY_RADIUS * (float) Math.cos(angle);
            float sy = cy + DISPLAY_RADIUS * (float)(Math.sin(angle) * Math.cos(inclination));
            float sz = cz + DISPLAY_RADIUS * (float)(Math.sin(angle) * Math.sin(inclination));
            // Tangente: d/dθ (cos θ, sin θ * cos i, sin θ * sin i) = (-sin θ, cos θ * cos i, cos θ * sin i)
            float tx = (float) -Math.sin(angle);
            float ty = (float)(Math.cos(angle) * Math.cos(inclination));
            float tz = (float)(Math.cos(angle) * Math.sin(inclination));

            boolean inCone = false;
            if (isReceiverGlasses) {
                // Echter Zenith-Kegel: horizontaler Abstand des Satelliten zum Spieler
                double realSatX = SatelliteEntity.ORBIT_RADIUS * Math.cos(angle);
                double realSatZ = SatelliteEntity.ORBIT_RADIUS * Math.sin(angle);
                double vDist = SatelliteEntity.ORBIT_HEIGHT - cam.y;
                double maxHDist = vDist * Math.tan(CONE_HALF_ANGLE);
                double dx = realSatX - cam.x;
                double dz = realSatZ - cam.z;
                inCone = dx * dx + dz * dz <= maxHDist * maxHDist;
            }

            int mr = inCone ?   0 : 120;
            int mg = inCone ? 255 : 120;
            int mb = inCone ?   0 : 120;
            int ma = inCone ? 255 : 140;

            // Kreuz-Marker: ein Arm entlang Tangente, ein Arm entlang Ring-Normale
            drawLine(lines, matrix, normal,
                    sx - MARKER_SIZE * tx, sy - MARKER_SIZE * ty, sz - MARKER_SIZE * tz,
                    sx + MARKER_SIZE * tx, sy + MARKER_SIZE * ty, sz + MARKER_SIZE * tz,
                    mr, mg, mb, ma, 0, normY, normZ);
            drawLine(lines, matrix, normal,
                    sx, sy - MARKER_SIZE * normY, sz - MARKER_SIZE * normZ,
                    sx, sy + MARKER_SIZE * normY, sz + MARKER_SIZE * normZ,
                    mr, mg, mb, ma, 0, normY, normZ);

            // Richtungspfeil
            float tipX = sx + ARROW_LEN * tx;
            float tipY = sy + ARROW_LEN * ty;
            float tipZ = sz + ARROW_LEN * tz;
            drawLine(lines, matrix, normal, sx, sy, sz, tipX, tipY, tipZ, 0, 220, 255, 220, 0, normY, normZ);
            float s = ARROW_LEN * 0.3f;
            drawLine(lines, matrix, normal,
                    tipX - tx*s*0.6f + normY*s*0.4f, tipY - ty*s*0.6f - normZ*s*0.4f, tipZ - tz*s*0.6f,
                    tipX, tipY, tipZ, 0, 220, 255, 220, 0, normY, normZ);
            drawLine(lines, matrix, normal,
                    tipX - tx*s*0.6f - normY*s*0.4f, tipY - ty*s*0.6f + normZ*s*0.4f, tipZ - tz*s*0.6f,
                    tipX, tipY, tipZ, 0, 220, 255, 220, 0, normY, normZ);
        }

        // ── 20°-Empfangskreis (nur ReceiverGlasses) ──────────────────────────
        // Horizontaler Kreis auf Orbit-Höhe (Y=100) um den Spieler,
        // Radius = (ORBIT_HEIGHT - playerY) * tan(20°)
        if (isReceiverGlasses) {
            float orbitY    = (float) SatelliteEntity.ORBIT_HEIGHT;
            float vertDist  = orbitY - cy;
            float coneR     = Math.max(vertDist * (float) Math.tan(CONE_HALF_ANGLE), 5f);

            // Füllung: parallele Linien quer durch den Kreis (leichtes Grün)
            float step = Math.max(coneR / 12f, 0.5f);
            for (float dz = -coneR; dz <= coneR; dz += step) {
                float halfW = (float) Math.sqrt(Math.max(0, coneR * coneR - dz * dz));
                if (halfW < 0.1f) continue;
                drawLine(lines, matrix, normal,
                        cx - halfW, orbitY, cz + dz,
                        cx + halfW, orbitY, cz + dz,
                        0, 200, 80, 80, 0, 1, 0);
            }

            // Kreis-Umrandung auf Orbit-Höhe (hell und opak)
            for (int i = 0; i < CONE_SEGMENTS; i++) {
                double a1 = 2 * Math.PI * i       / CONE_SEGMENTS;
                double a2 = 2 * Math.PI * (i + 1) / CONE_SEGMENTS;
                float ex1 = cx + coneR * (float) Math.cos(a1);
                float ez1 = cz + coneR * (float) Math.sin(a1);
                float ex2 = cx + coneR * (float) Math.cos(a2);
                float ez2 = cz + coneR * (float) Math.sin(a2);
                drawLine(lines, matrix, normal, ex1, orbitY, ez1, ex2, orbitY, ez2,
                        0, 255, 80, 255, 0, 1, 0);
            }

            // 4 Mantellinien vom Spielerauge zur Kreislinie
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2.0 * i;
                float ex = cx + coneR * (float) Math.cos(a);
                float ez = cz + coneR * (float) Math.sin(a);
                drawLine(lines, matrix, normal, cx, cy, cz, ex, orbitY, ez,
                        0, 255, 80, 200, 0, 1, 0);
            }
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

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
        int total = allSats.size();

        String text;
        if (isOrbit) {
            text = total == 0
                    ? "\u00a7cOrbit Glasses \u00a77\u2014 Keine Satelliten"
                    : "\u00a7aOrbit Glasses \u00a77\u2014 " + total + " Satellit" + (total == 1 ? "" : "en");
        } else {
            double vDist = SatelliteEntity.ORBIT_HEIGHT - player.getY();
            double maxHDist = Math.max(vDist, 1.0) * Math.tan(CONE_HALF_ANGLE);
            double mhd2 = maxHDist * maxHDist;
            long inCone = allSats.stream().filter(s -> {
                double dx = SatelliteEntity.ORBIT_RADIUS * Math.cos(s.angle()) - player.getX();
                double dz = SatelliteEntity.ORBIT_RADIUS * Math.sin(s.angle()) - player.getZ();
                return dx * dx + dz * dz <= mhd2;
            }).count();
            text = inCone == 0
                    ? "\u00a7cReceiver Glasses \u00a77\u2014 Kein Signal"
                    : "\u00a7aReceiver Glasses \u00a77\u2014 " + inCone + " Satellit" + (inCone == 1 ? "" : "en") + " im 20\u00b0-Bereich";
        }

        event.getGuiGraphics().drawString(mc.font, text, 4, 4, 0xFFFFFF, true);
    }

    private static void drawLine(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  int r, int g, int b, int a,
                                  float nx, float ny, float nz) {
        vc.vertex(m, x1, y1, z1).color(r, g, b, a).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, a).normal(n, nx, ny, nz).endVertex();
    }
}
