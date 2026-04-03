package de.weinschenk.starlink.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.weinschenk.starlink.Starlink;
import de.weinschenk.starlink.entity.SatelliteEntity;
import de.weinschenk.starlink.entity.SatelliteType;
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

import java.util.EnumMap;
import java.util.Map;

public class SatelliteRenderer extends EntityRenderer<SatelliteEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(Starlink.MODID, "satellite"), "main");

    private static final Map<SatelliteType, ResourceLocation> TEXTURES;

    static {
        TEXTURES = new EnumMap<>(SatelliteType.class);
        for (SatelliteType type : SatelliteType.values()) {
            TEXTURES.put(type, new ResourceLocation(Starlink.MODID,
                    "textures/entity/satellite_" + type.key + ".png"));
        }
    }

    private final ModelPart body;
    private final ModelPart solarLeft;
    private final ModelPart solarRight;

    public SatelliteRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(LAYER_LOCATION);
        this.body = root.getChild("body");
        this.solarLeft = root.getChild("solar_left");
        this.solarRight = root.getChild("solar_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3f, -2f, -2f, 6, 4, 4),
                PartPose.ZERO);

        root.addOrReplaceChild("solar_left",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-11f, -0.5f, -2f, 8, 1, 4),
                PartPose.ZERO);

        root.addOrReplaceChild("solar_right",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(3f, -0.5f, -2f, 8, 1, 4),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(SatelliteEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(6.0f, 6.0f, 6.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(entity.getAngle())));

        ResourceLocation texture = TEXTURES.get(entity.getSatelliteType());
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entitySolid(texture));

        body.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        solarLeft.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        solarRight.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SatelliteEntity entity) {
        return TEXTURES.get(entity.getSatelliteType());
    }
}
