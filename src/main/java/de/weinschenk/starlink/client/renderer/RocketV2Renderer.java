package de.weinschenk.starlink.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.entity.RocketV2Entity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Wiederverwendet das Rocket-V1-Modell bis eigene V2-Assets fertig sind. */
public class RocketV2Renderer extends EntityRenderer<RocketV2Entity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Starlink.MODID, "textures/entity/rocket.png");

    private final ModelPart body;
    private final ModelPart nose;
    private final ModelPart finLeft;
    private final ModelPart finRight;
    private final ModelPart finFront;
    private final ModelPart finBack;

    public RocketV2Renderer(EntityRendererProvider.Context context) {
        super(context);
        // Gleiches Layer wie RocketRenderer
        ModelPart root = context.bakeLayer(RocketRenderer.LAYER_LOCATION);
        this.body     = root.getChild("body");
        this.nose     = root.getChild("nose");
        this.finLeft  = root.getChild("fin_left");
        this.finRight = root.getChild("fin_right");
        this.finFront = root.getChild("fin_front");
        this.finBack  = root.getChild("fin_back");
    }

    @Override
    public void render(RocketV2Entity entity, float entityYaw, float partialTick,
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
    public ResourceLocation getTextureLocation(RocketV2Entity entity) {
        return TEXTURE;
    }
}
