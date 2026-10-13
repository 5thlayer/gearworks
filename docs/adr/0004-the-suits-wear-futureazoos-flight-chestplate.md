---
status: accepted
---

# The suits wear Futureazoo's flight chestplate, so Gearworks never earns

The suits' icons are Futureazoo's flight chestplate, from github.com/Futureazoo/TextureRepository,
retinted per tier. It reads as a powered suit with the cyan core the rest of Gearworks' art shares,
which no permissively licensed chestplate we found did. Its license, CC BY-NC-SA 4.0, allows
retinting but binds the result: the three icons and the original stay CC BY-NC-SA, and no copy of
Gearworks may earn money.

## Considered Options

- **Our own suit icon, drawn in its spirit.** Stays MIT with no strings; kept as the fallback.
- **Futureazoo's permission for a CC BY grant.** Lifts both clauses; not asked for.
- **Suits from CC BY or MIT sets** (Foreck's, cleannrooster's rune armor, unused-textures). Medieval
  or magic armor, not a powered suit.
- **Relicensing Gearworks.** Lifts nothing: NonCommercial binds whoever ships the textures, whatever
  the mod's own license, and ShareAlike already only binds those files.

## Consequences

- Monetization is off for Gearworks on Modrinth, and the CurseForge account stays out of its
  Rewards Program, which covers every project at once. Turning either on means first replacing the
  three icons with MIT art.
- `REUSE.toml` annotates the icons and `art/flight_chestplate.png` as CC BY-NC-SA 4.0; the code and
  every other texture stay MIT. The README credits Futureazoo, which the license's attribution needs.
- A pack that bundles the jar itself, rather than listing it, takes on NonCommercial with it.
