"""Writes the mod's JSON resources: item/block models, loot tables, tags and worldgen.

Run from anywhere:  python tools/gen_data.py
Language files are hand-written (assets/goblinraiders/lang) and not touched here.
"""
import json
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
R = ROOT / 'src/main/resources'
A = R / 'assets/goblinraiders'
D = R / 'data/goblinraiders'


def w(path, obj):
    os.makedirs(path.parent, exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write('\n')


# ---------------------------------------------------------------- item / block models
def generated(tex):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"goblinraiders:item/{tex}"}}


for n in ['goblin_sword', 'goblin_dagger', 'goblin_club', 'king_scepter']:
    w(A / f'models/item/{n}.json', {"parent": "minecraft:item/handheld", "textures": {"layer0": f"goblinraiders:item/{n}"}})
# The goblin bow copies the vanilla bow model: held like a bow, three drawn states.
BOW_DISPLAY = {
    "thirdperson_righthand": {"rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
w(A / 'models/item/goblin_bow.json', {
    "parent": "minecraft:item/generated", "textures": {"layer0": "goblinraiders:item/goblin_bow"}, "display": BOW_DISPLAY,
    "overrides": [
        {"predicate": {"pulling": 1}, "model": "goblinraiders:item/goblin_bow_pulling_0"},
        {"predicate": {"pulling": 1, "pull": 0.65}, "model": "goblinraiders:item/goblin_bow_pulling_1"},
        {"predicate": {"pulling": 1, "pull": 0.9}, "model": "goblinraiders:item/goblin_bow_pulling_2"}]})
for i in range(3):
    w(A / f'models/item/goblin_bow_pulling_{i}.json', {"parent": "goblinraiders:item/goblin_bow",
                                                         "textures": {"layer0": f"goblinraiders:item/goblin_bow_pulling_{i}"}})
for n in ['goblin_helmet', 'goblin_chestplate', 'goblin_leggings', 'goblin_boots', 'teleport_amulet', 'goblin_coin', 'goblin_note',
          'concealment_amulet', 'goblin_grub', 'loot_sack', 'goblin_crown', 'vassal_charter']:
    w(A / f'models/item/{n}.json', generated(n))
for g in ['small_goblin', 'ruthless_goblin', 'neutral_goblin', 'giant_goblin', 'goblin_trader', 'goblin_elder', 'crazy_goblin',
          'goblin_shaman', 'goblin_king', 'goblin_child', 'goblin_villager', 'goblin_sage', 'goblin_headman']:
    w(A / f'models/item/{g}_spawn_egg.json', {"parent": "minecraft:item/template_spawn_egg"})
w(A / 'models/item/goblin_stash.json', {"parent": "goblinraiders:block/goblin_stash"})
w(A / 'models/block/goblin_stash.json', {"parent": "minecraft:block/orientable", "textures": {
    "top": "goblinraiders:block/goblin_stash_top", "front": "goblinraiders:block/goblin_stash_front",
    "side": "goblinraiders:block/goblin_stash_side", "particle": "goblinraiders:block/goblin_stash_side"}})
variants = {}
for facing, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
    variants[f"facing={facing}"] = {"model": "goblinraiders:block/goblin_stash", **({"y": y} if y else {})}
w(A / 'blockstates/goblin_stash.json', {"variants": variants})


# ---------------------------------------------------------------- sounds
def files(prefix, volume=1.0):
    """Every sound file sounds/<prefix>_1.ogg, _2.ogg ... there is (made by tools/sounds/synth.py and voices.py)."""
    n = len([p for p in (A / 'sounds').glob(f'{prefix}_*.ogg') if p.stem[len(prefix) + 1:].isdigit()])
    assert n, f'no sounds/{prefix}_*.ogg'
    return [{"name": f"goblinraiders:{prefix}_{k}", **({"volume": volume} if volume != 1.0 else {})} for k in range(1, n + 1)]


SOUNDS = {
    # Every voice is made by tools/sounds/synth.py (a small formant speech synthesizer): nothing borrowed from the game.
    "entity.goblin.ambient": ("goblin.ambient", files("goblin_ambient")),
    "entity.goblin.talk": ("goblin.talk", files("goblin_talk")),
    "entity.goblin.laugh": ("goblin.laugh", files("goblin_laugh")),
    "entity.goblin.angry": ("goblin.angry", files("goblin_angry")),
    "entity.goblin.hurt": ("goblin.hurt", files("goblin_hurt")),
    "entity.goblin.death": ("goblin.death", files("goblin_death")),
    "entity.goblin.yes": ("goblin.yes", files("goblin_yes")),
    "entity.goblin.no": ("goblin.no", files("goblin_no")),
    "entity.ruthless_goblin.ambient": ("ruthless_goblin.ambient", files("ruthless_ambient")),
    "entity.ruthless_goblin.hurt": ("ruthless_goblin.hurt", files("ruthless_hurt")),
    "entity.ruthless_goblin.death": ("ruthless_goblin.death", files("ruthless_death")),
    "entity.giant_goblin.ambient": ("giant_goblin.ambient", files("giant_ambient")),
    "entity.giant_goblin.roar": ("giant_goblin.roar", files("giant_roar")),
    "entity.giant_goblin.hurt": ("giant_goblin.hurt", files("giant_hurt")),
    "entity.giant_goblin.death": ("giant_goblin.death", files("giant_death")),
    "entity.giant_goblin.attack": ("giant_goblin.attack", files("giant_attack")),
    "entity.goblin_trader.haggle": ("goblin_trader.haggle", files("trader_haggle")),
    "entity.goblin_trader.yell": ("goblin_trader.yell", files("trader_yell")),
    "entity.goblin_elder.ambient": ("goblin_elder.ambient", files("elder_ambient")),
    "entity.goblin_elder.talk": ("goblin_elder.talk", files("elder_talk")),
    "entity.crazy_goblin.laugh": ("crazy_goblin.laugh", files("crazy_laugh")),
    # Played to a player whose base was just raided: the user's own sound (tools/sounds/raid_alarm.mp3 -> sounds/raid_alarm.ogg).
    "event.goblin_raid": ("goblin_raid", [{"name": "goblinraiders:raid_alarm"}]),
    "entity.goblin_shaman.ambient": ("goblin_shaman.ambient", files("shaman_ambient")),
    "entity.goblin_shaman.spell": ("goblin_shaman.spell", files("shaman_spell")),
    "entity.goblin_king.ambient": ("goblin_king.ambient", files("king_ambient")),
    "entity.goblin_king.roar": ("goblin_king.roar", files("king_roar")),

    # Goblins in more situations.
    "entity.goblin.snore": ("goblin.snore", files("snore")),
    # Made by tools/sounds/synth.py like everything below: nothing borrowed from the game.
    "entity.goblin.attack": ("goblin.attack", files("grunt")),
    "entity.goblin.celebrate": ("goblin.celebrate", files("gloat")),
    "entity.goblin.scared": ("goblin.scared", files("squeal")),
    "entity.goblin_child.giggle": ("goblin_child.giggle", files("giggle")),
    "entity.goblin.loot_jingle": ("goblin.loot_jingle", files("jingle", 0.7)),

    # The sound of the goblin settlements (see world/GoblinSoundscapes.java).
    "ambient.goblin.war_drums": ("ambient.war_drums", files("war_drums")),
    "ambient.goblin.war_horn": ("ambient.war_horn", files("war_horn")),
    "ambient.goblin.forge": ("ambient.forge", files("forge")),
    "ambient.goblin.sharpen": ("ambient.sharpen", files("sharpen")),
    "ambient.goblin.chop": ("ambient.chop", files("chop")),
    "ambient.goblin.chatter": ("ambient.chatter", files("chatter")),
    "ambient.goblin.coins": ("ambient.coins", files("coins")),
    "ambient.goblin.pipe": ("ambient.pipe", files("pipe_tune", 0.8)),
    "ambient.goblin.birds": ("ambient.birds", files("birds", 0.6)),
    "ambient.goblin.farm": ("ambient.farm", files("farm")),
    "ambient.goblin.bell": ("ambient.bell", files("bell")),
    "ambient.goblin.crickets": ("ambient.crickets", files("crickets", 0.6)),
    "ambient.goblin.owl": ("ambient.owl", files("owl", 0.7)),
    "ambient.goblin.mystic": ("ambient.mystic", files("chimes")),
}
w(A / 'sounds.json', {name: {"sounds": entries, "subtitle": f"subtitles.goblinraiders.{sub}"}
                      for name, (sub, entries) in SOUNDS.items()})


# ---------------------------------------------------------------- loot tables
def item(name, weight=1, count=None, extra=()):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    fs = []
    if count:
        fs.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}})
    fs += list(extra)
    if fs:
        e["functions"] = fs
    return e


