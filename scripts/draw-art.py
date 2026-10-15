#!/usr/bin/env python3
"""Draws Gearworks' item icons and worn suit textures.

Every upgrade and worn suit shares the flight chestplate's metal, lit from the top left. The
upgrades add one accent each; the suits share an orange core and differ by their plates, so a new
tier's art is a new entry in TIERS; the tier still needs its item model, its equipment asset and
its place in ArtTest. Run from the repository root; it overwrites the PNGs it draws.

The suits' icons are Futureazoo's flight chestplate (art/flight_chestplate.png, CC BY-NC-SA 4.0,
github.com/Futureazoo/TextureRepository), its cyan core recolored orange and its grey plates retinted to
each tier's. Iron gear keeps the original's own greys. The worn suits are drawn here in iron's greys and
retinted the same way, so each tier's worn look and icon share one ramp.

The suits' icons pulse, and GearGlowLayer lights the worn gem with entity/gear_glow.png at the same
pulse; PULSE_PERIOD and PULSE_DIM must match GlowPulse's.

    python3 scripts/draw-art.py
"""

import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path("src/main/resources/assets/gearworks/textures")
SUIT_BASE = Path("art/flight_chestplate.png")

# The metal of every upgrade and worn suit is the flight chestplate's: an outline and a ramp of
# purple-tinged greys, and for the worn suits an orange core.
METAL = {
    ".": None,
    "K": "#414047",  # outline
    "d": "#57555f",
    "m": "#6e6b78",
    "n": "#888591",
    "l": "#a2a0aa",
    "L": "#bab9c0",
    "w": "#dcdbdf",
    "W": "#f6f6f7",  # highlight
    "o": "#f07a24",  # core
    "O": "#fff0c8",  # core, glowing
}

# A to D are an upgrade's accent, from shaded to glowing.
ACCENTS = {
    "battery_upgrade": ("#7a1a1a", "#c42a22", "#ff5a3c", "#ffb08a"),
    "solar_upgrade": ("#1a2a6a", "#2a4fb0", "#4f86e8", "#b8dcff"),
    "speed_upgrade": ("#8a3a0e", "#d8641a", "#ff9a2e", "#ffd890"),
    "shield_upgrade": ("#0e5a72", "#2bbdc6", "#36d0da", "#c6fafd"),
    "jump_upgrade": ("#4c1f7a", "#7e3fc4", "#a86ce8", "#e2c4ff"),
    "night_vision_upgrade": ("#1d5a2a", "#2f9a3a", "#5ad04a", "#c8ff9a"),
    "flight_upgrade": ("#6a1a4a", "#b82a7a", "#e85aa8", "#ffc4e6"),
}

# S and Y, the solar upgrade's sun.
SUN = {"S": "#ffe680", "Y": "#ffb21c"}

