"""Goblin building library shared by all village types (see goblin_village.py).

Every building is drawn into its own small World with the door facing south (+z), its center at x = z = 0 and
y = 0 as the ground/floor layer. Villages paste them rotated so the door faces the plaza. Blocks that need block
entity data (loot chests, the stash) and goblins are kept next to the World and rotated along with it.
"""
import math
import os
import sys
from pathlib import Path

SCHEM = Path(os.environ.get('SCHEMATICS_DIR', r'C:\Users\yurig\.claude\minecraft\schematics'))
os.environ['MC_VERSION'] = '1.21.1'
sys.path.insert(0, str(SCHEM))
from lib import AIR, B, Mix, World, prand, roofs  # noqa: E402
from lib.world import ellipse_points  # noqa: E402

HUT_LOOT = 'goblinraiders:chests/goblin_hut'
STASH_LOOT = 'goblinraiders:chests/goblin_stash'
MARKET_LOOT = 'goblinraiders:chests/goblin_market'
ARMORY_LOOT = 'goblinraiders:chests/goblin_armory'
STASH = 'STASH'          # special marker: the placeholder barrel becomes the goblin stash on export
SITE = 'SITE'            # special marker: the placeholder fence gate becomes a building site stake on export
CRATE = 'CRATE'          # special marker: the placeholder barrel becomes a storehouse crate on export
BUILDER_LOOT = 'goblinraiders:chests/hamlet_builder'
KITCHEN_LOOT = 'goblinraiders:chests/hamlet_kitchen'
SAGE_LOOT = 'goblinraiders:chests/sage_tent'

LOGS = Mix({'spruce_log[axis=y]': 3, 'dark_oak_log[axis=y]': 2, 'stripped_spruce_log[axis=y]': 1}, salt='logs', clump=1.2)
MUD_WALL = Mix({'mud_bricks': 5, 'packed_mud': 3, 'mossy_cobblestone': 1}, salt='mud', clump=1.6)
STONE_WALL = Mix({'cobblestone': 4, 'mossy_cobblestone': 3, 'stone_bricks': 2, 'mossy_stone_bricks': 2}, salt='stone', clump=1.4)
FOOTING = Mix({'cobblestone': 3, 'mossy_cobblestone': 3}, salt='footing')
ROOF = {'spruce': 3, 'dark_oak': 2, 'mossy_cobblestone': 1}
MOSSY_ROOF = {'spruce': 2, 'dark_oak': 1, 'mossy_cobblestone': 2}
FLOOR = Mix({'spruce_planks': 3, 'packed_mud': 2}, salt='floor')

G = 'goblinraiders:'


class Canvas:
    """A World plus the extras that do not fit in it: block entity data and goblins."""

    def __init__(self, seed):
        self.seed = seed
        self.w = World(seed)
        self.special = {}      # (x, y, z) -> (state override or None or STASH, block entity nbt)
        self.entities = []     # (x, y, z, entity id, extra nbt)

    def chest(self, x, y, z, facing, loot=HUT_LOOT, kind='chest'):
        self.w.set(x, y, z, B(kind, facing=facing))
        self.special[(x, y, z)] = (None, {'id': f'minecraft:{kind}', 'LootTable': loot})

    def stash(self, x, y, z, facing):
        self.w.set(x, y, z, B('barrel', facing=facing))
        self.special[(x, y, z)] = (STASH, {'id': G + 'goblin_stash', 'LootTable': STASH_LOOT})

    def goblin(self, x, y, z, kind, **extra):
        self.entities.append((x, y, z, G + kind, extra))

    def lantern_post(self, x, z, height=2):
        for y in range(1, height + 1):
            self.w.set(x, y, z, 'spruce_fence')
        self.w.set(x, height + 1, z, B('lantern', hanging=False))

    def hanging_lantern(self, x, y, z):
        """Lantern right under the first block above (x, y, z)."""
        top = y
        while self.w.is_air(x, top + 1, z) and top < y + 8:
            top += 1
        self.w.set(x, top, z, B('lantern', hanging=True))

    def skull(self, x, y, z, rotation=0):
        self.w.set(x, y, z, B('skeleton_skull', rotation=rotation % 16))


class Building(Canvas):
    def __init__(self, seed, door=(0, 0)):
        super().__init__(seed)
        self.door = door       # the ground cell right in front of the door, local coordinates

    def footprint(self):
        x0, _, z0, x1, _, z1 = self.w.bounds()
        return x0, z0, x1, z1


def wall_to_roof(w, roof, cells, from_y, material, post=None, posts=()):
    """Closes the gap between a wall top and a sloped roof above it (sheds rise towards the back)."""
    for (x, z) in cells:
        under = roof.underside(x, z)
        if under is None:
            continue
        mat = post if (x, z) in posts and post else material
        for y in range(from_y, under + 1):
            if w.is_air(x, y, z):
                w.set(x, y, z, mat)


def rot(x, z, k):
    """Same rotation as World.paste: k quarter turns, (x, z) -> (-z, x) each."""
    for _ in range(k % 4):
        x, z = -z, x
    return x, z


# ================================================================ buildings