def looting(a, b):
    return {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
            "count": {"type": "minecraft:uniform", "min": a, "max": b}}


def chance(p, per_level):
    return {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
            "unenchanted_chance": p,
            "enchanted_chance": {"type": "minecraft:linear", "base": p + per_level, "per_level_above_first": per_level}}


KILLED = {"condition": "minecraft:killed_by_player"}
COIN = "goblinraiders:goblin_coin"
ARMOR = ["goblinraiders:goblin_helmet", "goblinraiders:goblin_chestplate", "goblinraiders:goblin_leggings", "goblinraiders:goblin_boots"]
DAMAGED = {"function": "minecraft:set_damage", "damage": {"type": "minecraft:uniform", "min": 0.3, "max": 0.9}}


def coins(a, b, p=1.0):
    """a..b coins with probability p (coins are meant to be scarce)."""
    pool = {"rolls": 1, "entries": [item(COIN, count=(a, b), extra=[looting(0, 1)])]}
    if p < 1.0:
        pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": p}]
    return pool


def entity(name, pools):
    w(D / f'loot_table/entities/{name}.json',
      {"type": "minecraft:entity", "random_sequence": f"goblinraiders:entities/{name}", "pools": pools})


def chest(name, pools):
    w(D / f'loot_table/chests/{name}.json',
      {"type": "minecraft:chest", "random_sequence": f"goblinraiders:chests/{name}", "pools": pools})