ICONS = {
    "battery_upgrade": [
        "................",
        "......KKKK......",
        ".....KLwlnK.....",
        "....KKKKKKKK....",
        "...KWwLLLlnmK...",
        "...KwddddddmK...",
        "...KwdCDDCdmK...",
        "...KwdBCCBdmK...",
        "...KwddddddmK...",
        "...KwdBCCBdmK...",
        "...KwdBCCBdmK...",
        "...KwddddddmK...",
        "...KwdBCCBdmK...",
        "...KwddddddmK...",
        "...KlnnnmmmdK...",
        "....KKKKKKKK....",
    ],
    "solar_upgrade": [
        "..........S..S..",
        "...........YY...",
        "..........SYYYS.",
        ".KKKKKKKKKKYYKK.",
        "KWwwwwwwwwwwwlnK",
        "KwdddddddddddddK",
        "KwdDCBdDCBdDCBdK",
        "KwdCBAdCBAdCBAdK",
        "KwdBAAdBAAdBAAdK",
        "KwdddddddddddddK",
        "KwdDCBdDCBdDCBdK",
        "KwdCBAdCBAdCBAdK",
        "KwdBAAdBAAdBAAdK",
        "KlddddddddddddmK",
        ".KKKKKKnmKKKKKK.",
        "......KKKK......",
    ],
    "speed_upgrade": [
        "................",
        "................",
        "KKKK..KKKK......",
        "KDCBK.KDCBK.....",
        ".KDCBK.KDCBK....",
        "..KDCBK.KDCBK...",
        "...KDCBK.KDCBK..",
        "....KDCBK.KDCBK.",
        "....KCBAK.KCBAK.",
        "...KCBAK.KCBAK..",
        "..KCBAK.KCBAK...",
        ".KCBAK.KCBAK....",
        "KCBAK.KCBAK.....",
        "KKKK..KKKK......",
        "................",
        "................",
    ],
    "shield_upgrade": [
        "................",
        "..KKKKKKKKKKKK..",
        ".KWwwwwwwwwwlnK.",
        ".KwKKKKKKKKKKmK.",
        ".KwKDDCCCCCBKmK.",
        ".KwKDCCWWCCBKmK.",
        ".KwKCCWWWWCBKmK.",
        ".KwKCCCWWCBBKmK.",
        ".KwKBCCCCBBAKmK.",
        "..KwKBCCBBAKmK..",
        "..KlKBBBBAAKnK..",
        "...KlKBBAAKnK...",
        "....KlKAAKnK....",
        ".....KlKKnK.....",
        "......KnmK......",
        ".......KK.......",
    ],
    "jump_upgrade": [
        ".......KK.......",
        "......KDDK......",
        ".....KDCCBK.....",
        "....KDCCCBBK....",
        "...KDCCCCBBAK...",
        "...KKKKCBKKKK...",
        "......KCBK......",
        "......KCAK......",
        "...KKKKKKKKKK...",
        "...KWwwLLlnmK...",
        "...KKKKKKKKKK...",
        ".....KwLlnK.....",
        "....KKKKKKKK....",
        ".....KwLlnK.....",
        "...KWwLLLlnmK...",
        "...KKKKKKKKKK...",
    ],
    "night_vision_upgrade": [
        "................",
        "................",
        "................",
        "................",
        "..KKKK....KKKK..",
        ".KWwlnK..KwwlnK.",
        "dKDDCBKmmKDDCBKd",
        "dKDCBBKddKDCBBKd",
        "dKCBBAKmmKCBBAKd",
        "dKBAAAKKKKBAAAKd",
        ".KlnnmK..KlnnmK.",
        "..KKKK....KKKK..",
        "................",
        "................",
        "................",
        "................",
    ],
    "flight_upgrade": [
        "................",
        "..........KKKK..",
        "........KKWwlK..",
        "......KKWwlnK...",
        ".....KWwlnKKDK..",
        "....KWlnKKDDCK..",
        "...KwlnKDDCCK...",
        "...KlnKDCCBBK...",
        "..KlnKDCCBBK....",
        "..KnKDCBBAAK....",
        ".KnmKCBBAKK.....",
        ".KmKCBAAK.......",
        ".KdKBAKK........",
        "KdKAKK..........",
        "KKKK............",
        "................",
    ],
}

# The suits' icons' core, from shaded to glowing, onto which the flight chestplate's cyan one moves.
CORE = ("#5a1a08", "#b8401a", "#f07a24", "#ffb85a", "#fff0c8")

# The pulse of the suits' gem, as GlowPulse has it: brightest every PERIOD ticks, dimming to DIM.
PULSE_PERIOD = 40
PULSE_DIM = 0.35
# The icons' pulse, in frames of equal length that the game blends between. An icon is never in the
# dark, so its gem dims only to ICON_DIM of its colour, not to PULSE_DIM.
PULSE_FRAMES = 8
ICON_DIM = 0.6

# The gem's glow, blended fullbright over the worn suit: the gem brighter than the worn one, so the
# glow only ever lightens it, and a halo around it at HALO_ALPHA.
GLOW = {"o": "#ffb85a", "O": "#fff0c8", "halo": "#ff9a3c"}
HALO_ALPHA = 0x50

# Each tier's plates, 1 to 4 from shaded to highlight; iron's are the flight chestplate's own greys.
TIERS = {
    "iron_gear": ("#888591", "#a2a0aa", "#cdccd1", "#f6f6f7"),
    "diamond_gear": ("#16716c", "#2fb9ad", "#74e8dc", "#c8fff8"),
    "netherite_gear": ("#2b2427", "#4a3f43", "#6e5d62", "#a08a8f"),
}

