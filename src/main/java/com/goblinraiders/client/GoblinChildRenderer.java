package com.goblinraiders.client;

import com.goblinraiders.entity.GoblinChild;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Goblin children in two looks: the small goblin's skin (slim arms) or the giant's (wide). The model is drawn as a
 * baby - big head, half-size body - so a child stands about one block tall.
 */
public class GoblinChildRenderer extends EntityRenderer<GoblinChild> {
    private static final float SCALE = 0.9F;
    private final GoblinRenderer<GoblinChild> small;
    private final GoblinRenderer<GoblinChild> giant;

    public GoblinChildRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.small = new GoblinRenderer<>(context, "small_goblin", true, SCALE);
        this.giant = new GoblinRenderer<>(context, "giant_goblin", false, SCALE);
        this.shadowRadius = 0.25F;
    }

    private GoblinRenderer<GoblinChild> look(GoblinChild child) {
        return child.getVariant() == GoblinChild.VARIANT_GIANT ? this.giant : this.small;
    }

    @Override
    public void render(GoblinChild child, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        this.look(child).render(child, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GoblinChild child) {
        return this.look(child).getTextureLocation(child);
    }
}
