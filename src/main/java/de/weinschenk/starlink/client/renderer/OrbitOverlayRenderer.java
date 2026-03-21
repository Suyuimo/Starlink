package de.weinschenk.starlink.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.weinschenk.starlink.client.tracking.SatelliteTrackingClient;
import de.weinschenk.starlink.dimension.ModDimensions;
import de.weinschenk.starlink.entity.SatelliteEntity;
import de.weinschenk.starlink.item.GlassesFilterMode;
import de.weinschenk.starlink.item.OrbitGlassesItem;
import de.weinschenk.starlink.item.ReceiverGlassesItem;
import de.weinschenk.starlink.network.SatelliteRenderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static de.weinschenk.starlink.Starlink.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class OrbitOverlayRenderer {

    // 3D world display
    private static final float  DISPLAY_RADIUS          = 400f;
    private static final float  MARKER_SIZE             = 10f;
    private static final float  ARROW_LEN               = 20f;
    private static final int    RING_SEGMENTS           = 128;
    private static final double CONE_HALF_ANGLE         = Math.toRadians(20.0);
    // 90° = senkrechte Ringe → kreuzen sich am Zenit → Satelliten immer über dem Spieler sichtbar
    private static final double DISPLAY_INCLINATION        = Math.toRadians(90.0);
    private static final double DISPLAY_INCLINATION_WOBBLE = Math.toRadians(8.0);

    // 2D radar
    private static final float  DOT_RADIUS      = 4.0f;

    private static final int[][] ORBIT_COLORS = {
        {100, 180, 255},
        {255, 180,  50},
        {180,  80, 255},
        {255,  80,  80},
        { 80, 255,  80},
        {255, 255,  80},
        {255, 150, 200},
        {150, 255, 240},
    };

    // -------------------------------------------------------------------------

    private record OrbitDisplayParams(
            double cosNode, double sinNode, double cosInc, double sinInc,
            float nnx, float nny, float nnz) {}

    private static OrbitDisplayParams orbitDisplayParams(int oid, long gameTime) {
        // 15°-Schritte → 24 Orbits decken den vollen Himmel ab, danach Wiederholung
        double node = oid * Math.PI / 12.0;
        double inc  = DISPLAY_INCLINATION + DISPLAY_INCLINATION_WOBBLE
                      * Math.sin(gameTime / 800.0 + oid * Math.PI / 3.5);
        double cosNode = Math.cos(node), sinNode = Math.sin(node);
        double cosInc  = Math.cos(inc),  sinInc  = Math.sin(inc);
        return new OrbitDisplayParams(cosNode, sinNode, cosInc, sinInc,
                (float)(-sinNode * sinInc), (float)(-cosInc), (float)(cosNode * sinInc));
    }

    // =========================================================================
    // 3D World Rendering
    // =========================================================================

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

        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        GlassesFilterMode filterMode = GlassesFilterMode.get(headStack);
        List<SatelliteRenderData> satellites = applyFilter(SatelliteTrackingClient.getSatellites(), filterMode);
        if (satellites.isEmpty()) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        float cx = (float) cam.x, cy = (float) cam.y, cz = (float) cam.z;
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;

        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = ps.last().pose();
        Matrix3f normal = ps.last().normal();

        Set<Integer> activeOrbits = new LinkedHashSet<>();
        for (SatelliteRenderData sat : satellites) activeOrbits.add(sat.orbitId() % ORBIT_COLORS.length);

        // Orbit rings
        for (int oid : activeOrbits) {
            OrbitDisplayParams p = orbitDisplayParams(oid, gameTime);
            int[] oc = ORBIT_COLORS[oid];
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double a1 = 2*Math.PI*i/RING_SEGMENTS, a2 = 2*Math.PI*(i+1)/RING_SEGMENTS;
                float rx1 = cx + DISPLAY_RADIUS*(float)(Math.cos(a1)*p.cosNode() - Math.sin(a1)*p.sinNode()*p.cosInc());
                float ry1 = cy + DISPLAY_RADIUS*(float)(Math.sin(a1)*p.sinInc());
                float rz1 = cz + DISPLAY_RADIUS*(float)(Math.cos(a1)*p.sinNode() + Math.sin(a1)*p.cosNode()*p.cosInc());
                float rx2 = cx + DISPLAY_RADIUS*(float)(Math.cos(a2)*p.cosNode() - Math.sin(a2)*p.sinNode()*p.cosInc());
                float ry2 = cy + DISPLAY_RADIUS*(float)(Math.sin(a2)*p.sinInc());
                float rz2 = cz + DISPLAY_RADIUS*(float)(Math.cos(a2)*p.sinNode() + Math.sin(a2)*p.cosNode()*p.cosInc());
                drawLine3D(lines, matrix, normal, rx1,ry1,rz1, rx2,ry2,rz2,
                        oc[0],oc[1],oc[2],160, p.nnx(),p.nny(),p.nnz());
            }
        }

        // Satellite markers + arrows
        for (SatelliteRenderData sat : satellites) {
            int oid = sat.orbitId() % ORBIT_COLORS.length;
            double angle = sat.angle();
            OrbitDisplayParams p = orbitDisplayParams(oid, gameTime);

            float sx = cx + DISPLAY_RADIUS*(float)(Math.cos(angle)*p.cosNode() - Math.sin(angle)*p.sinNode()*p.cosInc());
            float sy = cy + DISPLAY_RADIUS*(float)(Math.sin(angle)*p.sinInc());
            float sz = cz + DISPLAY_RADIUS*(float)(Math.cos(angle)*p.sinNode() + Math.sin(angle)*p.cosNode()*p.cosInc());
            float tx = (float)(-Math.sin(angle)*p.cosNode() - Math.cos(angle)*p.sinNode()*p.cosInc());
            float ty = (float)( Math.cos(angle)*p.sinInc());
            float tz = (float)(-Math.sin(angle)*p.sinNode() + Math.cos(angle)*p.cosNode()*p.cosInc());

            boolean inCone = false;
            if (isReceiverGlasses) {
                double playerAngle = Math.atan2(cam.z, cam.x);
                double da = angle - playerAngle;
                da -= 2 * Math.PI * Math.floor((da + Math.PI) / (2 * Math.PI));
                inCone = Math.abs(da) <= CONE_HALF_ANGLE;
            }

            int[] oc = ORBIT_COLORS[oid];
            int mr = inCone ? 0 : oc[0], mg = inCone ? 255 : oc[1],
                mb = inCone ? 0 : oc[2], ma = inCone ? 255 : 180;

            drawLine3D(lines, matrix, normal,
                    sx-MARKER_SIZE*tx, sy-MARKER_SIZE*ty, sz-MARKER_SIZE*tz,
                    sx+MARKER_SIZE*tx, sy+MARKER_SIZE*ty, sz+MARKER_SIZE*tz,
                    mr, mg, mb, ma, p.nnx(), p.nny(), p.nnz());
            drawLine3D(lines, matrix, normal,
                    sx-MARKER_SIZE*p.nnx(), sy-MARKER_SIZE*p.nny(), sz-MARKER_SIZE*p.nnz(),
                    sx+MARKER_SIZE*p.nnx(), sy+MARKER_SIZE*p.nny(), sz+MARKER_SIZE*p.nnz(),
                    mr, mg, mb, ma, p.nnx(), p.nny(), p.nnz());

            float tipX = sx+ARROW_LEN*tx, tipY = sy+ARROW_LEN*ty, tipZ = sz+ARROW_LEN*tz;
            drawLine3D(lines, matrix, normal, sx, sy, sz, tipX, tipY, tipZ,
                    0, 220, 255, 220, p.nnx(), p.nny(), p.nnz());
            float s = ARROW_LEN * 0.3f;
            drawLine3D(lines, matrix, normal,
                    tipX-tx*s*0.6f+p.nnx()*s*0.4f, tipY-ty*s*0.6f+p.nny()*s*0.4f, tipZ-tz*s*0.6f+p.nnz()*s*0.4f,
                    tipX, tipY, tipZ, 0, 220, 255, 220, p.nnx(), p.nny(), p.nnz());
            drawLine3D(lines, matrix, normal,
                    tipX-tx*s*0.6f-p.nnx()*s*0.4f, tipY-ty*s*0.6f-p.nny()*s*0.4f, tipZ-tz*s*0.6f-p.nnz()*s*0.4f,
                    tipX, tipY, tipZ, 0, 220, 255, 220, p.nnx(), p.nny(), p.nnz());
        }

        ps.popPose();
        buffers.endBatch(RenderType.lines());
    }

    // =========================================================================
    // 2D HUD Rendering
    // =========================================================================

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Nicht rendern wenn eine GUI/Screen offen ist
        if (mc.screen != null) return;

        var headItem = player.getItemBySlot(EquipmentSlot.HEAD).getItem();
        boolean isOrbit    = headItem instanceof OrbitGlassesItem;
        boolean isReceiver = headItem instanceof ReceiverGlassesItem;
        if (!isOrbit && !isReceiver) return;

        // Nur anzeigen wenn der Spieler nach oben schaut.
        // xRot: negativ = Blick nach oben, positiv = Blick nach unten
        // Vollständig sichtbar ab -30°, ausgeblendet ab 0° (Horizont)
        float pitch = player.getXRot();
        if (pitch >= 0) return;
        float skyAlpha = Math.min(1.0f, -pitch / 30.0f); // 0..1

        ItemStack headStack2 = player.getItemBySlot(EquipmentSlot.HEAD);
        GlassesFilterMode filterMode2 = GlassesFilterMode.get(headStack2);
        List<SatelliteRenderData> allSats = applyFilter(SatelliteTrackingClient.getSatellites(), filterMode2);
        int total = SatelliteTrackingClient.getSatellites().size(); // Gesamtzahl ungefiltert für Statuszeile

        GuiGraphics graphics = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // Dark space tint – Opazität abhängig vom Blickwinkel
        int tintAlpha = (int)(0x44 * skyAlpha);
        graphics.fill(0, 0, sw, sh, (tintAlpha << 24) | 0x000A1E);

        // 2D Radar (top-right corner)
        float radarR  = 105f;
        float radarCx = sw - radarR - 14;
        float radarCy = radarR + 14;
        drawRadar2D(graphics, radarCx, radarCy, radarR, allSats, isReceiver, player, skyAlpha);

        // Status line
        String text;
        if (isOrbit) {
            text = total == 0
                    ? "\u00a7cOrbit Glasses \u00a77\u2014 Keine Satelliten"
                    : "\u00a7aOrbit Glasses \u00a77\u2014 " + total + " Satellit" + (total == 1 ? "" : "en");
        } else {
            double playerAngle = Math.atan2(player.getZ(), player.getX());
            long inCone = allSats.stream().filter(s -> {
                double da = s.angle() - playerAngle;
                da -= 2 * Math.PI * Math.floor((da + Math.PI) / (2 * Math.PI));
                return Math.abs(da) <= CONE_HALF_ANGLE;
            }).count();
            text = inCone == 0
                    ? "\u00a7cReceiver Glasses \u00a77\u2014 Kein Signal"
                    : "\u00a7aReceiver Glasses \u00a77\u2014 " + inCone
                      + " Satellit" + (inCone == 1 ? "" : "en") + " im 20\u00b0-Bereich";
        }
        int textAlpha = (int)(255 * skyAlpha);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        graphics.drawString(mc.font, text, 4, 4, textColor, true);
    }

    // -------------------------------------------------------------------------

    private static void drawRadar2D(GuiGraphics graphics, float cx, float cy, float radarR,
                                     List<SatelliteRenderData> sats, boolean showCone,
                                     Player player, float alpha) {
        PoseStack ps = graphics.pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator ts = Tesselator.getInstance();
        BufferBuilder buf;
        Matrix4f mat;

        // Himmels-HUD: Spieler in der Mitte (= Zenit).
        // relAngle=0 → r=0 (Mitte/direkt über dir), relAngle=90° → r=radarR (Rand).
        // 12 Uhr = aktuelle Blickrichtung des Spielers.
        double playerAzimuth = Math.atan2(player.getZ(), player.getX());
        double yawRad        = Math.toRadians(player.getYRot());
        double facingAngle   = Math.atan2(Math.cos(yawRad), -Math.sin(yawRad));

        // ── Hintergrundkreis ──────────────────────────────────────────────────
        buf = ts.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        mat = ps.last().pose();
        buf.vertex(mat, cx, cy, 0).color(0, 5, 18, (int)(210 * alpha)).endVertex();
        for (int i = 0; i <= 64; i++) {
            double a = 2 * Math.PI * i / 64;
            buf.vertex(mat, cx + radarR*(float)Math.cos(a), cy + radarR*(float)Math.sin(a), 0)
               .color(0, 3, 12, (int)(200 * alpha)).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());

        // ── Signal-Kegel-Ring ─────────────────────────────────────────────────
        float coneR = (float)(CONE_HALF_ANGLE / (Math.PI / 2)) * radarR;
        buf = ts.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        mat = ps.last().pose();
        for (int i = 0; i < 64; i++) {
            double a1 = 2*Math.PI*i/64, a2 = 2*Math.PI*(i+1)/64;
            thickLine(buf, mat,
                    cx + coneR*(float)Math.cos(a1), cy + coneR*(float)Math.sin(a1),
                    cx + coneR*(float)Math.cos(a2), cy + coneR*(float)Math.sin(a2),
                    5f, 0, 180, 80, (int)(50 * alpha));
            thickLine(buf, mat,
                    cx + coneR*(float)Math.cos(a1), cy + coneR*(float)Math.sin(a1),
                    cx + coneR*(float)Math.cos(a2), cy + coneR*(float)Math.sin(a2),
                    1.5f, 0, 220, 100, (int)(160 * alpha));
        }
        BufferUploader.drawWithShader(buf.end());

        // ── Kompasslinien (relativ zur Blickrichtung) ─────────────────────────
        buf = ts.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        mat = ps.last().pose();
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2;
            thickLine(buf, mat, cx, cy,
                    cx + radarR*(float)Math.sin(a), cy - radarR*(float)Math.cos(a),
                    0.8f, 30, 60, 100, (int)(80 * alpha));
        }
        BufferUploader.drawWithShader(buf.end());

        // ── Satelliten-Punkte ─────────────────────────────────────────────────
        buf = ts.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        mat = ps.last().pose();

        for (SatelliteRenderData sat : sats) {
            int oid = sat.orbitId() % ORBIT_COLORS.length;
            int[] oc = ORBIT_COLORS[oid];

            double relAngle = sat.angle() - playerAzimuth;
            relAngle -= 2 * Math.PI * Math.floor((relAngle + Math.PI) / (2 * Math.PI));
            float r = (float)(Math.min(Math.abs(relAngle) / (Math.PI / 2), 1.0) * radarR);

            double deltaAz = sat.angle() - facingAngle;
            float sx = cx + r * (float)Math.sin(deltaAz);
            float sy = cy - r * (float)Math.cos(deltaAz);

            boolean inCone = showCone && Math.abs(relAngle) <= CONE_HALF_ANGLE;
            int dr = inCone ? 0 : oc[0], dg = inCone ? 255 : oc[1], db = inCone ? 0 : oc[2];
            dot(buf, mat, sx, sy, DOT_RADIUS + 3f, dr, dg, db, (int)(50  * alpha));
            dot(buf, mat, sx, sy, DOT_RADIUS,       dr, dg, db, (int)(255 * alpha));
        }

        // Spieler-Marker in der Mitte
        int pa = (int)(255 * alpha);
        thickLine(buf, mat, cx-7, cy,   cx+7, cy,   2.5f, 255, 40, 40, pa);
        thickLine(buf, mat, cx,   cy-7, cx,   cy+7, 2.5f, 255, 40, 40, pa);
        dot(buf, mat, cx, cy, 2.5f, 255, 255, 255, pa);
        BufferUploader.drawWithShader(buf.end());

        // ── Rand-Ring ─────────────────────────────────────────────────────────
        buf = ts.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        mat = ps.last().pose();
        for (int i = 0; i < 64; i++) {
            double a1 = 2*Math.PI*i/64, a2 = 2*Math.PI*(i+1)/64;
            thickLine(buf, mat,
                    cx + radarR*(float)Math.cos(a1), cy + radarR*(float)Math.sin(a1),
                    cx + radarR*(float)Math.cos(a2), cy + radarR*(float)Math.sin(a2),
                    1.5f, 60, 120, 200, (int)(200 * alpha));
        }
        BufferUploader.drawWithShader(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Filtert die Satelliten-Liste nach dem Glas-Filter-Modus. */
    private static List<SatelliteRenderData> applyFilter(List<SatelliteRenderData> all,
                                                          GlassesFilterMode mode) {
        return switch (mode) {
            case ALL          -> all;
            case PUBLIC_ONLY  -> all.stream().filter(s -> !s.isPrivate()).toList();
            case PRIVATE_ONLY -> all.stream().filter(SatelliteRenderData::isPrivate).toList();
        };
    }

    private static void thickLine(BufferBuilder buf, Matrix4f mat,
                                   float x1, float y1, float x2, float y2,
                                   float thickness, int r, int g, int b, int a) {
        float dx = x2-x1, dy = y2-y1;
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        if (len < 0.01f) return;
        float nx = -dy/len * thickness * 0.5f;
        float ny =  dx/len * thickness * 0.5f;
        buf.vertex(mat, x1+nx, y1+ny, 0).color(r,g,b,a).endVertex();
        buf.vertex(mat, x1-nx, y1-ny, 0).color(r,g,b,a).endVertex();
        buf.vertex(mat, x2-nx, y2-ny, 0).color(r,g,b,a).endVertex();
        buf.vertex(mat, x2+nx, y2+ny, 0).color(r,g,b,a).endVertex();
    }

    private static void dot(BufferBuilder buf, Matrix4f mat,
                             float cx, float cy, float r, int R, int G, int B, int A) {
        buf.vertex(mat, cx-r, cy-r, 0).color(R,G,B,A).endVertex();
        buf.vertex(mat, cx-r, cy+r, 0).color(R,G,B,A).endVertex();
        buf.vertex(mat, cx+r, cy+r, 0).color(R,G,B,A).endVertex();
        buf.vertex(mat, cx+r, cy-r, 0).color(R,G,B,A).endVertex();
    }

    private static void drawLine3D(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    int r, int g, int b, int a,
                                    float nx, float ny, float nz) {
        vc.vertex(m, x1, y1, z1).color(r,g,b,a).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r,g,b,a).normal(n, nx, ny, nz).endVertex();
    }
}