entity('small_goblin', [
    coins(1, 1, 0.4),
    {"rolls": 1, "conditions": [KILLED, chance(0.04, 0.02)], "entries": [item("goblinraiders:teleport_amulet")]}])
entity('ruthless_goblin', [
    coins(1, 2, 0.5),
    {"rolls": 1, "entries": [item("minecraft:bone", count=(0, 2), extra=[looting(0, 1)])]}])
entity('neutral_goblin', [
    coins(1, 2, 0.5),
    {"rolls": 1, "entries": [item("minecraft:potato", count=(0, 2)), item("minecraft:bread", count=(0, 1))]}])
entity('giant_goblin', [
    coins(1, 3, 0.8),
    {"rolls": 1, "entries": [item("minecraft:leather", count=(1, 3), extra=[looting(0, 1)])]},
    {"rolls": 1, "conditions": [KILLED, chance(0.12, 0.03)], "entries": [item(a) for a in ARMOR]}])
entity('goblin_trader', [
    coins(2, 5),
    {"rolls": 1, "conditions": [KILLED, chance(0.25, 0.05)], "entries": [item("goblinraiders:teleport_amulet")]},
    {"rolls": 1, "entries": [item("minecraft:emerald", count=(0, 2))]}])
entity('goblin_elder', [
    coins(1, 1, 0.5),
    {"rolls": 1, "entries": [item("minecraft:book", count=(1, 2)), item("minecraft:brown_mushroom", count=(1, 3))]}])
# Children, settlers and the sage drop nothing (workers drop what they carry, from code).
entity('goblin_child', [])
entity('goblin_villager', [])
entity('goblin_sage', [])
entity('goblin_headman', [])
entity('crazy_goblin', [
    coins(1, 1, 0.3),
    {"rolls": 1, "entries": [item("minecraft:red_mushroom", 2, (1, 2)), item("minecraft:brown_mushroom", 2, (1, 2)),
                             item("minecraft:cookie", 1, (1, 3)), {"type": "minecraft:empty", "weight": 2}]}])
entity('goblin_shaman', [
    coins(1, 2, 0.6),
    {"rolls": 1, "entries": [item("minecraft:glass_bottle", 2, (1, 2)), item("minecraft:spider_eye", 2, (1, 2)),
                             item("minecraft:brown_mushroom", 2, (1, 3)), item("minecraft:redstone", 1, (1, 4))]},
    {"rolls": 1, "conditions": [KILLED, chance(0.08, 0.03)], "entries": [item("goblinraiders:concealment_amulet")]}])
# The king always drops his crown and scepter.
entity('goblin_king', [
    {"rolls": 1, "entries": [item("goblinraiders:goblin_crown")]},
    {"rolls": 1, "entries": [item("goblinraiders:king_scepter")]},
    {"rolls": 1, "entries": [item(COIN, count=(8, 16))]},
    {"rolls": 1, "entries": [item("minecraft:gold_block", count=(1, 3))]},
    {"rolls": 1, "entries": [item("goblinraiders:concealment_amulet"), item("goblinraiders:teleport_amulet")]}])

# Regular chests in the huts.
chest('goblin_hut', [
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 7}, "entries": [
        item(COIN, 2, (1, 2)), item("minecraft:rotten_flesh", 4, (1, 4)), item("minecraft:bone", 4, (1, 3)),
        item("minecraft:potato", 4, (1, 4)), item("minecraft:bread", 3, (1, 2)), item("minecraft:leather", 3, (1, 3)),
        item("minecraft:string", 3, (1, 4)), item("minecraft:iron_nugget", 3, (2, 7)), item("minecraft:gold_nugget", 3, (2, 7)),
        item("minecraft:torch", 2, (1, 6)), item("minecraft:mushroom_stew", 1), item("goblinraiders:goblin_grub", 3, (1, 2))]},
    {"rolls": 1, "entries": [
        {"type": "minecraft:empty", "weight": 6}, item("goblinraiders:teleport_amulet", 1),
        item("goblinraiders:goblin_boots", 1, extra=[DAMAGED]), item("goblinraiders:goblin_helmet", 1, extra=[DAMAGED])]}])

