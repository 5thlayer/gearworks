#!/usr/bin/env python3
"""Draws Gearworks' item icons and worn suit textures.

Every upgrade and worn suit shares one palette: a gunmetal frame with a dark outline, lit from the
top left, and one accent of its own. The suits share a cyan core and differ by their plates, so a
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

# A, B and C are an upgrade's accent, from shaded to glowing.
ACCENTS = {
    "battery_upgrade": ("#6e1414", "#d4302a", "#ff8a6a"),
    "solar_upgrade": ("#1c2f78", "#2f5fd0", "#9cc8ff"),
    "speed_upgrade": ("#7a330c", "#e8741c", "#ffc860"),
    "shield_upgrade": ("#0d4d68", "#1fa2d2", "#a6f2ff"),
    "jump_upgrade": ("#4c1a78", "#9c4ee0", "#e2b6ff"),
    "night_vision_upgrade": ("#1a5c26", "#46c040", "#c4ff8a"),
}

# S and Y, the solar upgrade's sun.
SUN = {"S": "#ffe680", "Y": "#ffb21c"}

ICONS = {
    "battery_upgrade": [
        "......KKKK......",
        "......KwlK......",
        "...KKKKKKKKKK...",
        "...KwlllllllK...",
        "...KlKKKKKKmK...",
        "...KlKCCCCKmK...",
        "...KlKBBBBKmK...",
        "...KlKAAAAKmK...",
        "...KlKCCCCKmK...",
        "...KlKBBBBKmK...",
        "...KlKAAAAKmK...",
        "...KlKCCCCKmK...",
        "...KlKBBBBKmK...",
        "...KlKKKKKKmK...",
        "...KmmmmmmmdK...",
        "...KKKKKKKKKK...",
    ],
    "solar_upgrade": [
        "..........S..S..",
        "...........YY...",
        "..........SYYYS.",
        "KKKKKKKKKKKYYKKK",
        "KwlllllllllllmmK",
        "KlKKKKKKKKKKKKdK",
        "KlKCBBKCBBKCBBdK",
        "KlKBBAKBBAKBBAdK",
        "KlKKKKKKKKKKKKdK",
        "KlKCBBKCBBKCBBdK",
        "KlKBBAKBBAKBBAdK",
        "KlKKKKKKKKKKKKdK",
        "KmddddddddddddKK",
        "KKKKKKKdmKKKKKK.",
        "......KdmK......",
        ".....KKKKKK.....",
    ],
    "speed_upgrade": [
        "................",
        "................",
        ".KKKK..KKKK.....",
        ".KCBK..KCBK.....",
        "..KCBK..KCBK....",
        "...KCBK..KCBK...",
        "....KCBK..KCBK..",
        ".....KCBK..KCBK.",
        ".....KBAK..KBAK.",
        "....KBAK..KBAK..",
        "...KBAK..KBAK...",
        "..KBAK..KBAK....",
        ".KBAK..KBAK.....",
        ".KKKK..KKKK.....",
        "................",
        "................",
    ],
    "shield_upgrade": [
        "................",
        ".KKKKKKKKKKKKKK.",
        ".KwllllllllllmK.",
        ".KlKKKKKKKKKKdK.",
        ".KlKCCCBBBBAKdK.",
        ".KlKCCBBKBBAKdK.",
        ".KlKCBBKCKBAKdK.",
        ".KlKBBKCCCKAKdK.",
        ".KlKBBBKCKBAKdK.",
        ".KmKBBBBKBBAKdK.",
        "..KmKBBBBBAKdK..",
        "..KmKKBBBAKKdK..",
        "...KmKKBAKKdK...",
        "....KmdKKddK....",
        ".....KKddKK.....",
        ".......KK.......",
    ],
    "jump_upgrade": [
        ".......KK.......",
        "......KCCK......",
        ".....KCCBBK.....",
        "....KCCBBBAK....",
        "...KCBBBBBAAK...",
        "...KKKKBAKKKK...",
        "......KBAK......",
        "......KBAK......",
        "...KKKKKKKKKK...",
        "...KwlllllllK...",
        "....KlmmmmdK....",
        "...KKKKKKKKKK...",
        "....KlmmmmdK....",
        "...KKKKKKKKKK...",
        "...KmmmmmmmdK...",
        "...KKKKKKKKKK...",
    ],
    "night_vision_upgrade": [
        "................",
        "................",
        "................",
        "...KKKK..KKKK...",
        "..KwllmKKwllmK..",
        ".KlKKKKmmKKKKdK.",
        "KdlKCCBKKCCBKddK",
        "KdlKCBAKKCBAKddK",
        "KdlKBAAKKBAAKddK",
        ".KmKKKKmmKKKKdK.",
        "..KmmmdKKmmmdK..",
        "...KKKK..KKKK...",
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
        palette = dict(FRAME, **SUN, **dict(zip("ABC", ACCENTS[name])))
        icon(rows, palette).save(items / f"{name}.png")
    base = Image.open(SUIT_BASE).convert("RGBA")
    for name, plates in TIERS.items():
        palette = dict(FRAME, **dict(zip("1234", plates)))
        suit = base if name == "iron_gear" else retinted(base, ("#141011",) + plates)
        suit.save(items / f"{name}.png")
        worn(palette).save(humanoid / f"{name}.png")


if __name__ == "__main__":
    main()
