# Changelog

Each version has two sections: Players, what a player or pack developer sees, and Consumers, what a mod building against Gearworks can use or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

### Players

- **Iron Gear**, the tier-1 suit, worn in the chest slot with an iron chestplate's armor. Its 5×5 **Gear Grid** opens from the worn suit with a key (J by default), and holds the first three Gear Upgrades: **Battery** (1×2, 100,000 FE), **Solar** (1×1, 5 FE/t in daylight under open sky) and **Speed** (2×2, about +26% walking speed while it has FE, drawing 20 FE/t while walking). Any mod's charger fills the suit. Speed upgrades sum toward the `speedCeiling` server config (+100% by default) and never reach it. (#1)

### Consumers

- An item with a `gearworks:gear_upgrade` item data map entry (`width`, `height`, `stat`, `amount`, optional `draw`) fits the Gear Grid with no code; `gearworks:gear_grid` sizes each suit's grid. The suit exposes `Capabilities.Energy.ITEM` over the sum of its batteries. (#1)
