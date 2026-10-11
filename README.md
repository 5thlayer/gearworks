# Gearworks

<!-- The maintainer's section for players goes here: what Gearworks is, why it exists and how it feels to play. -->

**Not released yet.** Gearworks is being designed and built; the design is
[5thlayer/factoryworks#666](https://github.com/5thlayer/factoryworks/issues/666).

**NeoForge, Minecraft 26.1.2 only.**

## Planned

- **One suit, three tiers.** Gear is worn in the chest slot, giving the armor of an iron, diamond or netherite chestplate, and each tier has a larger **Gear Grid**.
- **A grid to lay out.** Gear Upgrades cover rectangles of the grid: battery, solar, speed, jump, shield and night vision, and Flight at the top.
- **Levels with no cap.** At the **Gear Bench** an upgrade's Level rises, each costing more than the last; speed, jump and shield approach a Ceiling, and Flight's FE draw falls toward a Floor.
- **Powered by FE.** The suit stores power only in its battery upgrades, and any mod's charger fills it.

## For pack authors

Every amount, rate, cost and curve will be datapack data, and top Levels cost materials through `c:` tags with vanilla defaults (netherite, nether stars).

## For mod authors

A mod adds a Gear Upgrade with an item and an entry in Gearworks' data map naming its size and Stat. Gearworks owns every behaviour; no mod ships code for Gear.

## Developing

A 5thlayer Library, built on the libworks convention plugin. See `GLOSSARY.md` for the domain glossary. `sh ./gradlew build` runs the JUnit tests on a plain JVM, and `sh ./gradlew runGameTestServer` the game tests headless. Releases go through `sh ./gradlew release` (`docs/agents/releases.md`).

## License

MIT. Source is on [GitHub](https://github.com/5thlayer/gearworks).
