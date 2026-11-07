"""Goblin villages: 4 types x 2 variants plus the mega village, built from the shared buildings in goblin_buildings.py.

    camp      small fenced camp: tents, the chief's hut with the stash, crazy goblins
    village   palisade village: chief's hall, huts, the elder's hut (the elder lives there), tower, farms
    mega      goblin city: stone walls, central market, the trader, the elder, keep, barracks, arena, ~50 goblins
    sage_tent the sage's lone tent by a campfire
    market    trading village: market square with stalls and the goblin trader's shop
    warcamp   stone-walled war camp: keep, barracks, the giant's arena, towers, smithy

Built with the schematics toolkit (C:/Users/yurig/.claude/minecraft/schematics, override with SCHEMATICS_DIR)
against Minecraft 1.21.1 block data (`python -m lib.extract_data --version 1.21.1` there).

Run:  python tools/structures/goblin_village.py [name ...]
Writes src/main/resources/data/goblinraiders/structure/goblin_village/<name>.nbt and previews tools/previews/village_<name>.png.

Build coordinates: x east, z south, y up. y = 0 is the ground layer (it replaces the surface blocks of the world),
the campfire in the middle of the plaza is at x = z = 0. A start jigsaw under the campfire makes the structure start
chunk corner land exactly on the village center, which is what raid notes point at. Buildings are placed around the
plaza with their doors facing it; the packer checks they do not overlap each other, the gate roads or the wall.
"""
import math
import random
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import goblin_buildings as gb  # noqa: E402  (sets up the toolkit path and MC_VERSION)
from goblin_buildings import AIR, B, BUILDINGS, CRATE, FOOTING, LOGS, SITE, STASH, STONE_WALL, Canvas, Mix, prand, rot, totem  # noqa: E402
import json  # noqa: E402
from lib import terrain, weathering  # noqa: E402
from lib import blocks as BL  # noqa: E402
from lib import checks, connect, mcdata, nbt, render  # noqa: E402
from lib.world import ellipse_points  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / 'src/main/resources/data/goblinraiders/structure/goblin_village'
BLUEPRINT_DIR = ROOT / 'src/main/resources/data/goblinraiders/goblin_blueprint'
PREVIEWS = ROOT / 'tools/previews'
CLEAR_H = 12                     # air is placed up to this height inside the wall (clears hills)
GATE_DIRS = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}
DOOR_ROT = {'south': 0, 'west': 1, 'north': 2, 'east': 3}   # quarter turns that make a south door face this way
PATH = ('dirt_path', 'dirt_path', 'coarse_dirt')


