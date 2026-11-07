package com.goblinraiders.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Contents of a goblin note: the village (its center) where the stolen loot went. */
public record RaidNote(int x, int z) {
    public static final Codec<RaidNote> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(RaidNote::x),
            Codec.INT.fieldOf("z").forGetter(RaidNote::z)
    ).apply(instance, RaidNote::new));

    public static final StreamCodec<ByteBuf, RaidNote> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, RaidNote::x,
            ByteBufCodecs.INT, RaidNote::z,
            RaidNote::new);
}
