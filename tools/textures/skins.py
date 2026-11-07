"""Goblin skins in one style, the small goblin's (64x64 player-skin layout).

The small goblin (small_goblin.png, drawn by hand) is the model: flat tones without noise, a yellow-green skin in seven
tones, black bar brows, white eyes with a coloured iris, a dark nose stripe, white fangs, ears drawn as dark outlines on
the sides of the head, leather and linen in short ramps. Every other goblin is painted here on the same head and the
same bare body from pixel maps, and then dressed:

    crazy_goblin            wild grey hair, one goggling eye, a gap-toothed grin, a torn shirt, patched trousers (slim)
    goblin_elder            washed-out old skin, long grey hair, white brows and goatee, a tooth necklace, a leaf skirt (slim)
    ruthless_goblin         goggles up on the forehead, an iron breastplate over a red tunic, pauldrons, greaves
    neutral_goblin          the plain goblin: a grey fur mantle, a loincloth, linen wraps (also the disguise skin)
    giant_goblin            heavy brow, tusks, a bandolier, a belt with a gold buckle, a fur loincloth
    goblin_trader           a striped cap, a red scarf, a dark cape, a long leather coat with pouches, a gold tooth
    goblin_shaman           a hide hood with feathers and moss, face paint, a bone necklace, a belt of potion vials
    goblin_king             a gold crown, gold teeth, a skull on the chest, a fur collar and a red cape
    goblin_villager         a settler: linen shirt, leather vest, trousers
    goblin_villager_female  a goblin woman: auburn hair in braids, a red dress with an apron (slim)
    goblin_headman          the headman: a hat with a band and a feather, a dark green coat, the chain of office
    goblin_sage             the hermit sage: the elder's face, a long white beard, a deep blue hood and robe

The eyes tell the temper: red for raiders, amber for neutral and peaceful goblins, cyan for the wise old ones.

Maps have one character per pixel: digits are the goblin's skin tones (0 darkest .. 6 lightest), letters are shared
materials (see MATERIALS) or the goblin's two accent ramps (u v x y z and U V X Y Z), '.' leaves the pixel as it is and
'_' clears it (overlay layers). The sides, tops and bottoms of the body, arms and legs follow from their front and back.

Run:  python tools/textures/skins.py   (lineup in tools/previews/goblin_skins.png)
The first hand-made skins of these goblins are kept in tools/textures/original/.
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SKINS = ROOT / 'src/main/resources/assets/goblinraiders/textures/entity/goblin'
PREVIEWS = ROOT / 'tools/previews'


def rgba(hex_color, a=255):
    h = hex_color.lstrip('#')
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a


def shade(color, amount):
    r, g, b, a = color
    return (max(0, min(255, r + amount)), max(0, min(255, g + amount)), max(0, min(255, b + amount)), a)


def ramp(*hexes):
    return [rgba(h) for h in hexes]


def fade(tones, t):
    """Washes a ramp out toward a warm grey (old skin)."""
    out = []
    for r, g, b, a in tones:
        lum = 0.3 * r + 0.59 * g + 0.11 * b
        grey = (lum * 1.05, lum, lum * 0.85)
        out.append(tuple(min(255, int(round(c * (1 - t) + gc * t))) for c, gc in zip((r, g, b), grey)) + (a,))
    return out


# Seven tones each, darkest first. SKIN, LEATHER and CLOTH are the small goblin's own colours.
SKIN = ramp('#425908', '#53700a', '#64870c', '#769f0e', '#87b610', '#99cd12', '#aae414')
OLD_SKIN = fade(SKIN, 0.45)
LEATHER = ramp('#2b1c04', '#412b06', '#593b08', '#704a0a', '#87590c', '#9f680e', '#b67810')
CLOTH = ramp('#7e703c', '#907f44', '#a18f4c', '#b09d56', '#c0b179', '#d8cfac', '#ece6cf')
METAL = ramp('#2e2e33', '#46464d', '#5e5e66', '#7a7a82', '#9a9aa2', '#bcbcc2', '#dedee2')
GOLD = ramp('#5a3c00', '#7a5400', '#9e7206', '#c4960e', '#e0b418', '#f2cc30', '#fff07a')
RED = ramp('#3a0a08', '#5c100c', '#7e1812', '#a0221a', '#be3024', '#d84634', '#ec6a52')
FUR_GRAY = ramp('#35322f', '#4a4642', '#615c56', '#7a746d', '#948d85', '#aea69d', '#c9c1b7')
FUR_BROWN = ramp('#24170c', '#362313', '#4c321c', '#634226', '#7a5331', '#93663e', '#ab7b4e')
LEAF = ramp('#122410', '#1a3416', '#24461c', '#2f5a22', '#3b6f28', '#4a852f', '#5d9b38')
MOSS = ramp('#2e3010', '#3e4114', '#50541a', '#636820', '#787d28', '#8e9331', '#a6ab3d')
GREY_HAIR = ramp('#3e3e3e', '#585858', '#727272', '#8e8e8e', '#aaaaaa', '#c6c6c6', '#e2e2e2')
RAGS = ramp('#2c2a2e', '#3e3b41', '#524e55', '#67626a', '#7c7780', '#938e97', '#aca7b0')
PATCHED = ramp('#1b1724', '#262033', '#332b44', '#423856', '#524768', '#64587b', '#796d90')
LILAC = ramp('#3a3060', '#52478a', '#6c62aa', '#8a82c6', '#aaa3dc', '#c8c2ec', '#e2def8')
CAPE = ramp('#1c1620', '#29202e', '#372c3e', '#463850', '#564662', '#685676', '#7c698a')
DRESS = ramp('#3e100e', '#5a1a16', '#7a2a24', '#963832', '#ae4840', '#c45c50', '#d87466')
AUBURN = ramp('#3a170a', '#552313', '#70301b', '#8b4024', '#a3532f', '#b9693e', '#cc8253')
COAT = ramp('#0f231b', '#163224', '#1e432f', '#27563b', '#316a48', '#3d7f57', '#4c9468')
TROUSERS = ramp('#241e16', '#332b20', '#43392b', '#534736', '#635643', '#756752', '#897b65')
ROBE = ramp('#0e1434', '#151e4a', '#1d2a62', '#26387c', '#304796', '#405cae', '#5774c2')
RAMPS = [SKIN, OLD_SKIN, LEATHER, CLOTH, METAL, GOLD, RED, FUR_GRAY, FUR_BROWN, LEAF, MOSS, GREY_HAIR, RAGS, PATCHED,
         LILAC, CAPE, DRESS, AUBURN, COAT, TROUSERS, ROBE]

EYE_RED, EYE_AMBER, EYE_CYAN = rgba('#d30909'), rgba('#e8a317'), rgba('#2fd3e6')

MATERIALS = {
    'B': rgba('#1a1a1a'), 'W': rgba('#ffffff'), 'w': rgba('#d9d9d9'),
    **{ch: LEATHER[k] for k, ch in enumerate('abcdefg')},
    **{ch: CLOTH[k] for k, ch in enumerate('hijklmn')},
    **{ch: METAL[k] for k, ch in enumerate('Nopqrst')},
    'H': GOLD[2], 'G': GOLD[4], 'J': GOLD[6],
    'Q': RED[1], 'R': RED[3], 'S': RED[5],
}


def tone(color, steps):
    """The same colour `steps` tones lighter (or darker, if negative) on its ramp."""
    if color[3] == 0:
        return color
    for tones in RAMPS:
        if color in tones:
            return tones[max(0, min(len(tones) - 1, tones.index(color) + steps))]
    return shade(color, 16 * steps)


def faces(u, v, w, h, d):
    """Face rectangles (x, y, width, height) of a box UV at (u, v)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