class Village(Canvas):
    def __init__(self, name, seed, radius, plaza):
        super().__init__(seed)
        self.name = name
        self.R = radius
        self.plaza = plaza
        self.rng = random.Random(seed)
        self.hm = {}
        self.low = set()         # columns taken by placed buildings near the ground (y <= 2)
        self.high = set()        # columns taken by anything of a placed building (roofs included)
        self.roads = []          # (x0, z0, x1, z1) segments buildings must keep clear of
        self.doors = []
        self.reserved = set()    # columns kept free (building sites, trees): no lantern posts or goblins there

    # ------------------------------------------------------------ ground, wall, plaza
    def ground(self, mix=None):
        mix = mix or Mix({'grass_block': 9, 'coarse_dirt': 3, 'podzol': 2, 'moss_block': 1}, salt='ground', clump=2.5)
        plaza = Mix({'coarse_dirt': 5, 'packed_mud': 3, 'gravel': 2, 'dirt_path': 2}, salt='plaza', clump=1.4)
        R = self.R
        for x in range(-R - 1, R + 2):
            for z in range(-R - 1, R + 2):
                d = math.hypot(x, z)
                if d > R + 0.5:
                    continue
                self.w.set(x, 0, z, plaza if d <= self.plaza + 0.5 else mix)
                self.hm[(x, z)] = 0
                if d <= R - 0.5:
                    self.w.fill(x, 1, z, x, CLEAR_H, z, AIR)

    def wall(self, style, gates):
        """style: 'fence' (camp), 'palisade' (logs), 'stone' (stone base, logs above, towers at the gates)."""
        R, w = self.R, self.w
        gate_cells = set()
        for g in gates:
            dx, dz = GATE_DIRS[g]
            for t in range(-1, 2):
                for depth in range(-2, 3):
                    gate_cells.add((dx * (R + depth) + (t if dx == 0 else 0), dz * (R + depth) + (t if dz == 0 else 0)))
        for (x, z) in ellipse_points(0, 0, R, R, filled=False):
            if (x, z) in gate_cells:
                continue
            w.set(x, 0, z, FOOTING)
            if style == 'fence':
                post = (x * 7 + z * 3) % 4 == 0
                if post:
                    w.fill(x, 1, z, x, 2, z, LOGS)
                    w.set(x, 3, z, 'spruce_fence')
                else:
                    w.fill(x, 1, z, x, 2, z, 'spruce_fence')
            elif style == 'palisade':
                h = 3 + int(prand(self.seed, 'palh', x, 0, z) * 3)
                w.fill(x, 1, z, x, h, z, LOGS)
                w.set(x, h + 1, z, 'spruce_fence' if prand(self.seed, 'palt', x, 0, z) < 0.7 else 'dark_oak_fence')
            else:
                w.fill(x, 1, z, x, 2, z, STONE_WALL)
                h = 4 + int(prand(self.seed, 'stoneh', x, 0, z) * 2)
                w.fill(x, 3, z, x, h, z, LOGS)
                w.set(x, h + 1, z, 'spruce_fence')
        for g in gates:
            self.gate(g, style)

    def gate(self, direction, style):
        dx, dz = GATE_DIRS[direction]
        R, w = self.R, self.w
        cx, cz = dx * R, dz * R
        side = (1, 0) if dx == 0 else (0, 1)
        top = 3 if style == 'fence' else 6
        post_block = B('dark_oak_log', axis='y')
        for s in (-2, 2):
            px, pz = cx + side[0] * s, cz + side[1] * s
            if style == 'stone':
                # Gate towers: 3x3 stone columns with fence battlements.
                out = 1 if s > 0 else -1
                for ox in range(0, 3):            # outwards from the gate, so the passage stays 3 wide
                    for oz in range(-1, 2):
                        tx, tz = px + side[0] * ox * out + dx * oz, pz + side[1] * ox * out + dz * oz
                        w.fill(tx, 0, tz, tx, 7, tz, STONE_WALL)
                        w.set(tx, 8, tz, 'mossy_cobblestone_wall')
                self.skull(px, 9, pz, {'north': 8, 'south': 0, 'east': 12, 'west': 4}[direction])
            else:
                w.fill(px, 1, pz, px, top, pz, post_block)
                self.skull(px, top + 1, pz, {'north': 8, 'south': 0, 'east': 12, 'west': 4}[direction])
        axis = 'x' if dx == 0 else 'z'
        lintel_y = top if style != 'stone' else 6
        for s in range(-1, 2):
            w.set(cx + side[0] * s, lintel_y, cz + side[1] * s, B('dark_oak_log', axis=axis))
            w.fill(cx + side[0] * s, 1, cz + side[1] * s, cx + side[0] * s, lintel_y - 1, cz + side[1] * s, AIR)
        self.hanging_lantern(cx, lintel_y - 1, cz)
        # Road from the gate to the plaza.
        terrain.path(self.w, [(cx, cz), (dx * self.plaza, dz * self.plaza)], width=3, stops=PATH, hm=self.hm, salt='g' + direction)
        self.roads.append((cx, cz, dx * (self.plaza - 1), dz * (self.plaza - 1)))

    def center(self, benches=True, totems=4, market=False):
        """Bonfire with the start jigsaw under it, log benches and totem poles."""
        w = self.w
        w.set(0, 0, 0, 'mossy_cobblestone')
        self.special[(0, 0, 0)] = ('minecraft:jigsaw[orientation=up_north]', {
            'id': 'minecraft:jigsaw', 'name': 'goblinraiders:village_center', 'target': 'minecraft:empty',
            'pool': 'minecraft:empty', 'final_state': 'minecraft:mossy_cobblestone', 'joint': 'rollable',
            'placement_priority': 0, 'selection_priority': 0})
        w.set(0, 1, 0, B('campfire', lit=True, signal_fire=False, facing='south'))
        for (x, z) in ellipse_points(0, 0, 1.5, 1.5, filled=False):
            if (x, z) != (0, 0):
                w.set(x, 0, z, FOOTING)
        if benches:
            for (x, z, axis) in [(-3, 0, 'z'), (3, 0, 'z'), (0, 3, 'x'), (0, -3, 'x')][:3 if not market else 2]:
                if axis == 'z':
                    w.fill(x, 1, z - 1, x, 1, z + 1, B('stripped_spruce_log', axis='z'))
                else:
                    w.fill(x - 1, 1, z, x + 1, 1, z, B('stripped_spruce_log', axis='x'))
        r = self.plaza - 1
        spots = [(-r, -r, 'south'), (r, -r, 'south'), (-r, r, 'north'), (r, r, 'north')]
        for (x, z, facing) in spots[:totems]:
            k = int(round(r * 0.72))
            totem(w, int(math.copysign(k, x)), int(math.copysign(k, z)), facing)

    # ------------------------------------------------------------ buildings
    def place(self, kind, x, z, door, seed_salt=''):
        """Pastes building `kind` with its center at (x, z) and its door facing `door`. Returns its footprint."""
        # crc32, not hash(): Python salts string hashes per run, and the templates must come out the same every time.
        b = BUILDINGS[kind](zlib.crc32(repr((self.seed, kind, seed_salt, x, z)).encode()) & 0xFFFFFF)
        k = DOOR_ROT[door]
        self.w.paste(b.w, x, 0, z, rotate=k)
        for (bx, by, bz), value in b.special.items():
            rx, rz = rot(bx, bz, k)
            self.special[(rx + x, by, rz + z)] = value
        for (ex, ey, ez, eid, extra) in b.entities:
            rx, rz = rot(ex, ez, k)
            self.entities.append((rx + x, ey, rz + z, eid, extra))
        if getattr(b, 'reserve', False):
            for (bx, _, bz) in b.w.blocks:
                rx, rz = rot(bx, bz, k)
                self.reserved.add((rx + x, rz + z))
        dx, dz = rot(*b.door, k)
        self.doors.append((dx + x, dz + z))

    @staticmethod
    def cells(b, x, z, k):
        """Columns a building takes once placed: (near the ground, anywhere)."""
        low, high = set(), set()
        for (bx, by, bz), state in b.w.blocks.items():
            if BL.is_air(state):
                continue
            rx, rz = rot(bx, bz, k)
            high.add((rx + x, rz + z))
            if by <= 2:
                low.add((rx + x, rz + z))
        return low, high

    def fits(self, low, high):
        R = self.R
        for (x, z) in low:
            d = math.hypot(x, z)
            if d > R - 2.0 or d < self.plaza + 0.5:
                return False
            for (rx0, rz0, rx1, rz1) in self.roads:
                if _segment_box_distance((rx0, rz0), (rx1, rz1), (x, z, x, z)) < 2.0:
                    return False
        for (x, z) in high:
            if math.hypot(x, z) > R - 0.6:
                return False
        grown = {(x + dx, z + dz) for (x, z) in low for dx in (-1, 0, 1) for dz in (-1, 0, 1)}
        return not (grown & self.low) and not (high & self.high)

    def ring(self, kind, angle, required=True):
        """Puts a building at `angle` degrees (0 = south, 90 = west) as close to the plaza as it fits, door to the center."""
        a = math.radians(angle)
        ux, uz = -math.sin(a), math.cos(a)
        door = _facing_towards(-ux, -uz)
        probe = BUILDINGS[kind](1)
        k = DOOR_ROT[door]
        for d in [self.plaza + 2 + i * 0.5 for i in range(int(self.R * 2))]:
            x, z = int(round(ux * d)), int(round(uz * d))
            low, high = self.cells(probe, x, z, k)
            if self.fits(low, high):
                self.low |= low
                self.high |= high
                self.place(kind, x, z, door)
                return True
        if required:
            raise RuntimeError(f'{self.name}: no room for {kind} at {angle} degrees')
        print(f'  {self.name}: skipped {kind} at {angle} degrees (no room)')
        return False

    def at(self, kind, x, z, door):
        """Fixed placement (inside the plaza, e.g. market stalls); still registers the footprint."""
        low, high = self.cells(BUILDINGS[kind](1), x, z, DOOR_ROT[door])
        self.low |= low
        self.high |= high
        self.place(kind, x, z, door)

    def paths_to_doors(self):
        for (x, z) in self.doors:
            d = math.hypot(x, z)
            if d <= self.plaza + 1:
                continue
            ex, ez = x * self.plaza / d, z * self.plaza / d
            terrain.path(self.w, [(x, z), (ex, ez)], width=2, stops=PATH, hm=self.hm, salt=f'd{x},{z}')

    # ------------------------------------------------------------ goblins and dressing
    def free(self, x, z, height=2, width=1):
        for ox in range(width):
            for oz in range(width):
                if (x + ox, z + oz) in self.reserved:
                    return False
                cell = self.w.get(x + ox, 0, z + oz)
                if cell is None or BL.bid(cell) in ('water', 'farmland') or not mcdata.exists(BL.bid(cell)):
                    return False
                for y in range(1, height + 1):
                    if not self.w.is_air(x + ox, y, z + oz):
                        return False
        return True

    def scatter_goblins(self, counts):
        """Free goblins on the plaza and the paths, away from buildings."""
        top = self.w.bounds()[4]
        for kind, n in counts.items():
            # A giant is twice as tall as a door: it stands under the open sky, never in a hall it could not leave.
            big = kind == 'giant_goblin'
            placed, tries = 0, 0
            while placed < n and tries < 500:
                tries += 1
                a = self.rng.uniform(0, 2 * math.pi)
                d = self.rng.uniform(2.5, self.R - 4)
                x, z = int(round(math.sin(a) * d)), int(round(math.cos(a) * d))
                if self.free(x, z, top if big else 2, 2 if big else 1) and all(abs(x - e[0]) + abs(z - e[2]) > 2 for e in self.entities):
                    self.goblin(x, 1, z, kind)
                    placed += 1
            if placed < n:
                raise RuntimeError(f'{self.name}: no room for {kind}')

    def plant_trees(self, n, kinds=('oak', 'birch')):
        """Trees between the buildings and around the edge (the lumberjack fells them and plants new ones)."""
        placed = 0
        for i in range(600):
            if placed >= n:
                break
            a = self.rng.uniform(0, 2 * math.pi)
            d = self.rng.uniform(self.plaza + 4, self.R - 1)
            x, z = int(round(math.sin(a) * d)), int(round(math.cos(a) * d))
            if all(self.free(x + dx, z + dz, 7) for dx in range(-2, 3) for dz in range(-2, 3)):
                gb.tree(self.w, x, z, kinds[placed % len(kinds)], self.seed + placed)
                self.reserved |= {(x + dx, z + dz) for dx in range(-3, 4) for dz in range(-3, 4)}
                placed += 1
        print(f'  {self.name}: {placed} trees')

    def scatter_settlers(self, jobs):
        """Settlers on the paths and the square, each with its trade (a man or a woman at random)."""
        for i, job in enumerate(jobs):
            for tries in range(500):
                a = self.rng.uniform(0, 2 * math.pi)
                d = self.rng.uniform(2.5, self.R - 4)
                x, z = int(round(math.sin(a) * d)), int(round(math.cos(a) * d))
                if self.free(x, z, 2) and all(abs(x - e[0]) + abs(z - e[2]) > 2 for e in self.entities):
                    self.goblin(x, 1, z, 'goblin_villager', Job=gb.JOB[job], Female=self.rng.random() < 0.5)
                    break
            else:
                raise RuntimeError(f'{self.name}: no room for a {job}')

    def lantern_posts(self, n):
        placed = 0
        for i in range(200):
            if placed >= n:
                break
            a = self.rng.uniform(0, 2 * math.pi)
            d = self.rng.uniform(self.plaza + 0.5, self.R - 3)
            x, z = int(round(math.sin(a) * d)), int(round(math.cos(a) * d))
            if self.free(x, z, 3) and self.free(x + 1, z, 2) and self.free(x - 1, z, 2) and self.free(x, z + 1, 2) and self.free(x, z - 1, 2):
                self.lantern_post(x, z)
                placed += 1

    def dressing(self):
        flora_hm = dict(self.hm)
        terrain.flora(self.w, flora_hm, density=0.3, salt='flora',
                      avoid=lambda x, z: math.hypot(x, z) > self.R - 1.5 or math.hypot(x, z) < self.plaza + 1)
        for i in range(8):
            a = self.rng.uniform(0, 2 * math.pi)
            d = self.rng.uniform(self.plaza + 2, self.R - 2)
            x, z = int(round(math.sin(a) * d)), int(round(math.cos(a) * d))
            if self.free(x, z, 1):
                self.w.set(x, 1, z, 'red_mushroom' if prand(self.seed, 'mush', x, 0, z) < 0.5 else 'brown_mushroom')
                self.w.set(x, 0, z, 'podzol')
        R = self.R
        weathering(self.w, (-R - 2, 0, -R - 2, R + 2, 24, R + 2), base_y=0, moss=0.45, cracks=0.06, vines=0.25)

    # ------------------------------------------------------------ export
    def export(self, stash=True):
        return write_template(self, self.name, OUT_DIR, stash=stash)


