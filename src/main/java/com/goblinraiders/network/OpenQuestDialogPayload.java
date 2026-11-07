package com.goblinraiders.network;

import com.goblinraiders.GoblinRaiders;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: open (or refresh) the headman's window - the player's current errand ("" for none), how far along
 * it is and how many errands the player has done so far.
 */
public record OpenQuestDialogPayload(int entityId, String quest, int progress, int completed) implements CustomPacketPayload {
    public static final Type<OpenQuestDialogPayload> TYPE = new Type<>(GoblinRaiders.id("open_quest_dialog"));
    public static final StreamCodec<ByteBuf, OpenQuestDialogPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenQuestDialogPayload::entityId,
            ByteBufCodecs.STRING_UTF8, OpenQuestDialogPayload::quest,
            ByteBufCodecs.VAR_INT, OpenQuestDialogPayload::progress,
            ByteBufCodecs.VAR_INT, OpenQuestDialogPayload::completed,
            OpenQuestDialogPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
