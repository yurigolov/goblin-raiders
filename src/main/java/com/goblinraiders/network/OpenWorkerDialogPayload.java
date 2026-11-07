package com.goblinraiders.network;

import com.goblinraiders.GoblinRaiders;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: open (or refresh) the window of the player's worker child - its job ({@code WorkJob}),
 * whether it works somewhere or follows the player, what keeps it from working ({@code GoblinChild.Problem}) and how
 * many items it carries.
 */
public record OpenWorkerDialogPayload(int entityId, int job, boolean working, int problem, int carried) implements CustomPacketPayload {
    public static final Type<OpenWorkerDialogPayload> TYPE = new Type<>(GoblinRaiders.id("open_worker_dialog"));
    public static final StreamCodec<ByteBuf, OpenWorkerDialogPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenWorkerDialogPayload::entityId,
            ByteBufCodecs.VAR_INT, OpenWorkerDialogPayload::job,
            ByteBufCodecs.BOOL, OpenWorkerDialogPayload::working,
            ByteBufCodecs.VAR_INT, OpenWorkerDialogPayload::problem,
            ByteBufCodecs.VAR_INT, OpenWorkerDialogPayload::carried,
            OpenWorkerDialogPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
