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
| **Copper Golem** | **Any mod's copper** tagged into the standard `c:` sub-tags (`ingots`, `nuggets`, `raw_materials`, `ores`, `storage_blocks`), plus PolyFactory `copper_plate` & `crushed_raw_copper`. |
| **Iron Golem** | **Any mod's iron** via the same `c:` sub-tags, plus PolyFactory `crushed_raw_iron` and the **steel** family (`steel_ingot`, `steel_plate`, `steel_nugget`, `steel_mesh`, `steel_gear`, `large_steel_gear`). |
| **Tuff Golem** | **Any mod's tuff blocks** — Blockus tuff tiles/pillar/paving, all the brick variants (carved, cracked, mossy, herringbone, copper-tuff + weathering/waxed states), PolyDecorations tuff statues, etc. Tuff has no convention tag, so these are matched by name (`tuff` token); edible items are never matched. |

The carnivore rule matches Ancestral Archetypes itself: a dish counts if it contains real meat or fish (the base mod allows `rabbit_stew`), while vegetarian dishes do not. Golem heal values mirror the base mod's numbers (e.g. an iron ingot heals 4 HP, so steel ingot does too).

Carnivore and chocolate-allergy diets are item **tags**, handled by data files. (Chocolate stays Farmer's Delight-only on purpose — it's the only mod in Polymania that adds a cocoa food.) The golem "metal eater" diets are Java `HashMap`s in Ancestral Archetypes that a datapack can't reach, so this mod includes a tiny Fabric entrypoint that, at server start, feeds those maps from the standard `c:` copper/iron convention tags (covering any mod), an explicit list for modded metals that don't use those sub-tags (PolyFactory), and a name-scan for `tuff` blocks (which have no convention tag). It works by reflection — no compile-time dependency on Ancestral Archetypes, existing entries are never overwritten, and it's a safe no-op if a mod is missing.

## Requirements

| Mod | Required? | Why |
|-----|-----------|-----|
| [Ancestral Archetypes](https://modrinth.com/mod/ancestral-archetypes) | **Yes** | The mod being patched. |
| Fabric API | **Yes** | Server-lifecycle hook for the golem patch. |
| [Farmer's Delight Refabricated](https://modrinth.com/mod/farmers-delight-refabricated) | Recommended | Adds the meat/fish dishes & cocoa foods (bundled in Polymania, id `farmersdelight`). |
| [PolyFactory](https://modrinth.com/mod/polyfactory) | Recommended | Adds the copper/steel the golems eat. |
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
