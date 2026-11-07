"""Peaceful goblin villages, put together like villager villages, on the land as it is.

The village grows from jigsaw pieces: the square in the middle (the headman by the notice board, the common
storehouse, a campfire), streets that follow the terrain, and along the streets the settlers' homes, the workshops of
their trades (each with its job block: composter, fletching table, grindstone, stonecutter, smoker), fields, groves and
building sites. Every piece is a template of its own; the structure goblinraiders:goblin_hamlet assembles them in the
world (pools in tools/gen_data.py: hamlet/center -> hamlet/streets, fallback hamlet/terminators -> hamlet/houses).

Every home houses one settler (its trade is rolled when it spawns and swapped for a trade the village has a job
block for); the family house also a child.

Run:  python tools/structures/goblin_hamlet.py
Writes src/main/resources/data/goblinraiders/structure/hamlet/<piece>.nbt and previews tools/previews/hamlet_<piece>.png.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import goblin_buildings as gb  # noqa: E402  (sets up the toolkit path and MC_VERSION)
import goblin_village as gv  # noqa: E402
from goblin_buildings import AIR, B, CRATE, Building, Canvas, Mix, prand, roofs  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'src/main/resources/data/goblinraiders/structure/hamlet'
G = 'goblinraiders:'
STREETS, HOUSES = G + 'hamlet/streets', G + 'hamlet/houses'
STREET_IN, STREET_OUT = G + 'hamlet_street_in', G + 'hamlet_street_out'
HOUSE, SLOT = G + 'hamlet_house', G + 'hamlet_house_slot'
PATH = Mix({'dirt_path': 6, 'coarse_dirt': 1}, salt='hpath')
EDGE = Mix({'grass_block': 5, 'coarse_dirt': 1}, salt='hedge')
PLAZA = Mix({'coarse_dirt': 4, 'packed_mud': 3, 'gravel': 2, 'dirt_path': 3}, salt='hplaza', clump=1.4)


class Piece(Canvas):
    def __init__(self, name, seed):
        super().__init__(seed)
        self.name = name


def jigsaw(c, x, y, z, facing, name, target='minecraft:empty', pool='minecraft:empty', final='minecraft:dirt_path'):
    """A horizontal jigsaw block facing `facing`; it turns into `final` once the village is assembled."""
    c.w.set(x, y, z, final.split(':')[1])
    c.special[(x, y, z)] = (f'minecraft:jigsaw[orientation={facing}_up]', {
        'id': 'minecraft:jigsaw', 'name': name, 'target': target, 'pool': pool, 'final_state': final, 'joint': 'rollable',
        'placement_priority': 0, 'selection_priority': 0})


def street_out(c, x, z, facing):
    jigsaw(c, x, 0, z, facing, STREET_OUT, STREET_IN, STREETS)


def house_slot(c, x, z, facing):
    jigsaw(c, x, 0, z, facing, SLOT, HOUSE, HOUSES, final='minecraft:grass_block')


def sign(c, x, y, z, facing, key, wall=True):
    """A sign with one translated line in the middle."""
    c.w.set(x, y, z, B('oak_wall_sign', facing=facing) if wall else B('oak_sign', rotation=0))
    empty = '{"text":""}'
    text = {'messages': [empty, '{"translate":"%s"}' % key, empty, empty], 'color': 'black', 'has_glowing_text': False}
    c.special[(x, y, z)] = (None, {'id': 'minecraft:sign', 'front_text': text,
                                   'back_text': {'messages': [empty] * 4, 'color': 'black', 'has_glowing_text': False},
                                   'is_waxed': False})


# ================================================================ the square

def storehouse(c, x0, x1, z0, z1, seed):
    """The common storehouse: an open-fronted hall with rows of storehouse crates along the walls."""
    w = c.w
    top = 4
    w.fill(x0, 0, z0, x1, 0, z1, gb.FOOTING)
    w.fill(x0 + 1, 0, z0, x1 - 1, 0, z1 - 1, Mix({'spruce_planks': 4, 'packed_mud': 1}, salt='store'))
    w.walls(x0, 1, z0, x1, top - 1, z1, Mix({'spruce_planks': 4, 'stripped_spruce_log[axis=y]': 1}, salt='swall'))
    for x in range(x0, x1 + 1, 4):
        w.fill(x, 1, z0, x, top, z0, B('spruce_log', axis='y'))
        w.fill(x, 1, z1, x, top, z1, B('spruce_log', axis='y'))
    w.fill(x0, top, z0, x1, top, z0, B('stripped_spruce_log', axis='x'))
    w.fill(x0, top, z1, x1, top, z1, B('stripped_spruce_log', axis='x'))
    for x in (x0, x1):
        w.fill(x, 1, z0, x, top, z0, B('spruce_log', axis='y'))
        w.fill(x, top, z0 + 1, x, top, z1 - 1, B('stripped_spruce_log', axis='z'))
    w.fill(x0 + 1, 1, z0 + 1, x1 - 1, top - 1, z1 - 1, AIR)
    # Open front between the posts.
    for x in range(x0 + 1, x1):
        if (x - x0) % 4:
            w.fill(x, 1, z0, x, top - 1, z0, AIR)
    roof = roofs.gable(x0, z0, x1, z1, top, axis='x', pitch=2, overhang=1, gable_overhang=1)
    roof.build(w, gb.ROOF, verge='spruce', ridge='slab')
    for gx in (x0, x1):
        roof.fill_gable(w, 'x', 'spruce_planks', top, gx, (z0, z1))
    # Crates: two rows high along the back wall, one row along the sides.
    for x in range(x0 + 1, x1):
        for y in (1, 2) if x % 2 == 0 else (1,):
            w.set(x, y, z1 - 1, 'barrel[facing=up]')
            c.special[(x, y, z1 - 1)] = (CRATE, {'id': G + 'storehouse_crate'})
    for z in range(z0 + 2, z1 - 1, 2):
        for x in (x0 + 1, x1 - 1):
            w.set(x, 1, z, 'barrel[facing=up]')
            c.special[(x, 1, z)] = (CRATE, {'id': G + 'storehouse_crate'})
    w.set(x0 + 2, 1, z0 + 2, 'hay_block')
    w.set(x1 - 2, 1, z0 + 2, 'crafting_table')
    for x in range(x0 + 3, x1 - 1, 5):
        c.hanging_lantern(x, top - 1, (z0 + z1) // 2)
    sign(c, (x0 + x1) // 2, top, z0 - 1, 'north', 'goblinraiders.sign.storehouse')


def center(name, seed, variant):
    """The square: campfire and benches, the notice board with the headman, a well or a stall, the storehouse behind."""
    c = Piece(name, seed)
    w = c.w
    width, depth = 21, 15
    for x in range(width):
        for z in range(depth):
            w.set(x, 0, z, PLAZA)
            w.fill(x, 1, z, x, 4, z, AIR)
    cx, cz = 10, 7
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            w.set(cx + dx, 0, cz + dz, 'mossy_cobblestone')
    w.set(cx, 1, cz, B('campfire', lit=True, signal_fire=False, facing='south'))
    w.fill(cx - 3, 1, cz - 1, cx - 3, 1, cz + 1, B('stripped_spruce_log', axis='z'))
    w.fill(cx + 3, 1, cz - 1, cx + 3, 1, cz + 1, B('stripped_spruce_log', axis='z'))
    w.fill(cx - 1, 1, cz + 3, cx + 1, 1, cz + 3, B('stripped_spruce_log', axis='x'))
    # The notice board and the headman.
    bx, bz = (15, 3) if variant == 1 else (4, 3)
    w.fill(bx - 1, 1, bz, bx - 1, 3, bz, 'dark_oak_fence')
    w.fill(bx + 1, 1, bz, bx + 1, 3, bz, 'dark_oak_fence')
    w.fill(bx - 1, 2, bz, bx + 1, 3, bz, 'dark_oak_planks')
    w.set(bx - 1, 4, bz, 'dark_oak_slab[type=bottom]')
    w.set(bx, 4, bz, 'dark_oak_slab[type=bottom]')
    w.set(bx + 1, 4, bz, 'dark_oak_slab[type=bottom]')
    sign(c, bx, 3, bz + 1, 'south', 'goblinraiders.sign.notice_board')
    w.set(bx - 1, 2, bz + 1, B('oak_wall_sign', facing='south'))
    w.set(bx + 1, 2, bz + 1, B('oak_wall_sign', facing='south'))
    c.goblin(bx, 1, bz + 3, 'goblin_headman')
    # A well or a market stall on the other side.
    other = gb.well(seed) if variant == 1 else gb.market_stall(seed, 'green')
    ox = 4 if variant == 1 else 15
    w.paste(other.w, ox, 0, 4)
    for (x, z) in ((1, 1), (width - 2, 1), (1, depth - 2), (width - 2, depth - 2)):
        c.lantern_post(x, z)
    w.set(cx + 5, 1, cz + 4, 'barrel[facing=up]')
    w.set(cx - 5, 1, cz + 4, 'potted_red_mushroom' if variant == 1 else 'potted_fern')
    storehouse(c, 2, width - 3, depth, depth + 7, seed)
    # Streets leave the square to the north, west and east.
    street_out(c, cx, 0, 'north')
    street_out(c, 0, cz, 'west')
    street_out(c, width - 1, cz, 'east')
    return c


# ================================================================ streets

def street(name, length, slots, seed, posts=()):
    """A straight street running south; houses can stand on both sides at `slots`."""
    c = Piece(name, seed)
    for z in range(length):
        for x in range(5):
            c.w.set(x, 0, z, PATH if 1 <= x <= 3 else EDGE)
            c.w.set(x, 1, z, AIR)
            c.w.set(x, 2, z, AIR)
    jigsaw(c, 2, 0, 0, 'north', STREET_IN)
    street_out(c, 2, length - 1, 'south')
    for z in slots:
        house_slot(c, 0, z, 'west')
        house_slot(c, 4, z, 'east')
    for (x, z) in posts:
        c.lantern_post(x, z)
    return c


def crossing(name, seed):
    """A crossroads: streets go on ahead, left and right."""
    c = Piece(name, seed)
    for x in range(9):
        for z in range(9):
            road = 3 <= x <= 5 or 3 <= z <= 5
            c.w.set(x, 0, z, PATH if road else EDGE)
            c.w.set(x, 1, z, AIR)
            c.w.set(x, 2, z, AIR)
    jigsaw(c, 4, 0, 0, 'north', STREET_IN)
    street_out(c, 4, 8, 'south')
    street_out(c, 0, 4, 'west')
    street_out(c, 8, 4, 'east')
    c.lantern_post(1, 1)
    c.lantern_post(7, 7)
    return c


def street_end(name, seed):
    """Where a street runs out: a hay bale and a lantern."""
    c = Piece(name, seed)
    for z in range(4):
        for x in range(5):
            c.w.set(x, 0, z, PATH if 1 <= x <= 3 and z < 3 else EDGE)
            c.w.set(x, 1, z, AIR)
    jigsaw(c, 2, 0, 0, 'north', STREET_IN)
    c.w.set(1, 1, 3, 'hay_block')
    c.lantern_post(3, 3)
    return c


# ================================================================ houses and workshops

def as_piece(name, b):
    """A building with the jigsaw at its door (it joins the street there)."""
    b.name = name
    x, z = b.door
    jigsaw(b, x, 0, z, 'south', HOUSE)
    return b


def home(name, seed, r=3, child=False):
    """A settler's own round hut (a hay bed, a chest, a workbench); the family hut has a child too."""
    b = gb.round_hut(seed, r, 'sleep')
    b.goblin(0, 1, 0, 'goblin_villager', Female=prand(seed, 'f', 0, 0, 0) < 0.5)
    if child:
        b.goblin(1, 1, 1, 'goblin_child')
    return as_piece(name, b)


