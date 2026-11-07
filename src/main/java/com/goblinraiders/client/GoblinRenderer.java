package com.goblinraiders.client;

import com.goblinraiders.entity.AbstractGoblin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** Renders a goblin with its player skin, scaled to the size of its kind. */
public class GoblinRenderer<T extends AbstractGoblin> extends HumanoidMobRenderer<T, GoblinModel<T>> {
    private final String skin;
    private final float scale;
    /** The frame's partial tick, for the face (getTextureLocation does not get one). */
    private float partialTick;

    public GoblinRenderer(EntityRendererProvider.Context context, String skin, boolean slim, float scale) {
        super(context, new GoblinModel<>(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5F * scale);
        this.skin = skin;
        this.scale = scale;
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        this.partialTick = partialTick;
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        SpeechBubbles.render(entity, poseStack, buffers, packedLight, this.entityRenderDispatcher, this.getFont());
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(this.scale, this.scale, this.scale);
    }

    /** The skin with the goblin's current expression baked in (see {@link GoblinFace}). */
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        GoblinFace face = GoblinFace.of(entity, this.partialTick);
        return GoblinFaceTextures.get(this.skin, face.eyes(), face.mouth());
    }
}
