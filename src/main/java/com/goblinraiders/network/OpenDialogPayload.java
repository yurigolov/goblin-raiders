package com.goblinraiders.network;

import com.goblinraiders.GoblinRaiders;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.trading.MerchantOffers;

/** Server to client: open the dialog window of a goblin (trader or elder). Offers are empty for the elder. */
public record OpenDialogPayload(int entityId, MerchantOffers offers) implements CustomPacketPayload {
    public static final Type<OpenDialogPayload> TYPE = new Type<>(GoblinRaiders.id("open_dialog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialogPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenDialogPayload::entityId,
            MerchantOffers.STREAM_CODEC, OpenDialogPayload::offers,
            OpenDialogPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
