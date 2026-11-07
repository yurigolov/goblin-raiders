package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.item.ConcealmentAmuletItem;
import com.goblinraiders.item.GoblinBowItem;
import com.goblinraiders.item.GoblinClubItem;
import com.goblinraiders.item.GoblinNoteItem;
import com.goblinraiders.item.KingScepterItem;
import com.goblinraiders.item.TeleportAmuletItem;
import com.goblinraiders.item.VassalCharterItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(GoblinRaiders.MODID);

    /** Same numbers as the diamond tier. */
    public static final Tier GOBLIN_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10,
            () -> Ingredient.of(Items.DIAMOND));

    public static final DeferredItem<SwordItem> GOBLIN_SWORD = REGISTER.register("goblin_sword",
            () -> new SwordItem(GOBLIN_TIER, new Item.Properties().attributes(SwordItem.createAttributes(GOBLIN_TIER, 3, -2.4F))));

    /** Stuns on every hit. Giant goblins sometimes carry one. */
    public static final DeferredItem<GoblinClubItem> GOBLIN_CLUB = REGISTER.register("goblin_club",
            () -> new GoblinClubItem(new Item.Properties().durability(350).attributes(GoblinClubItem.createAttributes())));

    /** Arrows deal 1.5x the damage of a regular bow. */
    public static final DeferredItem<GoblinBowItem> GOBLIN_BOW = REGISTER.register("goblin_bow",
            () -> new GoblinBowItem(new Item.Properties().durability(500)));

    /** Iron sword damage (6) with 1.5x its attack speed (2.4 per second). */
    public static final DeferredItem<SwordItem> GOBLIN_DAGGER = REGISTER.register("goblin_dagger",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -1.6F))));

    public static final DeferredItem<ArmorItem> GOBLIN_HELMET = armor("goblin_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> GOBLIN_CHESTPLATE = armor("goblin_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> GOBLIN_LEGGINGS = armor("goblin_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> GOBLIN_BOOTS = armor("goblin_boots", ArmorItem.Type.BOOTS);

    public static final DeferredItem<TeleportAmuletItem> TELEPORT_AMULET = REGISTER.register("teleport_amulet",
            () -> new TeleportAmuletItem(new Item.Properties().durability(32).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> GOBLIN_COIN = REGISTER.registerSimpleItem("goblin_coin");

    /** Turns the player into a goblin for a minute. */
    public static final DeferredItem<ConcealmentAmuletItem> CONCEALMENT_AMULET = REGISTER.register("concealment_amulet",
            () -> new ConcealmentAmuletItem(new Item.Properties().durability(16).rarity(Rarity.RARE)));

    /** Goblin grub: twice an apple's food, but ten seconds of hunger. */
    public static final DeferredItem<Item> GOBLIN_GRUB = REGISTER.register("goblin_grub",
            () -> new Item(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(8).saturationModifier(0.3F)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 200, 0), 1.0F)
                    .usingConvertsTo(Items.BOWL)
                    .build())));

    /** What caravan goblins carry the loot in (only for show). */
    public static final DeferredItem<Item> LOOT_SACK = REGISTER.register("loot_sack", () -> new Item(new Item.Properties().stacksTo(1)));

    /** The goblin king's crown: a netherite-strength helmet; goblins do not attack its wearer unprovoked. */
    public static final DeferredItem<ArmorItem> GOBLIN_CROWN = REGISTER.register("goblin_crown",
            () -> new ArmorItem(ModArmorMaterials.GOBLIN_CROWN, ArmorItem.Type.HELMET, new Item.Properties().fireResistant()
                    .rarity(Rarity.EPIC).durability(ArmorItem.Type.HELMET.getDurability(ModArmorMaterials.CROWN_DURABILITY_FACTOR))));

    public static final DeferredItem<KingScepterItem> KING_SCEPTER = REGISTER.register("king_scepter",
            () -> new KingScepterItem(new Item.Properties().durability(600).rarity(Rarity.EPIC).attributes(KingScepterItem.createAttributes())));

    public static final DeferredItem<VassalCharterItem> VASSAL_CHARTER = REGISTER.register("vassal_charter",
            () -> new VassalCharterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<GoblinNoteItem> GOBLIN_NOTE = REGISTER.register("goblin_note",
            () -> new GoblinNoteItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> GOBLIN_STASH = REGISTER.registerSimpleBlockItem(ModBlocks.GOBLIN_STASH);
    public static final DeferredItem<BlockItem> BUILDING_SITE = REGISTER.registerSimpleBlockItem(ModBlocks.BUILDING_SITE);
    public static final DeferredItem<BlockItem> STOREHOUSE_CRATE = REGISTER.registerSimpleBlockItem(ModBlocks.STOREHOUSE_CRATE);

    public static final DeferredItem<DeferredSpawnEggItem> SMALL_GOBLIN_SPAWN_EGG = REGISTER.register("small_goblin_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SMALL_GOBLIN, 0x87B610, 0x704A0A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> RUTHLESS_GOBLIN_SPAWN_EGG = REGISTER.register("ruthless_goblin_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.RUTHLESS_GOBLIN, 0x9A9AA2, 0xBE3024, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> NEUTRAL_GOBLIN_SPAWN_EGG = REGISTER.register("neutral_goblin_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.NEUTRAL_GOBLIN, 0x87B610, 0x948D85, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GIANT_GOBLIN_SPAWN_EGG = REGISTER.register("giant_goblin_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GIANT_GOBLIN, 0x769F0E, 0xE0B418, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOBLIN_TRADER_SPAWN_EGG = REGISTER.register("goblin_trader_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOBLIN_TRADER, 0x87590C, 0xBE3024, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOBLIN_ELDER_SPAWN_EGG = REGISTER.register("goblin_elder_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOBLIN_ELDER, 0x91A742, 0xAAAAAA, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOBLIN_SHAMAN_SPAWN_EGG = REGISTER.register("goblin_shaman_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOBLIN_SHAMAN, 0x7A5331, 0x87B610, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOBLIN_KING_SPAWN_EGG = REGISTER.register("goblin_king_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOBLIN_KING, 0xA0221A, 0xF2CC30, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOBLIN_CHILD_SPAWN_EGG = REGISTER.register("goblin_child_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOBLIN_CHILD, 0x99CD12, 0xC0B179, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOBLIN_VILLAGER_SPAWN_EGG = REGISTER.register("goblin_villager_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOBLIN_VILLAGER, 0xD8CFAC, 0x87B610, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOBLIN_HEADMAN_SPAWN_EGG = REGISTER.register("goblin_headman_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOBLIN_HEADMAN, 0x27563B, 0xE0B418, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOBLIN_SAGE_SPAWN_EGG = REGISTER.register("goblin_sage_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOBLIN_SAGE, 0x26387C, 0xECE6CF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CRAZY_GOBLIN_SPAWN_EGG = REGISTER.register("crazy_goblin_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CRAZY_GOBLIN, 0x7C7780, 0x87B610, new Item.Properties()));

    private static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type) {
        return REGISTER.register(name, () -> new ArmorItem(ModArmorMaterials.GOBLIN, type,
                new Item.Properties().durability(type.getDurability(ModArmorMaterials.DURABILITY_FACTOR))));
    }

    private ModItems() {
    }
}