# The humanoid layer is 64x32; the chest slot draws its body (u 16-40, v 16-32) and arms (u 40-56).
# Each map below is one face, placed at its u, v. Like a chestplate's, the body opens at the neck in
# front and narrows to the waist, its last row and the sides' last three left bare, and its top and
# bottom faces are left bare, so nothing floats under the waist or lids the neck.
BODY_FRONT = [  # 8x12 at 20, 20
    "43....21",
    "322..211",
    "3dmllmd1",
    "32KOoK21",
    "32KooK21",
    "dd2KK2dd",
    "mmmmmmmd",
    "33221111",
    "mmmmmmmd",
    ".322111.",
    "..mmmd..",
    "........",
]
BODY_BACK = [  # 8x12 at 32, 20
    "43222221",
    "32222211",
    "32222211",
    "32222211",
    "32222211",
    "32222211",
    "22221111",
    "mmmmmmmd",
    "32211111",
    ".222111.",
    "..mmmd..",
    "........",
]
BODY_SIDE = [  # 4x12 at 16, 20 and 28, 20
    "3221",
    "2211",
    "2211",
    "dmmd",
    "dmmd",
    "dmmd",
    "mmmd",
    "2211",
    "mmmd",
    "....",
    "....",
    "....",
]
ARM_SIDE = [  # 4x12, at 40, 44, 48 and 52, 20
    "4332",
    "3221",
    "2211",
    "1111",
    "dmmd",
    "dmmd",
    "dmmd",
    "dmmd",
    "....",
    "....",
    "....",
    "....",
]
ARM_TOP = [  # 4x4 at 44, 16
    "4332",
    "3322",
    "3221",
    "2211",
]