def home_tent(name, seed, color):
    b = gb.tent(seed, color)
    b.goblin(0, 1, 0, 'goblin_villager', Female=prand(seed, 'f', 0, 0, 0) < 0.5)
    return as_piece(name, b)


def lumber_yard(name, seed):
    b = gb.lumber_yard(seed)
    gb.tree(b.w, -2, -6, 'oak', seed)
    gb.tree(b.w, 3, -7, 'birch', seed + 1)
    return as_piece(name, b)


def grove(name, seed):
    """A few trees for the lumberjacks."""
    b = Building(seed)
    for x in range(-5, 6):
        for z in range(-5, 5):
            b.w.set(x, 0, z, 'grass_block')
    for i, (x, z, kind) in enumerate([(-3, -2, 'oak'), (2, -3, 'birch'), (0, 2, 'oak')]):
        gb.tree(b.w, x, z, kind, seed + i)
    b.door = (0, 5)
    return as_piece(name, b)


PIECES = {
    'center_1': lambda: center('center_1', 501, 1),
    'center_2': lambda: center('center_2', 502, 2),
    'street_long': lambda: street('street_long', 12, (3, 8), 511, posts=((0, 5), (4, 11))),
    'street_short': lambda: street('street_short', 7, (3,), 512, posts=((4, 6),)),
    'street_cross': lambda: crossing('street_cross', 513),
    'street_end': lambda: street_end('street_end', 514),
    'home_small': lambda: home('home_small', 521),
    'home_small_2': lambda: home('home_small_2', 522),
    'home_family': lambda: home('home_family', 523, r=4, child=True),
    'home_tent': lambda: home_tent('home_tent', 524, 'brown'),
    'farm': lambda: as_piece('farm', gb.hamlet_farm(531)),
    'farm_wheat': lambda: as_piece('farm_wheat', gb.hamlet_farm(532, ('wheat', 'beetroots', 'carrots'))),
    'lumber_yard': lambda: lumber_yard('lumber_yard', 533),
    'grove': lambda: grove('grove', 534),
    'mine_adit': lambda: as_piece('mine_adit', gb.mine_adit(535)),
    'builder_yard': lambda: as_piece('builder_yard', gb.builder_yard(536)),
    'kitchen': lambda: as_piece('kitchen', gb.kitchen(537)),
    'site_hut': lambda: as_piece('site_hut', gb.building_site(541, 'hut_small')),
    'site_big': lambda: as_piece('site_big', gb.building_site(542, 'hut_big')),
    'site_garden': lambda: as_piece('site_garden', gb.building_site(543, 'garden')),
}

if __name__ == '__main__':
    names = sys.argv[1:] or list(PIECES)
    for n in names:
        piece = PIECES[n]()
        gv.write_template(piece, n, OUT, stash=False, preview_prefix='hamlet_')
    print(f'{len(names)} pieces written to {OUT}')
