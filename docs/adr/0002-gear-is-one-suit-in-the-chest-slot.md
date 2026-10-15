---
status: accepted
---

# Gear is one suit in the chest slot

ADR 0005 renames the suit the Suit and makes Gear the family it belongs to; this decision stands.

Gear is a single suit worn in the chest slot, and only it holds a Gear Grid; the head, leg and foot
slots stay vanilla. One grid gives the player one layout to plan and one FE pool to manage, as
Factorio's armor does, and gives the endgame one item to cost. Its three tiers each give the armor of
the chestplate of their material, so the suit is never weaker than what it replaces.

## Considered Options

- **A grid in each of four armor pieces.** Spreads a player's upgrades over four screens and four FE
  pools, and makes "which piece holds the battery" a chore rather than a choice.
- **Armor and held tools, each with a grid.** Widens the Module past its first release; a carried piece
  can still come later as Gear of its own.

## Consequences

- The suit's item id and its grid's saved contents go into worlds, so splitting it into pieces later
  breaks saves (ADR 0001).