# Layout of a skin: (u, v, w, h, d) of each box; arms are 3 wide on slim skins.
HEAD, HAT = (0, 0, 8, 8, 8), (32, 0, 8, 8, 8)
BODY, JACKET = (16, 16, 8, 12, 4), (16, 32, 8, 12, 4)
R_LEG, R_PANTS = (0, 16, 4, 12, 4), (0, 32, 4, 12, 4)
L_LEG, L_PANTS = (16, 48, 4, 12, 4), (0, 48, 4, 12, 4)


def arm_boxes(slim):
    w = 3 if slim else 4
    return (40, 16, w, 12, 4), (32, 48, w, 12, 4), (40, 32, w, 12, 4), (48, 48, w, 12, 4)


# ---------------------------------------------------------------- the small goblin's head and a bare body
HEAD_TOP = ["23333332", "34444443", "44444444", "44444444", "44444444", "44444544", "34444453", "23333332"]
HEAD_BOTTOM = ["22222222", "22111122", "21111112", "21111112", "21111112", "21111112", "22111122", "22222222"]
HEAD_RIGHT = ["44444444", "42222444", "44234224", "44234444", "44233444", "44422224", "43333444", "33333333"]
HEAD_LEFT = ["44444444", "44422224", "42243244", "44443244", "44433244", "42222444", "44433334", "33333333"]
HEAD_BACK = ["43333334", "44444444", "42111124", "44444444", "44211244", "44444444", "34444443", "23333332"]
# Forehead, brows, eyes (E = iris), cheeks and nose, fangs, teeth, chin.
FACE = ["45665544", "1BB11BB1", "4WE23EW4", "45523554", "44423444", "4wW22Ww4", "34wWWw43", "23444432"]

TORSO_FRONT = ["33455433", "34455443", "34555543", "33422433", "34433443", "34322343",
               "34433443", "34322343", "34433443", "33433433", "33444433", "23333332"]
TORSO_BACK = ["23333332", "34433443", "32433423", "34433443", "34433443", "34433443",
              "34433443", "34433443", "34433443", "33433433", "33333333", "23333332"]
ARM_FRONT = ["4554", "4554", "3443", "3443", "3443", "2332", "3443", "3443", "3443", "3443", "3443", "2332"]
ARM_BACK = ["3443", "3333", "3333", "3333", "3333", "2332", "3333", "3333", "3333", "3333", "3333", "2222"]
SLIM_ARM_FRONT = ["454", "454", "343", "343", "343", "232", "343", "343", "343", "343", "343", "232"]
SLIM_ARM_BACK = ["343", "333", "333", "333", "333", "232", "333", "333", "333", "333", "333", "222"]
LEG_FRONT = ["3443", "3443", "3443", "3443", "3443", "2332", "3443", "3443", "3443", "3443", "3333", "2222"]
LEG_BACK = ["3333", "3333", "3333", "3333", "3333", "3333", "3333", "3333", "3333", "3333", "3333", "2222"]


def mirror(rows):
    return [row[::-1] for row in rows]


def face(*changes):
    """The small goblin's face with some rows replaced: face((1, '10011001'), ...)."""
    rows = list(FACE)
    for j, row in changes:
        rows[j] = row
    return rows


