# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Cover and icon, in the suite's style (Fieldworks' and Craftworks' make-cover.py): a Gear Grid running
# past the edges, its upgrades drawn from Gearworks' own icons (scripts/draw-art.py) at their real sizes,
# so nothing of Mojang's is in them (libworks ADR 0012). Run from the repo root with Pillow installed.
import os
import random

from PIL import Image, ImageDraw, ImageFont

ITEMS = 'src/main/resources/assets/gearworks/textures/item/'
BG = (24, 26, 32)
ACCENT = (240, 140, 40)  # the suit's orange core
FADE = 90  # 0 leaves the grid at full strength, 255 hides it
# Each Gear Upgrade and the rectangle it covers, columns by rows, as the data map sizes them.
UPGRADES = [('battery', 1, 2), ('solar', 1, 1), ('speed', 2, 2), ('shield', 2, 2), ('jump', 1, 2),
            ('night_vision', 1, 1), ('flight', 2, 5), ('boost', 1, 3)]

_cache = {}


def tex(name, px):
    if (name, px) not in _cache:
        icon = Image.open(f'{ITEMS}{name}_upgrade.png').convert('RGBA')
        _cache[name, px] = icon.resize((px, px), Image.NEAREST)
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
    # a Gear Grid running past the edges: upgrades covering their rectangles of it, and empty cells between
    rng = random.Random(seed)
    img = Image.new('RGBA', (W, H), BG + (255,))
    d = ImageDraw.Draw(img)
    g = s // 8
    pitch = s + g
    cols, rows = W // pitch + 2, H // pitch + 2
    ox, oy = -(pitch // 2), -(pitch // 3)
    used = [[False] * cols for _ in range(rows)]

    def fits(r, c, cw, ch):
        return all(r + j < rows and c + i < cols and not used[r + j][c + i]
                   for j in range(ch) for i in range(cw))

    for r in range(rows):
        for c in range(cols):
            if used[r][c]:
                continue
            x, y = ox + c * pitch, oy + r * pitch
            tries = rng.sample(UPGRADES, len(UPGRADES)) if rng.random() < 0.6 else []
            pick = next((u for u in tries if fits(r, c, u[1], u[2])), None)
            if not pick:
                used[r][c] = True
                slot(d, x, y, s, s, s)
                continue
            name, cw, ch = pick
            for j in range(ch):
                for i in range(cw):
                    used[r + j][c + i] = True
            w, h = cw * pitch - g, ch * pitch - g
            slot(d, x, y, w, h, s, ACCENT + (255,))
            px = max(16, min(w, h) * 3 // 4 // 16 * 16)  # whole pixels of the 16-pixel icon
            img.alpha_composite(tex(name, px), (x + (w - px) // 2, y + (h - px) // 2))
    return img


def font(names, size):
    for n in names:
        if os.path.exists(n):
            return ImageFont.truetype(n, size)
    raise SystemExit('needs one of ' + ', '.join(names))


def make(W, H, out, title_size, s, seed):
    img = grid(W, H, s, seed)
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


make(1280, 640, 'publish/gearworks-cover.png', 150, 72, 7)
make(512, 512, 'publish/gearworks-icon.png', 80, 56, 7)