# What the village stash holds before any raid.
chest('goblin_stash', [
    {"rolls": 1, "entries": [item(COIN, 1, (2, 6))]},
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
        item("minecraft:iron_ingot", 5, (1, 5)), item("minecraft:gold_ingot", 4, (1, 4)), item("minecraft:emerald", 3, (1, 3)),
        item("minecraft:diamond", 1, (1, 2)), item("minecraft:golden_apple", 1), item("minecraft:ender_pearl", 2, (1, 2)),
        item("minecraft:saddle", 1), item("minecraft:name_tag", 1), item("minecraft:clock", 1), item("minecraft:compass", 1)]},
    {"rolls": 1, "entries": [item("goblinraiders:teleport_amulet", 3), {"type": "minecraft:empty", "weight": 2}]},
    {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 5}, item("goblinraiders:goblin_sword", 2)]
        + [item(a) for a in ARMOR]}])

# Market stalls: food and odds and ends for sale.
chest('goblin_market', [
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
        item(COIN, 2, (1, 2)), item("minecraft:baked_potato", 4, (2, 6)), item("minecraft:bread", 3, (1, 4)),
        item("minecraft:apple", 3, (1, 4)), item("minecraft:pumpkin_pie", 2, (1, 2)), item("minecraft:leather", 3, (1, 4)),
        item("minecraft:emerald", 2, (1, 2)), item("minecraft:lead", 2), item("minecraft:name_tag", 1),
        item("minecraft:ender_pearl", 1), item("goblinraiders:goblin_grub", 3, (1, 3))]}])

# War camp armory: goblin gear is more common here.
chest('goblin_armory', [
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
        item(COIN, 2, (1, 2)), item("minecraft:iron_ingot", 4, (1, 4)), item("minecraft:arrow", 3, (4, 12)),
        item("minecraft:shield", 1), item("minecraft:iron_sword", 2, extra=[DAMAGED]), item("minecraft:bone", 3, (2, 5))]},
    {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 3}, item("goblinraiders:goblin_sword", 2, extra=[DAMAGED])]
        + [item(a, 1, extra=[DAMAGED]) for a in ARMOR]},
    {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 12}, item("goblinraiders:goblin_club", 1, extra=[DAMAGED]),
                             item("goblinraiders:goblin_bow", 1, extra=[DAMAGED]), item("goblinraiders:goblin_dagger", 1, extra=[DAMAGED])]}])

# The king's treasury in the throne hall.
chest('goblin_royal', [
    {"rolls": 1, "entries": [item(COIN, 1, (4, 10))]},
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
        item("minecraft:gold_ingot", 5, (2, 6)), item("minecraft:diamond", 2, (1, 3)), item("minecraft:emerald", 3, (2, 5)),
        item("minecraft:golden_apple", 2), item("minecraft:enchanted_golden_apple", 1), item("goblinraiders:goblin_grub", 3, (1, 3))]},
    {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 2}, item("goblinraiders:concealment_amulet", 2),
                             item("goblinraiders:goblin_sword", 1), item("goblinraiders:goblin_chestplate", 1)]}])

