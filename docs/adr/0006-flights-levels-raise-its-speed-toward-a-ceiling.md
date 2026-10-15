---
status: accepted
---

# Flight's Levels raise its speed toward a Ceiling

Flight flew at vanilla's creative speed whatever its Level, which made a fresh suit too fast, and its
Levels only lowered its draw toward a Floor. A Flight Upgrade now reads its `amount` as its speed, a
fraction of vanilla's creative flying speed, and its Levels raise it toward a `flightCeiling` while
still lowering the draw. The Library's own upgrade starts near half of vanilla's speed and passes it
at Level 9. This amends ADR 0003's Consequences: Flight is on or off, one to a suit, yet has a
Ceiling as well as a Floor, because its speed is what grows.

## Considered Options

- **Speed growing linearly.** Levels have no cap, so enough of them would make flight outrun the
  world loading around the player.
- **A hard cap with the Level still rising.** Rejected in ADR 0003: Levels past the cap would be paid
  for and give nothing.
- **Levels that only lower the draw, as before.** Leaves a Level 0 suit as fast as creative flight.
- **Speed from config, ignoring the data map.** Flight would read its numbers unlike every other Stat,
  and a Consumer's flight upgrade could not set its own.

## Consequences

- A Consumer's flight upgrade has its `amount` read for the first time, so an entry written before
  this flies at that fraction of vanilla's speed (a breaking change, ADR 0001).
- The suit sets the flying speed only while it is the reason the player flies: not in creative or
  spectator, nor while another mod grants flight. It leaves a mark that saves with the player while it
  does, and puts vanilla's speed back once when it stops, so it never overwrites a speed it did not
  set and a player who logs in without the suit is not left slow.
- A suit still holds one Flight Upgrade; Levels, not a second upgrade, raise its speed.