def chief_hall(seed, half_w=5, half_d=4, stone=False, guard=True, stash=True):
    """Rectangular hall of the chief with the stash, a throne and some loot. The door is 3 wide."""
    b = Building(seed)
    x0, x1, z0, z1, top = -half_w, half_w, -half_d, half_d, 4
    walls = STONE_WALL if stone else MUD_WALL
    w = b.w
    w.fill(x0, 0, z0, x1, 0, z1, FOOTING)
    w.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, Mix({'spruce_planks': 4, 'dark_oak_planks': 1}, salt='hfloor'))
    w.walls(x0, 1, z0, x1, 1, z1, FOOTING)
    w.walls(x0, 2, z0, x1, top, z1, walls)
    posts = sorted({x0, -2, 2, x1})
    for x in posts:
        for z in (z0, z1):
            w.fill(x, 1, z, x, top, z, B('dark_oak_log', axis='y'))
    for x in (x0, x1):
        w.fill(x, 1, 0, x, top, 0, B('dark_oak_log', axis='y'))
    for z in (z0, z1):
        w.fill(x0, top, z, x1, top, z, B('stripped_dark_oak_log', axis='x'))
    for x in (x0, x1):
        w.fill(x, top, z0 + 1, x, top, z1 - 1, B('stripped_dark_oak_log', axis='z'))
    w.fill(x0 + 1, 1, z0 + 1, x1 - 1, top, z1 - 1, AIR)
    for x in (-2, 2):
        w.fill(x, top, z0 + 1, x, top, z1 - 1, B('stripped_dark_oak_log', axis='z'))
    roof = roofs.gable(x0, z0, x1, z1, top, axis='x', pitch=2, overhang=1, gable_overhang=1)
    roof.build(w, ROOF, verge='dark_oak', ridge='slab')
    for gx in (x0, x1):
        roof.fill_gable(w, 'x', walls, top, gx, (z0, z1))
    # Door, barred windows, skulls over the door.
    w.fill(-1, 1, z1, 1, 3, z1, AIR)
    for x in (x0 + 2, x1 - 2):
        if abs(x) > 2:
            w.set(x, 2, z1, 'iron_bars')
            w.set(x, 2, z0, 'iron_bars')
    for z in (z0 + 2, z1 - 2):
        w.set(x0, 2, z, 'iron_bars')
        w.set(x1, 2, z, 'iron_bars')
    b.skull(-2, top + 1, z1, 0)
    b.skull(2, top + 1, z1, 0)
    # Interior: the stash and the throne on a dais at the back, loot around.
    w.fill(-1, 0, z0 + 1, 1, 0, z0 + 2, 'mossy_stone_bricks')
    if stash:
        b.stash(x0 + 2, 1, z0 + 1, 'south')
    else:
        b.chest(x0 + 2, 1, z0 + 1, 'south')
    w.set(0, 1, z0 + 1, B('dark_oak_stairs', facing='north'))
    w.set(-1, 1, z0 + 1, B('dark_oak_trapdoor', facing='west', half='bottom', open=True))
    w.set(1, 1, z0 + 1, B('dark_oak_trapdoor', facing='east', half='bottom', open=True))
    w.fill(0, 1, z0 + 3, 0, 1, z1 - 1, 'green_carpet')
    b.chest(x1 - 2, 1, z0 + 1, 'south')
    w.set(x1 - 1, 1, z0 + 1, 'barrel[facing=up]')
    w.set(x1 - 1, 2, z0 + 1, 'barrel[facing=up]')
    w.set(x1 - 1, 1, z0 + 2, 'hay_block')
    w.set(x0 + 1, 1, z0 + 1, 'barrel[facing=up]')
    w.set(x0 + 1, 1, z1 - 1, 'crafting_table')
    w.set(x1 - 1, 1, z1 - 1, B('smoker', facing='west'))
    w.set(x0 + 1, 1, 0, 'cauldron')
    for x in (-2, 2):
        for z in (z0 + 2, z1 - 2):
            b.hanging_lantern(x, top - 1, z)
    b.lantern_post(-3, z1 + 2)
    b.lantern_post(3, z1 + 2)
    if guard:
        b.goblin(2, 1, z1 + 2, 'ruthless_goblin')
    b.door = (0, z1 + 1)
    return b


def round_hut(seed, r=3, interior='sleep', roof=ROOF, wall_top=4):
    """Round mud hut with a steep cone roof; the door faces south."""
    b = Building(seed)
    w = b.w
    fill = ellipse_points(0, 0, r, r, filled=True)
    rim = set(ellipse_points(0, 0, r, r, filled=False))
    for (x, z) in fill:
        w.set(x, 0, z, FOOTING if (x, z) in rim else FLOOR)
        if (x, z) in rim:
            w.set(x, 1, z, FOOTING)
            w.fill(x, 2, z, x, wall_top, z, MUD_WALL)
        else:
            w.fill(x, 1, z, x, wall_top, z, AIR)
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if (dx * r, dz * r) in rim:
            w.fill(dx * r, 1, dz * r, dx * r, wall_top, dz * r, B('spruce_log', axis='y'))
    w.fill(0, 1, r, 0, 2, r, AIR)
    w.set(0, 3, r, B('stripped_spruce_log', axis='x'))
    for (wx, wz) in ((r, 0), (-r, 0)):
        w.set(wx, 2, wz, AIR)
    cone = roofs.cone(0, 0, r, wall_top, pitch=3, overhang=1)
    cone.build(w, roof)
    peak = max(int(cone.surface(0, 0)), wall_top + 1)
    w.set(0, peak, 0, 'spruce_fence')
    b.skull(0, peak + 1, 0, int(prand(seed, 'skull', 0, 0, 0) * 16))
    under = cone.underside(0, 0)
    if under is not None and under > wall_top:
        w.set(0, under, 0, 'chain[axis=y]')
        w.set(0, under - 1, 0, B('lantern', hanging=True))
    else:
        w.set(0, wall_top, 0, B('lantern', hanging=True))
    INTERIORS[interior](b, r)
    b.door = (0, r + 1)
    return b


def _sleep(b, r):
    b.chest(0, 1, -r + 1, 'south')
    b.w.set(1, 1, -r + 1, 'hay_block')
    b.w.set(r - 1, 1, 0, 'crafting_table')
    b.w.set(-r + 1, 1, 0, 'hay_block')
    b.w.set(-r + 1, 1, -1, 'hay_block')


def _shaman(b, r):
    b.chest(0, 1, -r + 1, 'south', kind='barrel')
    b.w.set(-r + 1, 1, 0, 'cauldron')
    b.w.set(r - 1, 1, 0, 'brewing_stand')
    b.w.set(-1, 1, -r + 1, 'potted_red_mushroom')
    b.w.set(1, 1, -r + 1, 'potted_brown_mushroom')
    b.goblin(0, 1, 0, 'goblin_shaman')


