package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class ModTags {
    /** Structures that count as goblin villages: raids send loot to the nearest one. */
    public static final TagKey<Structure> GOBLIN_VILLAGES = TagKey.create(Registries.STRUCTURE, GoblinRaiders.id("goblin_villages"));

    /** Peaceful goblin villages (the sage can point the way to the nearest one). */
    public static final TagKey<Structure> GOBLIN_HAMLETS = TagKey.create(Registries.STRUCTURE, GoblinRaiders.id("goblin_hamlets"));

    /** The goblin king's cities. */
    public static final TagKey<Structure> GOBLIN_CAPITALS = TagKey.create(Registries.STRUCTURE, GoblinRaiders.id("goblin_capitals"));

    /** Items goblins never steal. */
    public static final TagKey<Item> RAID_PROOF = TagKey.create(Registries.ITEM, GoblinRaiders.id("raid_proof"));

    private ModTags() {
    }
}
