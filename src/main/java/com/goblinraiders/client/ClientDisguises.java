package com.goblinraiders.client;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.network.DisguisePayload;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Draws disguised players as goblins: a player renderer with a goblin skin takes over for them, including the
 * first-person arm. Which players are disguised comes from {@link DisguisePayload}.
 */
@EventBusSubscriber(modid = GoblinRaiders.MODID, value = Dist.CLIENT)
public final class ClientDisguises {
    private static final ResourceLocation SKIN = GoblinRaiders.id("textures/entity/goblin/neutral_goblin.png");
    private static final IntSet DISGUISED = new IntOpenHashSet();
    @Nullable
    private static PlayerRenderer goblinLook;
    private static boolean rendering;

    public static void set(DisguisePayload payload) {
        if (payload.disguised()) {
            DISGUISED.add(payload.entityId());
        } else {
            DISGUISED.remove(payload.entityId());
        }
    }

    public static boolean isDisguised(int entityId) {
        return DISGUISED.contains(entityId);
    }

    /** A player renderer that uses the goblin skin. */
    private static final class GoblinLookRenderer extends PlayerRenderer {
        GoblinLookRenderer(EntityRendererProvider.Context context) {
            super(context, false);
        }

        @Override
        public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
            return SKIN;
        }
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        goblinLook = new GoblinLookRenderer(event.getContext());
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (rendering || goblinLook == null || !(event.getEntity() instanceof AbstractClientPlayer player) || !isDisguised(player.getId())) {
            return;
        }
        event.setCanceled(true);
        rendering = true;
        try {
            float yaw = Mth.lerp(event.getPartialTick(), player.yRotO, player.getYRot());
            goblinLook.render(player, yaw, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        } finally {
            rendering = false;
        }
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (goblinLook == null || !isDisguised(player.getId())) {
            return;
        }
        event.setCanceled(true);
        // Drawn here rather than through PlayerRenderer#renderRightHand: that one fires RenderArmEvent again (endless
        // recursion) and always uses the player's own skin.
        PlayerModel<AbstractClientPlayer> model = goblinLook.getModel();
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        arm.visible = true;
        arm.xRot = 0.0F;
        arm.render(pose, buffers.getBuffer(RenderType.entitySolid(SKIN)), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        sleeve.visible = true;
        sleeve.xRot = 0.0F;
        sleeve.render(pose, buffers.getBuffer(RenderType.entityTranslucent(SKIN)), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DISGUISED.clear();
    }

    /** For the dev tour: whether the local player is drawn as a goblin. */
    public static boolean isLocalPlayerDisguised() {
        return Minecraft.getInstance().player != null && isDisguised(Minecraft.getInstance().player.getId());
    }

    private ClientDisguises() {
    }
}
