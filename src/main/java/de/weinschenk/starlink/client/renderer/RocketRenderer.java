package de.weinschenk.starlink.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.entity.RocketEntity;
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

public class RocketRenderer extends EntityRenderer<RocketEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(Starlink.MODID, "rocket"), "main");

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Starlink.MODID, "textures/entity/rocket.png");

    private final ModelPart body;
    private final ModelPart nose;
    private final ModelPart finLeft;
    private final ModelPart finRight;
    private final ModelPart finFront;
    private final ModelPart finBack;

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(LAYER_LOCATION);
        this.body    = root.getChild("body");
        this.nose    = root.getChild("nose");
        this.finLeft  = root.getChild("fin_left");
        this.finRight = root.getChild("fin_right");
        this.finFront = root.getChild("fin_front");
        this.finBack  = root.getChild("fin_back");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Hauptkörper: 6x24x6 (schlanker Zylinder approximiert als Box)
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3f, -12f, -3f, 6, 24, 6),
                PartPose.ZERO);

        // Spitze: 4x8x4
        root.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-2f, -20f, -2f, 4, 8, 4),
                PartPose.ZERO);

        // Fins (4 Stabilisierungsflossen unten)
        root.addOrReplaceChild("fin_left",
                CubeListBuilder.create().texOffs(0, 30)
                        .addBox(-7f, 4f, -1f, 4, 8, 2),
                PartPose.ZERO);

        root.addOrReplaceChild("fin_right",
                CubeListBuilder.create().texOffs(0, 30)
                        .addBox(3f, 4f, -1f, 4, 8, 2),
                PartPose.ZERO);

        root.addOrReplaceChild("fin_front",
                CubeListBuilder.create().texOffs(12, 30)
                        .addBox(-1f, 4f, -7f, 2, 8, 4),
                PartPose.ZERO);

        root.addOrReplaceChild("fin_back",
                CubeListBuilder.create().texOffs(12, 30)
                        .addBox(-1f, 4f, 3f, 2, 8, 4),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(RocketEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(6.0f, 6.0f, 6.0f);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entitySolid(TEXTURE));
        body.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        nose.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        finLeft.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        finRight.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        finFront.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        finBack.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RocketEntity entity) {
        return TEXTURE;
    }
}
