package com.goblinraiders.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.client.ClientDialogs;
import com.goblinraiders.client.ClientDisguises;
import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.GoblinKing;
import com.goblinraiders.entity.GoblinTrader;
import com.goblinraiders.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = GoblinRaiders.MODID)
public final class ModNetwork {
    /** How close (squared distance) a player must be to answer a goblin. */
    private static final double TALK_RANGE_SQR = 10.0 * 10.0;

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // Client handlers are only ever invoked on the client, so the client classes are never loaded on a server.
        registrar.playToClient(OpenDialogPayload.TYPE, OpenDialogPayload.STREAM_CODEC, (payload, context) -> ClientDialogs.open(payload));
        registrar.playToClient(OpenKingDialogPayload.TYPE, OpenKingDialogPayload.STREAM_CODEC, (payload, context) -> ClientDialogs.openKing(payload));
        registrar.playToClient(OpenWorkerDialogPayload.TYPE, OpenWorkerDialogPayload.STREAM_CODEC, (payload, context) -> ClientDialogs.openWorker(payload));
        registrar.playToClient(OpenQuestDialogPayload.TYPE, OpenQuestDialogPayload.STREAM_CODEC, (payload, context) -> ClientDialogs.openQuest(payload));
        registrar.playToClient(DisguisePayload.TYPE, DisguisePayload.STREAM_CODEC, (payload, context) -> ClientDisguises.set(payload));
        registrar.playToServer(DialogChoicePayload.TYPE, DialogChoicePayload.STREAM_CODEC, ModNetwork::handleChoice);
    }

    private static void handleChoice(DialogChoicePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.level().getEntity(payload.entityId()) instanceof AbstractGoblin goblin)
                || !goblin.isAlive() || player.distanceToSqr(goblin) > TALK_RANGE_SQR) {
            return;
        }
        goblin.onDialogChoice(player, payload.choice());
    }

    // ---------------------------------------------------------------- who looks like a goblin

    /** Tells everyone in the player's level (who has this mod) how to draw them. */
    private static void sendDisguise(Player player, boolean disguised) {
        if (player.level() instanceof ServerLevel level) {
            DisguisePayload payload = new DisguisePayload(player.getId(), disguised);
            for (ServerPlayer watcher : level.players()) {
                send(watcher, payload);
            }
        }
    }

    /** Skips connections without the channel (vanilla-style connections, such as the game test mock players). */
    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof Player player && event.getEffectInstance().is(ModEffects.DISGUISE)) {
            sendDisguise(player, true);
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof Player player && event.getEffect().is(ModEffects.DISGUISE.getKey())) {
            sendDisguise(player, false);
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEntity() instanceof Player player && event.getEffectInstance() != null && event.getEffectInstance().is(ModEffects.DISGUISE)) {
            sendDisguise(player, false);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player watched && watched.hasEffect(ModEffects.DISGUISE)
                && event.getEntity() instanceof ServerPlayer watcher) {
            send(watcher, new DisguisePayload(watched.getId(), true));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().hasEffect(ModEffects.DISGUISE)) {
            sendDisguise(event.getEntity(), true);
        }
    }

    private ModNetwork() {
    }
}
