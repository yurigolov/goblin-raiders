package com.goblinraiders.network;

import com.goblinraiders.GoblinRaiders;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: a player put on or lost the goblin disguise. Effects of other players are not synced by the game,
 * so this tells every client watching which players to draw as goblins.
 */
public record DisguisePayload(int entityId, boolean disguised) implements CustomPacketPayload {
    public static final Type<DisguisePayload> TYPE = new Type<>(GoblinRaiders.id("disguise"));
    public static final StreamCodec<ByteBuf, DisguisePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DisguisePayload::entityId,
            ByteBufCodecs.BOOL, DisguisePayload::disguised,
            DisguisePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
