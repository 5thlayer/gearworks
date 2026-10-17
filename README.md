![Gearworks](https://raw.githubusercontent.com/5thlayer/gearworks/main/publish/gearworks-cover.png)

Gearworks adds one **Suit**, which you wear in the chest slot and build up for the whole game, and the **Bore Lance**, a spear that mines a whole **Bore** of rock at once. Inside the Suit is a **Gear Grid**: lay out upgrades for battery, solar, speed, jump, shield, night vision, flight and boost, each covering a rectangle of the grid, then raise their **Levels** with no cap. Everything runs on FE.

**Early days.** Version 0.1.0 has the Suit, its grid and every upgrade but Boost; the Bore Lance and Boost come in the next release.

## Why?

I wanted a suit like Mekanism's MekaSuit: one piece of armor that grows with you from the early game to the end, powered like the rest of your base.

I also wanted the layout to matter, as in Factorio's modular armor, where every module takes room in a grid and a bigger suit is a bigger grid. Levels never cap, so there is always something to spend power and materials on, but each Stat closes in on a Ceiling, so no suit breaks the game.

**NeoForge, Minecraft 26.1.2 only.** Please report anything odd on [GitHub](https://github.com/5thlayer/gearworks/issues).

## Features

- **One suit, three tiers.** The Iron, Diamond and Netherite Suit are worn in the chest slot with the armor of their material's chestplate, and each tier's Gear Grid is larger: 9×3, 9×4 and 9×6. A tier is made from the one below and keeps its upgrades, their Levels and its FE.
- **A grid to lay out.** Gear Upgrades cover rectangles of the grid: Battery, Solar, Speed, Shield, Jump, Night Vision and Boost, and Flight, 2×5, which fits only the Netherite Suit. The grid opens from the worn Suit anywhere, with a key (J by default).
- **Levels with no cap.** At the **Gear Bench** an upgrade's Level rises, each costing more FE and materials than the last, from copper and redstone up to netherite and nether stars; any mod's pipes and hoppers stock the bench. Speed, jump and shield approach a Ceiling, and Flight flies faster and draws less with each Level. Two upgrades of one Stat together still stay under its Ceiling.
- **Flight and Boost.** Flight is creative-style flight while the Suit has FE, and a Suit that runs dry lets you down gently. Boost throws a flying player along their gaze, as a firework throws a glider, then cools down.
- **Powered by FE.** The Suit stores power only in its battery upgrades, solar upgrades trickle it in, and any mod's charger fills it. Its FE stays on the HUD.
- **A lance that bores.** The **Bore Lance** is a spear with Lunge built in and a mining tool in one, in iron, diamond and netherite. It breaks a whole Bore of rock at once, up to 5×3 and three deep, and a key (B by default) steps through the Bores; hold left-click while sneaking to mine one block. Its lunge bores through the rock ahead, at a far higher cost. The netherite lance runs on FE, which any charger or the worn Suit fills.
- **Plays alone in vanilla.** Gearworks needs no other mod; in the -works suite it is where your factory's power goes to work on you.

## For pack authors

Each upgrade's size, Stat, amounts and FE draw are the `gearworks:gear_upgrade` item data map, each tier's grid `gearworks:gear_grid`, and each upgrade's Level costs `gearworks:level_cost`, whose materials are `c:` tags with vanilla defaults. The Ceilings, Floors and the Netherite Bore Lance's FE costs are in the server config. The blocks a Bore Lance mines and bores are the block tag `gearworks:mineable/bore_lance`, a pickaxe's by default.

## For mod authors

A mod adds a Gear Upgrade with an item, an entry in the `gearworks:gear_upgrade` data map naming its size and Stat, and one in `gearworks:level_cost` pricing its Levels. Gearworks owns every behaviour; no mod ships code for the Suit. The Suit and the Netherite Bore Lance expose `Capabilities.Energy.ITEM`. `CHANGELOG.md` lists what each version changed for a mod building against it.

## Dependencies

None beyond NeoForge.

## Art

The cover and icon are a Gear Grid of Gearworks' own upgrade icons (`publish/make-cover.py`), until an in-game screenshot or an artist's art replaces them.

The suits' icons are [Futureazoo](https://github.com/Futureazoo/TextureRepository)'s flight chestplate, retinted for each tier, under [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/). Gearworks is never monetized while it carries them.

The Gear Bench is built from [Malcolm Riley](https://github.com/malcolmriley/unused-textures)'s unused textures, under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) (`NOTICE`).

## Developing

A 5thlayer Module, released on its own to Modrinth and CurseForge. The design is [5thlayer/factoryworks#666](https://github.com/5thlayer/factoryworks/issues/666). See `GLOSSARY.md` for the domain glossary. `sh ./gradlew build` runs the JUnit tests on a plain JVM, and `sh ./gradlew runGameTestServer` the game tests headless. Releases go through the libworks plugin's `release` task (`docs/agents/releases.md`). The Modrinth slug is `gearworks-adamico`, since `gearworks` was taken there; CurseForge's is `gearworks`.

## License

MIT, except the suits' icons, which are CC BY-NC-SA 4.0, and the Gear Bench's textures, which are CC BY 4.0 (see Art). Source is on [GitHub](https://github.com/5thlayer/gearworks).
Releases will be on [Modrinth](https://modrinth.com/mod/NyNdPneT) and [CurseForge](https://www.curseforge.com/projects/1739524).