w(D / 'loot_table/blocks/goblin_stash.json', {
    "type": "minecraft:block", "random_sequence": "goblinraiders:blocks/goblin_stash",
    "pools": [{"rolls": 1, "bonus_rolls": 0,
               "entries": [{"type": "minecraft:item", "name": "goblinraiders:goblin_stash",
                            "functions": [{"function": "minecraft:copy_components", "source": "block_entity",
                                           "include": ["minecraft:custom_name"]}]}],
               "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

# ---------------------------------------------------------------- tags
MC = R / 'data/minecraft/tags'
w(MC / 'item/swords.json', {"replace": False, "values": ["goblinraiders:goblin_sword", "goblinraiders:goblin_dagger"]})
w(MC / 'item/enchantable/sharp_weapon.json', {"replace": False, "values": ["goblinraiders:goblin_club", "goblinraiders:king_scepter"]})
w(MC / 'item/enchantable/bow.json', {"replace": False, "values": ["goblinraiders:goblin_bow"]})
w(MC / 'item/enchantable/durability.json', {"replace": False, "values": ["goblinraiders:goblin_club", "goblinraiders:goblin_bow",
                                                                         "goblinraiders:king_scepter"]})
for tag, armor in zip(['head_armor', 'chest_armor', 'leg_armor', 'foot_armor'], ARMOR):
    w(MC / f'item/{tag}.json', {"replace": False, "values": [armor] + (["goblinraiders:goblin_crown"] if tag == 'head_armor' else [])})
w(MC / 'item/trimmable_armor.json', {"replace": False, "values": ARMOR})
w(MC / 'block/mineable/axe.json', {"replace": False, "values": ["goblinraiders:goblin_stash", "goblinraiders:building_site",
                                                                "goblinraiders:storehouse_crate"]})
w(D / 'tags/item/raid_proof.json', {"replace": False, "values": ["goblinraiders:goblin_note", "goblinraiders:vassal_charter"]})

# ---------------------------------------------------------------- recipes
# Goblin grub: whatever a goblin finds, in a bowl.
w(D / 'recipe/goblin_grub.json', {
    "type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [{"item": "minecraft:bowl"}, {"item": "minecraft:rotten_flesh"}, {"item": "minecraft:brown_mushroom"},
                    {"item": "minecraft:potato"}],
    "result": {"id": "goblinraiders:goblin_grub", "count": 1}})
VILLAGE_TYPES = ['goblin_camp', 'goblin_village', 'goblin_market', 'goblin_warcamp', 'goblin_megavillage']
w(D / 'tags/worldgen/structure/goblin_villages.json', {"replace": False, "values": [f"goblinraiders:{t}" for t in VILLAGE_TYPES]})
w(D / 'tags/worldgen/biome/has_structure/goblin_village.json', {"replace": False, "values": [
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
    "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:savanna",
    "minecraft:meadow", "minecraft:dark_forest"]})
# The mega village needs a lot of fairly flat land.
w(D / 'tags/worldgen/biome/has_structure/goblin_megavillage.json', {"replace": False, "values": [
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:meadow", "minecraft:forest",
    "minecraft:taiga"]})


# ---------------------------------------------------------------- worldgen
def spawn(kind, weight, lo, hi):
    return {"type": f"goblinraiders:{kind}", "weight": weight, "minCount": lo, "maxCount": hi}


# Village types: structure id -> (template pool, templates, structure set weight, natural spawns).
# Every type is one template piece; the start jigsaw sits in the village center, so the structure start chunk corner
# (what raid notes point at) is exactly the middle of the village. Templates come from tools/structures/goblin_village.py.
VILLAGES = {
    'goblin_camp': ('goblin_village/camp', ['camp_1', 'camp_2'], 3,
                    [spawn("small_goblin", 4, 1, 3), spawn("crazy_goblin", 4, 1, 2), spawn("ruthless_goblin", 2, 1, 2)]),
    # The classic village keeps its old pool and template names, so villages saved in existing worlds still load.
    'goblin_village': ('goblin_village/center', ['village', 'village_2'], 4,
                       [spawn("small_goblin", 5, 1, 3), spawn("ruthless_goblin", 3, 1, 2), spawn("neutral_goblin", 4, 1, 3),
                        spawn("giant_goblin", 1, 1, 1), spawn("crazy_goblin", 2, 1, 2)]),
    'goblin_market': ('goblin_village/market', ['market_1', 'market_2'], 3,
                      [spawn("neutral_goblin", 6, 1, 3), spawn("small_goblin", 3, 1, 3), spawn("ruthless_goblin", 2, 1, 2)]),
    'goblin_warcamp': ('goblin_village/warcamp', ['warcamp_1', 'warcamp_2'], 2,
                       [spawn("ruthless_goblin", 5, 1, 3), spawn("small_goblin", 3, 1, 3), spawn("giant_goblin", 2, 1, 1),
                        spawn("crazy_goblin", 1, 1, 2)]),
    'goblin_megavillage': ('goblin_village/mega', ['mega_1'], 1,
                           [spawn("small_goblin", 4, 1, 3), spawn("ruthless_goblin", 4, 1, 3), spawn("neutral_goblin", 4, 1, 3),
                            spawn("giant_goblin", 1, 1, 1), spawn("crazy_goblin", 2, 1, 2)]),
}
assert list(VILLAGES) == VILLAGE_TYPES

for structure, (pool, templates, weight, spawns) in VILLAGES.items():
    w(D / f'worldgen/structure/{structure}.json', {
        "type": "minecraft:jigsaw",
        "biomes": "#goblinraiders:has_structure/" + ('goblin_megavillage' if structure == 'goblin_megavillage' else 'goblin_village'),
        "step": "surface_structures",
        "terrain_adaptation": "beard_thin",
        "start_pool": f"goblinraiders:{pool}",
        "start_jigsaw_name": "goblinraiders:village_center",
        "size": 1,             # 0 would drop even the start piece (JigsawPlacement adds pieces only when depth > 0)
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80,
        "use_expansion_hack": False,
        "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": spawns}}})
    w(D / f'worldgen/template_pool/{pool}.json', {
        "fallback": "minecraft:empty",
        "elements": [{"weight": 1, "element": {
            "element_type": "minecraft:single_pool_element", "location": f"goblinraiders:goblin_village/{t}",
            "processors": "goblinraiders:goblin_village", "projection": "rigid"}} for t in templates]})
w(D / 'worldgen/structure_set/goblin_villages.json', {
    "structures": [{"structure": f"goblinraiders:{name}", "weight": v[2]} for name, v in VILLAGES.items()],
    # Never closer than 12 chunks to a spot where a villager village may generate.
    "placement": {"type": "minecraft:random_spread", "spacing": 36, "separation": 12, "salt": 739164852,
                  "exclusion_zone": {"other_set": "minecraft:villages", "chunk_count": 12}}})


# ---------------------------------------------------------------- the peaceful villages and the sage's tents
# Not goblin villages: raids never take loot there, and no monsters spawn inside a peaceful village at all.
w(D / 'tags/worldgen/structure/goblin_hamlets.json', {"replace": False, "values": ["goblinraiders:goblin_hamlet"]})
w(D / 'tags/worldgen/structure/goblin_capitals.json', {"replace": False, "values": ["goblinraiders:goblin_megavillage"]})
w(D / 'tags/worldgen/structure/goblin_sage_tents.json', {"replace": False, "values": ["goblinraiders:goblin_sage_tent"]})
w(D / 'tags/worldgen/biome/has_structure/goblin_hamlet.json', {"replace": False, "values": [
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest", "minecraft:flower_forest",
    "minecraft:birch_forest", "minecraft:taiga", "minecraft:savanna", "minecraft:cherry_grove"]})
w(D / 'tags/worldgen/biome/has_structure/goblin_sage_tent.json', {"replace": False, "values": [
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest", "minecraft:flower_forest",
    "minecraft:birch_forest", "minecraft:dark_forest", "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:snowy_plains",
    "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:desert", "minecraft:windswept_hills", "minecraft:cherry_grove",
    "minecraft:old_growth_spruce_taiga", "minecraft:old_growth_pine_taiga"]})
PEACEFUL = {
    'goblin_sage_tent': ('goblin_village/sage', ['sage_tent_1', 'sage_tent_2'], {}),
}
# The peaceful village is built like a villager village (tools/structures/goblin_hamlet.py): the square, streets that
# follow the land (terrain_matching) and houses along them, joined by jigsaw blocks.
# goblinraiders:hamlet is minecraft:jigsaw that also puts out the lava it was built over (world/HamletStructure.java).
w(D / 'worldgen/structure/goblin_hamlet.json', {
    "type": "goblinraiders:hamlet",
    "biomes": "#goblinraiders:has_structure/goblin_hamlet",
    "step": "surface_structures",
    "terrain_adaptation": "beard_thin",
    "start_pool": "goblinraiders:hamlet/center",
    "size": 6,
    "start_height": {"absolute": 0},
    "project_start_to_heightmap": "WORLD_SURFACE_WG",
    "max_distance_from_center": 80,
    "use_expansion_hack": False,
    "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": []}}})


