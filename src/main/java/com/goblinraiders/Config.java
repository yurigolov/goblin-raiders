package com.goblinraiders;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/goblinraiders-common.toml */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Goblin raids on player bases").push("raids");
    }

    public static final ModConfigSpec.BooleanValue RAIDS_ENABLED = BUILDER
            .comment("Whether goblins raid player bases at all")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue CHECK_INTERVAL_TICKS = BUILDER
            .comment("How often (in ticks) every online player is checked for a possible raid")
            .defineInRange("checkIntervalTicks", 600, 20, 72000);

    public static final ModConfigSpec.DoubleValue RAID_CHANCE = BUILDER
            .comment("Chance of a raid on each check, once the player is far from the base and the cooldown has passed")
            .defineInRange("raidChance", 0.05, 0.0, 1.0);

    public static final ModConfigSpec.IntValue COOLDOWN_TICKS = BUILDER
            .comment("Minimum time between two raids on the same player (and before the first one), in ticks. 24000 = one Minecraft day")
            .defineInRange("cooldownTicks", 72000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MIN_DISTANCE = BUILDER
            .comment("The player must be at least this many blocks away from the base (or in another dimension)")
            .defineInRange("minDistanceFromBase", 96, 0, 100000);

    public static final ModConfigSpec.IntValue BASE_RADIUS = BUILDER
            .comment("Chests within this radius of each other count as one base")
            .defineInRange("baseRadius", 32, 4, 256);

    public static final ModConfigSpec.IntValue MAX_CHESTS = BUILDER
            .comment("How many chests (or barrels) goblins rob in one raid")
            .defineInRange("maxChestsPerRaid", 3, 1, 64);

    public static final ModConfigSpec.DoubleValue STEAL_FRACTION = BUILDER
            .comment("Share of the item stacks taken from each robbed chest (1.0 = everything)")
            .defineInRange("stealFraction", 1.0, 0.05, 1.0);

    public static final ModConfigSpec.BooleanValue CARAVANS = BUILDER
            .comment("Raided loot leaves with a goblin caravan that can be caught on the way to the village;",
                    "false puts it straight into the village stash")
            .define("caravans", true);

    public static final ModConfigSpec.IntValue VILLAGE_SEARCH_RADIUS = BUILDER
            .comment("How far (in chunks) to look for the goblin village that takes the loot")
            .defineInRange("villageSearchRadiusChunks", 100, 8, 300);

    static {
        BUILDER.pop();
        BUILDER.comment("Sounds").push("sounds");
    }

    public static final ModConfigSpec.BooleanValue AMBIENCE = BUILDER
            .comment("The sound of goblin settlements: war drums, chatter, hammers, pipes, birds, crickets at night")
            .define("ambience", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
