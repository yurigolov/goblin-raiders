package com.goblinraiders.client;

import com.goblinraiders.entity.GoblinVillager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Settlers: the man's skin on the wide model, the woman's (long hair, a dress) on the slim one and a little smaller. */
public class GoblinVillagerRenderer extends EntityRenderer<GoblinVillager> {
    private final GoblinRenderer<GoblinVillager> man;
    private final GoblinRenderer<GoblinVillager> woman;

    public GoblinVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.man = new GoblinRenderer<>(context, "goblin_villager", false, 1.0F);
        this.woman = new GoblinRenderer<>(context, "goblin_villager_female", true, 0.95F);
        this.shadowRadius = 0.5F;
    }

    private GoblinRenderer<GoblinVillager> look(GoblinVillager villager) {
        return villager.isFemale() ? this.woman : this.man;
    }

    @Override
    public void render(GoblinVillager villager, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        this.look(villager).render(villager, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GoblinVillager villager) {
        return this.look(villager).getTextureLocation(villager);
    }
}
