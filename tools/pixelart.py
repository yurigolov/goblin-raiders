"""Small helpers for making Minecraft-style pixel-art textures.

A sprite is a list of equal-length strings; each character is a palette key,
'.' is transparent. Use `outline()` to add a dark border around everything.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
PREVIEW_DIR = ROOT / "tools" / "previews"


def hex_rgba(color):
    color = color.lstrip("#")
    if len(color) == 6:
        color += "ff"
    return tuple(int(color[i:i + 2], 16) for i in range(0, 8, 2))


def blank(w=16, h=16):
    return [["." for _ in range(w)] for _ in range(h)]


def in_egg(x, y):
    """True for pixels inside the standard 16x16 spawn-egg silhouette."""
    nx = (x - 7.5) / 5.6
    ny = (y - 9.0) / (6.4 if y >= 9 else 7.4)
    return nx * nx + ny * ny <= 1.0


def outline(grid, key="O", diagonal=False):
    """Paint `key` on every empty pixel that touches a filled one."""
    h, w = len(grid), len(grid[0])
    steps = [(1, 0), (-1, 0), (0, 1), (0, -1)]
    if diagonal:
        steps += [(1, 1), (1, -1), (-1, 1), (-1, -1)]
    out = [row[:] for row in grid]
    for y in range(h):
        for x in range(w):
            if grid[y][x] != ".":
                continue
            for dx, dy in steps:
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and grid[ny][nx] not in (".", key):
                    out[y][x] = key
                    break
    return out


def render(grid, palette):
    h, w = len(grid), len(grid[0])
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), hex_rgba(palette[ch]))
    return img


def save(grid, palette, texture_path, name, scale=24):
    """Write the real texture plus an enlarged preview on a checkerboard."""
    img = render(grid, palette)
    texture_path = ROOT / texture_path
    texture_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(texture_path)

    big = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    bg = Image.new("RGBA", big.size, (0, 0, 0, 255))
    for y in range(img.height):
        for x in range(img.width):
            shade = (200, 200, 200, 255) if (x + y) % 2 else (170, 170, 170, 255)
            bg.paste(shade, (x * scale, y * scale, (x + 1) * scale, (y + 1) * scale))
    bg.alpha_composite(big)
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
    preview = PREVIEW_DIR / f"{name}.png"
    bg.save(preview)
    print(f"texture: {texture_path}\npreview: {preview}")
