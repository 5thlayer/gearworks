# Gearworks

Gear: a suit the player wears in the chest slot, whose Gear Grid holds upgrades that run on FE and
level without a cap, and a Bore Lance that is both spear and mining tool. It plays alone in vanilla, and
is the -works suite's Sink that drains the player.

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
The family of things Gearworks makes for the player to wear or carry: the Suit and the Bore Lance.
_Avoid_: equipment (that is belt equipment in the suite)

**Suit**:
The Gear the player wears in the chest slot, in three tiers. Each tier gives the armor of the
chestplate of its material and has a larger Gear Grid; a tier is made from the one below and keeps its
upgrades. Its item ids still say `gear`, from before the family had a second member.
_Avoid_: Gear (the family), armor, power armor

**Gear Grid**:
The two-dimensional area in a piece of Gear that holds its upgrades, each covering a rectangle of it.
It opens anywhere. Only the Suit has one.
_Avoid_: equipment grid, upgrade grid

**Gear Upgrade**:
An item that fits a Gear Grid and gives the Suit one Stat. Any mod can make one.
_Avoid_: equipment, module, augment

**Level**:
How far a Gear Upgrade has been raised, carried by the upgrade itself. It has no cap, and each level
costs more than the last by a constant factor.
_Avoid_: tier (a tier is the suit's), rank

**Stat**:
One effect Gearworks gives the Suit, such as battery, solar, speed, jump, shield, night vision, flight
or boost. Gearworks owns every Stat; a Gear Upgrade only names which one it gives and how much.
_Avoid_: ability, perk, attribute (vanilla's attributes are one way a Stat may act)

**Ceiling**:
The value a Stat approaches but never reaches however many Levels it has. It holds for the suit's
total: two upgrades of one Stat together stay under it.
_Avoid_: cap, max

**Floor**:
The value a Stat's upgrade approaches but never goes under as its Level lowers it: an FE draw, as
night vision's, Flight's and Boost's are, so the Stat never becomes free, or a Cooldown, so a Boost
never comes back at once.

**Boost**:
The Stat that throws the flying player forward along their gaze for a moment, as a firework throws a
gliding one, at a key's press. Each costs FE, pushes for its Thrust and starts a Cooldown; Levels make
it cheaper toward its Floor, quicker to come back toward the Cooldown's Floor, and stronger toward its
Ceiling. A suit holds one.
_Avoid_: dash, afterburner, boost module, boosted jump (Jump's)

**Thrust**:
The moment of a Boost that pushes the player, a fixed time however high its Level, which landing or
flight ending cuts short.
_Avoid_: burn, impulse

**Cooldown**:
The time after a Boost before the next one can fire, starting with its Thrust and never shorter than
it. Levels shorten it toward its Floor.
_Avoid_: recharge, delay

**Gear Bench**:
The placed block where the player raises the Level of one Gear Upgrade inside a suit. Automation stocks
its cost; the player raises one Level at a time. The suit it holds floats above it, turning.
_Avoid_: upgrade station, workbench, anvil

### Bore Lance

**Bore Lance**:
The Gear that is both a spear and a mining tool, in three tiers of iron, diamond and netherite, each
made from the one below. Each tier has Lunge built in and mines rock up to its material's level. The
iron tier wears out and breaks; the diamond tier wears out but never breaks, stopping at none left
until repaired; the netherite tier runs on FE, which a worn Suit tops up, and is dry without the FE for
a Bore's block, when it still fights and mines single blocks; one holding less than a lunge's cost, dry
or not, doesn't lunge. It has no Gear Grid.
_Avoid_: spear (vanilla's), drill, powered spear

**Bore**:
The block of rock a Bore Lance breaks at once, laid on the face it hit and three deep: 1×1, 3×3 or
5×3 wide by tier. A key steps through its tier's Bore and the smaller ones; sneaking breaks one block,
which is no Bore.
_Avoid_: AoE, area, mode, pattern

**Lunge Boring**:
A Bore Lance breaking its Bore through whatever its tier mines in its path while Lunge carries the
player forward, until a block beyond its tier stops it. It costs far more than mining the same blocks.
_Avoid_: dash mining, tunnelling
