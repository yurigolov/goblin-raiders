"""All hand-made textures of the mod: item icons, the goblin stash block and the goblin armor layers.

Run:  python tools/textures/gen_textures.py
Writes PNGs into src/main/resources/assets/goblinraiders/textures and enlarged previews into tools/previews.
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from pixelart import PREVIEW_DIR, ROOT, hex_rgba, outline, render, save  # noqa: E402

TEX = "src/main/resources/assets/goblinraiders/textures"

# Goblin metal: blackened iron with a green cast, bronze trim, a teal-green gem.
METAL = {"O": "#141810", "H": "#b8c99d", "L": "#86a06e", "M": "#5d7550", "D": "#3a4a33",
         "B": "#c99a3e", "b": "#7d5320", "k": "#0d100b", "G": "#1f8a6e", "g": "#3fd6a8", "P": "#d4fff1"}


def grid(rows):
    assert all(len(r) == 16 for r in rows), [len(r) for r in rows]
    return [list(r) for r in rows] + [list("." * 16) for _ in range(16 - len(rows))]


# ---------------------------------------------------------------- sword
def sword():
    """A crude, broad goblin cleaver-sword, tip in the top right like vanilla swords."""
    g = [["." for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            s, k = x + y, x - y          # s: across the blade, k: along it (tip at k = 13)
            if -4 <= k <= 11 and 14 <= s <= 17 or 11 <= k <= 13 and s == 15 or k == 12 and s == 14:
                g[y][x] = {14: "H", 15: "L", 16: "M", 17: "D"}[s]
            elif -2 <= k <= 6 and s == 18:
                g[y][x] = "D"            # the cleaver belly
            elif k in (-5, -6) and 11 <= s <= 19:
                g[y][x] = "B" if s != 15 else "G"   # crossguard with a gem
            elif -11 <= k <= -7 and 14 <= s <= 16:
                g[y][x] = "W" if (k + s) % 4 else "w"
            elif k in (-12, -13) and 14 <= s <= 16:
                g[y][x] = "b" if s != 15 else "B"
    # Saw teeth along the back of the blade.
    for k in (-1, 3, 7):
        for y in range(16):
            for x in range(16):
                if x - y == k and x + y == 13:
                    g[y][x] = "H"
    g = outline(g, "O")
    pal = dict(METAL, W="#4a2f1b", w="#7a5232")
    save(g, pal, f"{TEX}/item/goblin_sword.png", "goblin_sword")


# ---------------------------------------------------------------- armor icons
def armor_icons():
    helmet = grid([
        "................",
        "..O..........O..",
        "..OBO......OBO..",
        "...OBOOOOOOBO...",
        "...OHHLLLLMMO...",
        "..OHLLLLLLLMDO..",
        "..OHLBLLLLBLDO..",
        "..OLLLLLLLLMDO..",
        "..ObBBBBBBBBbO..",
        "..OLMkkkkkkMDO..",
        "..OLMO....OMDO..",
        "..OLMO....OMDO..",
        "..OMDO....ODDO..",
        "...OO......OO...",
    ])
    chestplate = grid([
        "................",
        "..OOOO....OOOO..",
        ".OBLLMO..OLLMBO.",
        ".OBHLMOOOOLLMBO.",
        ".OBHLLLLLLLLMBO.",
        ".OOOHLBLLBLMOOO.",
        "...OHLLLLLLMO...",
        "...OHLLGgLLMO...",
        "...OLLBBBBLMO...",
        "...OLLLLLLLMO...",
        "...ObBBBBBBbO...",
        "...OLLLBLLLDO...",
        "...OMMMMMMDDO...",
        "...OOOOOOOOOO...",
    ])
    leggings = grid([
        "................",
        "...OOOOOOOOOO...",
        "...ObBBBBBBbO...",
        "...OHLLBBLLMO...",
        "...OHLLLLLLMO...",
        "...OHLMOOHLMO...",
        "...OHMO..OLMO...",
        "...OHMO..OLMO...",
        "...OBBO..OBBO...",
        "...OLMO..OLDO...",
        "...OLMO..OLDO...",
        "...OLMO..OLDO...",
        "...OMDO..OMDO...",
        "...OOOO..OOOO...",
    ])
    boots = grid([
        "................",
        "................",
        "................",
        "................",
        "................",
        "...OOOO....OOOO.",
        "...OHLO....OHLO.",
        "...ObBO....ObBO.",
        "...OHLO....OHLO.",
        "O..OHLO.O..OHLO.",
        "OOOOHLO.OOOOHLO.",
        ".OHLLLO..OHLLLO.",
        ".OMMMDO..OMMMDO.",
        ".OOOOOO..OOOOOO.",
    ])
    for name, g in [("goblin_helmet", helmet), ("goblin_chestplate", chestplate),
                    ("goblin_leggings", leggings), ("goblin_boots", boots)]:
        save(g, METAL, f"{TEX}/item/{name}.png", name)


# ---------------------------------------------------------------- amulet
def amulet():
    g = grid([
        "..c.........c...",
        "..C.........C...",
        "...c.......c....",
        "...C.......C....",
        "....c.....c.....",
        ".....cOBOc......",
        ".....ObBbO......",
        "....ObBGBbO.....",
        "...ObBGgGBbO....",
        "..ObBGgPgGBbO...",
        "...ObBGgGBbO....",
        "....ObBGBbO.....",
        ".....ObBbO......",
        "......ObO.......",
        ".......O........",
    ])
    save(g, dict(METAL, c="#9a9a8c", C="#55554c"), f"{TEX}/item/teleport_amulet.png", "teleport_amulet")


# ---------------------------------------------------------------- coin
def coin():
    # A goblin head with pointy ears stamped on the face of the coin.
    stamp = ["...EE...",
             "E.EEEE.E",
             "EEE..EEE",
             ".EEEEEE.",
             "..E..E..",
             "...EE..."]
    g = [["." for _ in range(16)] for _ in range(16)]
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > 6.2:
                continue
            light = (cx - x) + (cy - y)              # lit from the top left
            if d > 5.0:
                g[y][x] = "R" if light > 0 else "r"
            else:
                g[y][x] = "Y" if light > -2 else "y"
    for j, row in enumerate(stamp):
        for i, ch in enumerate(row):
            if ch == "E":
                g[4 + j][4 + i] = "e"
    g[3][5] = g[4][4] = "S"                         # glint
    g = outline(g, "O")
    pal = {"O": "#3b2606", "R": "#f2c94c", "r": "#b5841c", "Y": "#e6b432", "y": "#cf9a25", "e": "#8a5a10", "S": "#fff4c2"}
    save(g, pal, f"{TEX}/item/goblin_coin.png", "goblin_coin")


# ---------------------------------------------------------------- note
def note():
    g = grid([
        "................",
        "..OOOOOOOOOO....",
        "..OPPPPPPPPPOO..",
        "..OPkkkPkkPPPO..",
        ".OPPPPPPPPPPPO..",
        ".OPkkPkkkkPPPO..",
        ".OPPPPPPPPPPpO..",
        "..OPkkkkPkkPpO..",
        "..OPPPPPPPPPPO..",
        "..OPkkPkkkPGPO..",
        "..OPPPPPPPGGGO..",
        ".OPPkkkPPGGGGGO.",
        ".OPPPPPPPPGGGO..",
        ".OOpPPPPPPPGPO..",
        "...OOOOOOOOOOO..",
    ])
    pal = {"O": "#5a4528", "P": "#eadfbd", "p": "#c8b68a", "k": "#4b4436", "G": "#4f9a3a"}
    save(g, pal, f"{TEX}/item/goblin_note.png", "goblin_note")


# ---------------------------------------------------------------- goblin stash block
WOOD = ["#3b2a1a", "#4a3521", "#56402a", "#644b31"]
IRON = {"dark": "#2b2f27", "mid": "#4d5545", "light": "#7c876d"}
MOSS = ["#3f6b2a", "#4f8233", "#5f9a3c"]


def _wood(rng, x, y):
    plank = y // 4
    edge = y % 4 == 3
    shade = 0 if edge else 1 + (rng.random() < 0.35) + ((x * 7 + plank * 13) % 9 == 0)
    return WOOD[min(shade, 3)]


def stash_textures():
    rng = random.Random(7)
    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            side.putpixel((x, y), hex_rgba(_wood(rng, x, y)))
    # Iron bands top and bottom, corner posts, rivets, moss creeping up from the ground.
    for x in range(16):
        for y in (0, 1, 14, 15):
            side.putpixel((x, y), hex_rgba(IRON["light"] if y in (0, 14) else IRON["dark"]))
    for y in range(16):
        for x in (0, 15):
            side.putpixel((x, y), hex_rgba(IRON["mid"]))
    for x in (3, 8, 12):
        side.putpixel((x, 1), hex_rgba("#b8c0a6"))
        side.putpixel((x, 15), hex_rgba("#b8c0a6"))
    for x in range(1, 15):
        h = rng.choice([0, 0, 1, 1, 2, 3])
        for y in range(13 - h, 14):
            if rng.random() < 0.8:
                side.putpixel((x, y), hex_rgba(rng.choice(MOSS)))

    front = side.copy()
    lock = ["..OOOO..",
            ".OBBBBO.",
            "OBgBBgBO",
            "OBBBBBBO",
            "OBkkkkBO",
            ".OBkkBO.",
            "..OBBO.."]
    lpal = {"O": "#3b2606", "B": "#c99a3e", "g": "#3fd6a8", "k": "#1c1408"}
    for j, row in enumerate(lock):
        for i, ch in enumerate(row):
            if ch != ".":
                front.putpixel((4 + i, 4 + j), hex_rgba(lpal[ch]))
    # Ears of the goblin face lock.
    for (x, y) in [(3, 5), (2, 4), (12, 5), (13, 4)]:
        front.putpixel((x, y), hex_rgba("#c99a3e"))

    top = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            top.putpixel((x, y), hex_rgba(_wood(rng, y, x)))   # planks run the other way
    for i in range(16):
        for p in [(i, 0), (i, 15), (0, i), (15, i)]:
            top.putpixel(p, hex_rgba(IRON["mid"]))
    for x in range(16):
        top.putpixel((x, 7), hex_rgba(IRON["light"]))
        top.putpixel((x, 8), hex_rgba(IRON["dark"]))
    for p in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        top.putpixel(p, hex_rgba("#b8c0a6"))

    for name, img in [("goblin_stash_side", side), ("goblin_stash_front", front), ("goblin_stash_top", top)]:
        path = ROOT / TEX / "block" / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
    sheet = Image.new("RGBA", (16 * 3 + 4, 16), (0, 0, 0, 0))
    for i, img in enumerate([front, side, top]):
        sheet.paste(img, (i * 18, 0))
    sheet.resize((sheet.width * 12, sheet.height * 12), Image.NEAREST).save(PREVIEW_DIR / "goblin_stash.png")


# ---------------------------------------------------------------- armor layers (64x32)
def _box_faces(u, v, w, h, d):
    """Face rectangles (x0, y0, width, height) of a Minecraft box UV at (u, v) for a w x h x d box."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