def hamlet_pool(name, pieces, projection, fallback="minecraft:empty"):
    w(D / f'worldgen/template_pool/hamlet/{name}.json', {
        "fallback": fallback,
        "elements": [{"weight": weight, "element": {
            "element_type": "minecraft:single_pool_element", "location": f"goblinraiders:hamlet/{piece}",
            "processors": "minecraft:empty" if projection == "terrain_matching" else "goblinraiders:goblin_village",
            "projection": projection}} for piece, weight in pieces]})


hamlet_pool('center', [('center_1', 1), ('center_2', 1)], 'rigid')
hamlet_pool('streets', [('street_long', 4), ('street_short', 4), ('street_cross', 2)], 'terrain_matching',
            fallback='goblinraiders:hamlet/terminators')
hamlet_pool('terminators', [('street_end', 1)], 'terrain_matching')
hamlet_pool('houses', [('home_small', 5), ('home_small_2', 4), ('home_family', 4), ('home_tent', 2),
                       ('farm', 5), ('farm_wheat', 3), ('lumber_yard', 3), ('grove', 3), ('mine_adit', 2),
                       ('builder_yard', 3), ('kitchen', 2), ('site_hut', 3), ('site_big', 1), ('site_garden', 2)], 'rigid')
for structure, (pool, templates, overrides) in PEACEFUL.items():
    w(D / f'worldgen/structure/{structure}.json', {
        "type": "minecraft:jigsaw",
        "biomes": f"#goblinraiders:has_structure/{structure}",
        "step": "surface_structures",
        "terrain_adaptation": "beard_thin",
        "start_pool": f"goblinraiders:{pool}",
        "start_jigsaw_name": "goblinraiders:village_center",
        "size": 1,
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80,
        "use_expansion_hack": False,
        "spawn_overrides": overrides})
    w(D / f'worldgen/template_pool/{pool}.json', {
        "fallback": "minecraft:empty",
        "elements": [{"weight": 1, "element": {
            "element_type": "minecraft:single_pool_element", "location": f"goblinraiders:goblin_village/{t}",
            "processors": "goblinraiders:goblin_village", "projection": "rigid"}} for t in templates]})
w(D / 'worldgen/structure_set/goblin_hamlets.json', {
    "structures": [{"structure": "goblinraiders:goblin_hamlet", "weight": 1}],
    # Peaceful goblins keep their distance from the raiders.
    "placement": {"type": "minecraft:random_spread", "spacing": 40, "separation": 14, "salt": 512036771,
                  "exclusion_zone": {"other_set": "goblinraiders:goblin_villages", "chunk_count": 8}}})
w(D / 'worldgen/structure_set/goblin_sage_tents.json', {
    "structures": [{"structure": "goblinraiders:goblin_sage_tent", "weight": 1}],
    "placement": {"type": "minecraft:random_spread", "spacing": 28, "separation": 10, "salt": 98173321,
                  "exclusion_zone": {"other_set": "minecraft:villages", "chunk_count": 4}}})