def _storage(b, r):
    b.chest(0, 1, -r + 1, 'south')
    b.w.set(1, 1, -r + 1, 'barrel[facing=up]')
    b.w.set(-1, 1, -r + 1, 'barrel[facing=up]')
    b.w.set(-1, 2, -r + 1, 'barrel[facing=up]')
    b.w.set(r - 1, 1, 0, B('smoker', facing='west'))
    b.w.set(-r + 1, 1, 0, 'hay_block')


def _chief(b, r):
    """Camp chief: the stash lives in his hut."""
    b.stash(0, 1, -r + 1, 'south')
    b.w.set(1, 1, -r + 1, 'barrel[facing=up]')
    b.w.set(-1, 1, -r + 1, B('dark_oak_stairs', facing='north'))
    b.w.set(r - 1, 1, 0, 'hay_block')
    b.chest(-r + 1, 1, 0, 'east')


def _elder(b, r):
    """The elder's home: books, candles, potions and a fire pit."""
    w = b.w
    for x in range(-2, 3):
        if (x, -r + 1) != (0, -r + 1):
            w.set(x, 1, -r + 1, 'bookshelf')
            w.set(x, 2, -r + 1, 'bookshelf' if abs(x) == 2 else B('candle', candles=1 + (x + 2) % 4, lit=True))
    w.set(0, 1, -r + 1, B('lectern', facing='south'))
    w.set(-r + 1, 1, 0, 'cauldron')
    w.set(r - 1, 1, 0, 'brewing_stand')
    w.set(r - 1, 1, -1, 'potted_fern')
    w.set(-r + 1, 1, 1, 'potted_red_mushroom')
    w.set(r - 1, 1, 1, B('candle', candles=3, lit=True))
    w.set(-1, 1, 1, 'brown_carpet')
    w.set(1, 1, 1, 'brown_carpet')
    b.chest(-r + 1, 1, -1, 'east')
    b.goblin(0, 1, 0, 'goblin_elder')


INTERIORS = {'sleep': _sleep, 'shaman': _shaman, 'storage': _storage, 'chief': _chief, 'elder': _elder}


def elder_hut(seed):
    return round_hut(seed, r=4, interior='elder', roof=MOSSY_ROOF, wall_top=4)


def tent(seed, color='green'):
    """A-frame wool tent for camps, open at the front."""
    b = Building(seed)
    w = b.w
    wool = Mix({f'{color}_wool': 4, 'brown_wool': 1}, salt='tent' + color)
    for z in range(-2, 3):
        w.set(-2, 0, z, 'coarse_dirt')
        w.set(2, 0, z, 'coarse_dirt')
        for x in range(-1, 2):
            w.set(x, 0, z, 'coarse_dirt' if prand(seed, 'tf', x, 0, z) < 0.6 else 'podzol')
        w.set(-2, 1, z, wool)
        w.set(2, 1, z, wool)
        w.set(-1, 2, z, wool)
        w.set(1, 2, z, wool)
        w.set(0, 3, z, wool)
        w.fill(-1, 1, z, 1, 1, z, AIR)
        w.set(0, 2, z, AIR)
    w.fill(-1, 1, -2, 1, 1, -2, wool)
    w.set(0, 2, -2, wool)
    w.fill(0, 1, -3, 0, 3, -3, 'spruce_fence')          # the back pole
    w.set(0, 3, 3, 'spruce_fence')                      # in front only the ridge pole sticks out: the doorway stays free
    w.set(-1, 1, -1, 'hay_block')
    w.set(-1, 1, 0, 'brown_carpet')
    b.chest(1, 1, -1, 'west')
    b.door = (0, 4)
    return b


def watchtower(seed, height=8):
    """3x3 log tower with a ladder, a railed platform and a hip roof; a small goblin keeps watch."""
    b = Building(seed)
    w = b.w
    for x in range(-1, 2):
        for z in range(-1, 2):
            if x in (-1, 1) or z in (-1, 1):
                w.fill(x, 1, z, x, height - 1, z, LOGS)
            w.set(x, 0, z, FOOTING)
    w.fill(0, 1, 1, 0, 2, 1, AIR)                       # entrance on the south side
    w.fill(0, 1, 0, 0, height, 0, B('ladder', facing='south'))
    w.set(0, 1, -1, B('spruce_log', axis='y'))
    w.fill(-2, height, -2, 2, height, 2, 'spruce_planks')
    w.set(0, height, 0, B('ladder', facing='south'))
    for x in range(-2, 3):
        for z in range(-2, 3):
            edge = abs(x) == 2 or abs(z) == 2
            corner = abs(x) == 2 and abs(z) == 2
            if corner:
                w.fill(x, height + 1, z, x, height + 3, z, 'spruce_fence')
            elif edge:
                w.set(x, height + 1, z, 'spruce_fence')
    roofs.hip(-2, -2, 2, 2, height + 3, pitch=2, overhang=1).build(w, ROOF)
    w.set(1, height + 1, 1, B('lantern', hanging=False))
    b.goblin(1, height + 1, -1, 'small_goblin')
    b.door = (0, 2)
    return b


