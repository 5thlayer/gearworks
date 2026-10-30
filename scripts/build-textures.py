#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT
"""Builds sprites from Malcolm Riley's unused textures in data/art/ (NOTICE).

The Gear Bench's top is the workbench top with the suits' orange core let into the middle of its dark
grid: a six-pixel gem, outlined in the core's darkest shade and lit from the top left like every
Gearworks sprite, its shades the CORE ramp scripts/draw-art.py recolors the suits' cores to. The suit
the bench holds floats above it.

The Railgun's icons are the electric drill's base, mirrored to point to the top right as a crossbow's
icon is held, its greys and its wooden grip retinted to the netherite suit's plates and its copper coil
to the suits' core, with a barrel of two copper rails run from its head's socket to the corner. Charged,
the rails and the bore between them glow an electric blue. The barrel has no outline, so either edge,
which first person sees nearly edge-on, is a rail's colour.

Run from the repository root; it needs Pillow. `--check` builds and compares pixels, not PNG bytes,
which another Pillow may encode differently, and fails when a committed texture is stale.
"""

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "data" / "art"
BLOCK = ROOT / "src" / "main" / "resources" / "assets" / "gearworks" / "textures" / "block"
ITEM = BLOCK.parent / "item"

# draw-art.py's CORE, darkest to lightest.
CORE = ("#5a1a08", "#b8401a", "#f07a24", "#ffb85a", "#fff0c8")
# The gem, by its shade in CORE; "." keeps the workbench top. Its top-left corner sits at GEM_AT.
GEM = (
    ".0000.",
    "043320",
    "034221",
    "032211",
    "022110",
    ".0000.",
)
GEM_AT = (5, 5)

# draw-art.py's netherite plates, darkest to lightest, with a shade darker and one lighter at either end.
NETHERITE = ("#141011", "#2b2427", "#4a3f43", "#6e5d62", "#a08a8f", "#cdb9be")
COPPER = ("#6d3a28", "#b4684d", "#e0905f")
SPARK = ("#2a7fd0", "#4fb8ff", "#c8f0ff")
# The barrel, by its diagonal across from the muzzle at the top right: the bore on x + y = 14, a rail
# either side, from the socket's edge at x = 9 out to the corner.
BORE, RAILS, BARREL_FROM = 14, (13, 15), 9


def color(hex_code):
    return tuple(int(hex_code[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def bench_top():
    image = Image.open(ART / "device_workbench_top.png").convert("RGBA")
    pixels = image.load()
    left, top = GEM_AT
    for y, row in enumerate(GEM):
        for x, shade in enumerate(row):
            if shade != ".":
                pixels[left + x, top + y] = color(CORE[int(shade)])
    return image


def luminance(rgb):
    return (rgb[0] * 299 + rgb[1] * 587 + rgb[2] * 114) / 1000


def ramp(shades, light):
    """The shade of the ramp at that light, from 0 to 1, blended between its two nearest."""
    at = max(0.0, min(1.0, light)) * (len(shades) - 1)
    low, high = color(shades[int(at)]), color(shades[min(len(shades) - 1, int(at) + 1)])
    t = at - int(at)
    return tuple(round(a + (b - a) * t) for a, b in zip(low, high))


def is_copper(rgb):
    """The coil's shades, far redder than green; not the grip's wood, nor the cord's red plug, with hardly any green."""
    r, g, b = rgb[:3]
    return r - g > 45 and g > 30


def is_tinted(rgb):
    """The grip's wood and the cord's plug, which are no grey."""
    r, g, b = rgb[:3]
    return r - b > 20


def railgun(charged):
    drill = Image.open(ART / "tool_drill_base_electric.png").convert("RGBA").transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    image = Image.new("RGBA", drill.size, (0, 0, 0, 0))
    lightest = max(luminance(drill.getpixel((x, y))) for y in range(16) for x in range(16) if drill.getpixel((x, y))[3])
    for y in range(16):
        for x in range(16):
            pixel = drill.getpixel((x, y))
            if not pixel[3]:
                continue
            light = luminance(pixel) / lightest
            if is_copper(pixel):
                image.putpixel((x, y), ramp(CORE[:4], light))
            elif is_tinted(pixel):
                image.putpixel((x, y), ramp(NETHERITE[:3], light * 1.6))
            else:
                image.putpixel((x, y), ramp(NETHERITE, light))
    barrel = {}
    for y in range(16):
        for x in range(BARREL_FROM, 16):
            if x + y == BORE:
                barrel[x, y] = SPARK[2] if charged else NETHERITE[0]
            elif x + y in RAILS and x > BARREL_FROM:
                lit = x + y == RAILS[0]
                barrel[x, y] = (SPARK[1] if lit else SPARK[0]) if charged else COPPER[2] if lit else COPPER[1]
    for (x, y), shade in barrel.items():
        image.putpixel((x, y), color(shade))
    return image


def current(path, image):
    if not path.is_file():
        return False
    committed = Image.open(path).convert("RGBA")
    return committed.size == image.size and committed.tobytes() == image.tobytes()


TEXTURES = {
    BLOCK / "gear_bench" / "top.png": bench_top,
    ITEM / "railgun.png": lambda: railgun(False),
    ITEM / "railgun_charged.png": lambda: railgun(True),
}


def main():
    check = "--check" in sys.argv
    stale = False
    for path, build in TEXTURES.items():
        image = build()
        if check:
            fresh = current(path, image)
            print(("ok   %s is current" if fresh else "FAIL %s is stale -- re-run scripts/build-textures.py")
                  % path.relative_to(ROOT))
            stale = stale or not fresh
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path)
        print(f"wrote {path.relative_to(ROOT)}")
    return 1 if stale else 0


if __name__ == "__main__":
    raise SystemExit(main())
