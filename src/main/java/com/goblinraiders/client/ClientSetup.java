package com.goblinraiders.client;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.registry.ModEntities;
import com.goblinraiders.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

@EventBusSubscriber(modid = GoblinRaiders.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // The small goblin, the elder and the crazy goblin have slim (Alex) arms, the others classic ones. 1.0 = player size (1.8 blocks).
        event.registerEntityRenderer(ModEntities.SMALL_GOBLIN.get(), ctx -> new GoblinRenderer<>(ctx, "small_goblin", true, 0.833F));
        event.registerEntityRenderer(ModEntities.RUTHLESS_GOBLIN.get(), ctx -> new GoblinRenderer<>(ctx, "ruthless_goblin", false, 1.0F));
        event.registerEntityRenderer(ModEntities.NEUTRAL_GOBLIN.get(), ctx -> new GoblinRenderer<>(ctx, "neutral_goblin", false, 1.0F));
        event.registerEntityRenderer(ModEntities.GIANT_GOBLIN.get(), ctx -> new GoblinRenderer<>(ctx, "giant_goblin", false, 2.0F));
        event.registerEntityRenderer(ModEntities.GOBLIN_TRADER.get(), ctx -> new GoblinRenderer<>(ctx, "goblin_trader", false, 1.0F));
        event.registerEntityRenderer(ModEntities.GOBLIN_ELDER.get(), ctx -> new GoblinRenderer<>(ctx, "goblin_elder", true, 0.833F));
        event.registerEntityRenderer(ModEntities.CRAZY_GOBLIN.get(), ctx -> new GoblinRenderer<>(ctx, "crazy_goblin", true, 0.833F));
        event.registerEntityRenderer(ModEntities.GOBLIN_SHAMAN.get(), ctx -> new GoblinRenderer<>(ctx, "goblin_shaman", false, 1.0F));
        event.registerEntityRenderer(ModEntities.GOBLIN_CHILD.get(), GoblinChildRenderer::new);
        event.registerEntityRenderer(ModEntities.GOBLIN_VILLAGER.get(), GoblinVillagerRenderer::new);
        event.registerEntityRenderer(ModEntities.GOBLIN_HEADMAN.get(), ctx -> new GoblinRenderer<>(ctx, "goblin_headman", false, 1.0F));
        event.registerEntityRenderer(ModEntities.GOBLIN_SAGE.get(), ctx -> new GoblinRenderer<>(ctx, "goblin_sage", false, 0.95F));
        event.registerEntityRenderer(ModEntities.GOBLIN_KING.get(), ctx -> new GoblinRenderer<>(ctx, "goblin_king", false, 3.0F / 1.8F));
    }

    /** The goblin bow bends like the vanilla bow: same "pull" and "pulling" model predicates. */
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resources -> GoblinFaceTextures.clear());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.GOBLIN_BOW.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                    entity == null || entity.getUseItem() != stack ? 0.0F
                            : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
            ItemProperties.register(ModItems.GOBLIN_BOW.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
    }

    private ClientSetup() {
    }
}