def workshop(seed, worker='neutral_goblin'):
    """Open smithy shed: anvil, smithing table, blast furnace, grindstone, water."""
    b = Building(seed)
    w = b.w
    x0, x1, z0, z1, top = -4, 3, -2, 2, 3
    w.fill(x0, 0, z0, x1, 0, z1, Mix({'cobblestone': 3, 'gravel': 2, 'andesite': 1}, salt='shop'))
    for x in (x0, 0, x1):
        for z in (z0, z1):
            w.fill(x, 1, z, x, top, z, B('dark_oak_log', axis='y'))
    w.fill(x0 + 1, 1, z0, x1 - 1, 2, z0, FOOTING)
    w.fill(x0, 1, z0 + 1, x0, 2, z1 - 1, FOOTING)
    w.fill(x0, top, z0, x1, top, z0, B('stripped_dark_oak_log', axis='x'))
    w.fill(x0, top, z1, x1, top, z1, B('stripped_dark_oak_log', axis='x'))
    w.fill(x0 + 1, 1, z0 + 1, x1 - 1, 2, z1, AIR)
    roof = roofs.shed(x0, z0, x1, z1, top, low='south', pitch=1, overhang=1, side_overhang=1)
    roof.build(w, 'spruce')
    for x in (x0, x1):
        w.fill(x, top, z0 + 1, x, top, z1 - 1, B('stripped_dark_oak_log', axis='z'))
    posts = {(x, z) for x in (x0, 0, x1) for z in (z0, z1)}
    back = [(x, z0) for x in range(x0, x1 + 1)]
    sides = [(x, z) for x in (x0, x1) for z in range(z0, z1 + 1)]
    wall_to_roof(w, roof, back + sides, top + 1, 'spruce_planks', post=B('dark_oak_log', axis='y'), posts=posts)
    w.set(x0 + 1, 1, z0 + 1, B('anvil', facing='east'))
    w.set(x0 + 2, 1, z0 + 1, 'smithing_table')
    w.set(x0 + 3, 1, z0 + 1, B('blast_furnace', facing='south'))
    w.set(x0 + 4, 1, z0 + 1, B('grindstone', face='floor', facing='south'))
    w.set(x1 - 1, 1, z0 + 1, 'water_cauldron[level=3]')
    w.set(x0 + 1, 1, z1 - 1, 'barrel[facing=up]')
    b.chest(x0 + 1, 1, z1 - 2, 'east', loot=ARMORY_LOOT, kind='barrel')
    b.hanging_lantern(-2, top - 1, 0)
    if worker:
        b.goblin(1, 1, 1, worker)
    b.door = (0, z1 + 1)
    return b


def farm(seed, crops=('potatoes', 'beetroots', 'carrots')):
    """Fenced crop plot with a water trough in the middle; the gate faces south."""
    b = Building(seed)
    w = b.w
    for x in range(-3, 4):
        for z in range(-3, 4):
            edge = abs(x) == 3 or abs(z) == 3
            if edge:
                w.set(x, 0, z, 'coarse_dirt')
                if (x, z) != (0, 3):
                    w.set(x, 1, z, 'spruce_fence')
            elif x == 0 and z == 0:
                w.set(x, 0, z, 'water')
                w.set(x, 1, z, 'spruce_trapdoor[facing=north,half=bottom,open=false]')
            else:
                w.set(x, 0, z, 'farmland[moisture=7]')
                crop = crops[0] if x < 0 else crops[2] if x > 0 else crops[1]
                age = int(prand(seed, 'crop', x, 0, z) * (4 if crop == 'beetroots' else 8))
                w.set(x, 1, z, f'{crop}[age={age}]')
    w.set(0, 1, 3, B('spruce_fence_gate', facing='south'))
    w.set(3, 2, 3, 'jack_o_lantern[facing=south]')
    b.door = (0, 4)
    return b


def cage(seed):
    b = Building(seed)
    w = b.w
    for x in range(-1, 2):
        for z in range(-1, 2):
            w.set(x, 0, z, 'cobblestone')
            if abs(x) == 1 or abs(z) == 1:
                w.fill(x, 1, z, x, 2, z, 'iron_bars')
            w.set(x, 3, z, 'dark_oak_slab[type=bottom]')
    w.set(0, 1, 0, 'cobweb')
    w.set(-1, 0, -1, 'mossy_cobblestone')
    b.door = (0, 2)
    return b


def market_stall(seed, color='red'):
    """3x3 stall under a striped wool canopy; goods in the back row."""
    b = Building(seed)
    w = b.w
    for x in range(-1, 2):
        for z in range(-1, 2):
            w.set(x, 0, z, 'spruce_planks' if z < 1 else 'coarse_dirt')
            w.set(x, 3, z, f'{color}_wool' if (x + z) % 2 == 0 else 'white_wool')
    for x in (-1, 1):
        for z in (-1, 1):
            w.fill(x, 1, z, x, 2, z, 'spruce_fence')
    goods = ['hay_block', 'melon', 'pumpkin', 'barrel[facing=up]', 'composter[level=6]']
    w.set(0, 1, -1, goods[int(prand(seed, 'goods', 0, 0, 0) * len(goods))])
    b.chest(1, 1, -1, 'south', loot=MARKET_LOOT, kind='barrel')
    w.set(0, 1, 0, 'spruce_slab[type=top]')
    w.set(0, 2, 0, 'potted_brown_mushroom' if prand(seed, 'pot', 0, 0, 0) < 0.5 else 'candle[candles=2,lit=true]')
    b.door = (0, 2)
    return b


