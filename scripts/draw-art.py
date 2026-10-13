#!/usr/bin/env python3
"""Draws Gearworks' item icons and worn suit textures.

The upgrades share the flight chestplate's metal, lit from the top left, and one accent each. The
worn suits share a gunmetal frame with a dark outline. The suits share a cyan core and differ by their plates, so a
new tier's art is a new entry in TIERS; the tier still needs its item model, its equipment asset and
its place in ArtTest. Run from the repository root; it overwrites the PNGs it draws.

The suits' icons are Futureazoo's flight chestplate (art/flight_chestplate.png, CC BY-NC-SA 4.0,
github.com/Futureazoo/TextureRepository), its grey plates retinted to each tier's and its cyan core
kept. Iron gear keeps the original's own greys.

    python3 scripts/draw-art.py
"""

from pathlib import Path

from PIL import Image

ROOT = Path("src/main/resources/assets/gearworks/textures")
SUIT_BASE = Path("art/flight_chestplate.png")

FRAME = {
    ".": None,
    "K": "#1b1e24",  # outline
    "d": "#363c45",  # gunmetal, shaded
    "m": "#58616c",  # gunmetal
    "l": "#8a949f",  # gunmetal, lit
    "w": "#c8d0d8",  # gunmetal, highlight
    "o": "#2fb8d8",  # core
    "O": "#c8f8ff",  # core, glowing
}

# The upgrades' metal is the flight chestplate's: an outline and a ramp of purple-tinged greys.
UPGRADE_FRAME = {
    ".": None,
    "K": "#414047",  # outline
    "d": "#57555f",
    "m": "#6e6b78",
    "n": "#888591",
    "l": "#a2a0aa",
    "L": "#bab9c0",
    "w": "#dcdbdf",
    "W": "#f6f6f7",  # highlight
}

# A to D are an upgrade's accent, from shaded to glowing; the shield's are the suits' core.
ACCENTS = {
    "battery_upgrade": ("#7a1a1a", "#c42a22", "#ff5a3c", "#ffb08a"),
    "solar_upgrade": ("#1a2a6a", "#2a4fb0", "#4f86e8", "#b8dcff"),
    "speed_upgrade": ("#8a3a0e", "#d8641a", "#ff9a2e", "#ffd890"),
    "shield_upgrade": ("#0e5a72", "#2bbdc6", "#36d0da", "#c6fafd"),
    "jump_upgrade": ("#4c1f7a", "#7e3fc4", "#a86ce8", "#e2c4ff"),
    "night_vision_upgrade": ("#1d5a2a", "#2f9a3a", "#5ad04a", "#c8ff9a"),
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
}

# Each tier's plates, 1 to 4 from shaded to highlight; the netherite tier's are dark, so its trim lights them.
TIERS = {
    "iron_gear": ("#5e6266", "#9a9fa4", "#cdd1d5", "#f2f4f6"),
    "diamond_gear": ("#16716c", "#2fb9ad", "#74e8dc", "#c8fff8"),
    "netherite_gear": ("#2b2427", "#4a3f43", "#6e5d62", "#a08a8f"),
}

# The humanoid layer is 64x32; the chest slot draws its body (u 16-40, v 16-32) and arms (u 40-56).
# Each map below is one face, placed at its u, v.
BODY_FRONT = [  # 8x12 at 20, 20
    "43222221",
    "3dmllmd1",
    "32dmmd21",
    "32KOoK21",
    "32KooK21",
    "dd2KK2dd",
    "mmmmmmmd",
    "33221111",
    "mmmmmmmd",
    "32211111",
    "dmmmmmmd",
    "Kddddddd",
]
BODY_BACK = [  # 8x12 at 32, 20
    "32222221",
    "2dddddd1",
    "2dmmmmd1",
    "2dmoomd1",
    "2dmoomd1",
    "2dmmmmd1",
    "2dddddd1",
    "mmmmmmmd",
    "32211111",
    "mmmmmmmd",
    "dmmmmmmd",
    "Kddddddd",
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
    "2111",
    "dmmd",
    "Kddd",
]
BODY_TOP = [  # 8x4 at 20, 16
    "33222221",
    "3dmllmd1",
    "3dmmmmd1",
    "22222211",
]
BODY_BOTTOM = [  # 8x4 at 28, 16
    "dddddddd",
    "dddddddd",
    "dddddddd",
    "dddddddd",
]
ARM_SIDE = [  # 4x12, at 40, 44, 48 and 52, 20
    "4332",
    "3221",
    "2211",
    "1111",
    "dmmd",
    "moom",
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
    """A plate pixel, not one of the core's or its glass ring's, which lean blue."""
    return rgb[2] - rgb[0] < 20


def retinted(base, plates):
    """The base with its greys moved onto the plates' ramp by brightness; anything else is kept."""
    image = base.copy()
    greys = [p for p in base.get_flattened_data() if p[3] and is_grey(p)]
    lows, highs = min(sum(p[:3]) for p in greys), max(sum(p[:3]) for p in greys)
    ramp = [color(plate) for plate in plates]
    for y in range(base.height):
        for x in range(base.width):
            pixel = base.getpixel((x, y))
            if pixel[3] and is_grey(pixel):
                t = (sum(pixel[:3]) - lows) / (highs - lows) * (len(ramp) - 1)
                i = min(int(t), len(ramp) - 2)
                f = t - i
                image.putpixel((x, y), tuple(round(a + (b - a) * f) for a, b in zip(ramp[i], ramp[i + 1])))
    return image


def worn(palette):
    image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint(image, BODY_TOP, palette, 20, 16)
    paint(image, BODY_BOTTOM, palette, 28, 16)
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
        palette = dict(UPGRADE_FRAME, **SUN, **dict(zip("ABCD", ACCENTS[name])))
        icon(rows, palette).save(items / f"{name}.png")
    base = Image.open(SUIT_BASE).convert("RGBA")
    for name, plates in TIERS.items():
        palette = dict(FRAME, **dict(zip("1234", plates)))
        suit = base if name == "iron_gear" else retinted(base, ("#141011",) + plates)
        suit.save(items / f"{name}.png")
        worn(palette).save(humanoid / f"{name}.png")


if __name__ == "__main__":
    main()