# Chests of the peaceful village and the sage's tent.
chest('hamlet_builder', [
    {"rolls": 1, "entries": [item("minecraft:oak_log", 1, (16, 24))]},
    {"rolls": 1, "entries": [item("minecraft:cobblestone", 1, (32, 48))]},
    {"rolls": 1, "entries": [item("minecraft:spruce_log", 1, (8, 16))]}])
chest('hamlet_kitchen', [
    {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
        item("minecraft:potato", 3, (3, 8)), item("minecraft:carrot", 3, (3, 8)), item("minecraft:beetroot", 2, (2, 6)),
        item("minecraft:bowl", 2, (2, 6)), item("goblinraiders:goblin_grub", 2, (1, 3)), item("minecraft:bread", 2, (1, 3))]}])
chest('sage_tent', [
    {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
        item("minecraft:book", 4, (1, 3)), item("minecraft:paper", 3, (2, 6)), item("minecraft:amethyst_shard", 2, (1, 4)),
        item("minecraft:candle", 2, (1, 3)), item("minecraft:glass_bottle", 2, (1, 3)), item(COIN, 2, (1, 3)),
        item("goblinraiders:goblin_grub", 1)]},
    {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 3}, item("goblinraiders:concealment_amulet", 1),
                             item("goblinraiders:teleport_amulet", 1),
                             {"type": "minecraft:item", "name": "minecraft:book", "weight": 2,
                              "functions": [{"function": "minecraft:enchant_randomly"}]}]}])

# The builders' stake: a block model with a post and a red ribbon, no texture of its own.
w(A / 'blockstates/building_site.json', {"variants": {
    f"facing={f}": {"model": "goblinraiders:block/building_site", "y": r} for f, r in
    (("south", 0), ("west", 90), ("north", 180), ("east", 270))}})
w(A / 'models/block/building_site.json', {
    "parent": "minecraft:block/block", "ambientocclusion": False,
    "textures": {"post": "minecraft:block/stripped_spruce_log", "ribbon": "minecraft:block/red_wool",
                 "particle": "minecraft:block/stripped_spruce_log"},
    "elements": [
        {"from": [7, 0, 7], "to": [9, 15, 9], "faces": {d: {"texture": "#post"} for d in ("north", "south", "east", "west", "up", "down")}},
        {"from": [9, 10, 8], "to": [14, 13, 8.5], "faces": {d: {"texture": "#ribbon"} for d in ("north", "south", "east", "west", "up", "down")}},
        {"from": [6.5, 13, 6.5], "to": [9.5, 14, 9.5], "faces": {d: {"texture": "#ribbon"} for d in ("north", "south", "east", "west", "up", "down")}}]})
w(A / 'models/item/building_site.json', {"parent": "goblinraiders:block/building_site"})
# The storehouse crate of the peaceful villages.
w(A / 'blockstates/storehouse_crate.json', {"variants": {"": {"model": "goblinraiders:block/storehouse_crate"}}})
w(A / 'models/block/storehouse_crate.json', {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "side": "goblinraiders:block/storehouse_crate_side", "top": "goblinraiders:block/storehouse_crate_top",
    "bottom": "goblinraiders:block/storehouse_crate_top"}})
