package com.goblinraiders.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;

/**
 * Small talk with goblins (the talk window). Lines live under {@code goblinraiders.dialog.<voice>.<topic>.<n>}; a voice
 * without lines of its own for a topic answers with the neutral goblin's. Topic titles and the buttons are under
 * {@code goblinraiders.dialog.neutral.*}.
 */
public final class GoblinTalk {
    public static final String GREETING = "greeting";
    public static final String THANKS = "thanks";
    public static final String[] TOPICS = {"life", "news", "tale", "advice"};
    private static final String FALLBACK = "neutral";
    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();
    private static final java.util.Set<String> SETTLER_TRADES = java.util.Set.of("farmer", "lumberjack", "miner", "builder", "cook");

    static {
        lines(FALLBACK, GREETING, 4, "life", 5, "news", 6, "tale", 5, "advice", 6, THANKS, 3);
        lines("small", GREETING, 3);
        lines("ruthless", GREETING, 3);
        lines("giant", GREETING, 3);
        lines("shaman", GREETING, 3, "advice", 3);
        lines("crazy", GREETING, 3, "life", 3, "news", 3, "tale", 3, "advice", 3);
        lines("child", GREETING, 4, "life", 3, "news", 3, "tale", 3, "advice", 3, THANKS, 2);
        lines("villager", GREETING, 4, "life", 4, "news", 5, "tale", 4, "advice", 5, THANKS, 3);
        lines("farmer", GREETING, 2);
        lines("lumberjack", GREETING, 2);
        lines("miner", GREETING, 2);
        lines("builder", GREETING, 2);
        lines("cook", GREETING, 2);
        lines("sage", GREETING, 3, "wisdom", 8);
        lines("headman", GREETING, 3);
    }

    private static void lines(String voice, Object... topicsAndCounts) {
        for (int i = 0; i < topicsAndCounts.length; i += 2) {
            COUNTS.put(voice + "." + topicsAndCounts[i], (Integer) topicsAndCounts[i + 1]);
        }
    }

    /** A random line of {@code voice} on {@code topic}. */
    public static String line(String voice, String topic, RandomSource random) {
        Integer count = COUNTS.get(voice + "." + topic);
        if (count == null && SETTLER_TRADES.contains(voice)) {
            voice = "villager";
            count = COUNTS.get(voice + "." + topic);
        }
        if (count == null) {
            voice = FALLBACK;
            count = COUNTS.get(FALLBACK + "." + topic);
        }
        return "goblinraiders.dialog." + voice + "." + topic + "." + (1 + random.nextInt(count));
    }

    /** Every translation key this class can produce. */
    public static List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        COUNTS.forEach((voiceTopic, count) -> {
            for (int n = 1; n <= count; n++) {
                keys.add("goblinraiders.dialog." + voiceTopic + "." + n);
            }
        });
        return keys;
    }

    private GoblinTalk() {
    }
}
