---
status: accepted
---

# Gear is a family, and the Suit is its chest piece

Gear named the chest-slot suit alone until the Bore Lance came: a spear that mines, which belongs with
the suit but holds no grid. Gear now names the family, the suit is the Suit, and the Bore Lance is
Gear without a Gear Grid. ADR 0002's decision stands: one Suit in the chest slot holds the only grid.

## Considered Options

- **The Bore Lance outside Gear.** Keeps Gear meaning the suit, but leaves the Library's two items
  with no word for both, and a held piece ADR 0002 left room for would need one anyway.
- **Renaming the item ids to `suit`.** Breaks saves and every Consumer and pack that names them
  (ADR 0001), to change names no player reads.
- **Renaming Gear Grid, Gear Upgrade and Gear Bench to Suit's.** Breaks the words Consumers build
  against, and shuts the door on a later piece of Gear holding a grid.

## Consequences

- Item and data ids keep `gear` where they mean the Suit (`iron_gear`, the `gear_upgrade` data map);
  display names and docs say Suit.
- Gear Grid, Gear Upgrade and Gear Bench keep their names though only the Suit has a grid.
