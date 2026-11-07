package com.goblinraiders.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.RandomSource;

/**
 * What goblins say. Lines live in the language files under
 * {@code goblinraiders.speech.<voice>.<topic>.<n>} (n from 1) and conversations under
 * {@code goblinraiders.chat.<conversation>.<line>}. The counts here must match the language files;
 * {@code GoblinGameTests.everySpeechLineIsTranslated} checks that.
 */
public final class Speech {
    public static final String IDLE = "idle";
    /** Shouted when a goblin starts attacking a player. */
    public static final String BATTLE = "battle";
    /** Shouted now and then during a fight. */
    public static final String TAUNT = "taunt";
    /** Trader only: yelled after the player refuses to trade. */
    public static final String YELL = "yell";
    /** Trader only: when an angry trader is clicked. */
    public static final String GO_AWAY = "go_away";
    /** Trader only: after a trade. */
    public static final String THANKS = "thanks";
    /** Trader only: when the anger wears off. */
    public static final String CALM = "calm";
    /** Elder only: when someone hurts him. */
    public static final String HELP = "help";
    /** Talking in their sleep (bubble only). */
    public static final String SLEEP = "sleep";
    /** Startled awake. */
    public static final String WOKEN = "woken";
    /** Waking up in the morning. */
    public static final String MORNING = "morning";
    /** A watchman (or a woken sleeper) spotted an intruder. */
    public static final String ALARM = "alarm";
    /** A watchman muttering by the fire. */
    public static final String WATCH = "watch";
    /** Lines every goblin shares unless its voice has its own: {@code goblinraiders.speech.common.<topic>.<n>}. */
    public static final String COMMON = "common";

    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();
    /** Voices of the settlers' trades: what they lack comes from {@code villager}. */
    private static final java.util.Set<String> SETTLER_TRADES = java.util.Set.of("farmer", "lumberjack", "miner", "builder", "cook");

    static {
        lines("small", IDLE, 6, BATTLE, 5, TAUNT, 5);
        lines("ruthless", IDLE, 5, BATTLE, 5, TAUNT, 5);
        lines("neutral", IDLE, 6, BATTLE, 4, TAUNT, 3);
        lines("giant", IDLE, 5, BATTLE, 4, TAUNT, 4);
        lines("crazy", IDLE, 7, BATTLE, 5, TAUNT, 6);
        lines("trader", IDLE, 5, BATTLE, 3, TAUNT, 4, YELL, 4, GO_AWAY, 4, THANKS, 4, CALM, 3);
        lines("elder", IDLE, 6, HELP, 3);
        lines("shaman", IDLE, 6, BATTLE, 4, TAUNT, 4, "spell", 6);
        lines("king", IDLE, 4, BATTLE, 4, TAUNT, 4, "challenge", 3, "submit", 3, "tribute", 2, CALM, 2, "traitor", 3);
        lines("child", IDLE, 6, HELP, 3, "play", 4, "work", 4, "no_chest", 2, "not_mine", 2, "no_field", 2, "diamond", 2, WOKEN, 2);
        lines("trader", WOKEN, 3);
        lines("villager", IDLE, 6, HELP, 3, "thanks", 3, "work", 3, "no_chest", 2, "no_field", 2, "not_mine", 2, "no_trees", 2,
                "no_kitchen", 2, "no_food", 2, "no_wood", 2, "no_stone", 2, "built", 2);
        lines("farmer", IDLE, 3, "work", 3);
        lines("lumberjack", IDLE, 3, "work", 3);
        lines("miner", IDLE, 3, "work", 3);
        lines("builder", IDLE, 3, "work", 3);
        lines("cook", IDLE, 3, "work", 3);
        lines("sage", IDLE, 6, HELP, 3);
        lines("headman", IDLE, 4, HELP, 2, "give", 3, "thanks", 3, "sigh", 2);
        lines("elder", WOKEN, 2);
        lines(COMMON, SLEEP, 6, WOKEN, 5, MORNING, 3, ALARM, 5, WATCH, 6);
    }

    /** Number of lines in each conversation between two goblins (speakers alternate, first one starts). */
    public static final int[] CONVERSATIONS = {4, 3, 4, 3, 4, 4, 3, 4, 3, 4, 3, 4};

    private static void lines(String voice, Object... topicsAndCounts) {
        for (int i = 0; i < topicsAndCounts.length; i += 2) {
            COUNTS.put(voice + "." + topicsAndCounts[i], (Integer) topicsAndCounts[i + 1]);
        }
    }

    public static Optional<String> random(String voice, String topic, RandomSource random) {
        Integer count = COUNTS.get(voice + "." + topic);
        if (count == null && SETTLER_TRADES.contains(voice)) {
            voice = "villager";
            count = COUNTS.get(voice + "." + topic);
        }
        if (count == null) {
            voice = COMMON;
            count = COUNTS.get(COMMON + "." + topic);
        }
        if (count == null) {
            return Optional.empty();
        }
        return Optional.of("goblinraiders.speech." + voice + "." + topic + "." + (1 + random.nextInt(count)));
    }

    public static String conversationLine(int conversation, int line) {
        return "goblinraiders.chat." + (conversation + 1) + "." + (line + 1);
    }

    /** Every translation key this class can produce. */
    public static List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        COUNTS.forEach((voiceTopic, count) -> {
            for (int n = 1; n <= count; n++) {
                keys.add("goblinraiders.speech." + voiceTopic + "." + n);
            }
        });
        for (int c = 0; c < CONVERSATIONS.length; c++) {
            for (int l = 0; l < CONVERSATIONS[c]; l++) {
                keys.add(conversationLine(c, l));
            }
        }
        return keys;
    }

    private Speech() {
    }
}
