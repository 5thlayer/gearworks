![Gearworks](https://raw.githubusercontent.com/5thlayer/gearworks/main/publish/gearworks-cover.png)

Gearworks adds one suit, **Gear**, that you wear in the chest slot and build up for the whole game. Inside it is a **Gear Grid**: lay out upgrades for battery, solar, speed, jump, shield, night vision and flight, each covering a rectangle of the grid, then raise their **Levels** with no cap. Everything runs on FE.

**Not released yet.** Gearworks is being designed and built; the design is
[5thlayer/factoryworks#666](https://github.com/5thlayer/factoryworks/issues/666).

## Why?

I wanted a suit like Mekanism's MekaSuit: one piece of armor that grows with you from the early game to the end, powered like the rest of your base.

I also wanted the layout to matter, as in Factorio's modular armor, where every module takes room in a grid and a bigger suit is a bigger grid. Levels never cap, so there is always something to spend power and materials on, but each Stat closes in on a Ceiling, so no suit breaks the game.

**NeoForge, Minecraft 26.1.2 only.** Please report anything odd on [GitHub](https://github.com/5thlayer/gearworks/issues).

## Planned

- **One suit, three tiers.** Gear is worn in the chest slot, giving the armor of an iron, diamond or netherite chestplate, and each tier has a larger Gear Grid. A tier is made from the one below and keeps its upgrades.
- **A grid to lay out.** Gear Upgrades cover rectangles of the grid: battery, solar, speed, jump, shield and night vision, and Flight at the top. The grid opens anywhere.
- **Levels with no cap.** At the **Gear Bench** an upgrade's Level rises, each costing more than the last; speed, jump and shield approach a Ceiling, and Flight's FE draw falls toward a Floor. Two upgrades of one Stat together still stay under its Ceiling.
- **Powered by FE.** The suit stores power only in its battery upgrades, and any mod's charger fills it.
- **Plays alone in vanilla.** Gearworks needs no other mod; in the -works suite it is where your factory's power goes to work on you.

## For pack authors

Every amount, rate, cost and curve will be datapack data, and top Levels cost materials through `c:` tags with vanilla defaults (netherite, nether stars).

## For mod authors

A mod adds a Gear Upgrade with an item and an entry in Gearworks' data map naming its size and Stat. Gearworks owns every behaviour; no mod ships code for Gear. `CHANGELOG.md` lists what each version changed for a mod building against it.

## Dependencies

None beyond NeoForge.

## Art

The cover and icon are drawn from vanilla Minecraft item textures, until an in-game screenshot or an artist's art replaces them (`publish/make-cover.py`).

## Developing

A 5thlayer Library, released on its own to Modrinth and CurseForge. See `GLOSSARY.md` for the domain glossary. `sh ./gradlew build` runs the JUnit tests on a plain JVM, and `sh ./gradlew runGameTestServer` the game tests headless. Releases go through the libworks plugin's `release` task (`docs/agents/releases.md`). The Modrinth slug is `gearworks-adamico`, since `gearworks` was taken there; CurseForge's is `gearworks`.

## License

MIT. Source is on [GitHub](https://github.com/5thlayer/gearworks).
Releases will be on [Modrinth](https://modrinth.com/mod/NyNdPneT) and [CurseForge](https://www.curseforge.com/projects/1739524).