def color(hex_code):
    return tuple(int(hex_code[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def paint(image, rows, palette, u=0, v=0):
    for y, row in enumerate(rows):
        assert len(row) == len(rows[0]), (rows, y)
        for x, key in enumerate(row):
            if palette[key] is not None:
                image.putpixel((u + x, v + y), color(palette[key]))


def icon(rows, palette):
    assert len(rows) == 16
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    paint(image, rows, palette)
    return image


def is_grey(rgb):
    """A plate pixel: neither the core and its glass ring, which lean blue, nor an orange core."""
    return abs(rgb[2] - rgb[0]) < 20


def is_core(rgb):
    """One of the flight chestplate's core pixels, or its glass ring's."""
    return rgb[2] - rgb[0] >= 20


def recolored(base, layers):
    """The base with each pixel that a layer picks moved onto that layer's ramp by brightness.

    A layer is a pick and a ramp, and pixels are picked in the base, so a pixel moved by one layer
    is never picked again by another. Pixels no layer picks are kept.
    """
    image = base.copy()
    pixels = [p for p in base.get_flattened_data() if p[3]]
    for pick, colors in layers:
        picked = [sum(p[:3]) for p in pixels if pick(p)]
        lows, highs = min(picked), max(picked)
        ramp = [color(c) for c in colors]
        for y in range(base.height):
            for x in range(base.width):
                pixel = base.getpixel((x, y))
                if pixel[3] and pick(pixel):
                    t = (sum(pixel[:3]) - lows) / max(highs - lows, 1) * (len(ramp) - 1)
                    i = min(int(t), len(ramp) - 2)
                    f = t - i
                    image.putpixel((x, y), tuple(round(a + (b - a) * f) for a, b in zip(ramp[i], ramp[i + 1])))
    return image


def pulse(age):
    wave = math.cos(2 * math.pi * age / PULSE_PERIOD)
    return PULSE_DIM + (1 - PULSE_DIM) * (wave + 1) / 2


def pulsing(icon, gem):
    """The icon as a strip of PULSE_FRAMES frames, its gem's pixels dimmed and brightened in turn."""
    strip = Image.new("RGBA", (icon.width, icon.height * PULSE_FRAMES), (0, 0, 0, 0))
    for frame in range(PULSE_FRAMES):
        # The brightest frame keeps the icon's own colours, the dimmest takes them down to ICON_DIM.
        fraction = (pulse(frame * PULSE_PERIOD / PULSE_FRAMES) - PULSE_DIM) / (1 - PULSE_DIM)
        shade = ICON_DIM + (1 - ICON_DIM) * fraction
        image = icon.copy()
        for x, y in gem:
            r, g, b, a = icon.getpixel((x, y))
            image.putpixel((x, y), (round(r * shade), round(g * shade), round(b * shade), a))
        strip.paste(image, (0, frame * icon.height))
    return strip


def glow():
    """The gem's glow: its pixels on the worn suit's front, and a halo on those around them."""
    image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    gem = {(x, y): key for y, row in enumerate(BODY_FRONT) for x, key in enumerate(row) if key in "oO"}
    for (x, y), key in gem.items():
        image.putpixel((20 + x, 20 + y), color(GLOW[key]))
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if (x + dx, y + dy) not in gem:
                    image.putpixel((20 + x + dx, 20 + y + dy), color(GLOW["halo"])[:3] + (HALO_ALPHA,))
    return image


# The Bore Lance: a boring head of its tier's ramp, threaded, on a dark shaft whose collar holds the
# suits' orange gem. Its icon points to the top right as a spear's does; held, it is drawn at twice the
# size pointing to the top left, for vanilla's spear_in_hand model. Each is outlined in the metal's
# outline and lit from the top left.
LANCES = {
    "iron_bore_lance": TIERS["iron_gear"],
    "diamond_bore_lance": TIERS["diamond_gear"],
    "netherite_bore_lance": TIERS["netherite_gear"],
}
SHAFT = ("#414047", "#57555f", "#6e6b78")


def lance_sprite(size, tip, axis, cone, radius, ramp):
    """A lance of that size, its tip at the pixel tip and pointing back along the unit axis to the handle.

    Each pixel is placed by how far back from the tip it lies and how far off the axis: the cone
    widens to radius over cone pixels, the collar and its gem sit just behind it, and the shaft runs
    on to the corner. The thread is a band every other pixel back, slanted across the cone.
    """
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    side = (-axis[1], axis[0])
    filled = {}
    for y in range(size):
        for x in range(size):
            dx, dy = x - tip[0], y - tip[1]
            back = dx * axis[0] + dy * axis[1]
            off = dx * side[0] + dy * side[1]
            # Lit from the top left: the side facing it shades lighter.
            lit = (-side[0] - side[1]) * off > 0
            if 0 <= back <= cone and abs(off) <= back * radius / cone + 0.5:
                thread = int((back + off * 0.7) / 1.6) % 2
                shade = 2 + lit - thread if back > 1 else 3
                filled[x, y] = ramp[max(0, min(3, shade))]
            elif cone < back <= cone + 2 and abs(off) <= radius * 0.55:
                filled[x, y] = CORE[3] if abs(off) < 0.75 and lit else CORE[2] if abs(off) < 0.75 else SHAFT[2]
            elif cone + 2 < back and abs(off) <= 0.75:
                filled[x, y] = SHAFT[2] if lit else SHAFT[1]
    for (x, y), shade in filled.items():
        image.putpixel((x, y), color(shade))
    for y in range(size):
        for x in range(size):
            if (x, y) in filled:
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                image.putpixel((x, y), color(METAL["K"]))
    return image


def lance_icon(ramp):
    return lance_sprite(16, (14, 1), (-0.7071, 0.7071), 7.5, 2.6, ramp)


def lance_in_hand(ramp):
    return lance_sprite(32, (1, 1), (0.7071, 0.7071), 11, 3.6, ramp)


def worn(palette):
    image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint(image, BODY_SIDE, palette, 16, 20)
    paint(image, BODY_FRONT, palette, 20, 20)
    paint(image, BODY_SIDE, palette, 28, 20)
    paint(image, BODY_BACK, palette, 32, 20)
    paint(image, ARM_TOP, palette, 44, 16)
    for u in (40, 44, 48, 52):
        paint(image, ARM_SIDE, palette, u, 20)
    return image


def main():
    items = ROOT / "item"
    humanoid = ROOT / "entity/equipment/humanoid"
    humanoid.mkdir(parents=True, exist_ok=True)
    for name, rows in ICONS.items():
        palette = dict(METAL, **SUN, **dict(zip("ABCD", ACCENTS[name])))
        icon(rows, palette).save(items / f"{name}.png")
    base = Image.open(SUIT_BASE).convert("RGBA")
    iron = worn(dict(METAL, **dict(zip("1234", TIERS["iron_gear"]))))
    gem = [(x, y) for y in range(base.height) for x in range(base.width) if base.getpixel((x, y))[3] and is_core(base.getpixel((x, y)))]
    animation = {"animation": {"frametime": PULSE_PERIOD // PULSE_FRAMES, "interpolate": True}}
    for name, plates in TIERS.items():
        layers = [(is_core, CORE)]
        if name != "iron_gear":
            layers.append((is_grey, ("#141011",) + plates))
        pulsing(recolored(base, layers), gem).save(items / f"{name}.png")
        (items / f"{name}.png.mcmeta").write_text(json.dumps(animation, indent=2) + "\n")
        recolored(iron, layers[1:]).save(humanoid / f"{name}.png")
    glow().save(ROOT / "entity/gear_glow.png")
    for name, ramp in LANCES.items():
        lance_icon(ramp).save(items / f"{name}.png")
        lance_in_hand(ramp).save(items / f"{name}_in_hand.png")


if __name__ == "__main__":
    main()
