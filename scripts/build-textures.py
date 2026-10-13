#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT
"""Builds sprites from Malcolm Riley's unused textures in data/art/ (NOTICE).

The Gear Bench's top is the workbench top with the suits' orange core let into the middle of its dark
grid: a six-pixel gem, outlined in the core's darkest shade and lit from the top left like every
Gearworks sprite, its shades the CORE ramp scripts/draw-art.py recolors the suits' cores to. The suit
the bench holds floats above it.

Run from the repository root; it needs Pillow. `--check` builds and compares pixels, not PNG bytes,
which another Pillow may encode differently, and fails when a committed texture is stale.
"""

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "data" / "art"
BLOCK = ROOT / "src" / "main" / "resources" / "assets" / "gearworks" / "textures" / "block"

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


def current(path, image):
    if not path.is_file():
        return False
    committed = Image.open(path).convert("RGBA")
    return committed.size == image.size and committed.tobytes() == image.tobytes()


TEXTURES = {
    BLOCK / "gear_bench" / "top.png": bench_top,
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
