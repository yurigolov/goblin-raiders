"""Structure for the dev game tests: a 12x6 stone pen with barrier walls.

Run:  python tools/structures/test_pen.py
Writes src/main/resources/data/goblinraiders/structure/test_pen.nbt (uses the NBT writer of the schematics toolkit,
see goblin_village.py for its location).

The test runner force-loads only the chunks the structure covers, so everything a test uses must be inside it.
"""
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCHEM = Path(os.environ.get('SCHEMATICS_DIR', r'C:\Users\yurig\.claude\minecraft\schematics'))
sys.path.insert(0, str(SCHEM))
from lib import nbt  # noqa: E402

DATA_VERSION = 3955          # Minecraft 1.21.1
W, H, L = 14, 5, 8           # walls on the outer ring, floor at y = 0

PALETTE = ["minecraft:air", "minecraft:stone", "minecraft:barrier"]
AIR, STONE, BARRIER = range(3)

blocks = []
for x in range(W):
    for y in range(H):
        for z in range(L):
            wall = x in (0, W - 1) or z in (0, L - 1)
            state = STONE if y == 0 else BARRIER if wall and y <= 3 else AIR
            blocks.append(nbt.Compound({
                "pos": nbt.List(nbt.INT, [nbt.Int(x), nbt.Int(y), nbt.Int(z)]),
                "state": nbt.Int(state),
            }))

root = nbt.Compound({
    "DataVersion": nbt.Int(DATA_VERSION),
    "size": nbt.List(nbt.INT, [nbt.Int(W), nbt.Int(H), nbt.Int(L)]),
    "palette": nbt.List(nbt.COMPOUND, [nbt.Compound({"Name": nbt.String(n)}) for n in PALETTE]),
    "blocks": nbt.List(nbt.COMPOUND, blocks),
    "entities": nbt.List(nbt.COMPOUND, []),
})

out = ROOT / "src/main/resources/data/goblinraiders/structure/test_pen.nbt"
out.parent.mkdir(parents=True, exist_ok=True)
nbt.write(out, "", root)
print(f"structure: {out}")
