# Gearworks

Gear: one suit the player wears in the chest slot, whose Gear Grid holds upgrades that run on FE and
level without a cap. It plays alone in vanilla, and is the -works suite's Sink that drains the player.

## Language

The Library's terms, each with what it is and the words to avoid. `/domain-modeling` adds them as they are resolved.

### Parties

**Gearworks**:
This Library.

**Consumer**:
A mod that builds against this Library, such as another Library, or one that adds Gear Upgrades.
_Avoid_: client (collides with the game's client side), dependent, integration

### Gear

**Gear**:
The suit the player wears in the chest slot, in three tiers. Each tier gives the armor of the
chestplate of its material and has a larger Gear Grid; a tier is made from the one below and keeps its
upgrades.
_Avoid_: equipment (that is belt equipment in the suite), armor, power armor

**Gear Grid**:
The two-dimensional area in a piece of Gear that holds its upgrades, each covering a rectangle of it.
It opens anywhere.
_Avoid_: equipment grid, upgrade grid

**Gear Upgrade**:
An item that fits a Gear Grid and gives the Gear one Stat. Any mod can make one.
_Avoid_: equipment, module, augment

**Level**:
How far a Gear Upgrade has been raised, carried by the upgrade itself. It has no cap, and each level
costs more than the last by a constant factor.
_Avoid_: tier (a tier is the suit's), rank

**Stat**:
One effect Gearworks gives Gear, such as battery, solar, speed, jump, shield, night vision or flight.
Gearworks owns every Stat; a Gear Upgrade only names which one it gives and how much.
_Avoid_: ability, perk, attribute (vanilla's attributes are one way a Stat may act)

**Ceiling**:
The value a Stat approaches but never reaches however many Levels it has. It holds for the suit's
total: two upgrades of one Stat together stay under it.
_Avoid_: cap, max

**Floor**:
The FE draw that a Stat's upgrade approaches but never goes under as its Level lowers the draw, as
night vision's and Flight's do, so the Stat never becomes free.

**Gear Bench**:
The placed block where the player raises the Level of one Gear Upgrade inside a suit. Automation stocks
its cost; the player raises one Level at a time. The suit it holds floats above it, turning.
_Avoid_: upgrade station, workbench, anvil