class Skin:
    def __init__(self, tones=SKIN, eye=EYE_RED, slim=False, a1=None, a2=None, extra=None):
        self.img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        self.slim = slim
        self.legend = dict(MATERIALS)
        self.legend.update({str(k): c for k, c in enumerate(tones)})
        self.legend['E'] = eye
        for chars, accent in (('uvxyz', a1), ('UVXYZ', a2)):
            if accent:
                self.legend.update({ch: accent[k + 1] for k, ch in enumerate(chars)})
        self.legend.update(extra or {})
        self.r_arm, self.l_arm, self.r_sleeve, self.l_sleeve = arm_boxes(slim)
        self.head(top=HEAD_TOP, bottom=HEAD_BOTTOM, right=HEAD_RIGHT, left=HEAD_LEFT, back=HEAD_BACK, front=FACE)
        self.torso(TORSO_FRONT, TORSO_BACK)
        self.arms(SLIM_ARM_FRONT if slim else ARM_FRONT, SLIM_ARM_BACK if slim else ARM_BACK)
        self.legs(LEG_FRONT, LEG_BACK)

    # -------------------------------------------------------- painting
    def draw(self, rect, rows):
        x0, y0, w, h = rect
        assert len(rows) == h and all(len(row) == w for row in rows), f'{w}x{h} expected: {rows}'
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch != '.':
                    self.img.putpixel((x0 + i, y0 + j), (0, 0, 0, 0) if ch == '_' else self.legend[ch])

    def read(self, rect):
        x0, y0, w, h = rect
        return [[self.img.getpixel((x0 + i, y0 + j)) for i in range(w)] for j in range(h)]

    def put(self, rect, pixels):
        x0, y0, _, _ = rect
        for j, row in enumerate(pixels):
            for i, color in enumerate(row):
                self.img.putpixel((x0 + i, y0 + j), color)

    def boxed(self, box, **maps):
        f = faces(*box)
        for name, rows in maps.items():
            if rows is not None:
                self.draw(f[name], rows)

    def wrap(self, box):
        """Sides from the edges of the front and back (a shade darker inside), top and bottom from the end rows."""
        u, v, w, h, d = box
        f = faces(*box)
        front, back = self.read(f['front']), self.read(f['back'])
        right, left = [], []
        for j in range(h):
            fr, fl, br, bl = front[j][0], front[j][w - 1], back[j][w - 1], back[j][0]
            right.append([br, tone(br, -1), tone(fr, -1), fr])
            left.append([fl, tone(fl, -1), tone(bl, -1), bl])
        self.put(f['right'], right)
        self.put(f['left'], left)
        top = [[tone(front[0][i] if jj >= d // 2 else back[0][w - 1 - i], 1) for i in range(w)] for jj in range(d)]
        self.put(f['top'], top)
        self.put(f['bottom'], [[tone(front[h - 1][i], -1) for i in range(w)] for _ in range(d)])

    # -------------------------------------------------------- body parts (maps paint over what is there)
    def head(self, **maps):
        self.boxed(HEAD, **maps)

    def hat(self, **maps):
        self.boxed(HAT, **maps)

    def torso(self, front, back):
        self.boxed(BODY, front=front, back=back)
        self.wrap(BODY)

    def jacket(self, front, back):
        self.boxed(JACKET, front=front, back=back)
        self.wrap(JACKET)

    def _pair(self, right_box, left_box, front, back, left_front, left_back):
        self.boxed(right_box, front=front, back=back)
        self.boxed(left_box, front=left_front or mirror(front), back=left_back or mirror(back))
        self.wrap(right_box)
        self.wrap(left_box)

    def arms(self, front, back, left_front=None, left_back=None):
        """The right arm as maps; the left one is its mirror image unless given."""
        self._pair(self.r_arm, self.l_arm, front, back, left_front, left_back)

    def sleeves(self, front, back, left_front=None, left_back=None):
        self._pair(self.r_sleeve, self.l_sleeve, front, back, left_front, left_back)

    def legs(self, front, back, left_front=None, left_back=None):
        self._pair(R_LEG, L_LEG, front, back, left_front, left_back)

    def pants(self, front, back, left_front=None, left_back=None):
        self._pair(R_PANTS, L_PANTS, front, back, left_front, left_back)

    def save(self, name):
        self.img.save(SKINS / name)


def clear(h, w=8):
    return ['_' * w] * h


# ---------------------------------------------------------------- the goblins
def crazy():
    s = Skin(slim=True, a1=RAGS, a2=GREY_HAIR, extra={'A': PATCHED[1], 'C': PATCHED[2], 'D': PATCHED[3], 'F': PATCHED[4]})
    # One brow shot up over a goggling eye, a gap-toothed grin.
    s.head(front=face((0, '4BB66554'), (1, '11111BB1'), (3, '4WW23554'), (5, '4WBWWBW4'), (6, '34wwww43')),
           top=["VXVVXVXV", "XVUVVXUV", "VXVVUVXV", "UVXVVVVX", "VXVVXVUV", "XVUXVVXV", "VXVVXVXV", "XVXVVXVX"])
    s.hat(top=["_VX_VXV_", "VXVVXVVX", "XVUVVXUV", "VX_VUVXV", "UVXVV_VX", "VXVVXVUV", "XVUXVVXV", "V_XV_VX_"],
          front=["X_V__XV_"] + clear(7),
          right=["VXV_VX_V", "_V__X___"] + clear(6),
          left=["V_XV_VXV", "___X__V_"] + clear(6),
          back=["VXVVXVVX", "XVUVVXUV", "V_XV_VX_", "_V___X__"] + clear(4))
    s.torso(["yx3443xy", "yyx44xyy", "yyyxyyzy", "yxyyyyyy", "yyyvyyxy", "yyyyyyyy",
             "yx3yyy4y", "y334x3y3", "kjkkjkkj", "CDDDDDDC", "CDDFDDDC", "ACCCCCCA"],
            ["yyyyyyyy", "yxyyyyxy", "yyylmlyy", "yyymmlyy", "yyyllmyy", "yyyyyxyy",
             "yyxyyyyy", "y3yyx3yy", "kjkkjkkj", "CDDDDDDC", "CDDDDDDC", "ACCCCCCA"])
    # The right sleeve hangs in tatters, the left one is torn off; bandages on the forearms.
    s.arms(["yxy", "yyy", "yyx", "3y3", "...", "...", "...", "mlm", "lml", "mlm", "...", "..."],
           ["yyy", "xyy", "yyy", "y3y", "...", "...", "...", "mlm", "lml", "mlm", "...", "..."],
           left_front=["yxy", "3y3", "...", "...", "...", "...", "...", "...", "mlm", "lml", "...", "..."],
           left_back=["yyy", "y3y", "...", "...", "...", "...", "...", "...", "mlm", "lml", "...", "..."])
    s.legs(["CDDC", "CDFC", "CDDC", "CDDC", "ClmC", "CmlC", "CDDC", "CDDC", "3CD3", "....", "....", "...."],
           ["CCCC", "CACC", "CCCC", "CCAC", "CCCC", "CCCC", "CACC", "CCCC", "3C3C", "....", "....", "...."],
           left_front=["CDDC", "CFDC", "CDDC", "CDDC", "CDDC", "CDDC", "CDFC", "CDDC", "C3D3", "....", "....", "...."],
           left_back=["CCCC", "CCAC", "CCCC", "CACC", "CCCC", "CCCC", "CCAC", "CCCC", "C3C3", "....", "....", "...."])
    s.save('crazy_goblin.png')


def elder():
    s = Skin(tones=OLD_SKIN, eye=EYE_CYAN, slim=True, a1=LEAF, a2=GREY_HAIR)
    # Deep-set glowing eyes under bushy grey brows, a wrinkled brow, a drooping moustache, a long combed grey beard
    # with a cyan bead; bald on top, long grey hair behind; a hide shawl with a fringe over the shoulders.
    s.head(front=["42322324", "ZZY22YZZ", "Y1E23E1Y", "42423424", "42ZYYZ24", "4ZY22YZ4", "XZY22YZX", "XYZYYZYX"],
           top=["23333332", "34444443", "44434444", "42444424", "44444434", "44344444", "34444443", "23333332"])
    s.hat(top=["YYZYYZYY", "X______X", "Y______Y", "X______X", "Y______Y", "X______X", "Y______Y", "________"],
          back=["YYZYYZYY", "YXYYYXYY", "YXYYYXYY", "YXYZYXYY", "YXYYYXYZ", "YXYYYXYY", "YXYYYXYY", "Y_YX_YX_"],
          right=["YYZY____", "XYYX____", "XYYX____", "XYZX____", "XYYX____", "XYYXY___", "XYYXYZ__", "X_YXYZY_"],
          left=["____YZYY", "____XYYX", "____XYYX", "____XZYX", "____XYYX", "___YXYYX", "__ZYXYYX", "_YZYXY_X"])
    s.jacket(clear(12), ["YYZYYZYY", "YXYYYXYY", "YXYYYXYY", "_XY_YX_Y", "__Y___X_"] + clear(7))
    s.torso(["ddYZZYdd", "dcXZZXcd", "dcnYZncd", "d3nZYn3d", "c342X43c", "c343E43c",
             "e344443e", "33244233", "uvuuvuuv", "zyxzyxzy", "yxvyxvyx", "x3vx3vx3"],
            ["dddddddd", "dcddddcd", "ddcddcdd", "dfdffdfd", "dddddddd", "cddddddc",
             "e3ed3ede", "33433433", "uvuuvuuv", "yzxyzxyz", "xvyxvyxv", "3vx3vx3v"])
    s.arms(["ddd", "cdc", "e.e", "...", "...", "222", "...", "...", "eEe", "...", "...", "..."],
           ["ddd", "dcd", "e.e", "...", "...", "222", "...", "...", "efe", "...", "...", "..."])
    s.sleeves(["ddd", "c_c"] + clear(10, 3), ["ddd", "d_d"] + clear(10, 3))
    s.legs(["zyxz", "yxvy", "x3v3", "....", "....", "2332", "....", "....", "....", "lkkl", "kllk", "...."],
           ["yzxy", "xvyx", "3x3v", "....", "....", "....", "....", "....", "....", "lkkl", "kllk", "...."])
    s.save('goblin_elder.png')


def ruthless():
    s = Skin(a2=LILAC)
    # Angry brows, a scar on the cheek; goggles pushed up on the forehead, a strap round the head, a leather cap.
    s.head(front=face((0, '4B5665B4'), (3, '455235Q4'), (4, '44423Q44')))
    s.hat(front=["pZYppZYp", "oYXooYXo"] + clear(6),
          right=["cbcbcbpp", "bcbcbcop"] + clear(6),
          left=["ppbcbcbc", "pocbcbcb"] + clear(6),
          back=["bcbGGcbc", "cbcbcbcb"] + clear(6),
          top=["dcdqrdcd", "cdcrqcdc"] * 4)
    s.torso(["SR3443RS", "RpqrrqpR", "RpqrsqpR", "RpqqqqpR", "RopqqpoR", "RoqppqoR",
             "RoppppoR", "QNooooNQ", "bbcGGcbb", "RRSRRSRR", "RQRRRQRR", "QQQQQQQQ"],
            ["SRRRRRRS", "RbRRRRbR", "RRbRRbRR", "RRRbbRRR", "RRRbbRRR", "RRbRRbRR",
             "RbRRRRbR", "QRRRRRRQ", "bbcbbcbb", "RRSRRSRR", "RQRRRQRR", "QQQQQQQQ"])
    s.arms(["qrrq", "pqqp", "oppo", "RSSR", "QRRQ", "....", "....", "....", "pqqp", "prqp", "oppo", "...."],
           ["pqqp", "oppo", "NooN", "RRRR", "QRRQ", "....", "....", "....", "oppo", "pqqp", "oppo", "...."])
    s.sleeves(["rssr", "qrrq"] + clear(10, 4), ["qrrq", "pqqp"] + clear(10, 4))
    s.legs(["cddc", "cddc", "cdec", "cddc", "cddc", "pqqp", "oppo", "opqo", "oppo", "bccb", "bccb", "abba"],
           ["cccc", "bccb", "cccc", "cccc", "bccb", "oppo", "NooN", "oppo", "NooN", "bbbb", "bbbb", "aaaa"])
    s.pants(["RSSR", "QRRQ", "_QQ_"] + clear(9, 4), ["RRRR", "QRRQ", "_QQ_"] + clear(9, 4))
    s.save('ruthless_goblin.png')


def neutral():
    s = Skin(eye=EYE_AMBER, a1=FUR_GRAY)
    # The plain goblin: dark green brows, a grey fur mantle, a leather loincloth, linen wraps.
    s.head(front=face((1, '10011001')))
    fur = ["zzyzzzyz", "yzyyzyyy", "yyxyyyxy", "xyyyxyyy", "yyyxyyyx", "yxyyyxyy", "yyxyyyyx"]
    s.torso(["zy3443yz", "yx4554xy", "x345543x", "33422433", "34433443", "34322343",
             "34433443", "34322343", "bbcmmcbb", "cddeeddc", "3cdeedc3", "33cddc33"],
            fur + ["y3xy3yx3", "bbcbbcbb", "cddddddc", "cdcddcdc", "3cddddc3"])
    s.jacket(["zy____yz", "y______y"] + clear(10), fur[:6] + ["y_x_y_x_"] + clear(5))
    s.arms(["zyyz", "yxxy", "....", "....", "....", "....", "....", "....", "cddc", "bccb", "....", "...."],
           ["yzzy", "xyyx", "....", "....", "....", "....", "....", "....", "cccc", "bccb", "....", "...."])
    s.sleeves(["zyyz", "y__y"] + clear(10, 4), ["yzzy", "x__x"] + clear(10, 4))
    s.legs(["..cd", "..de", "...c", "....", "....", "....", "....", "....", "kllk", "lmml", "kllk", "jkkj"],
           ["....", "....", "....", "....", "....", "....", "....", "....", "kllk", "lkkl", "kllk", "jkkj"])
    s.save('neutral_goblin.png')


def giant():
    s = Skin(a1=FUR_BROWN)
    # Heavy brow over small eyes, tusks from the lower jaw.
    s.head(front=face((0, '43355334'), (2, '41E23E14'), (4, '44W23W44'), (5, '43W11W34'), (6, '34422443')))
    s.torso(["3d455433", "3de55443", "34de5543", "334de433", "3443de43", "34555de3",
             "345555d3", "34555543", "bbbGJbbb", "abbHGbba", "uvxvvxvu", "vuvxxvuv"],
            ["233333d2", "34433de3", "3243de23", "3443d443", "344de443", "34de3443",
             "3de33443", "3d433443", "bbbbbbbb", "abbbbbba", "vxvuvxvu", "uvxvuvxv"])
    s.arms(["....", "....", "....", "....", "....", "....", "....", "....", "cddc", "bGGb", "....", "...."],
           ["....", "....", "....", "....", "....", "....", "....", "....", "cccc", "bccb", "....", "...."])
    s.legs(["vxvu", "uvxv", "u..v", "....", "....", "....", "....", "....", "kllk", "lmml", "kllk", "...."],
           ["xvuv", "vuvx", "v..u", "....", "....", "....", "....", "....", "kllk", "lkkl", "kllk", "...."])
    s.pants(["vxvu", "u__v"] + clear(10, 4), ["xvuv", "v__u"] + clear(10, 4))
    s.save('giant_goblin.png')


def trader():
    s = Skin(eye=EYE_AMBER, a1=CAPE)
    # A friendly face with a gold tooth; a striped cap with a tassel.
    s.head(front=face((1, '10011001'), (5, '4wW22Gw4')))
    s.hat(top=["QmQmQmQm", "RnRnRnRn"] * 4,
          front=["RnRnRnRn"] + clear(7),
          right=["RnRnRnRn", "QmQmQmQm"] + clear(6),
          left=["nRnRnRnR", "mQmQmQmQ"] + clear(6),
          back=["nRnRnRnR", "mQmQmQmQ", "___G____", "___G____", "__GJG___"] + clear(3))
    cape = ["zzzzzzzz", "yyyyyyyy", "yyxyyxyy", "yyxyyxyy", "yyxyyxyy", "yyxyyxyy", "yyxyyxyy", "xxvxxvxx"]
    s.torso(["zyRSSRyz", "yydRSdyy", "xdemRedx", "xdemmedx", "ddemmedd", "ddemmedd",
             "ddemmedd", "bbbGGbbb", "dkkdcdGd", "djjdcded", "ddedcded", "cdddcddc"],
            cape + ["vvvvvvvv", "dddcdddd", "dddcdddd", "cddcddcc"])
    s.jacket(["zyRSSRyz", "y______y"] + clear(10), cape + clear(4))
    s.arms(["zyyz", "yyyy", "xyyx", "deed", "deed", "dccd", "deed", "deed", "ffff", "....", "....", "...."],
           ["zzzz", "yyyy", "xyyx", "dddd", "dddd", "cddc", "dddd", "dddd", "ffff", "....", "....", "...."])
    s.sleeves(["zyyz", "y__y"] + clear(10, 4), ["zzzz", "y__y"] + clear(10, 4))
    s.legs(["jkkj", "jkkj", "jkkj", "jkkj", "jkkj", "ijji", "jkkj", "jkkj", "jkkj", "cddc", "bccb", "abba"],
           ["jjjj", "jjjj", "jjjj", "jjjj", "jjjj", "ijji", "jjjj", "jjjj", "jjjj", "cccc", "bbbb", "aaaa"])
    s.pants(["deed", "deed", "dccd", "_dd_"] + clear(8, 4), ["dddd", "dddd", "cddc", "_cc_"] + clear(8, 4))
    s.save('goblin_trader.png')


def shaman():
    vials = {'!': rgba('#d83a3a'), '#': rgba('#3a6ad8'), '$': rgba('#52c43a'), '%': rgba('#a24ad8')}
    s = Skin(a1=FUR_BROWN, a2=MOSS, extra=vials)
    # Red war paint under the eyes; a hide hood with moss and feathers.
    s.head(front=face((3, '4SS23SS4')))
    s.hat(top=["xxxxxxxx", "xyyXYyyx", "yyyyyXyy", "yyyyyyyy", "yXYyyyyy", "yyyyyyXy", "yyyyyyyy", "zzzzzzzz"],
          front=["zzzzzzzz", "y______y", "y______y", "x______x", "y______y", "x______x", "y______y", "________"],
          right=["xyySyyzy", "xyyRyyyy", "xXYnyyyy", "xyXyyyyy", "xyyyyXyy", "xyyyyyyy", "xyyyyyyy", "xxyyyy__"],
          left=["yzyySyyx", "yyyyRyyx", "yyyynYXx", "yyyyyXyx", "yyXyyyyx", "yyyyyyyx", "yyyyyyyx", "__yyyyxx"],
          back=["xyyyyyyx", "xyXYyyyx", "xyyXyyyx", "xyyyyyyx", "xyyyXYyx", "xyyyyyyx", "xyyyyyyx", "x_yy_yy_"])
    s.torso(["yy3443yy", "yyn44nyy", "yyynnyyy", "xyyyXyyx", "xyXYyyyx", "xyyyyyXx",
             "xyyyyyyx", "bgbgbgbb", "b!b#b$b%", "yyyxyyyy", "yyyxyXyy", "y3x3y3x3"],
            ["xyyyyyyx", "xyXYyyyx", "xyyXyyyx", "xyyyyyyx", "xyyyXYyx", "xyyyyyyx",
             "xyyyyyyx", "bbbbbbbb", "bbbbbbbb", "yyyxyyyy", "yyyxyXyy", "3y3x3y3x"])
    s.arms(["yzzy", "y..y", "....", ".SS.", "....", "....", "....", "....", "nmmn", "....", "....", "...."],
           ["yyyy", "y..y", "....", "....", "....", "....", "....", "....", "mmmm", "....", "....", "...."])
    s.legs(["yyxy", "yyxy", "y.x.", "..x.", "....", "....", "....", "....", "XYYX", "YXXY", "XYYX", "...."],
           ["yxyy", "yxyy", "y.x.", ".x..", "....", "....", "....", "....", "YXXY", "XYYX", "YXXY", "...."])
    s.pants(["yyxy", "y_x_", "_x__"] + clear(9, 4), ["yxyy", "y_x_", "_x__"] + clear(9, 4))
    s.save('goblin_shaman.png')


def king():
    s = Skin(a1=FUR_GRAY)
    # Angry brows, a scar, gold teeth; an open gold crown with rubies.
    s.head(front=face((0, '4B5665B4'), (3, '455235Q4'), (4, '44423Q44'), (5, '4wG22Gw4'), (6, '34wGGw43')))
    s.hat(front=["J__GG__J", "GGRGGRGG"] + clear(6),
          right=["__J__J__", "GGGGGGGG"] + clear(6),
          left=["__J__J__", "GGGGGGGG"] + clear(6),
          back=["J__GG__J", "GGGRRGGG"] + clear(6))
    s.torso(["Gu3443uG", "v3nnnn3v", "34nmmn43", "34BnnB43", "34mBBm43", "34nlln43",
             "34433443", "34322343", "HGGRRGGH", "cddeeddc", "cdcddcdc", "bcbccbcb"],
            ["uvuvvuvu", "GRRSRRRG", "GRRRSRRG", "GRSRRRRG", "GRRRRSRG", "GRRSRRRG",
             "GRRRRRRG", "GRSRRRSG", "GRRRSRRG", "GRRRRRRG", "GRSRRRRG", "HGHGHGHG"])
    s.jacket(["uvu__uvu", "v______v"] + clear(10),
             ["uvuvvuvu", "GRRSRRRG", "GRRRSRRG", "GRSRRRRG", "GRRRRSRG", "GRRSRRRG",
              "GRRRRRRG", "GRSRRRSG", "GRRRSRRG", "GRRRRRRG", "GRSRRRRG", "HGHGHGHG"])
    s.arms(["....", "....", "GJJG", "HGGH", "....", "....", "....", "....", "cddc", "dGGd", "cddc", "...."],
           ["....", "....", "GGGG", "HGGH", "....", "....", "....", "....", "cccc", "cGGc", "cccc", "...."])
    s.legs(["cdde", "dcdc", "b..b", "....", "....", "....", "....", "vuvu", "cddc", "cddc", "bccb", "abba"],
           ["cddc", "cdcd", "b..b", "....", "....", "....", "....", "uvuv", "cccc", "cccc", "bbbb", "aaaa"])
    s.pants(["cdde", "d__c"] + clear(10, 4), ["cddc", "c__d"] + clear(10, 4))
    s.save('goblin_king.png')


def villager():
    s = Skin(eye=EYE_AMBER, a1=TROUSERS)
    s.head(front=face((1, '10011001')))
    s.torso(["dcm44mcd", "ddcmmcdd", "dedmlded", "dedmlded", "ddemlddd", "dedmlded",
             "dedmlded", "dddmlddd", "bbbbGbbb", "xyyxxyyx", "xyxxxxyx", "vxxvvxxv"],
            ["dddddddd", "dedddddd", "dddcdddd", "deddcddd", "dddcdddd", "dedddddd",
             "dddcdddd", "dddcdddd", "bbbbbbbb", "xyyxxyyx", "xxxxxxxx", "vxxvvxxv"])
    s.arms(["mnnm", "mmmm", "lmml", "lmml", "kllk", "....", "....", "....", "....", "....", "....", "...."],
           ["mmmm", "lmml", "lmml", "llll", "kllk", "....", "....", "....", "....", "....", "....", "...."])
    s.legs(["xyyx", "xyyx", "xyyx", "xyyx", "xyyx", "vxxv", "xyyx", "xyyx", "xyyx", "cddc", "bccb", "abba"],
           ["xxxx", "xxxx", "xxxx", "xxxx", "xxxx", "vxxv", "xxxx", "xxxx", "xxxx", "cccc", "bbbb", "aaaa"])
    s.save('goblin_villager.png')


def villager_female():
    s = Skin(eye=EYE_AMBER, slim=True, a1=DRESS, a2=AUBURN, extra={'&': rgba('#c98a6e')})
    # Lashes, a blush, small fangs; auburn hair parted in the middle, braids with gold ribbons, a yellow flower.
    s.head(front=face((1, '10011001'), (2, 'BWE23EWB'), (3, '4&5235&4'), (5, '44w22w44'), (6, '344ww443')),
           top=["XYYVYYYX", "XYYVYYZX", "XYZVYZYX", "XYYVYYYX", "XZYVYYZX", "XYYVZYYX", "XYYVYYYX", "ZYYVYYZY"],
           back=["YYZYYZYY", "YXYYYXYY", "YXYYYXYY", "YXYZYXYY", "YXYYYXYZ", "YXYYYXYY", "XXYXXYXX", "23333332"],
           right=["XYYZYYYY", "XYYYYZYY", "XYY34224", "XYY34444", "XZY33444", "XYY22224", "XYY33444", "XXX33333"],
           left=["YYYYZYYX", "YYZYYYYX", "42243YYX", "44443YYX", "44433YZX", "42222YYX", "44433YYX", "33333XXX"])
    s.hat(front=["XYZYYZYX", "Y______Y", "X______X"] + clear(5),
          top=["YYYYYYYY"] + clear(6) + ["XYZYYZYX"],
          right=["YYYYYYYY", "XYYYYJYY", "__YV____", "__VY____", "__YV____", "__VY____", "__YV____", "__JJ____"],
          left=["YYYYYYYY", "YYYYYYYX", "____VY__", "____YV__", "____VY__", "____YV__", "____VY__", "____JJ__"],
          back=["YYZYYZYY", "YXYYYXYY", "YXYYYXYY", "_X_YY_X_"] + clear(4))
    skirt_front = ["yymn", "yymn", "yymn", "yymn", "yxmm", "yymn", "yymn", "xxll"]
    skirt_back = ["yyyy", "yyyy", "yxyy", "yxyy", "yxyy", "yyyy", "yyyy", "xxxx"]
    s.torso(["xy3443yx", "yyy44yyy", "yymnnmyy", "yymnnmyy", "yymnmmyy", "xymmmmyx",
             "vvvvvvvv", "ymnnnnmy", "ymnmnnmy", "xmnnnmmx", "ymmnnmmy", "xllllllx"],
            ["yyyyyyyy", "yyyvyyyy", "yyyvyyyy", "yyyvyyyy", "yyyvyyyy", "yyyvyyyy",
             "vvnmmnvv", "yyyyyyyy", "yxyyyyxy", "yxyyyyxy", "yyyyyyyy", "xxxxxxxx"])
    s.arms(["zyz", "yyy", "yyy", "yyy", "xyx", "mnm", "...", "...", "...", "...", "...", "..."],
           ["yyy", "yyy", "yyy", "yyy", "xxx", "mmm", "...", "...", "...", "...", "...", "..."])
    s.legs(skirt_front + ["vvvv", "....", "bccb", "abba"], skirt_back + ["vvvv", "....", "bbbb", "aaaa"])
    s.pants(skirt_front + clear(4, 4), skirt_back + clear(4, 4))
    s.save('goblin_villager_female.png')


def headman():
    s = Skin(eye=EYE_AMBER, a1=COAT, a2=TROUSERS)
    # A brown hat with a gold band and a red feather.
    s.head(front=face((1, '10011001')))
    s.hat(top=["ddddddSR", "dcccccSR", "dcccccdS", "dcccccdd", "dcccccdd", "dcccccdd", "dcccccdd", "dddddddd"],
          front=["GGGGGGGG"] + clear(7),
          right=["GGGGGGGG", "bbbbbbbb"] + clear(6),
          left=["GGGGGGGG", "bbbbbbbb"] + clear(6),
          back=["GGGGGGGG", "bbbbbbbb"] + clear(6))
    s.torso(["yy3443yy", "yyG44Gyy", "yyyGGyyy", "yyyJGyyy", "yyyGxyyy", "yyyyxyyy",
             "yyyGxyyy", "bbbGGbbb", "yyyyxyyy", "yxyyxyyx", "xyyyxyyx", "HGGHHGGH"],
            ["yyyyyyyy", "yzyyyyzy", "yyyyyyyy", "yyyyyyyy", "yyyyyyyy", "yyyyyyyy",
             "yyyyyyyy", "bbbbbbbb", "yyyxxyyy", "yxyxxyxy", "xyyxxyyx", "HGGHHGGH"])
    s.arms(["zyyz", "yyyy", "yyyy", "yxyy", "yyyy", "yyyy", "yyxy", "yyyy", "GJJG", "....", "....", "...."],
           ["yyyy", "yyyy", "yyyy", "yyyy", "xyyx", "yyyy", "yyyy", "yyyy", "GGGG", "....", "....", "...."])
    s.legs(["XYYX", "XYYX", "XYYX", "XYYX", "XYYX", "VXXV", "XYYX", "XYYX", "XYYX", "cddc", "bccb", "abba"],
           ["XXXX", "XXXX", "XXXX", "XXXX", "XXXX", "VXXV", "XXXX", "XXXX", "XXXX", "cccc", "bbbb", "aaaa"])
    s.pants(["yyxy", "yxyy", "GHGH"] + clear(9, 4), ["yyyy", "yxyy", "GHGH"] + clear(9, 4))
    s.save('goblin_headman.png')


def sage():
    s = Skin(tones=OLD_SKIN, eye=EYE_CYAN, a1=ROBE)
    # The elder's brows and eyes over a long white beard; a deep blue hood.
    s.head(front=["42322324", "nnm22mnn", "m1E23E1m", "42423424", "4n4234n4", "4nmmmmn4", "nmnnnnmn", "mnnmnnnm"])
    s.hat(top=["xxxxxxxx", "xyyyyyyx", "yyyyyyyy", "yyyyyyyy", "yyyyyyyy", "yyzzzzyy", "yzzzzzzy", "zzzzzzzz"],
          front=["zzzzzzzz", "y______y", "y______y", "x______x", "y______y", "x______x", "_mn__nm_", "__nmmn__"],
          right=["xyyyyyyz", "xyyyyyyy", "xyyyyyyy", "xyyyyyyy", "xyyyyyyy", "xyyyyyyy", "xyyyyyyy", "xxyyyy__"],
          left=["zyyyyyyx", "yyyyyyyx", "yyyyyyyx", "yyyyyyyx", "yyyyyyyx", "yyyyyyyx", "yyyyyyyx", "__yyyyxx"],
          back=["xyyyyyyx", "xyyyyyyx", "xyyxyyyx", "xyyxyyyx", "xyyyyyyx", "xyyyyyyx", "xyyxyyyx", "xxxxxxxx"])
    s.torso(["xynmmnyx", "xymnnmyx", "xynmnmyx", "xyymnyyx", "xyyynyyx", "xyyyyJyx",
             "xyyyyyyx", "jkkjjkkj", "yyxGGxyy", "yyxGGxyy", "yxyGGyxy", "HGHGHGHG"],
            ["yzyyyyzy", "xyyyyyyx", "xyyxyyyx", "xyyxyyyx", "xyyxyyyx", "xyyxyyyx",
             "xyyyyyyx", "jkkjjkkj", "xyyxyyyx", "xyyxyyyx", "xyyxyyyx", "HGHGHGHG"])
    s.jacket(clear(12), ["zzzzzzzz", "yzyyyyzy", "_y_x_y_x"] + clear(9))
    s.arms(["zyyz", "yyyy", "yyyy", "yxyy", "yyyy", "yyyy", "yyxy", "yyyy", "yyyy", "xyyx", "GJJG", "...."],
           ["yyyy", "yyyy", "yyyy", "yyyy", "yyyy", "xyyy", "yyyy", "yyyy", "yyyy", "xyyx", "GGGG", "...."])
    s.legs(["yyyy", "yxyy", "yxyy", "yyyy", "yxyy", "yxyy", "yyyy", "yxyy", "yxyy", "xxxx", "GHGH", "cddc"],
           ["yyyy", "yyyy", "yyxy", "yyxy", "yyyy", "yyxy", "yyxy", "yyyy", "yyyy", "xxxx", "HGHG", "cccc"])
    s.save('goblin_sage.png')


# ---------------------------------------------------------------- faces that move
# Overlays for the head's front face, baked into the skin by GoblinFaceTextures: eyes closed, angry, worried, happy or hurt,
# and a mouth open, snarling or smiling. Only the changed pixels are opaque, so a hat or a beard on the hat layer still
# covers them. They follow each goblin's own face: its skin tones, its brow colour (sampled over the right eye) and its
# eye pixels (white, light grey or an iris colour in rows 2 and 3).
FACE_STATES = ('closed', 'angry', 'worried', 'happy', 'hurt', 'open', 'snarl', 'smile')
FACE_DIR = SKINS / 'face'
WHITE, LIGHT, BLACK = MATERIALS['W'], MATERIALS['w'], MATERIALS['B']
EYE_COLORS = {WHITE, LIGHT, EYE_RED, EYE_AMBER, EYE_CYAN}
MOUTH_DARK, TONGUE = rgba('#2a0c08'), rgba('#9a2a24')
OLD_FACES = {'goblin_elder', 'goblin_sage'}


def read_face(img):
    return [[img.getpixel((8 + x, 8 + y)) for x in range(8)] for y in range(8)]


def expression(base, tones, state):
    """{(x, y): colour}: what changes on the 8x8 face `base` for one state."""
    brow, behind = base[1][5], base[1][3]
    eyes = [(x, y) for y in (2, 3) for x in range(8) if base[y][x] in EYE_COLORS]
    lid, lash = tones[4], tones[1]
    out = {}
    if state == 'closed':
        out.update({(x, y): lash if y == 2 else lid for x, y in eyes})
    elif state == 'angry':
        # Brows slant down to the nose, black unless they are grey hair; the whites narrow to a glare.
        ire = BLACK if brow in tones else brow
        out.update({(1, 0): ire, (6, 0): ire, (1, 1): behind, (6, 1): behind, (2, 1): ire, (5, 1): ire})
        out.update({(x, y): lash for x, y in eyes if base[y][x] in (WHITE, LIGHT) and x in (0, 1, 6, 7)})
    elif state == 'worried':
        # Brows rise in the middle.
        out.update({(2, 0): brow, (5, 0): brow, (1, 1): brow, (6, 1): brow, (2, 1): behind, (5, 1): behind})
    elif state == 'happy':
        # Brows up, the eyes squeezed shut over raised cheeks.
        out.update({(1, 0): brow, (2, 0): brow, (5, 0): brow, (6, 0): brow, (1, 1): behind, (2, 1): behind, (5, 1): behind, (6, 1): behind})
        out.update({(x, y): lash if y == 2 else lid for x, y in eyes})
        out.update({(x, 3): tones[6] for x in (1, 2, 5, 6) if (x, 3) not in out})
    elif state == 'hurt':
        # Eyes screwed shut: > <
        out.update({(x, y): lid for x, y in eyes})
        out.update({(1, 1): lash, (2, 2): lash, (1, 3): lash, (6, 1): lash, (5, 2): lash, (6, 3): lash, (2, 1): behind, (5, 1): behind})
    elif state == 'open':
        # The jaw drops a pixel: the lower teeth (or lip, or beard) move down, the mouth shows dark with a tongue.
        out.update({(x, 7): base[6][x] for x in range(2, 6)})
        out.update({(2, 6): MOUTH_DARK, (3, 6): TONGUE, (4, 6): TONGUE, (5, 6): MOUTH_DARK})
    elif state == 'snarl':
        # Teeth bared in a straight clench.
        out.update({(x, 5): WHITE for x in range(2, 6)})
        out.update({(2, 6): MOUTH_DARK, (3, 6): LIGHT, (4, 6): LIGHT, (5, 6): MOUTH_DARK})
    elif state == 'smile':
        # A wide grin: the teeth reach the corners, which curl up.
        out.update({(x, 6): WHITE for x in range(1, 7)})
        out.update({(0, 5): lash, (7, 5): lash, (0, 6): tones[3], (7, 6): tones[3]})
    return out


def face_overlays():
    """face/<skin>_<state>.png for every goblin skin."""
    FACE_DIR.mkdir(parents=True, exist_ok=True)
    for name, _ in LINEUP:
        img = Image.open(SKINS / f'{name}.png').convert('RGBA')
        base, tones = read_face(img), OLD_SKIN if name in OLD_FACES else SKIN
        for state in FACE_STATES:
            overlay = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
            for (x, y), color in expression(base, tones, state).items():
                overlay.putpixel((8 + x, 8 + y), color)
            overlay.save(FACE_DIR / f'{name}_{state}.png')


def face_sheet(out=PREVIEWS / 'goblin_faces.png', scale=10):
    """Every goblin's face (with its hat) in every state."""
    states = ('normal',) + FACE_STATES
    sheet = Image.new('RGBA', (len(states) * 9 * scale + 120, len(LINEUP) * 9 * scale + 16), (196, 210, 222, 255))
    draw = ImageDraw.Draw(sheet)
    for k, state in enumerate(states):
        draw.text((120 + k * 9 * scale, 2), state, fill=(0, 0, 0, 255))
    for row, (name, _) in enumerate(LINEUP):
        img = Image.open(SKINS / f'{name}.png').convert('RGBA')
        y0 = 16 + row * 9 * scale
        draw.text((4, y0 + 4 * scale), name, fill=(0, 0, 0, 255))
        for k, state in enumerate(states):
            head = img.crop((8, 8, 16, 16))
            if state != 'normal':
                head.alpha_composite(Image.open(FACE_DIR / f'{name}_{state}.png').convert('RGBA').crop((8, 8, 16, 16)))
            head.alpha_composite(img.crop((40, 8, 48, 16)))
            sheet.alpha_composite(head.resize((8 * scale, 8 * scale), Image.NEAREST), (120 + k * 9 * scale, y0))
    sheet.save(out)
    return out


# ---------------------------------------------------------------- lineup preview
def _part(img, box, face_name, overlay):
    x, y, w, h = faces(*box)[face_name]
    out = img.crop((x, y, x + w, y + h))
    ox, oy, _, _ = faces(*overlay)[face_name]
    out.alpha_composite(img.crop((ox, oy, ox + w, oy + h)))
    return out


def _view(img, slim, back):
    """A flat front (or back) view of a skin, overlays on top: 16x32."""
    r_arm, l_arm, r_sleeve, l_sleeve = arm_boxes(slim)
    aw = r_arm[2]
    f = 'back' if back else 'front'
    parts = [(_part(img, HEAD, f, HAT), (4, 0)), (_part(img, BODY, f, JACKET), (4, 8))]
    arms = [_part(img, r_arm, f, r_sleeve), _part(img, l_arm, f, l_sleeve)]
    legs = [_part(img, R_LEG, f, R_PANTS), _part(img, L_LEG, f, L_PANTS)]
    if back:
        arms.reverse()
        legs.reverse()
    parts += [(arms[0], (4 - aw, 8)), (arms[1], (12, 8)), (legs[0], (4, 20)), (legs[1], (8, 20))]
    canvas = Image.new('RGBA', (16, 32), (0, 0, 0, 0))
    for part, at in parts:
        canvas.alpha_composite(part, at)
    return canvas


LINEUP = [('small_goblin', True), ('crazy_goblin', True), ('goblin_elder', True), ('ruthless_goblin', False),
          ('neutral_goblin', False), ('giant_goblin', False), ('goblin_trader', False), ('goblin_shaman', False),
          ('goblin_king', False), ('goblin_villager', False), ('goblin_villager_female', True), ('goblin_headman', False),
          ('goblin_sage', False)]


def lineup(out=PREVIEWS / 'goblin_skins.png', scale=6):
    cols = 7
    cell_w, cell_h = 36 * scale, 34 * scale + 14
    rows = (len(LINEUP) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * cell_w, rows * cell_h), (196, 210, 222, 255))
    draw = ImageDraw.Draw(sheet)
    for n, (name, slim) in enumerate(LINEUP):
        img = Image.open(SKINS / f'{name}.png').convert('RGBA')
        x0, y0 = (n % cols) * cell_w + scale, (n // cols) * cell_h
        draw.text((x0, y0 + 2), name, fill=(0, 0, 0, 255))
        for k, back in enumerate((False, True)):
            v = _view(img, slim, back)
            sheet.alpha_composite(v.resize((16 * scale, 32 * scale), Image.NEAREST), (x0 + k * 17 * scale, y0 + 14))
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return out


def crate():
    """The storehouse crate: spruce boards in a dark frame with a cross brace; the top is a lid with two handles."""
    out = ROOT / 'src/main/resources/assets/goblinraiders/textures/block'
    board, board_dark, frame, nail, mark = rgba('#8a6a43'), rgba('#6f5434'), rgba('#3d2b1a'), rgba('#b9b2a4'), rgba('#5f9a3c')
    rng = random.Random(77)
    for name in ('side', 'top'):
        img = Image.new('RGBA', (16, 16))
        for y in range(16):
            for x in range(16):
                c = board if (y // 4) % 2 == 0 else board_dark
                c = shade(c, rng.randint(-8, 8))
                if x in (0, 15) or y in (0, 15):
                    c = frame
                elif name == 'side' and (x == y or x == 15 - y) and 1 <= x <= 14:
                    c = shade(frame, 20)
                elif name == 'top' and y in (5, 10) and 3 <= x <= 12:
                    c = frame
                img.putpixel((x, y), c)
        for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
            img.putpixel((x, y), nail)
        if name == 'side':
            for (x, y) in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
                img.putpixel((x, y), mark)
        img.save(out / f'storehouse_crate_{name}.png')
        img.resize((256, 256), Image.NEAREST).save(PREVIEWS / f'storehouse_crate_{name}.png')


GOBLINS = [crazy, elder, ruthless, neutral, giant, trader, shaman, king, villager, villager_female, headman, sage]

if __name__ == '__main__':
    for paint in GOBLINS:
        paint()
    crate()
    face_overlays()
    print('skins written to', SKINS, '- lineup:', lineup(), '- faces:', face_sheet())