w(A / 'models/item/storehouse_crate.json', {"parent": "goblinraiders:block/storehouse_crate"})
w(D / 'loot_table/blocks/storehouse_crate.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "goblinraiders:storehouse_crate"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
w(D / 'loot_table/blocks/building_site.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "goblinraiders:building_site"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
w(D / 'recipe/building_site.json', {
    "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["R", "S", "S"],
    "key": {"R": {"item": "minecraft:red_wool"}, "S": {"item": "minecraft:stick"}},
    "result": {"id": "goblinraiders:building_site", "count": 1}})


def random_swap(block, probability, output):
    return {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": block, "probability": probability},
            "location_predicate": {"predicate_type": "minecraft:always_true"}, "output_state": {"Name": output}}


w(D / 'worldgen/processor_list/goblin_village.json', {"processors": [{"processor_type": "minecraft:rule", "rules": [
    random_swap("minecraft:cobblestone", 0.25, "minecraft:mossy_cobblestone"),
    random_swap("minecraft:dirt_path", 0.15, "minecraft:coarse_dirt")]}]})

# ---------------------------------------------------------------- advancements
# The "Goblin Raiders" tab. Advancements granted from code (GoblinAdvancements.award) use one "impossible"
# criterion called "done"; the rest use vanilla triggers.
w(D / 'tags/entity_type/goblins.json', {"replace": False, "values": [
    f"goblinraiders:{g}" for g in ['small_goblin', 'ruthless_goblin', 'neutral_goblin', 'giant_goblin', 'goblin_trader',
                                   'goblin_elder', 'crazy_goblin', 'goblin_shaman', 'goblin_king']]})

IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}


def killed(entity):
    return {"done": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": entity}}}}


def located(structures):
    return {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": structures}}}}}


def effect(name):
    return {"done": {"trigger": "minecraft:effects_changed", "conditions": {"effects": {name: {}}}}}


def advancement(name, parent, icon, criteria=None, frame="task", hidden=False, xp=0):
    data = {"display": {"icon": {"id": icon},
                        "title": {"translate": f"advancements.goblinraiders.{name}.title"},
                        "description": {"translate": f"advancements.goblinraiders.{name}.description"},
                        "frame": frame, "show_toast": parent is not None, "announce_to_chat": parent is not None,
                        "hidden": hidden},
            "criteria": criteria or IMPOSSIBLE}
    if parent is None:
        data["display"]["background"] = "minecraft:textures/block/packed_mud.png"
    else:
        data["parent"] = f"goblinraiders:{parent}"
    if xp:
        data["rewards"] = {"experience": xp}
    w(D / f'advancement/{name}.json', data)


advancement('root', None, 'goblinraiders:goblin_sword', {"done": {"trigger": "minecraft:tick"}})
# Villages and their people.
advancement('find_village', 'root', 'minecraft:campfire', located('#goblinraiders:goblin_villages'))
advancement('megavillage', 'find_village', 'minecraft:gold_block', located('goblinraiders:goblin_megavillage'), frame='goal')
advancement('elder', 'find_village', 'minecraft:book')
advancement('neutral_talk', 'find_village', 'goblinraiders:neutral_goblin_spawn_egg')
advancement('treat', 'neutral_talk', 'goblinraiders:goblin_grub')
advancement('trade', 'find_village', 'goblinraiders:goblin_coin')
advancement('refuse', 'trade', 'minecraft:barrier')
advancement('grub', 'trade', 'goblinraiders:goblin_grub',
            {"done": {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": "goblinraiders:goblin_grub"}}}})
advancement('night_thief', 'find_village', 'minecraft:leather_boots', frame='goal')
advancement('spotted', 'find_village', 'minecraft:bell')
advancement('war_drums', 'find_village', 'minecraft:note_block')
# Raids and caravans.
advancement('robbed', 'root', 'goblinraiders:goblin_note')
advancement('got_it_back', 'robbed', 'goblinraiders:goblin_stash')
advancement('intercept', 'robbed', 'goblinraiders:loot_sack')
advancement('whole_caravan', 'intercept', 'minecraft:chest', frame='goal')
# Fighting.
advancement('kill_goblin', 'root', 'goblinraiders:goblin_sword', killed('#goblinraiders:goblins'))
advancement('kill_giant', 'kill_goblin', 'goblinraiders:goblin_club', killed('goblinraiders:giant_goblin'))
advancement('stun', 'kill_goblin', 'goblinraiders:goblin_club')
advancement('cursed', 'kill_goblin', 'minecraft:fermented_spider_eye', effect('goblinraiders:goblin_curse'))
advancement('disguise', 'root', 'goblinraiders:concealment_amulet', effect('goblinraiders:goblin_disguise'))
# The king and his horde.
advancement('vassal', 'megavillage', 'goblinraiders:vassal_charter')
advancement('hire', 'vassal', 'goblinraiders:ruthless_goblin_spawn_egg')
advancement('betray', 'vassal', 'goblinraiders:goblin_dagger', frame='goal')
advancement('regicide', 'megavillage', 'goblinraiders:goblin_crown', killed('goblinraiders:goblin_king'), frame='challenge', xp=100)
advancement('undercover', 'disguise', 'goblinraiders:ruthless_goblin_spawn_egg')
advancement('child_talk', 'find_village', 'goblinraiders:goblin_child_spawn_egg')
# Worker children from the king.
advancement('artel', 'vassal', 'goblinraiders:goblin_child_spawn_egg')
advancement('harvest', 'artel', 'minecraft:wheat')
advancement('haul', 'artel', 'minecraft:iron_pickaxe')
advancement('diamond', 'haul', 'minecraft:diamond', frame='goal')
# The peaceful villages and the sage.
advancement('hamlet', 'root', 'minecraft:wheat', located('#goblinraiders:goblin_hamlets'))
advancement('settler_talk', 'hamlet', 'goblinraiders:goblin_villager_spawn_egg')
advancement('settler_trade', 'settler_talk', 'goblinraiders:goblin_coin')
advancement('new_house', 'hamlet', 'goblinraiders:building_site', frame='goal')
advancement('sage', 'root', 'minecraft:book')
advancement('prophecy', 'sage', 'minecraft:ender_eye')
advancement('blessing', 'sage', 'minecraft:amethyst_shard')
advancement('quest', 'hamlet', 'minecraft:writable_book')
advancement('right_hand', 'quest', 'minecraft:bell', frame='goal')

print('resources written under', R)