def _plate(img, rect, rows=None, trim_top=False, trim_bottom=False, rivets=(), seed=0, gem=None, slit_row=None):
    """Paints a metal plate face: light-to-dark gradient, dark rim, optional bronze trims, rivets and a gem."""
    x0, y0, w, h = rect
    rng = random.Random(seed)
    rows = rows if rows is not None else range(h)
    for j in rows:
        for i in range(w):
            t = (i / max(1, w - 1) + j / max(1, h - 1)) / 2
            c = METAL["H"] if t < 0.15 else METAL["L"] if t < 0.45 else METAL["M"] if t < 0.8 else METAL["D"]
            if rng.random() < 0.12:
                c = METAL["M"] if c != METAL["M"] else METAL["L"]
            if i in (0, w - 1):
                c = METAL["D"]
            img.putpixel((x0 + i, y0 + j), hex_rgba(c))
    rows = list(rows)
    if trim_top and rows:
        for i in range(w):
            img.putpixel((x0 + i, y0 + rows[0]), hex_rgba(METAL["B"] if i % 3 else METAL["b"]))
    if trim_bottom and rows:
        for i in range(w):
            img.putpixel((x0 + i, y0 + rows[-1]), hex_rgba(METAL["b"] if i % 3 else METAL["B"]))
    for (i, j) in rivets:
        img.putpixel((x0 + i, y0 + j), hex_rgba(METAL["B"]))
    if gem:
        gi, gj = gem
        for (di, dj, c) in [(0, 0, "g"), (1, 0, "G"), (0, 1, "G"), (1, 1, "G"), (0, -1, "B"), (1, -1, "B"), (-1, 0, "B"),
                            (2, 0, "B"), (-1, 1, "B"), (2, 1, "B"), (0, 2, "B"), (1, 2, "B")]:
            img.putpixel((x0 + gi + di, y0 + gj + dj), hex_rgba(METAL[c]))
    if slit_row is not None:
        for i in range(1, w - 1):
            img.putpixel((x0 + i, y0 + slit_row), hex_rgba(METAL["k"]))


