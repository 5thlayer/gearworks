# Changelog

Each version has two sections: Players, what a player or pack developer sees, and Consumers, what a mod building against Gearworks can use or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

### Players

- **Iron Gear**, the tier-1 suit, worn in the chest slot with an iron chestplate's armor. Its 5×5 **Gear Grid** opens from the worn suit with a key (J by default), and holds the first three Gear Upgrades: **Battery** (1×2, 100,000 FE), **Solar** (1×1, 5 FE/t in daylight under open sky) and **Speed** (2×2, about +26% walking speed while it has FE, drawing 20 FE/t while walking). Any mod's charger fills the suit. Speed upgrades sum toward the `speedCeiling` server config (+100% by default) and never reach it. (#1)
- The worn suit's FE stays on the HUD, a bar and its numbers in the top-left corner, while the suit holds a battery. The `energyHud` client config hides it (`show`) or moves it to another corner (`corner`). The Config button in the mod list edits it, and the `speedCeiling` server config in single player. The suit's FE now reads with grouped digits in the client's language, on the HUD, its tooltip and its Gear Grid. (#5)
- The **Gear Bench**, a placed block that holds a suit. A click on an upgrade in its grid picks it, and Raise lifts its **Level** by one, with no cap. Each Level costs FE and `c:`-tagged materials, more by a constant factor (1.25 by default) each Level, from copper or iron and redstone up to netherite and nether stars from Level 15. Any mod's pipes, hoppers and cables stock the bench, which takes only what the next Level costs and gives nothing back; its screen shows that cost and how much of it is stocked, and Raise spends exactly it once all of it is. A Level stays on the upgrade when it moves to another suit, and each Level adds to what it gives: +50,000 FE a battery, +2 FE/t a solar, +10% walking speed and +5 FE/t a speed upgrade, whose sum still never reaches the `speedCeiling`. (#2)
- Gearworks' items are in the **Works** creative tab the -works mods share, and in no vanilla tab. The Gear Bench's empty suit slot and material slots show faded what they take, each material at the count the next Level needs. Gearworks nests libworks-runtime, so nothing more is installed.

### Consumers

- An item with a `gearworks:gear_upgrade` item data map entry (`width`, `height`, `stat`, `amount`, optional `draw`) fits the Gear Grid with no code; `gearworks:gear_grid` sizes each suit's grid. The suit exposes `Capabilities.Energy.ITEM` over the sum of its batteries. (#1)
- A `gearworks:gear_upgrade` entry takes optional `amount_per_level` and `draw_per_level`, added once per Level. The `gearworks:level_cost` item data map prices an upgrade's Levels (`energy`, `growth`, and `stages` of up to four `{tag, count}` materials each from a Level `from` on); an upgrade without an entry is never raised. An upgrade's Level is the `gearworks:level` component. The Gear Bench exposes `Capabilities.Item.BLOCK` and `Capabilities.Energy.BLOCK` on every face. (#2)
