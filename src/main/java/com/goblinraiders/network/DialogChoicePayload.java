package com.goblinraiders.network;

import com.goblinraiders.GoblinRaiders;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: what the player answered in a goblin dialog. */
public record DialogChoicePayload(int entityId, int choice) implements CustomPacketPayload {
    /** Trader: "let's trade". */
    public static final int ACCEPT = 0;
    /** Trader: "no way". */
    public static final int REFUSE = 1;
    /** The window was closed without an answer. */
    public static final int CLOSE = 2;
    /** King: join the horde. */
    public static final int KING_SUBMIT = 10;
    /** King: "then die". */
    public static final int KING_FIGHT = 11;
    /** King (vassals): pay the tribute now. */
    public static final int KING_PAY = 12;
    /** King (vassals): hire a goblin. */
    public static final int KING_HIRE_SMALL = 13;
    public static final int KING_HIRE_RUTHLESS = 14;
    public static final int KING_HIRE_GIANT = 15;
    /** King (vassals): leave the horde and turn on the king. */
    public static final int KING_BETRAY = 16;
    /** King (vassals): buy a goblin child to work on the player's farm or in their mine. */
    public static final int KING_BUY_FARMER = 17;
    public static final int KING_BUY_MINER = 18;
    /** Any goblin in the talk window: give it a bowl of goblin grub. */
    public static final int NEUTRAL_TREAT = 20;
    /** The headman: give me an errand / here is what you asked for / I give it up. */
    public static final int QUEST_TAKE = 50;
    public static final int QUEST_TURN_IN = 51;
    public static final int QUEST_DROP = 52;
    /** The sage: foretell where the settlements are / bless me. */
    public static final int SAGE_PROPHECY = 40;
    public static final int SAGE_BLESSING = 41;
    /** A worker child: work right here / follow me / change the job / hand over what you carry. */
    public static final int WORKER_STAY = 30;
    public static final int WORKER_FOLLOW = 31;
    public static final int WORKER_FARM = 32;
    public static final int WORKER_MINE = 33;
    public static final int WORKER_HAND_OVER = 34;

    public static final Type<DialogChoicePayload> TYPE = new Type<>(GoblinRaiders.id("dialog_choice"));
    public static final StreamCodec<ByteBuf, DialogChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DialogChoicePayload::entityId,
            ByteBufCodecs.VAR_INT, DialogChoicePayload::choice,
            DialogChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