def write_template(c, name, out_dir, stash=False, preview_prefix='village_'):
    """Writes canvas `c` as structure template out_dir/<name>.nbt (its ground layer at y = 0) and an isometric preview."""
    connect.finalize(c.w)
    x0, y0, z0, x1, y1, z1 = c.w.bounds()
    assert y0 == 0, f'{name}: template must start at the ground layer, got y0={y0}'
    size = (x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1)
    palette, index, blocks = [], {}, []
    stashes = 0
    for (x, y, z), state in sorted(c.w.blocks.items()):
        override, be = c.special.get((x, y, z), (None, None))
        if override == STASH:
            facing = BL.props(state).get('facing', 'south')
            override = f'goblinraiders:goblin_stash[facing={facing if facing in GATE_DIRS else "south"}]'
            stashes += 1
        elif override == SITE:
            facing = BL.props(state).get('facing', 'south')
            override = f'goblinraiders:building_site[facing={facing if facing in GATE_DIRS else "south"}]'
        elif override == CRATE:
            override = 'goblinraiders:storehouse_crate'
        block, props = _parse_state(override or BL.normalize(state))
        key = (block, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            entry = {'Name': nbt.String(block)}
            if props:
                entry['Properties'] = nbt.Compound({k: nbt.String(v) for k, v in props.items()})
            palette.append(nbt.Compound(entry))
        info = {'pos': nbt.List(nbt.INT, [nbt.Int(x - x0), nbt.Int(y - y0), nbt.Int(z - z0)]), 'state': nbt.Int(index[key])}
        if be:
            info['nbt'] = _to_tag(be)
        blocks.append(nbt.Compound(info))
    assert stashes == (1 if stash else 0), f'{name}: expected {int(stash)} stash, got {stashes}'
    ents = []
    for (x, y, z, eid, extra) in c.entities:
        assert c.w.is_air(x, y, z) and c.w.is_air(x, y + 1, z), f'{name}: {eid} at {(x, y, z)} is inside blocks'
        lx, ly, lz = x - x0, y - y0, z - z0
        tag = {'id': nbt.String(eid), 'PersistenceRequired': nbt.Byte(1)}
        tag.update({k: _to_tag(v) for k, v in extra.items()})
        ents.append(nbt.Compound({
            'pos': nbt.List(nbt.DOUBLE, [nbt.Tag(nbt.DOUBLE, lx + 0.5), nbt.Tag(nbt.DOUBLE, float(ly)), nbt.Tag(nbt.DOUBLE, lz + 0.5)]),
            'blockPos': nbt.List(nbt.INT, [nbt.Int(lx), nbt.Int(ly), nbt.Int(lz)]),
            'nbt': nbt.Compound(tag),
        }))
    root = nbt.Compound({
        'DataVersion': nbt.Int(mcdata.data_version()),
        'size': nbt.List(nbt.INT, [nbt.Int(s) for s in size]),
        'palette': nbt.List(nbt.COMPOUND, palette),
        'blocks': nbt.List(nbt.COMPOUND, blocks),
        'entities': nbt.List(nbt.COMPOUND, ents),
    })
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / f'{name}.nbt'
    nbt.write(out, '', root)
    kinds = {}
    for e in c.entities:
        kind = e[3].split(':')[1]
        if 'Job' in e[4]:
            kind += '/' + next(j for j, n in gb.JOB.items() if n == e[4]['Job'])
        kinds[kind] = kinds.get(kind, 0) + 1
    print(f'{name}: size {size}, {len(blocks)} blocks, goblins {kinds}')

    local = {(x - x0, y - y0, z - z0): s for (x, y, z), s in c.w.blocks.items() if not BL.is_air(s)}
    problems = [p for p in checks.physics(local, size) if p[0] == 'error']
    if problems:
        print(f'  physics: {len(problems)} errors, first: {problems[:5]}')
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    shots = [render.render_iso(local, size, k=k, title=f'{name} {t}') for k, t in [(0, 'SE'), (2, 'NW')]]
    render.side_by_side(shots).save(PREVIEWS / f'{preview_prefix}{name}.png')
    return out


def _facing_towards(dx, dz):
    if abs(dx) > abs(dz):
        return 'east' if dx > 0 else 'west'
    return 'south' if dz > 0 else 'north'


def _segment_box_distance(a, b, box):
    """Distance between a line segment and an axis-aligned box (sampled; good enough for layout)."""
    x0, z0, x1, z1 = box
    best = 1e9
    steps = int(max(abs(b[0] - a[0]), abs(b[1] - a[1])) * 2) + 1
    for i in range(steps + 1):
        t = i / steps
        px, pz = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
        dx = max(x0 - px, 0, px - x1)
        dz = max(z0 - pz, 0, pz - z1)
        best = min(best, math.hypot(dx, dz))
    return best


def _parse_state(state):
    name, _, rest = state.partition('[')
    if ':' not in name:
        name = 'minecraft:' + name
    props = {}
    if rest:
        for kv in rest.rstrip(']').split(','):
            k, v = kv.split('=')
            props[k] = v
    return name, props


def _to_tag(value):
    if isinstance(value, list):
        tags = [_to_tag(v) for v in value]
        return nbt.List(nbt.STRING if not value or isinstance(value[0], str) else nbt.COMPOUND, tags)
    if isinstance(value, bool):
        return nbt.Byte(1 if value else 0)
    if isinstance(value, int):
        return nbt.Int(value)
    if isinstance(value, dict):
        return nbt.Compound({k: _to_tag(v) for k, v in value.items()})
    return nbt.String(str(value))


# ================================================================ village types

def camp(name, seed, variant):
    v = Village(name, seed, radius=16 if variant == 1 else 17, plaza=4)
    v.ground()
    v.wall('fence', ['south'] if variant == 1 else ['north', 'east'])
    v.center(totems=2)
    v.ring('chief_hut', 180 if variant == 1 else 135)
    tents = ['tent_green', 'tent_brown', 'tent_lime']
    angles = [70, 290, 0] if variant == 1 else [250, 20, 320]
    for kind, angle in zip(tents[:2 + (variant == 2)], angles):
        v.ring(kind, angle)
    v.ring('cage' if variant == 1 else 'loot_heap', 115 if variant == 1 else 200, required=False)
    v.ring('loot_heap' if variant == 1 else 'cage', 245 if variant == 1 else 75, required=False)
    v.paths_to_doors()
    v.lantern_posts(2)
    v.dressing()
    v.scatter_goblins({'small_goblin': 3, 'crazy_goblin': 3, 'ruthless_goblin': 2, 'goblin_child': 1})
    return v


def village(name, seed, variant):
    v = Village(name, seed, radius=21 if variant == 1 else 22, plaza=6)
    v.ground()
    v.wall('palisade', ['west', 'east'] if variant == 1 else ['north', 'south'])
    v.center()
    if variant == 1:
        plan = [('hall', 180), ('elder_hut', 0), ('hut_sleep', 130), ('hut_shaman', 230), ('tower', 60),
                ('workshop', 315), ('farm', 35), ('cage', 285)]
    else:
        plan = [('hall', 90), ('elder_hut', 270), ('hut_shaman', 45), ('big_hut', 135), ('farm_wheat', 225),
                ('farm', 315), ('well', 0), ('tower', 200), ('loot_heap', 160)]
    for kind, angle in plan:
        v.ring(kind, angle, required=kind in ('hall', 'elder_hut'))
    v.paths_to_doors()
    v.lantern_posts(4)
    v.dressing()
    v.scatter_goblins({'small_goblin': 3, 'ruthless_goblin': 2, 'neutral_goblin': 5, 'giant_goblin': 1, 'crazy_goblin': 2, 'goblin_child': 3})
    return v


def market(name, seed, variant):
    v = Village(name, seed, radius=23, plaza=9)
    v.ground()
    v.wall('palisade', ['west', 'east', 'south'] if variant == 1 else ['north', 'west', 'east'])
    v.center(totems=4, market=True)
    # Stalls around the market square, facing the bonfire.
    stalls = ['stall_red', 'stall_green', 'stall_blue', 'stall_yellow', 'stall_purple']
    spots = [(-5, -4, 'east'), (5, -4, 'west'), (-5, 4, 'east'), (5, 4, 'west')] if variant == 1 else \
            [(-5, 0, 'east'), (5, 0, 'west'), (0, -5, 'south'), (-4, 4, 'north'), (4, 4, 'north')]
    for kind, (x, z, door) in zip(stalls, spots):
        v.at(kind, x, z, door)
    if variant == 1:
        plan = [('trader_shop', 180), ('small_hall', 230), ('hut_sleep', 130), ('hut_storage', 315), ('well', 45),
                ('workshop', 340), ('farm', 20)]
    else:
        plan = [('trader_shop', 0), ('small_hall', 135), ('big_hut', 225), ('hut_sleep', 45), ('farm', 315), ('well', 270)]
    for kind, angle in plan:
        v.ring(kind, angle, required=kind in ('trader_shop', 'small_hall'))
    v.paths_to_doors()
    v.lantern_posts(5)
    v.dressing()
    v.scatter_goblins({'neutral_goblin': 6, 'small_goblin': 3, 'ruthless_goblin': 2, 'crazy_goblin': 1, 'goblin_child': 3})
    return v


def warcamp(name, seed, variant):
    v = Village(name, seed, radius=24, plaza=6)
    v.ground(Mix({'coarse_dirt': 5, 'grass_block': 4, 'gravel': 2, 'podzol': 1}, salt='wground', clump=2.0))
    v.wall('stone', ['north', 'south'] if variant == 1 else ['east', 'west'])
    v.center(totems=4)
    if variant == 1:
        plan = [('keep', 90), ('arena', 270), ('longhouse', 140), ('longhouse', 220), ('hut_shaman', 40), ('workshop', 320),
                ('tower', 25)]
    else:
        plan = [('keep', 0), ('arena', 180), ('longhouse', 135), ('workshop', 45), ('tower', 225), ('tower', 315),
                ('cage', 300)]
    for kind, angle in plan:
        v.ring(kind, angle, required=kind in ('keep', 'arena'))
    v.paths_to_doors()
    v.lantern_posts(4)
    v.dressing()
    v.scatter_goblins({'ruthless_goblin': 4, 'small_goblin': 3, 'giant_goblin': 1, 'crazy_goblin': 2})
    return v


def megavillage(name, seed):
    """A goblin city: stone walls with four gate towers, a market in the middle and every kind of building around it."""
    v = Village(name, seed, radius=40, plaza=10)
    v.ground(Mix({'grass_block': 7, 'coarse_dirt': 4, 'podzol': 2, 'gravel': 1, 'moss_block': 1}, salt='mground', clump=2.5))
    v.wall('stone', ['north', 'south', 'east', 'west'])
    v.center(totems=4, market=True)
    stalls = ['stall_red', 'stall_green', 'stall_blue', 'stall_yellow', 'stall_purple', 'stall_red', 'stall_green', 'stall_blue']
    for i, kind in enumerate(stalls):
        a = math.radians(22.5 + 45 * i)
        x, z = int(round(-math.sin(a) * 7)), int(round(math.cos(a) * 7))
        v.at(kind, x, z, _facing_towards(-x, -z))
    required = ('throne_hall', 'keep', 'trader_shop', 'elder_hut', 'arena')
    plan = [('throne_hall', 160), ('keep', 110), ('trader_shop', 30), ('elder_hut', 210),
            ('arena', 255), ('longhouse', 60), ('longhouse', 300), ('workshop', 330),
            ('hut_sleep', 15), ('hut_shaman', 195), ('hut_storage', 165), ('big_hut', 345),
            ('tower', 45), ('tower', 135), ('tower', 225), ('tower', 315),
            ('farm', 100), ('farm_wheat', 260), ('farm', 80), ('well', 10), ('well', 190),
            ('cage', 110), ('cage', 290), ('hut_sleep', 50), ('hut_sleep', 230), ('big_hut', 170),
            ('workshop', 70), ('loot_heap', 140), ('loot_heap', 320), ('tent_green', 75), ('tent_brown', 255),
            ('tent_lime', 285), ('hut_storage', 20), ('hut_sleep', 200)]
    for kind, angle in plan:
        v.ring(kind, angle, required=kind in required)
    v.paths_to_doors()
    v.lantern_posts(12)
    v.dressing()
    v.scatter_goblins({'small_goblin': 8, 'ruthless_goblin': 6, 'neutral_goblin': 8, 'giant_goblin': 2, 'crazy_goblin': 4, 'goblin_child': 6})
    return v


def sage_camp(name, seed):
    """The sage's lone tent by a campfire, a log bench and a few flowers."""
    v = Village(name, seed, radius=7, plaza=2)
    v.ground(Mix({'grass_block': 8, 'coarse_dirt': 2, 'podzol': 1}, salt='sground', clump=2.0))
    v.center(benches=False, totems=0)
    v.at('sage_tent', 0, -4, 'south')
    v.w.fill(2, 1, 1, 2, 1, 2, B('stripped_spruce_log', axis='z'))
    v.lantern_posts(1)
    v.dressing()
    return v


def export_blueprints():
    """The buildings goblin builders put up, as JSON: [x, y, z, state] around the plot center, ground at y = 0."""
    BLUEPRINT_DIR.mkdir(parents=True, exist_ok=True)
    for name, make in gb.BLUEPRINTS.items():
        b = make(4242)
        connect.finalize(b.w)
        blocks = []
        for (x, y, z), state in sorted(b.w.blocks.items()):
            if BL.is_air(state):
                blocks.append([x, y, z, 'minecraft:air'])
                continue
            block, props = _parse_state(BL.normalize(state))
            if block in ('minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes', 'minecraft:beetroots'):
                props['age'] = '0'
            blocks.append([x, y, z, block + ('[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']' if props else '')])
        out = BLUEPRINT_DIR / f'{name}.json'
        out.write_text(json.dumps({'name': name, 'settlers': name.startswith('hut'), 'blocks': blocks}, separators=(',', ':')),
                       encoding='utf-8')
        print(f'blueprint {name}: {sum(1 for bl in blocks if bl[3] != "minecraft:air")} blocks')


VILLAGES = {
    'camp_1': lambda: camp('camp_1', 101, 1),
    'camp_2': lambda: camp('camp_2', 202, 2),
    'village': lambda: village('village', 1337, 1),
    'village_2': lambda: village('village_2', 404, 2),
    'market_1': lambda: market('market_1', 505, 1),
    'market_2': lambda: market('market_2', 606, 2),
    'warcamp_1': lambda: warcamp('warcamp_1', 707, 1),
    'warcamp_2': lambda: warcamp('warcamp_2', 808, 2),
    'mega_1': lambda: megavillage('mega_1', 909),
    'sage_tent_1': lambda: sage_camp('sage_tent_1', 3131),
    'sage_tent_2': lambda: sage_camp('sage_tent_2', 4141),
}
PEACEFUL = ('sage_tent_1', 'sage_tent_2')

if __name__ == '__main__':
    names = sys.argv[1:] or list(VILLAGES)
    for n in names:
        VILLAGES[n]().export(stash=n not in PEACEFUL)
    export_blueprints()
    print(f'previews: {PREVIEWS}')
