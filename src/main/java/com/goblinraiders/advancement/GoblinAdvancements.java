package com.goblinraiders.advancement;

import com.goblinraiders.GoblinRaiders;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * The "Goblin Raiders" advancement tab (data/goblinraiders/advancement, written by tools/gen_data.py).
 * The ones listed here have a single "impossible" criterion and are granted from code; the others use vanilla triggers.
 */
public final class GoblinAdvancements {
    public static final String ROBBED = "robbed";
    public static final String GOT_IT_BACK = "got_it_back";
    public static final String INTERCEPT = "intercept";
    public static final String WHOLE_CARAVAN = "whole_caravan";
    public static final String ELDER = "elder";
    public static final String NEUTRAL_TALK = "neutral_talk";
    public static final String TREAT = "treat";
    public static final String TRADE = "trade";
    public static final String REFUSE = "refuse";
    public static final String NIGHT_THIEF = "night_thief";
    public static final String WAR_DRUMS = "war_drums";
    public static final String SPOTTED = "spotted";
    public static final String STUN = "stun";
    public static final String VASSAL = "vassal";
    public static final String HIRE = "hire";
    public static final String BETRAY = "betray";
    public static final String UNDERCOVER = "undercover";
    public static final String CHILD_TALK = "child_talk";
    public static final String ARTEL = "artel";
    public static final String HARVEST = "harvest";
    public static final String HAUL = "haul";
    public static final String DIAMOND = "diamond";
    public static final String SETTLER_TALK = "settler_talk";
    public static final String SETTLER_TRADE = "settler_trade";
    public static final String NEW_HOUSE = "new_house";
    public static final String SAGE = "sage";
    public static final String PROPHECY = "prophecy";
    public static final String BLESSING = "blessing";
    public static final String QUEST = "quest";
    public static final String RIGHT_HAND = "right_hand";

    /** Granted from code. */
    public static final List<String> CODE = List.of(ROBBED, GOT_IT_BACK, INTERCEPT, WHOLE_CARAVAN, ELDER, NEUTRAL_TALK, TREAT, TRADE, REFUSE,
            NIGHT_THIEF, SPOTTED, STUN, VASSAL, HIRE, BETRAY, UNDERCOVER, CHILD_TALK, ARTEL, HARVEST, HAUL, DIAMOND, SETTLER_TALK,
            SETTLER_TRADE, NEW_HOUSE, SAGE, PROPHECY, BLESSING, QUEST, RIGHT_HAND);
    /** Every advancement of the tab, root first. */
    public static final List<String> ALL = List.of("root", "find_village", "megavillage", ELDER, NEUTRAL_TALK, TREAT, TRADE, REFUSE, "grub",
            NIGHT_THIEF, SPOTTED, ROBBED, GOT_IT_BACK, INTERCEPT, WHOLE_CARAVAN, "kill_goblin", "kill_giant", STUN, "cursed", "disguise",
            VASSAL, HIRE, BETRAY, "regicide", UNDERCOVER, CHILD_TALK, ARTEL, HARVEST, HAUL, DIAMOND, "hamlet", SETTLER_TALK,
            SETTLER_TRADE, NEW_HOUSE, SAGE, PROPHECY, BLESSING, QUEST, RIGHT_HAND);

    public static void award(Player player, String name) {
        if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
            AdvancementHolder advancement = serverPlayer.server.getAdvancements().get(GoblinRaiders.id(name));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "done");
            }
        }
    }

    public static boolean has(ServerPlayer player, String name) {
        AdvancementHolder advancement = player.server.getAdvancements().get(GoblinRaiders.id(name));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private GoblinAdvancements() {
    }
}
