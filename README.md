# Archetype Appetites

A tiny **server-side compatibility patch** that teaches [Ancestral Archetypes](https://modrinth.com/mod/ancestral-archetypes) about [Farmer's Delight](https://modrinth.com/mod/farmers-delight-refabricated) food.

By default, the cat and ocelot archetypes are **carnivores** — Ancestral Archetypes only lets them eat things in its `ancestralarchetypes:carnivore_foods` item tag (vanilla meats, fish, and a couple of meaty dishes like rabbit stew). That means all of Farmer's Delight's stews, sandwiches, roasts and fish dishes are *off the menu* for them, which feels inconsistent.

This patch adds Farmer's Delight's meat and fish foods to that tag (and to the parrot's `chocolate_allergy_foods` tag), using the **same rule the base mod uses**: if a dish contains real meat or fish, a carnivore can eat it — exactly the logic that puts vanilla `rabbit_stew` on the allowed list.

It is built for use alongside the [Polymania](https://modrinth.com/modpack/polymania) modpack, which ships Farmer's Delight Refabricated, but works with any setup that has both mods.

## Requirements

| Mod | Notes |
|-----|-------|
| [Ancestral Archetypes](https://modrinth.com/mod/ancestral-archetypes) | The diet mechanic this patches. |
| [Farmer's Delight Refabricated](https://modrinth.com/mod/farmers-delight-refabricated) | Provides the food (bundled in Polymania). Mod id `farmersdelight`. |
| Fabric Loader + Fabric API | Same as the two mods above. |

- Minecraft **1.21.5 – 1.21.11** (Fabric). The tag data is version-tolerant; the mod simply declares this range.
- The patch is **data only — no code**. It depends on both mods and does nothing on its own.

## Install

Drop `archetype-appetites-<version>.jar` into your server's `mods/` folder, next to Ancestral Archetypes and Farmer's Delight. That's it — the tags merge in automatically when the server loads. Grab the jar from [Releases](https://github.com/KazooKat/archetype-appetites/releases), or build it yourself (below).

## What changes

**Carnivores (cat, ocelot) can now eat:**

- **All conventionally-tagged meat and fish** via `#c:foods/raw_meat`, `#c:foods/cooked_meat`, `#c:foods/raw_fish`, `#c:foods/cooked_fish`. This covers Farmer's Delight cuts (minced beef, patties, chicken cuts, mutton chops, bacon, fish slices, …) **and** any other food mod that uses the standard `c:foods` convention tags.
- **Farmer's Delight meat & fish dishes** (listed explicitly, because Farmer's Delight's `meals` tag mixes meat and vegetarian dishes together): beef stew, chicken soup, fish stew, baked cod stew, bone broth, noodle soup, squid ink pasta, steak & potatoes, pasta with meatballs, pasta with mutton chop, roasted mutton chops, grilled salmon, roast chicken, honey glazed ham, shepherd's pie, bacon & eggs, bacon/chicken sandwiches, hamburger, ham, smoked ham, mutton wrap, barbecue stick, cod/salmon rolls, stuffed potato, dumplings, dog food.

**Parrot's chocolate allergy** now also reacts (poison) to Farmer's Delight `hot_cocoa` and `chocolate_pie_slice`, matching how it already reacts to the vanilla cookie.

**Deliberately *not* carnivore food** (no real meat/fish — kept consistent with the base mod, which excludes vegetarian dishes): kelp rolls (kelp + rice + carrot despite the name), cabbage rolls (filling is optional), fried egg / egg sandwich / fried rice (egg is not counted as meat by Ancestral Archetypes), milk bottle, and all soups, salads, desserts and grain dishes (vegetable soup, mixed salad, ratatouille, mushroom rice, pumpkin soup, cake, fruit pies, popsicles, …).

A few judgment calls worth knowing:

- **`dog_food` is included** — it's crafted from rotten flesh and raw meat, and Ancestral Archetypes already lets carnivores eat rotten flesh.
- **`cabbage_rolls` is excluded** — its filling can be entirely vegetarian, so meat isn't guaranteed.
- **`squid_ink_pasta` is included** — its recipe contains real fish, not just ink.

## How it works

Ancestral Archetypes gates eating with a runtime tag check (`stack.is(ancestralarchetypes:carnivore_foods)`). Minecraft merges item tags across all mods and datapacks, so this patch just ships its own additions to that tag — it never edits or overrides the base mod's entries (`"replace": false`). All Farmer's Delight entries are marked `"required": false`, so if an item is missing in a particular version the tag still loads cleanly instead of erroring.

## Build

The jar is just a zip of `src/main/resources/` (a valid Fabric mod needs nothing more for a data-only patch), so no Gradle or Minecraft toolchain is required.

```bash
# Linux / macOS / Git Bash (needs `zip`)
bash scripts/build.sh

# Windows PowerShell
pwsh scripts/build.ps1   # or: powershell -File scripts/build.ps1
```

The jar lands in `dist/`. CI ([`.github/workflows/release.yml`](.github/workflows/release.yml)) builds the same way and attaches the jar to a GitHub Release whenever a `v*` tag is pushed.

## License

[OSL-3.0](LICENSE).