def trader_shop(seed):
    """The trader's shop: a counter under a striped awning, shelves of barrels, and the trader himself."""
    b = Building(seed)
    w = b.w
    x0, x1, z0, z1, top = -3, 3, -2, 2, 3
    w.fill(x0, 0, z0, x1, 0, z1, 'spruce_planks')
    w.walls(x0, 1, z0, x1, top, z0, MUD_WALL)
    for x in (x0, x1):
        w.fill(x, 1, z0, x, top, z1, B('dark_oak_log', axis='y'))
    w.fill(x0 + 1, 1, z0 + 1, x1 - 1, top, z1, AIR)
    for x in (x0, x1):
        w.fill(x, 1, z0 + 1, x, 2, z1 - 1, 'spruce_planks')
    roof = roofs.shed(x0, z0, x1, z1, top, low='south', pitch=1, overhang=1, side_overhang=1)
    roof.build(w, 'spruce')
    wall_to_roof(w, roof, [(x, z0) for x in range(x0, x1 + 1)], top + 1, MUD_WALL)
    wall_to_roof(w, roof, [(x, z) for x in (x0, x1) for z in range(z0, z1 + 1)], top, 'spruce_planks',
                 post=B('dark_oak_log', axis='y'), posts={(x0, z1), (x1, z1)})
    for x in range(x0 - 1, x1 + 2):
        w.set(x, top, z1 + 2, 'red_wool' if x % 2 == 0 else 'yellow_wool')
        w.set(x, top - 1, z1 + 2, AIR)
    for x in (x0 - 1, x1 + 1):
        w.fill(x, 1, z1 + 2, x, top - 1, z1 + 2, 'spruce_fence')
    # Counter with a gap on the left for the trader.
    for x in range(x0 + 2, x1):
        w.set(x, 1, z1 - 1, B('spruce_stairs', facing='north', half='top'))
    w.set(x1 - 1, 2, z1 - 1, 'potted_red_mushroom')
    w.set(0, 2, z1 - 1, B('candle', candles=3, lit=True))
    # Shelves along the back wall.
    b.chest(x0 + 1, 1, z0 + 1, 'south', loot=MARKET_LOOT)
    w.set(x0 + 2, 1, z0 + 1, 'barrel[facing=up]')
    w.set(x0 + 2, 2, z0 + 1, 'barrel[facing=up]')
    w.set(0, 1, z0 + 1, 'decorated_pot')
    b.chest(x1 - 2, 1, z0 + 1, 'south', loot=MARKET_LOOT, kind='barrel')
    w.set(x1 - 1, 1, z0 + 1, B('anvil', facing='north'))
    b.hanging_lantern(0, top - 1, 0)
    bell_y = top - 1
    while w.is_air(x1 - 1, bell_y + 1, z1 - 1) and bell_y < top + 4:
        bell_y += 1
    w.set(x1 - 1, bell_y, z1 - 1, B('bell', attachment='ceiling', facing='north'))
    b.goblin(0, 1, 0, 'goblin_trader')
    b.door = (0, z1 + 3)
    return b


def longhouse(seed, half_w=6):
    """Barracks: long hall with hay beds and an armory chest."""
    b = Building(seed)
    w = b.w
    x0, x1, z0, z1, top = -half_w, half_w, -3, 3, 4
    w.fill(x0, 0, z0, x1, 0, z1, FOOTING)
    w.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, 'spruce_planks')
    w.walls(x0, 1, z0, x1, 1, z1, FOOTING)
    w.walls(x0, 2, z0, x1, top, z1, Mix({'spruce_planks': 3, 'mud_bricks': 2, 'stripped_spruce_log[axis=y]': 1}, salt='long'))
    for x in range(x0, x1 + 1, 3):
        for z in (z0, z1):
            w.fill(x, 1, z, x, top, z, B('dark_oak_log', axis='y'))
    w.fill(x0 + 1, 1, z0 + 1, x1 - 1, top, z1 - 1, AIR)
    roof = roofs.gable(x0, z0, x1, z1, top, axis='x', pitch=2, overhang=1, gable_overhang=1)
    roof.build(w, ROOF, verge='dark_oak', ridge='slab')
    for gx in (x0, x1):
        roof.fill_gable(w, 'x', 'spruce_planks', top, gx, (z0, z1))
    w.fill(-1, 1, z1, 1, 3, z1, AIR)
    for x in range(x0 + 1, x1, 2):
        if abs(x) > 1:
            w.set(x, 2, z0, 'spruce_trapdoor[facing=north,half=top,open=true]')
    for x in range(x0 + 1, x1):
        if abs(x) > 1:
            w.set(x, 1, z0 + 1, 'hay_block' if x % 2 else 'red_carpet')
    b.chest(x0 + 1, 1, z1 - 1, 'north', loot=ARMORY_LOOT)
    b.chest(x1 - 1, 1, z1 - 1, 'north', loot=ARMORY_LOOT, kind='barrel')
    w.set(x1 - 1, 1, 0, 'barrel[facing=up]')
    w.set(x0 + 1, 1, 0, B('grindstone', face='floor', facing='east'))
    for x in range(x0 + 2, x1 - 1, 4):
        b.hanging_lantern(x, top - 1, 0)
    b.goblin(-2, 1, 1, 'ruthless_goblin')
    b.door = (0, z1 + 1)
    return b


def giant_arena(seed, r=5):
    """A ring of stone and logs where a giant goblin lives, with bones and his cauldron."""
    b = Building(seed)
    w = b.w
    for (x, z) in ellipse_points(0, 0, r, r, filled=True):
        w.set(x, 0, z, Mix({'coarse_dirt': 4, 'gravel': 2, 'packed_mud': 2}, salt='arena'))
        w.fill(x, 1, z, x, 5, z, AIR)
    for (x, z) in ellipse_points(0, 0, r, r, filled=False):
        if z > r - 2 and abs(x) <= 1:
            continue
        w.set(x, 0, z, FOOTING)
        w.set(x, 1, z, 'mossy_cobblestone_wall')
        w.set(x, 2, z, 'spruce_fence')
    for (dx, dz) in ((1, 0), (-1, 0), (0, -1), (0.7, 0.7), (-0.7, 0.7), (0.7, -0.7), (-0.7, -0.7)):
        x, z = round(dx * r), round(dz * r)
        w.fill(x, 1, z, x, 4, z, B('dark_oak_log', axis='y'))
        b.skull(x, 5, z, int(math.degrees(math.atan2(-x, z)) / 22.5))
    w.set(2, 1, -2, 'water_cauldron[level=3]')
    w.set(-2, 1, -2, 'bone_block[axis=y]')
    w.set(-3, 1, -1, 'bone_block[axis=x]')
    w.set(-2, 2, -2, 'bone_block[axis=z]')
    w.set(3, 1, 1, 'hay_block')
    b.goblin(0, 1, 0, 'giant_goblin')
    b.door = (0, r + 1)
    return b


def well(seed):
    b = Building(seed)
    w = b.w
    for x in range(-1, 2):
        for z in range(-1, 2):
            if x == 0 and z == 0:
                w.set(0, 0, 0, 'water')
            else:
                w.set(x, 0, z, 'cobblestone')
                w.set(x, 1, z, 'mossy_cobblestone' if prand(seed, 'well', x, 0, z) < 0.5 else 'cobblestone')
    for x in (-1, 1):
        w.fill(x, 2, 0, x, 3, 0, 'spruce_fence')
    w.fill(-1, 4, -1, 1, 4, 1, 'spruce_slab[type=bottom]')
    w.set(0, 3, 0, 'chain[axis=y]')
    w.set(0, 2, 0, B('lantern', hanging=True))
    b.door = (0, 2)
    return b


