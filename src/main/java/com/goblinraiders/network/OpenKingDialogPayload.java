package com.goblinraiders.network;

import com.goblinraiders.GoblinRaiders;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: open the goblin king's dialog. For vassals it carries the ticks until the next tribute
 * (negative when overdue) and how many coins the player has.
 */
public record OpenKingDialogPayload(int entityId, boolean vassal, long ticksToTribute, int coins) implements CustomPacketPayload {
    public static final Type<OpenKingDialogPayload> TYPE = new Type<>(GoblinRaiders.id("open_king_dialog"));
    public static final StreamCodec<ByteBuf, OpenKingDialogPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenKingDialogPayload::entityId,
            ByteBufCodecs.BOOL, OpenKingDialogPayload::vassal,
            ByteBufCodecs.VAR_LONG, OpenKingDialogPayload::ticksToTribute,
            ByteBufCodecs.VAR_INT, OpenKingDialogPayload::coins,
            OpenKingDialogPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
