# Archetype Appetites

A **compatibility patch** that teaches [Ancestral Archetypes](https://modrinth.com/mod/ancestral-archetypes) about the food and materials added by the [Polymania](https://modrinth.com/modpack/polymania) modpack, so its archetypes' diet restrictions stay consistent instead of arbitrarily rejecting modded items.

Ancestral Archetypes gives several archetypes special diets:

- **Carnivores** (cat, ocelot) can only eat things in its `carnivore_foods` tag — vanilla meat, fish, and a few meaty dishes like rabbit stew.
- The **parrot** is poisoned by things in its `chocolate_allergy_foods` tag (vanilla cookie).
- The **copper / iron / tuff golems** heal by *eating metal* — but only the specific vanilla metal items the base mod hard-codes.

Out of the box, none of Polymania's modded food or metals fit those diets. This patch fixes that, following the base mod's own rules and heal values.

## What it does

| Archetype(s) | Now also accepts |
|---|---|
| **Cat, Ocelot** (carnivore) | All conventionally-tagged meat & fish (`#c:foods/*`, `#minecraft:fishes`) — covers Farmer's Delight cuts, **Gone Fishing** fish, and other food mods — plus Farmer's Delight meat/fish **dishes** (stews, sandwiches, roasts, ham, rolls…) and Gone Fishing fish stews. |
| **Parrot** (chocolate allergy) | Farmer's Delight `hot_cocoa` and `chocolate_pie_slice`. |
| **Copper Golem** | **Every copper item in the pack** — vanilla copper (Ancestral Archetypes' own values), Blockus copper bricks/gates/lanterns (all weathering & waxed states), Friends & Foes copper buttons, PolyDecorations copper braziers/campfires/lights, PolyFactory copper plate & crushed copper, copper tools (e.g. Farmer's Delight copper knife), and copper-tuff blocks. |
| **Iron Golem** | **Every iron & steel item** — vanilla iron, Blockus iron bricks/plating/gates, gofish iron crate, Farmer's Delight iron knife, and the PolyFactory steel family (ingot, nugget, plate, mesh, gear, large gear, block, button). |
| **Tuff Golem** | **Every tuff block** — Blockus tuff tiles/pillar/paving + every brick variant (carved, cracked, mossy, herringbone, copper-tuff in all weathering/waxed states) and PolyDecorations tuff statues. |

The carnivore rule matches Ancestral Archetypes itself: a dish counts if it contains real meat or fish (the base mod allows `rabbit_stew`), while vegetarian dishes do not. Golem heal values mirror the base mod's numbers (e.g. an iron ingot heals 4 HP, so steel ingot does too).

Carnivore and chocolate-allergy diets are item **tags**, handled by data files. (Chocolate stays Farmer's Delight-only on purpose — it's the only mod in Polymania that adds a cocoa food.) The golem "metal eater" diets are Java `HashMap`s in Ancestral Archetypes that a datapack can't reach, so this mod includes a tiny Fabric entrypoint that, at server start, scans the whole item registry once and routes every item whose id has a `copper`, `iron`, `steel`, or `tuff` name-token into the matching golem map, with a heal value derived from the item's form (slab, stairs, ingot, block, …) to mirror the base mod's numbers. Steel is treated as an iron alloy. It works by reflection — no compile-time dependency on Ancestral Archetypes, existing entries are never overwritten (so AA's tuned vanilla values win), and it's a safe no-op if Ancestral Archetypes is absent.

The scan is matched by whole `_`-separated token (so `stuffed_potato` is never mistaken for `tuff`), and it skips anything edible (an entry in a golem diet would otherwise stop *non*-golem players from eating it), spawn eggs, in-world display items, the `flint_and_steel` tool, and crafting "mixture" blends (e.g. `steel_alloy_mixture`).

## Requirements

| Mod | Required? | Why |
|-----|-----------|-----|
| [Ancestral Archetypes](https://modrinth.com/mod/ancestral-archetypes) | **Yes** | The mod being patched. |
| Fabric API | **Yes** | Server-lifecycle hook for the golem patch. |
| [Farmer's Delight Refabricated](https://modrinth.com/mod/farmers-delight-refabricated) | Recommended | Adds the meat/fish dishes & cocoa foods (bundled in Polymania, id `farmersdelight`). |
| [PolyFactory](https://modrinth.com/mod/polyfactory) | Recommended | Adds steel + the copper/iron items the golems eat (also fed by Blockus, Friends & Foes, PolyDecorations, etc. — any mod is covered automatically). |
| [Gone Fishing!](https://modrinth.com/mod/gone-fishing) | Suggested | Adds the extra fish & fish stews (id `go-fish`). |

Minecraft **1.21.5 – 1.21.11** (Fabric). Every modded entry is optional at runtime, so the patch loads cleanly even if some of those mods are absent.

## Install

Download `archetype-appetites-<version>.jar` from [Releases](https://github.com/KazooKat/archetype-appetites/releases) and drop it in your server's `mods/` folder alongside the mods above. No config needed.

## Build from source

It's a normal Fabric mod, built with Fabric Loom — only a JDK is required (Gradle and the JDK 21 toolchain are fetched automatically):

```bash
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

The finished jar is written to `build/libs/`. CI ([`.github/workflows/release.yml`](.github/workflows/release.yml)) runs the same build and attaches the jar to a GitHub Release on every `v*` tag.

## Project layout

```
src/main/java/com/kazookat/archetypeappetites/ArchetypeAppetites.java   # golem metal-eater bridge
src/main/resources/
  fabric.mod.json
  data/ancestralarchetypes/tags/item/
    carnivore_foods.json            # cat / ocelot additions (tags + dishes)
    chocolate_allergy_foods.json    # parrot additions
```

## License

[OSL-3.0](LICENSE).
