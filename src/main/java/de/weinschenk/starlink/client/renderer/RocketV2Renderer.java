package de.weinschenk.starlink.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.entity.RocketV2Entity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * V2-Rakete: breiterer Körper, Grid-Fins oben, Landungsbeine unten.
 * Eigenes Modell und eigene Textur (Starship-inspiriertes Design).
 */
public class RocketV2Renderer extends EntityRenderer<RocketV2Entity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(Starlink.MODID, "rocket_v2"), "main");

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Starlink.MODID, "textures/entity/rocket_v2.png");

    private final ModelPart body;
    private final ModelPart nose;
    private final ModelPart skirt;
    private final ModelPart gfinLeft;
    private final ModelPart gfinRight;
    private final ModelPart gfinFront;
    private final ModelPart gfinBack;
    private final ModelPart legLeft;
    private final ModelPart legRight;
    private final ModelPart legFront;
    private final ModelPart legBack;

    public RocketV2Renderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(LAYER_LOCATION);
        this.body      = root.getChild("body");
        this.nose      = root.getChild("nose");
        this.skirt     = root.getChild("skirt");
        this.gfinLeft  = root.getChild("gfin_left");
        this.gfinRight = root.getChild("gfin_right");
        this.gfinFront = root.getChild("gfin_front");
        this.gfinBack  = root.getChild("gfin_back");
        this.legLeft   = root.getChild("leg_left");
        this.legRight  = root.getChild("leg_right");
        this.legFront  = root.getChild("leg_front");
        this.legBack   = root.getChild("leg_back");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Hauptkörper: 10×32×10, vertikal zentriert ────────────────────────
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-5f, -16f, -5f, 10, 32, 10),
                PartPose.ZERO);

        // ── Spitze: 6×10×6, sitzt auf dem Körper ─────────────────────────────
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(40, 0)
                        .addBox(-3f, -26f, -3f, 6, 10, 6),
                PartPose.ZERO);

        // ── Triebwerks-Skirt: 12×4×12, breiter Boden ─────────────────────────
        root.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(0, 42)
                        .addBox(-6f, 16f, -6f, 12, 4, 12),
                PartPose.ZERO);

        // ── Grid-Fins (oben, 4 Stück) ─────────────────────────────────────────
        float gfinY = -12f;
        root.addOrReplaceChild("gfin_left",
                CubeListBuilder.create().texOffs(0, 58)
                        .addBox(-9f, gfinY, -2f, 4, 6, 4),
                PartPose.ZERO);

        root.addOrReplaceChild("gfin_right",
                CubeListBuilder.create().texOffs(16, 58)
                        .addBox(5f, gfinY, -2f, 4, 6, 4),
                PartPose.ZERO);

        root.addOrReplaceChild("gfin_front",
                CubeListBuilder.create().texOffs(32, 58)
                        .addBox(-2f, gfinY, -9f, 4, 6, 4),
                PartPose.ZERO);

        root.addOrReplaceChild("gfin_back",
                CubeListBuilder.create().texOffs(48, 58)
                        .addBox(-2f, gfinY, 5f, 4, 6, 4),
                PartPose.ZERO);

        // ── Landungsbeine (unten, nach außen geneigt ~25°) ────────────────────
        float legAngle = (float) Math.toRadians(25.0);
        root.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(40, 42)
                        .addBox(-1f, 0f, -1f, 2, 10, 2),
                PartPose.offsetAndRotation(-6f, 14f, 0f, 0f, 0f, -legAngle));

        root.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(48, 42)
                        .addBox(-1f, 0f, -1f, 2, 10, 2),
                PartPose.offsetAndRotation(6f, 14f, 0f, 0f, 0f, legAngle));

        root.addOrReplaceChild("leg_front",
                CubeListBuilder.create().texOffs(56, 42)
                        .addBox(-1f, 0f, -1f, 2, 10, 2),
                PartPose.offsetAndRotation(0f, 14f, -6f, legAngle, 0f, 0f));

        root.addOrReplaceChild("leg_back",
                CubeListBuilder.create().texOffs(64, 42)
                        .addBox(-1f, 0f, -1f, 2, 10, 2),
                PartPose.offsetAndRotation(0f, 14f, 6f, -legAngle, 0f, 0f));

        // Textur 128×128 – gibt genug UV-Raum für alle Teile
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void render(RocketV2Entity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(4.0f, 4.0f, 4.0f);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entitySolid(TEXTURE));
        body.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        nose.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        skirt.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        gfinLeft.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        gfinRight.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        gfinFront.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        gfinBack.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        legLeft.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        legRight.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        legFront.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        legBack.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RocketV2Entity entity) {
        return TEXTURE;
    }
}