def armor_layers():
    layer1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    layer2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    # Helmet: head box 8x8x8 at (0, 0). Bronze band around the brow, eye slit on the front.
    head = _box_faces(0, 0, 8, 8, 8)
    for n, face in head.items():
        if n == "bottom":
            continue
        _plate(layer1, face, seed=hash(n) % 97, trim_bottom=n in ("right", "left", "back"))
    _plate(layer1, head["front"], seed=5, slit_row=4, rivets=[(1, 1), (6, 1)])
    for i in range(8):                                           # brow band on the front
        layer1.putpixel((8 + i, 8 + 3), hex_rgba(METAL["B"] if i % 3 else METAL["b"]))
    for j in (5, 6, 7):                                          # open lower face
        for i in range(2, 6):
            layer1.putpixel((8 + i, 8 + j), (0, 0, 0, 0))
    for i in range(8):                                           # crest on top
        layer1.putpixel((8 + i, 3), hex_rgba(METAL["B"]))
        layer1.putpixel((8 + i, 4), hex_rgba(METAL["b"]))

    # Chestplate: body box 8x12x4 at (16, 16) and arm boxes 4x12x4 at (40, 16).
    body = _box_faces(16, 16, 8, 12, 4)
    for n, face in body.items():
        if n in ("top", "bottom"):
            _plate(layer1, face, seed=11)
        else:
            _plate(layer1, face, seed=hash(n) % 89, rows=range(0, 11), trim_bottom=True,
                   rivets=[(1, 2), (face[2] - 2, 2)] if face[2] > 4 else [])
    _plate(layer1, body["front"], seed=21, rows=range(0, 11), trim_bottom=True, rivets=[(1, 1), (6, 1), (1, 8), (6, 8)], gem=(3, 4))
    arm = _box_faces(40, 16, 4, 12, 4)
    for n, face in arm.items():
        if n == "bottom":
            continue
        rows = range(face[3]) if n == "top" else range(0, 5)      # pauldrons cover the shoulder only
        _plate(layer1, face, seed=hash(n) % 83, rows=rows, trim_bottom=n != "top")
        if n != "top":
            layer1.putpixel((face[0] + 1, face[1] + 1), hex_rgba(METAL["B"]))

    # Boots: lower part of the leg box 4x12x4 at (0, 16).
    leg = _box_faces(0, 16, 4, 12, 4)
    for n, face in leg.items():
        if n == "top":
            continue
        rows = range(face[3]) if n == "bottom" else range(7, 12)
        _plate(layer1, face, seed=hash(n) % 79, rows=rows, trim_top=n != "bottom")

    # Leggings (layer 2): the lower part of the body box (belt) and most of the legs.
    body2 = _box_faces(16, 16, 8, 12, 4)
    for n, face in body2.items():
        if n in ("top", "bottom"):
            continue
        _plate(layer2, face, seed=hash(n) % 71, rows=range(7, 12), trim_top=True)
    for i in range(3, 5):                                          # belt buckle
        for j in (8, 9):
            layer2.putpixel((body2["front"][0] + i, body2["front"][1] + j), hex_rgba(METAL["B"]))
    leg2 = _box_faces(0, 16, 4, 12, 4)
    for n, face in leg2.items():
        if n in ("top", "bottom"):
            continue
        _plate(layer2, face, seed=hash(n) % 67, rows=range(0, 10), trim_bottom=True)
        layer2.putpixel((face[0] + 1, face[1] + 5), hex_rgba(METAL["B"]))   # knee rivet

    out = ROOT / TEX / "models/armor"
    out.mkdir(parents=True, exist_ok=True)
    layer1.save(out / "goblin_layer_1.png")
    layer2.save(out / "goblin_layer_2.png")
    sheet = Image.new("RGBA", (64, 66), (40, 40, 40, 255))
    sheet.alpha_composite(layer1, (0, 0))
    sheet.alpha_composite(layer2, (0, 34))
    sheet.resize((64 * 8, 66 * 8), Image.NEAREST).save(PREVIEW_DIR / "goblin_armor_layers.png")