def loot_heap(seed):
    b = Building(seed)
    w = b.w
    w.set(-1, 1, -1, 'barrel[facing=up]')
    w.set(-1, 2, -1, 'barrel[facing=up]')
    w.set(0, 1, -1, 'hay_block')
    w.set(1, 1, -1, 'bone_block[axis=y]')
    w.set(-1, 1, 0, 'composter[level=4]')
    b.chest(1, 1, 0, 'west')
    w.set(0, 1, 0, 'brown_carpet')
    for x in range(-1, 2):
        for z in range(-1, 1):
            w.set(x, 0, z, 'coarse_dirt')
    b.door = (0, 2)
    return b


def totem(w, x, z, facing):
    """Totem pole: wall, log, glowing face and a skull on top."""
    w.set(x, 0, z, 'mossy_cobblestone')
    w.set(x, 1, z, 'mossy_cobblestone_wall')
    w.set(x, 2, z, B('dark_oak_log', axis='y'))
    w.set(x, 3, z, B('jack_o_lantern', facing=facing))
    w.set(x, 4, z, B('skeleton_skull', rotation={'south': 0, 'west': 4, 'north': 8, 'east': 12}[facing]))


def throne_hall(seed):
    """The goblin king's great hall: stone and dark oak, a gold throne on a dais, banners, the royal treasury.
    The king (three blocks tall) and two guards spawn inside; the door is five blocks high so he fits through."""
    b = Building(seed)
    w = b.w
    x0, x1, z0, z1, top = -8, 8, -7, 7, 7
    stone = Mix({'stone_bricks': 4, 'mossy_stone_bricks': 3, 'cracked_stone_bricks': 1}, salt='throne')
    w.fill(x0, 0, z0, x1, 0, z1, FOOTING)
    w.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, Mix({'dark_oak_planks': 5, 'spruce_planks': 1}, salt='tfloor'))
    w.walls(x0, 1, z0, x1, 3, z1, stone)
    w.walls(x0, 4, z0, x1, top, z1, Mix({'dark_oak_planks': 3, 'mud_bricks': 2}, salt='tupper'))
    for x in range(x0, x1 + 1, 4):
        for z in (z0, z1):
            w.fill(x, 1, z, x, top, z, B('dark_oak_log', axis='y'))
    for z in range(z0, z1 + 1, 7):
        for x in (x0, x1):
            w.fill(x, 1, z, x, top, z, B('dark_oak_log', axis='y'))
    for z in (z0, z1):
        w.fill(x0, top, z, x1, top, z, B('stripped_dark_oak_log', axis='x'))
    for x in (x0, x1):
        w.fill(x, top, z0 + 1, x, top, z1 - 1, B('stripped_dark_oak_log', axis='z'))
    w.fill(x0 + 1, 1, z0 + 1, x1 - 1, top, z1 - 1, AIR)
    roof = roofs.gable(x0, z0, x1, z1, top, axis='x', pitch=2, overhang=1, gable_overhang=1)
    roof.build(w, ROOF, verge='dark_oak', ridge='slab')
    for gx in (x0, x1):
        roof.fill_gable(w, 'x', 'dark_oak_planks', top, gx, (z0, z1))
    # Inner pillars and the beams across.
    for x in (-5, 5):
        for z in (-4, 0, 4):
            w.fill(x, 1, z, x, top - 1, z, B('dark_oak_log', axis='y'))
        w.fill(x, top - 1, z0 + 1, x, top - 1, z1 - 1, B('stripped_dark_oak_log', axis='z'))
    # Door five blocks high, windows with bars.
    w.fill(-1, 1, z1, 1, 5, z1, AIR)
    for x in (-6, -3, 3, 6):
        w.fill(x, 3, z0, x, 4, z0, 'iron_bars')
    for z in (-4, 4):
        w.fill(x0, 3, z, x0, 4, z, 'iron_bars')
        w.fill(x1, 3, z, x1, 4, z, 'iron_bars')
    for x in (-3, 3):
        b.skull(x, top + 1, z1, 0)
    # Red carpet from the door to the dais, the dais and the throne.
    w.fill(0, 1, z0 + 4, 0, 1, z1 - 1, 'red_carpet')
    w.fill(-1, 1, z0 + 4, -1, 1, z1 - 1, 'red_carpet')
    w.fill(1, 1, z0 + 4, 1, 1, z1 - 1, 'red_carpet')
    w.fill(-3, 1, z0 + 1, 3, 1, z0 + 3, 'polished_andesite')
    w.fill(-2, 2, z0 + 1, 2, 2, z0 + 1, 'gold_block')
    w.set(0, 2, z0 + 2, B('dark_oak_stairs', facing='north'))
    w.set(-1, 2, z0 + 2, B('dark_oak_trapdoor', facing='west', half='bottom', open=True))
    w.set(1, 2, z0 + 2, B('dark_oak_trapdoor', facing='east', half='bottom', open=True))
    w.fill(-1, 3, z0 + 1, 1, 4, z0 + 1, 'gold_block')
    w.set(0, 5, z0 + 1, 'gold_block')
    w.set(-3, 2, z0 + 1, B('jack_o_lantern', facing='south'))
    w.set(3, 2, z0 + 1, B('jack_o_lantern', facing='south'))
    # Banners on the walls, the treasury chests, pots and lanterns.
    for z in (-2, 2):
        w.set(x0 + 1, 4, z, B('green_wall_banner', facing='east'))
        w.set(x1 - 1, 4, z, B('green_wall_banner', facing='west'))
    b.chest(x0 + 1, 1, z0 + 1, 'south', loot='goblinraiders:chests/goblin_royal')
    b.chest(x1 - 1, 1, z0 + 1, 'south', loot='goblinraiders:chests/goblin_royal')
    for (x, z) in ((x0 + 1, z0 + 2), (x1 - 1, z0 + 2), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
        w.set(x, 1, z, 'decorated_pot')
    for x in (-5, 5):
        for z in (-2, 2):
            b.hanging_lantern(x, top - 2, z)
    b.hanging_lantern(0, top - 1, 0)
    b.goblin(0, 2, -2, 'goblin_king')
    b.goblin(-3, 1, 2, 'ruthless_goblin')
    b.goblin(3, 1, 2, 'ruthless_goblin')
    b.lantern_post(-3, z1 + 2)
    b.lantern_post(3, z1 + 2)
    b.door = (0, z1 + 1)
    return b


# ================================================================ the peaceful village and the sage

# Trades of the settlers (GoblinVillager "Job", the ordinal of WorkJob).
JOB = {'farmer': 1, 'miner': 2, 'lumberjack': 3, 'builder': 4, 'cook': 5}
ROCK = Mix({'stone': 6, 'andesite': 2, 'tuff': 1, 'diorite': 1}, salt='rock', clump=1.5)


def settler(b, x, y, z, job, female=None):
    """A settler with its trade; a woman or a man at random unless given."""
    if female is None:
        female = prand(b.seed, 'female', x, y, z) < 0.5
    b.goblin(x, y, z, 'goblin_villager', Job=JOB[job], Female=bool(female))


def builder_yard(seed):
    """The builders' yard: a lean-to over the workbench, the materials chest, a log pile, planks and scaffolding."""
    b = Building(seed)
    w = b.w
    x0, x1, z0, z1, top = -3, 3, -2, 2, 3
    w.fill(x0, 0, z0, x1, 0, z1, Mix({'coarse_dirt': 3, 'dirt_path': 2, 'oak_planks': 1}, salt='yard'))
    for x in (x0, x1):
        for z in (z0, z0 + 1):
            w.fill(x, 1, z, x, top, z, B('spruce_log', axis='y'))
    w.fill(x0, top + 1, z0, x1, top + 1, z0 + 1, 'spruce_slab[type=bottom]')
    w.set(-1, 1, z0, 'crafting_table')
    w.set(0, 1, z0, B('stonecutter', facing='south'))
    b.chest(1, 1, z0, 'south', loot=BUILDER_LOOT)
    w.set(2, 1, z0, 'barrel[facing=up]')
    w.fill(x0, 1, z1 - 1, x0 + 1, 1, z1, B('oak_log', axis='z'))
    w.set(x0, 2, z1 - 1, B('oak_log', axis='z'))
    w.fill(x1, 1, z1, x1, 1, z1, 'oak_planks')
    w.set(x1, 2, z1, 'oak_slab[type=bottom]')
    w.fill(x1 - 1, 1, z1, x1 - 1, 2, z1, 'scaffolding[bottom=false,distance=0,waterlogged=false]')
    b.hanging_lantern(0, top, z0 + 1)
    b.door = (0, z1 + 1)
    return b


def kitchen(seed):
    """An open-air kitchen under a slab roof: smoker, furnace, a cauldron of water, the pantry barrel, a table."""
    b = Building(seed)
    w = b.w
    x0, x1, z0, z1, top = -3, 3, -2, 2, 3
    w.fill(x0, 0, z0, x1, 0, z1, Mix({'packed_mud': 3, 'cobblestone': 2, 'coarse_dirt': 1}, salt='kitchen'))
    for x in (x0, x1):
        for z in (z0, z1):
            w.fill(x, 1, z, x, top, z, B('spruce_log', axis='y'))
    w.fill(x0, top + 1, z0, x1, top + 1, z1, 'spruce_slab[type=bottom]')
    w.set(-2, 1, z0, B('smoker', facing='south', lit=False))
    w.set(-1, 1, z0, B('furnace', facing='south', lit=False))
    w.set(0, 1, z0, 'water_cauldron[level=3]')
    b.chest(1, 1, z0, 'south', loot=KITCHEN_LOOT, kind='barrel')
    w.set(2, 1, z0, 'crafting_table')
    w.set(2, 1, 1, 'spruce_fence')
    w.set(2, 2, 1, 'spruce_pressure_plate')
    w.set(1, 1, 1, B('spruce_stairs', facing='east'))
    w.set(-2, 2, z0, 'potted_brown_mushroom')
    b.hanging_lantern(0, top, 0)
    b.door = (0, z1 + 1)
    return b


def hamlet_farm(seed, crops=('potatoes', 'wheat', 'carrots')):
    """A crop plot with a barrel and the composter (the farmer's job block) at the gate."""
    b = farm(seed, crops)
    b.w.set(3, 1, 4, 'barrel[facing=up]')
    b.w.set(-3, 1, 4, 'composter[level=0]')
    return b


def lumber_yard(seed):
    """Where the lumberjack keeps the logs: stacked piles, a chopping block, the fletching table (the job block)."""
    b = Building(seed)
    w = b.w
    x0, x1, z0, z1 = -3, 3, -2, 2
    w.fill(x0, 0, z0, x1, 0, z1, Mix({'coarse_dirt': 3, 'podzol': 2, 'dirt_path': 1}, salt='lumber'))
    for z in (z0, z0 + 1):
        w.fill(x0, 1, z, x0 + 2, 1, z, B('spruce_log', axis='x'))
    w.fill(x0, 2, z0, x0 + 2, 2, z0, B('spruce_log', axis='x'))
    w.fill(x1, 1, z0, x1, 1, z0 + 1, B('birch_log', axis='z'))
    w.set(1, 1, -1, B('stripped_oak_log', axis='y'))
    w.set(x1, 1, z1, 'barrel[facing=up]')
    w.set(x1 - 1, 1, z1, 'fletching_table')
    b.door = (0, z1 + 1)
    return b


def mine_adit(seed):
    """A rock outcrop with a timbered tunnel into it; the miner works by the grindstone at the end."""
    b = Building(seed)
    w = b.w
    x0, x1, z0, z1, top = -3, 3, -4, 2, 4
    cap = Mix({'grass_block': 3, 'moss_block': 1, 'coarse_dirt': 1}, salt='cap')
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rim_x, rim_z = abs(x) == 3, z in (z0, z1)
            h = top - (2 if rim_x and rim_z else 1 if rim_x or rim_z else 0)
            w.set(x, 0, z, ROCK)
            w.fill(x, 1, z, x, h, z, ROCK)
            w.set(x, h + 1, z, cap)
    w.fill(0, 1, -2, 0, 2, z1, AIR)
    for x in (-1, 1):
        w.fill(x, 1, z1, x, 2, z1, B('spruce_log', axis='y'))
    w.fill(-1, 3, z1, 1, 3, z1, B('stripped_spruce_log', axis='x'))
    w.set(0, 2, 0, B('wall_torch', facing='east'))
    w.fill(0, 1, 0, 0, 1, z1, B('rail', shape='north_south'))
    w.set(2, 1, z1 + 1, B('chest', facing='north'))
    w.set(0, 1, -2, B('grindstone', face='floor', facing='south'))   # the miner's job block at the end of the tunnel
    b.door = (0, z1 + 1)
    return b


def building_site(seed, blueprint):
    """An empty plot with a builder's stake: goblin builders put up `blueprint` here, the door where the stake faces."""
    plan = BLUEPRINTS[blueprint](seed)
    b = Building(seed)
    x0, z0, x1, z1 = plan.footprint()
    plot = Mix({'coarse_dirt': 2, 'dirt_path': 1, 'grass_block': 3}, salt='plot')
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.w.set(x, 0, z, plot)
            b.w.fill(x, 1, z, x, 3, z, AIR)
    b.w.set(0, 1, 0, B('oak_fence_gate', facing='south'))
    b.special[(0, 1, 0)] = (SITE, {'id': G + 'building_site', 'Blueprint': blueprint})
    b.door = plan.door
    b.reserve = True
    return b


def tree(w, x, z, kind, seed):
    """A small tree of natural (non-persistent) leaves on a trunk four to six logs tall."""
    h = 4 + int(prand(seed, 'th', x, 0, z) * 3)
    leaves = f'{kind}_leaves[distance=1,persistent=false,waterlogged=false]'
    for y in range(h - 1, h + 3):
        r = 2 if y <= h else 1
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                corner = abs(dx) == r and abs(dz) == r
                if corner and (y == h + 2 or prand(seed, 'tc', x + dx, y, z + dz) < 0.6):
                    continue
                if y == h + 2 and (dx, dz) != (0, 0) and abs(dx) + abs(dz) > 1:
                    continue
                if w.is_air(x + dx, y, z + dz):
                    w.set(x + dx, y, z + dz, leaves)
    for y in range(1, h + 1):
        w.set(x, y, z, B(f'{kind}_log', axis='y'))


def sage_tent(seed):
    """The sage's lone tent: a blue tent with a bedroll, books and a barrel of odds and ends; the sage sits inside."""
    b = tent(seed, 'blue')
    w = b.w
    b.special.pop((1, 1, -1), None)
    b.chest(1, 1, -1, 'west', loot=SAGE_LOOT, kind='barrel')
    w.set(1, 1, 0, 'bookshelf')
    w.set(-1, 1, 0, 'purple_carpet')
    w.set(-1, 1, 1, 'candle[candles=3,lit=true,waterlogged=false]')
    b.goblin(0, 1, 0, 'goblin_sage')
    return b


# Buildings goblin builders put up on building sites (exported as JSON blueprints by goblin_village.py).
BLUEPRINTS = {
    'hut_small': lambda s: round_hut(s, 3, 'sleep'),
    'hut_big': lambda s: round_hut(s, 4, 'sleep'),
    'garden': lambda s: farm(s, ('wheat', 'carrots', 'potatoes')),
}


BUILDINGS = {
    'hall': lambda s: chief_hall(s),
    'small_hall': lambda s: chief_hall(s, half_w=4, half_d=3),
    'keep': lambda s: chief_hall(s, half_w=6, half_d=5, stone=True),
    'great_hall': lambda s: chief_hall(s, half_w=6, half_d=4, stash=False),
    'throne_hall': throne_hall,
    'hut_sleep': lambda s: round_hut(s, 3, 'sleep'),
    'hut_shaman': lambda s: round_hut(s, 3, 'shaman'),
    'hut_storage': lambda s: round_hut(s, 3, 'storage'),
    'big_hut': lambda s: round_hut(s, 4, 'sleep'),
    'chief_hut': lambda s: round_hut(s, 3, 'chief', roof=MOSSY_ROOF),
    'elder_hut': elder_hut,
    'tent_green': lambda s: tent(s, 'green'),
    'tent_brown': lambda s: tent(s, 'brown'),
    'tent_lime': lambda s: tent(s, 'lime'),
    'tower': watchtower,
    'workshop': workshop,
    'farm': farm,
    'farm_wheat': lambda s: farm(s, ('wheat', 'beetroots', 'potatoes')),
    'cage': cage,
    'stall_red': lambda s: market_stall(s, 'red'),
    'stall_green': lambda s: market_stall(s, 'green'),
    'stall_blue': lambda s: market_stall(s, 'blue'),
    'stall_yellow': lambda s: market_stall(s, 'yellow'),
    'stall_purple': lambda s: market_stall(s, 'purple'),
    'trader_shop': trader_shop,
    'longhouse': longhouse,
    'arena': giant_arena,
    'well': well,
    'loot_heap': loot_heap,
    'builder_yard': builder_yard,
    'kitchen': kitchen,
    'hamlet_farm': hamlet_farm,
    'hamlet_farm_wheat': lambda s: hamlet_farm(s, ('wheat', 'beetroots', 'carrots')),
    'lumber_yard': lumber_yard,
    'mine_adit': mine_adit,
    'site_hut': lambda s: building_site(s, 'hut_small'),
    'site_big': lambda s: building_site(s, 'hut_big'),
    'site_garden': lambda s: building_site(s, 'garden'),
    'sage_tent': sage_tent,
}
