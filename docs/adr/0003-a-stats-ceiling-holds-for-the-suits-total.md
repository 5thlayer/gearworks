---
status: accepted
---

# A Stat's Ceiling holds for the suit's total

Levels have no cap, so speed, jump and shield follow a curve toward a Ceiling instead of growing
linearly. Upgrades of one Stat sum, as Factorio's exoskeletons do, and the Ceiling applies to that sum:
a second speed upgrade adds speed, but the two together stay under the one Ceiling. Battery and solar
grow linearly, since more of either never breaks the game.

## Considered Options

- **A Ceiling per upgrade.** Stacking upgrades would pass it, so it would bound nothing.
- **One upgrade per Stat.** Takes away the grid's main trade-off, more of one Stat against room for
  another.
- **A hard cap with the Level still rising.** Levels past the cap would be paid for and give nothing.

## Consequences

- An upgrade's contribution depends on what else the suit holds, so a tooltip shows the suit's total
  for the Stat, not the upgrade's value alone.
- The option rejected above, one upgrade per Stat, still fits a Stat that is on or off, which has no
  Ceiling and to which a second upgrade adds nothing: a suit holds one night vision upgrade, whose
  Levels lower its draw toward a Floor instead.
