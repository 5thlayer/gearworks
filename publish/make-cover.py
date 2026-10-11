# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, in the suite's style (Fieldworks' and Craftworks' make-cover.py), until an
# in-game screenshot or an artist's art replaces them. Run from the repo root with Pillow installed. The
# item textures come straight from the Minecraft client jar NeoForm caches; MC_CLIENT_JAR points elsewhere.
import io
import os
import random
import zipfile

from PIL import Image, ImageDraw, ImageFont

JAR = os.environ.get('MC_CLIENT_JAR', os.path.expanduser(
    '~/.gradle/caches/neoformruntime/artifacts/minecraft_26.1.2_client.jar'))
BG = (24, 26, 32)
ACCENT = (240, 200, 60)  # energy yellow: the suit runs on FE
FADE = 110  # 0 leaves the grid at full strength, 255 hides it
# One vanilla item for each Stat a Gear Upgrade gives.
STATS = ['item/redstone', 'block/daylight_detector_top', 'item/sugar', 'item/rabbit_foot',
         'item/turtle_scute', 'item/golden_carrot', 'item/elytra', 'item/netherite_chestplate']
# The rectangles an upgrade may cover, in grid cells.
SIZES = [(1, 1), (1, 1), (2, 1), (1, 2), (2, 2), (3, 2)]

_jar = zipfile.ZipFile(JAR)
_cache = {}


def tex(name, px):
    if (name, px) not in _cache:
        raw = _jar.read(f'assets/minecraft/textures/{name}.png')
        _cache[name, px] = Image.open(io.BytesIO(raw)).convert('RGBA').resize((px, px), Image.NEAREST)
    return _cache[name, px]


def slot(d, x, y, w, h, s, edge=None):
    # a GUI slot, as in the inventory: dark bevel top-left, light bevel bottom-right
    b = max(2, s // 12)
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=(52, 54, 62))
    d.rectangle([x, y, x + w - 1, y + b - 1], fill=(30, 31, 37))
    d.rectangle([x, y, x + b - 1, y + h - 1], fill=(30, 31, 37))
    d.rectangle([x, y + h - b, x + w - 1, y + h - 1], fill=(84, 87, 98))
    d.rectangle([x + w - b, y, x + w - 1, y + h - 1], fill=(84, 87, 98))
    if edge:
        d.rectangle([x, y, x + w - 1, y + h - 1], outline=edge, width=b)


def grid(W, H, s, seed):
    # a Gear Grid running past the edges: upgrades covering rectangles of it, and empty cells between
    rng = random.Random(seed)
    img = Image.new('RGBA', (W, H), BG + (255,))
    d = ImageDraw.Draw(img)
    g = s // 8
    pitch = s + g
    cols, rows = W // pitch + 2, H // pitch + 2
    ox, oy = -(pitch // 2), -(pitch // 3)
    used = [[False] * cols for _ in range(rows)]
    k = rng.randrange(len(STATS))
    for r in range(rows):
        for c in range(cols):
            if used[r][c]:
                continue
            cw, ch = rng.choice(SIZES) if rng.random() < 0.55 else (0, 0)
            fits = cw and all(r + j < rows and c + i < cols and not used[r + j][c + i]
                              for j in range(ch) for i in range(cw))
            x, y = ox + c * pitch, oy + r * pitch
            if not fits:
                used[r][c] = True
                slot(d, x, y, s, s, s)
                continue
            for j in range(ch):
                for i in range(cw):
                    used[r + j][c + i] = True
            w, h = cw * pitch - g, ch * pitch - g
            slot(d, x, y, w, h, s, ACCENT + (255,))
            px = min(w, h) * 3 // 4
            t = tex(STATS[k % len(STATS)], px)
            img.alpha_composite(t, (x + (w - px) // 2, y + (h - px) // 2))
            k += 3
    return img


def font(names, size):
    for n in names:
        if os.path.exists(n):
            return ImageFont.truetype(n, size)
    raise SystemExit('needs one of ' + ', '.join(names))


def make(W, H, out, title_size, s):
    img = grid(W, H, s, 7)
    img = Image.alpha_composite(img, Image.new('RGBA', (W, H), BG + (FADE,)))
    cy, bh = H // 2, int(title_size * 1.6)
    band = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(band).rectangle([0, cy - bh // 2, W, cy + bh // 2], fill=(16, 17, 22, 225))
    img = Image.alpha_composite(img, band)
    d = ImageDraw.Draw(img)
    lt = max(3, title_size // 25)
    d.rectangle([0, cy - bh // 2, W, cy - bh // 2 + lt], fill=ACCENT)
    d.rectangle([0, cy + bh // 2 - lt, W, cy + bh // 2], fill=ACCENT)
    f1 = font(['/System/Library/Fonts/Supplemental/Impact.ttf', '/usr/share/fonts/TTF/impact.ttf',
               os.path.expanduser('~/.local/share/fonts/msfonts/impact.ttf')], title_size)
    f2 = font(['/System/Library/Fonts/Supplemental/Arial Black.ttf', '/usr/share/fonts/TTF/ariblk.ttf',
               os.path.expanduser('~/.local/share/fonts/msfonts/ariblk.ttf')], int(title_size * 0.62))
    a, w = 'GEAR', 'works'
    b1, b2 = d.textbbox((0, 0), a, font=f1), d.textbbox((0, 0), w, font=f2)
    w1, w2, h1, h2 = b1[2] - b1[0], b2[2] - b2[0], b1[3] - b1[1], b2[3] - b2[1]
    pad, gap, sh = int(title_size * 0.16), int(title_size * 0.1), max(3, title_size // 20)
    bw = w2 + 2 * pad
    x = (W - (w1 + gap + bw)) // 2
    y = cy - h1 // 2 - b1[1]
    d.text((x + sh - b1[0], y + sh), a, font=f1, fill=(0, 0, 0))
    d.text((x - b1[0], y), a, font=f1, fill=ACCENT)
    bx, bt = x + w1 + gap, cy - h1 // 2
    d.rounded_rectangle([bx + sh, bt + sh, bx + bw + sh, bt + h1 + sh], radius=pad, fill=(0, 0, 0))
    d.rounded_rectangle([bx, bt, bx + bw, bt + h1], radius=pad, fill=(240, 240, 236))
    d.text((bx + pad - b2[0], bt + (h1 - h2) // 2 - b2[1]), w, font=f2, fill=BG)
    img.convert('RGB').save(out)


make(1280, 640, 'publish/gearworks-cover.png', 150, 72)
make(512, 512, 'publish/gearworks-icon.png', 80, 56)