def _diagonal(fn):
    """16x16 grid from fn(s, k): s = x + y runs across a bottom-left to top-right item, k = x - y along it."""
    g = [["." for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            ch = fn(x + y, x - y)
            if ch:
                g[y][x] = ch
    return g


def club():
    """A knobbly wooden club with iron studs and a leather grip."""
    def fn(s, k):
        if -13 <= k <= -10 and 15 <= s <= 16:
            return "b"                                     # pommel knob
        if -9 <= k <= -3 and 15 <= s <= 16:
            return "W" if (k + s) % 3 else "w"             # leather grip
        if -2 <= k <= 12:
            half = 1 + min(2, (k + 2) // 4)                # widens towards the head
            if k >= 11:
                half -= 1                                   # rounded tip
            lo, hi = 15 - half, 16 + half
            if lo <= s <= hi:
                if (k, s) in {(3, 13), (6, 19), (8, 12), (9, 18), (11, 15), (5, 15)}:
                    return "N"                             # iron studs
                t = (s - lo) / max(1, hi - lo)
                return "H" if t < 0.2 else "L" if t < 0.5 else "M" if t < 0.8 else "D"
        return None
    g = outline(_diagonal(fn), "O")
    pal = {"O": "#1f1408", "H": "#b88a52", "L": "#8f6436", "M": "#6e4a26", "D": "#4d3218",
           "N": "#aeb4a6", "W": "#4a2f1b", "w": "#7a5232", "b": "#7d5320"}
    save(g, pal, f"{TEX}/item/goblin_club.png", "goblin_club")


def dagger():
    """Short jagged goblin blade, bronze guard, leather wrap."""
    def fn(s, k):
        if 1 <= k <= 9 and 15 <= s <= 17 or 10 <= k <= 11 and s == 16:
            return {15: "H", 16: "L", 17: "M"}[s]
        if k in (-1, 0) and 13 <= s <= 19:
            return "B" if s not in (16,) else "G"
        if -6 <= k <= -2 and 16 <= s <= 17:
            return "W" if (k + s) % 4 else "w"
        if k in (-8, -7) and 16 <= s <= 17:
            return "b"
        return None
    g = _diagonal(fn)
    for k in (3, 7):                                        # a saw tooth on the back of the blade
        for y in range(16):
            for x in range(16):
                if x - y == k and x + y == 14:
                    g[y][x] = "H"
    g = outline(g, "O")
    pal = dict(METAL, W="#4a2f1b", w="#7a5232")
    save(g, pal, f"{TEX}/item/goblin_dagger.png", "goblin_dagger")


def _line(g, x0, y0, x1, y1, ch):
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        if 0 <= x0 < 16 and 0 <= y0 < 16:
            g[y0][x0] = ch
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def bows():
    """Recurve goblin bow: dark green-stained limbs with bronze tips; standby plus three drawn frames with an arrow."""
    tip_a, tip_b = (2, 13), (13, 2)

    def frame(pull, name):
        g = [["." for _ in range(16)] for _ in range(16)]
        # Limb: a curve bulging towards the top left; the grip in the middle.
        pts = []
        for i in range(41):
            t = i / 40
            cx, cy = 1.0 - pull * 0.25, 1.0 - pull * 0.25
            x = (1 - t) ** 2 * tip_a[0] + 2 * (1 - t) * t * cx + t * t * tip_b[0]
            y = (1 - t) ** 2 * tip_a[1] + 2 * (1 - t) * t * cy + t * t * tip_b[1]
            pts.append((round(x), round(y), t))
        for (x, y, t) in pts:
            g[y][x] = "B" if t < 0.08 or t > 0.92 else "W" if 0.42 < t < 0.58 else ("L" if abs(t - 0.5) < 0.3 else "M")
        # String from tip to tip through the drawn point.
        mid = (7.5 + pull * 1.6, 7.5 + pull * 1.6)
        sx, sy = round(mid[0]), round(mid[1])
        _line(g, tip_a[0], tip_a[1], sx, sy, "S")
        _line(g, sx, sy, tip_b[0], tip_b[1], "S")
        if pull > 0:
            # Arrow resting on the string, pointing to the top left.
            _line(g, sx, sy, sx - 7, sy - 7, "A")
            g[sy - 7][sx - 7] = "I"
            g[sy - 6][sx - 7] = "I"
            g[sy - 7][sx - 6] = "I"
            g[sy][sx] = "F"
            if sy + 1 < 16:
                g[sy + 1][sx] = "F"
            if sx + 1 < 16:
                g[sy][sx + 1] = "F"
        pal = {"B": "#c99a3e", "W": "#4a2f1b", "L": "#3f5a2e", "M": "#2c4020", "S": "#d8d8cc",
               "A": "#7a5232", "I": "#aeb4a6", "F": "#f0f0f0"}
        save(g, pal, f"{TEX}/item/{name}.png", name)

    frame(0, "goblin_bow")
    for i, pull in enumerate((1, 2, 3)):
        frame(pull, f"goblin_bow_pulling_{i}")


def stunned_icon():
    """18x18 effect icon: three little stars circling over a dizzy swirl."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for i in range(60):
        a = i / 60 * 2 * math.pi * 1.6
        r = 1.5 + i / 60 * 6.0
        x, y = 9 + math.cos(a) * r, 11 + math.sin(a) * r * 0.5
        if 0 <= x < 18 and 0 <= y < 18:
            img.putpixel((int(x), int(y)), hex_rgba("#9a9a9a"))
    star = ["..Y..", ".YWY.", "YWWWY", ".YWY.", "..Y.."]
    for (ox, oy) in ((1, 1), (12, 2), (7, 0)):
        for j, row in enumerate(star):
            for i, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((ox + i, oy + j), hex_rgba({"Y": "#e8b830", "W": "#fff6b0"}[ch]))
    path = ROOT / TEX / "mob_effect" / "stunned.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    img.resize((18 * 12, 18 * 12), Image.NEAREST).save(PREVIEW_DIR / "stunned.png")


def concealment_amulet():
    """A pendant with a little goblin mask carved in green stone; leather cord."""
    g = grid([
        "..c.........c...",
        "..c.........c...",
        "...c.......c....",
        "....c.....c.....",
        ".....cOOOOc.....",
        "....OOGGGGOO....",
        "...OGGgGGgGGO...",
        "..OGEGGGGGGEGO..",
        "..OGGkGGGGkGGO..",
        "..OGGGGGGGGGGO..",
        "...OGGkkkkGGO...",
        "....OGGGGGGO....",
        ".....OGGGGO.....",
        "......OOOO......",
    ])
    pal = {"O": "#1d2a14", "G": "#5f9a3c", "g": "#9ad46a", "E": "#4f8233", "k": "#13200c", "c": "#6b4a2a"}
    save(g, pal, f"{TEX}/item/concealment_amulet.png", "concealment_amulet")


def goblin_grub():
    """A wooden bowl of murky green stew with a bone and a mushroom sticking out."""
    g = grid([
        "................",
        "................",
        "..........w.....",
        "..........ww....",
        "...b.......w....",
        "..bWb.....rw....",
        "...b.w...rrr....",
        "......w...M.....",
        ".OOOOOOOOOOOOOO.",
        "OSSsSSSsSSSSsSSO",
        "OBSSSsSSSSSsSSBO",
        ".OBBBBBBBBBBBBO.",
        "..OBbBBBBBBbBO..",
        "...OOBBBBBBOO...",
        ".....OOOOOO.....",
    ])
    pal = {"O": "#2b1a0c", "S": "#6f8a2e", "s": "#9cb54a", "B": "#8a5a2c", "b": "#6b4320",
           "W": "#e8e0c8", "w": "#d8cfb4", "r": "#b02a2a", "M": "#e0d6c0"}
    save(g, pal, f"{TEX}/item/goblin_grub.png", "goblin_grub")


def loot_sack():
    g = grid([
        "................",
        "......OO........",
        ".....OttO.......",
        "......OO........",
        ".....OrrO.......",
        "....OSSSSOO.....",
        "...OSSsSSSSO....",
        "..OSSSSSSsSSO...",
        "..OSsSSSSSSSO...",
        ".OSSSSYSSSSSSO..",
        ".OSSSYYYSSsSSO..",
        ".OSSSSYSSSSSSO..",
        "..OSSSSSSSSSO...",
        "...OOSSSSSOO....",
        ".....OOOOO......",
    ])
    pal = {"O": "#2b1a0c", "S": "#9c7a4a", "s": "#7a5c34", "t": "#c8b088", "r": "#5a3a1e", "Y": "#e6b432"}
    save(g, pal, f"{TEX}/item/loot_sack.png", "loot_sack")


def goblin_crown():
    g = grid([
        "................",
        "................",
        "................",
        "..O....O....O...",
        ".OYO..OYO..OYO..",
        ".OYO..OYO..OYO..",
        ".OYYO.OYO.OYYO..",
        ".OYYYOYYYOYYYO..",
        ".OYGYYYRYYYGYO..",
        ".OYYYYYYYYYYYO..",
        ".OyyyyyyyyyyyO..",
        "..OOOOOOOOOOO...",
    ])
    pal = {"O": "#3b2606", "Y": "#f2c94c", "y": "#c79a24", "G": "#3fd65a", "R": "#c0303a"}
    save(g, pal, f"{TEX}/item/goblin_crown.png", "goblin_crown")


def king_scepter():
    """Gold rod, a green orb gripped by a little skull cage on top."""
    def fn(s, k):
        if 6 <= k <= 11 and 13 <= s <= 18:
            d = (k - 8.5) ** 2 + (s - 15.5) ** 2
            if d <= 3.2:
                return "g" if d < 1.2 else "G"
            if d <= 6.5:
                return "Y"
        if -12 <= k <= 5 and 15 <= s <= 16:
            return "Y" if s == 15 else "y"
        if k in (-2, -3) and 13 <= s <= 18:
            return "y"
        return None
    g = outline(_diagonal(fn), "O")
    pal = {"O": "#3b2606", "Y": "#f2c94c", "y": "#c79a24", "G": "#2fa84a", "g": "#9df0a8"}
    save(g, pal, f"{TEX}/item/king_scepter.png", "king_scepter")


def vassal_charter():
    g = grid([
        "................",
        "..OOOOOOOOOOO...",
        ".OPPPPPPPPPPPO..",
        ".OPkkPkkkPkPPO..",
        ".OPPPPPPPPPPPO..",
        ".OPkkkPkkkkPPO..",
        ".OPPPPPPPPPPPO..",
        ".OPkkPkkkPPPPO..",
        ".OPPPPPPPPPPPO..",
        ".OPPPPPPPRRPPO..",
        ".OPPPPPPRRRRPO..",
        ".OPPPPPPRRRRPO..",
        "..OOOOOOORROO...",
        ".........r.r....",
    ])
    pal = {"O": "#5a4528", "P": "#eadfbd", "k": "#4b4436", "R": "#b02a2a", "r": "#7a1a1a"}
    save(g, pal, f"{TEX}/item/vassal_charter.png", "vassal_charter")


def _icon18(rows, pal, name):
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), hex_rgba(pal[ch]))
    path = ROOT / TEX / "mob_effect" / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    img.resize((18 * 12, 18 * 12), Image.NEAREST).save(PREVIEW_DIR / f"{name}.png")


def effect_icons():
    disguise = [
        "..................",
        "..O............O..",
        ".OGO..........OGO.",
        ".OGGO.OOOOOO.OGGO.",
        "..OGGOGGGGGGOGGO..",
        "...OGGGGGGGGGGO...",
        "...OGGGGGGGGGGO...",
        "...OGYYGGGGYYGO...",
        "...OGYkGGGGkYGO...",
        "...OGGGGGGGGGGO...",
        "....OGGGddGGGO....",
        "....OGGWGGWGGO....",
        ".....OGGGGGGO.....",
        "......OOOOOO......",
    ]
    _icon18(disguise, {"O": "#1d2a14", "G": "#5f9a3c", "Y": "#e8d25a", "k": "#13200c", "d": "#3f6b2a", "W": "#f0f0e0"}, "goblin_disguise")
    curse = [
        "........P.........",
        "......P.PP........",
        ".......PPP.P......",
        "....OOOOOOOOO.....",
        "...OWWWWWWWWWO....",
        "..OWWWWWWWWWWWO...",
        "..OWkkWWWWWkkWO...",
        "..OWkPkWWWkPkWO...",
        "..OWkkWWWWWkkWO...",
        "..OWWWWWkWWWWWO...",
        "...OWWWkkkWWWO....",
        "....OWkWkWkWO.....",
        "....OWWWWWWWO.....",
        ".....OOOOOOO......",
    ]
    _icon18(curse, {"O": "#2a1030", "W": "#d8d0c0", "k": "#2a1030", "P": "#a040c0"}, "goblin_curse")


def crown_layer():
    """Armor layer for the crown: a gold band with points around the top of the head."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    faces = _box_faces(0, 0, 8, 8, 8)
    for name in ("front", "back", "left", "right"):
        x0, y0, w, h = faces[name]
        for i in range(w):
            spike = i % 3 == 1
            for j in range(0, 4):
                if j == 0 and not spike:
                    continue
                c = "#f2c94c" if j < 3 else "#c79a24"
                img.putpixel((x0 + i, y0 + j), hex_rgba(c))
        if name == "front":
            img.putpixel((x0 + 3, y0 + 2), hex_rgba("#3fd65a"))
            img.putpixel((x0 + 4, y0 + 2), hex_rgba("#c0303a"))
    out = ROOT / TEX / "models/armor"
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / "goblin_crown_layer_1.png")


def items_sheet():
    names = ["goblin_sword", "goblin_dagger", "goblin_club", "goblin_bow", "goblin_bow_pulling_2", "goblin_helmet", "goblin_chestplate", "goblin_leggings", "goblin_boots",
             "teleport_amulet", "goblin_coin", "goblin_note", "concealment_amulet", "goblin_grub",
             "loot_sack", "goblin_crown", "king_scepter", "vassal_charter"]
    tiles = [Image.open(ROOT / TEX / "item" / f"{n}.png").convert("RGBA") for n in names]
    s = 10
    sheet = Image.new("RGBA", (len(tiles) * (16 * s + 8), 16 * s), (139, 139, 139, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t.resize((16 * s, 16 * s), Image.NEAREST), (i * (16 * s + 8), 0))
    sheet.save(PREVIEW_DIR / "items.png")
    print(f"preview: {PREVIEW_DIR / 'items.png'}")


if __name__ == "__main__":
    sword()
    armor_icons()
    amulet()
    coin()
    note()
    stash_textures()
    armor_layers()
    club()
    dagger()
    bows()
    stunned_icon()
    concealment_amulet()
    goblin_grub()
    loot_sack()
    goblin_crown()
    king_scepter()
    vassal_charter()
    effect_icons()
    crown_layer()
    items_sheet()
