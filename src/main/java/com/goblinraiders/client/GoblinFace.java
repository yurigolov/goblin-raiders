package com.goblinraiders.client;

import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.CrazyGoblin;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.locale.Language;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;

/**
 * What a goblin's face shows this frame: an eyes overlay and a mouth overlay (null = the skin's own face). The mood
 * comes from what the goblin is saying (a battle cry is angry, thanks are happy, a call for help is worried) or, when
 * it says nothing, from whether it is fighting. The mouth moves while a line is being spoken, the eyes blink every few
 * seconds, a goblin in pain screws its eyes shut, a sleeper keeps them closed. Overlay names match the textures
 * {@code face/<skin>_<state>.png} drawn by tools/textures/skins.py.
 */
public record GoblinFace(@Nullable String eyes, @Nullable String mouth) {
    private enum Mood { CALM, ANGRY, HAPPY, WORRIED }

    private static final Map<String, Mood> TOPIC_MOODS = Map.ofEntries(
            Map.entry("battle", Mood.ANGRY), Map.entry("taunt", Mood.ANGRY), Map.entry("yell", Mood.ANGRY),
            Map.entry("go_away", Mood.ANGRY), Map.entry("alarm", Mood.ANGRY), Map.entry("challenge", Mood.ANGRY),
            Map.entry("traitor", Mood.ANGRY), Map.entry("spell", Mood.ANGRY),
            Map.entry("thanks", Mood.HAPPY), Map.entry("calm", Mood.HAPPY), Map.entry("play", Mood.HAPPY),
            Map.entry("diamond", Mood.HAPPY), Map.entry("built", Mood.HAPPY), Map.entry("morning", Mood.HAPPY),
            Map.entry("give", Mood.HAPPY), Map.entry("submit", Mood.HAPPY), Map.entry("tribute", Mood.HAPPY),
            Map.entry("help", Mood.WORRIED), Map.entry("woken", Mood.WORRIED), Map.entry("sigh", Mood.WORRIED),
            Map.entry("no_chest", Mood.WORRIED), Map.entry("no_field", Mood.WORRIED), Map.entry("not_mine", Mood.WORRIED),
            Map.entry("no_trees", Mood.WORRIED), Map.entry("no_kitchen", Mood.WORRIED), Map.entry("no_food", Mood.WORRIED),
            Map.entry("no_wood", Mood.WORRIED), Map.entry("no_stone", Mood.WORRIED));
    /** Ticks of moving mouth per character of the line, and the bounds. */
    private static final float TICKS_PER_CHAR = 1.1F;
    private static final int MIN_TALK = 12;
    private static final int BLINK_TICKS = 3;

    public static GoblinFace of(AbstractGoblin goblin, float partialTick) {
        float age = goblin.tickCount + partialTick;
        boolean jawOpen = talking(goblin) && syllableOpen(goblin, age) || goblin.getAttackAnim(partialTick) > 0.0F;
        if (goblin.hurtTime > 0) {
            return new GoblinFace("hurt", "open");
        }
        if (goblin.hasPose(Pose.SLEEPING)) {
            return new GoblinFace("closed", jawOpen ? "open" : null);   // talking in its sleep
        }
        Mood mood = mood(goblin);
        String eyes = switch (mood) {
            case ANGRY -> "angry";
            case HAPPY -> "happy";
            case WORRIED -> "worried";
            case CALM -> null;
        };
        if (mood != Mood.HAPPY && blinking(goblin, age)) {
            eyes = "closed";
        }
        String mouth = jawOpen ? "open" : switch (mood) {
            case ANGRY -> "snarl";
            case HAPPY -> "smile";
            default -> null;
        };
        return new GoblinFace(eyes, mouth);
    }

    private static Mood mood(AbstractGoblin goblin) {
        if (goblin.isSpeaking()) {
            // goblinraiders.speech.<voice>.<topic>.<n>
            String[] parts = goblin.getSpeech().split("\\.");
            if (parts.length >= 4) {
                Mood mood = TOPIC_MOODS.get(parts[3]);
                if (mood != null) {
                    return mood;
                }
                if (goblin instanceof CrazyGoblin && "idle".equals(parts[3])) {
                    return Mood.HAPPY;   // he finds his own nonsense hilarious
                }
            }
        }
        return goblin.isAggressive() ? Mood.ANGRY : Mood.CALM;
    }

    /** Whether the line on the goblin's bubble is still being spoken: longer lines take longer. */
    private static boolean talking(AbstractGoblin goblin) {
        if (!goblin.isSpeaking()) {
            return false;
        }
        String text = Language.getInstance().getOrDefault(goblin.getSpeech());
        int ticks = Mth.clamp((int) (text.length() * TICKS_PER_CHAR), MIN_TALK, AbstractGoblin.SPEECH_TICKS);
        return goblin.speechAge() < ticks;
    }

    /** The mouth opens on about three syllables of five, a syllable every three ticks. */
    private static boolean syllableOpen(AbstractGoblin goblin, float age) {
        int syllable = (int) (age / 3.0F);
        return Math.floorMod(syllable * 7 + goblin.getId() * 3, 5) < 3;
    }

    /** Every goblin blinks at its own pace, once every 2.5 to 6 seconds. */
    private static boolean blinking(AbstractGoblin goblin, float age) {
        int period = 50 + Math.floorMod(goblin.getId() * 37, 70);
        return Math.floorMod((int) age + goblin.getId() * 17, period) < BLINK_TICKS;
    }
}
