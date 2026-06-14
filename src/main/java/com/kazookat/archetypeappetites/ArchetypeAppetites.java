package com.kazookat.archetypeappetites;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Carnivore / chocolate-allergy diets are item tags, handled entirely by this mod's data files.
 *
 * <p>The golem "metal eater" diets are NOT tags - Ancestral Archetypes keeps them in Java
 * {@code HashMap<Item, Tuple<Float,Integer>>} fields ({@code COPPER_FOODS}, {@code IRON_FOODS},
 * {@code TUFF_FOODS}) and, at runtime, dynamically attaches a CONSUMABLE component to any held item
 * found in the matching map (eat time = {@code tuple.getB()/20s}) then heals {@code tuple.getA()}.
 * A datapack cannot reach those maps.
 *
 * <p>This initializer bridges the gap by reflecting into those maps at server start (no compile-time
 * dependency on Ancestral Archetypes; a safe no-op if it is absent). Rather than hard-code per-mod
 * item lists, it scans the whole item registry once and routes every item whose id has a
 * {@code copper} / {@code iron} / {@code steel} / {@code tuff} name-token into the right map, with a
 * heal value derived from the item's form (mirroring Ancestral Archetypes' own numbers). Steel is
 * treated as an iron alloy. Existing entries are never overwritten, so AA's tuned vanilla values win.
 *
 * <p>Guards keep the diets clean: items that are edible, spawn eggs, in-world display items, the
 * {@code flint_and_steel} tool and crafting "mixture" blends are skipped (an edible entry would also
 * stop non-golem players from eating it, since AA cancels metal/tuff eating for non-eaters).
 */
public class ArchetypeAppetites implements ModInitializer {
    public static final String MOD_ID = "archetype_appetites";
    private static final Logger LOG = LoggerFactory.getLogger("ArchetypeAppetites");
    private static final String AA_REGISTRY = "net.borisshoes.ancestralarchetypes.ArchetypeRegistry";

    @Override
    public void onInitialize() {
        // SERVER_STARTING fires after the item registry is frozen and Ancestral Archetypes is loaded.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> applyGolemFoods());
    }

    private static void applyGolemFoods() {
        final Class<?> registry;
        try {
            registry = Class.forName(AA_REGISTRY);
        } catch (ClassNotFoundException e) {
            LOG.info("Ancestral Archetypes not present - skipping golem metal-food patch.");
            return;
        }

        Map<Item, Tuple<Float, Integer>> copper = mapOf(registry, "COPPER_FOODS");
        Map<Item, Tuple<Float, Integer>> iron = mapOf(registry, "IRON_FOODS");
        Map<Item, Tuple<Float, Integer>> tuff = mapOf(registry, "TUFF_FOODS");

        int c = 0, i = 0, t = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            String path = id.getPath();
            if (skip(item, path)) {
                continue;
            }
            if (copper != null && hasToken(path, "copper")) {
                c += add(copper, item, copperHeal(path));
            }
            if (iron != null && (hasToken(path, "iron") || hasToken(path, "steel"))) {
                i += add(iron, item, ironHeal(path));
            }
            if (tuff != null && hasToken(path, "tuff")) {
                t += add(tuff, item, tuffHeal(path));
            }
        }
        LOG.info("Archetype Appetites: registered golem foods (+{} copper, +{} iron/steel, +{} tuff).", c, i, t);
    }

    /** Items that carry a metal name-token but must never become golem food. */
    private static boolean skip(Item item, String path) {
        return path.endsWith("spawn_egg")
                || item instanceof SpawnEggItem
                || path.endsWith("_world")          // in-world display items (e.g. PolyFactory placed gears)
                || hasToken(path, "mixture")        // e.g. polyfactory:steel_alloy_mixture (coal/redstone blend)
                || path.equals("flint_and_steel")
                || item.getDefaultInstance().has(DataComponents.FOOD); // never gate an edible item
    }

    private static int add(Map<Item, Tuple<Float, Integer>> map, Item item, float[] hv) {
        if (item == Items.AIR) {
            return 0;
        }
        return map.putIfAbsent(item, new Tuple<>(hv[0], (int) hv[1])) == null ? 1 : 0;
    }

    /** True if {@code token} is a whole '_'-separated segment of {@code path} (so "stuffed" != "tuff"). */
    private static boolean hasToken(String path, String token) {
        for (String part : path.split("_")) {
            if (part.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTool(String path) {
        return hasToken(path, "knife") || hasToken(path, "sword") || hasToken(path, "pickaxe")
                || hasToken(path, "axe") || hasToken(path, "hoe") || hasToken(path, "shovel")
                || hasToken(path, "spear") || hasToken(path, "shears");
    }

    // Heal values {amount, durationTicks} mirror Ancestral Archetypes' vanilla numbers, by item form.
    // Weathering state is not scaled (modded variants all use the base form value).

    private static float[] copperHeal(String path) {
        if (path.endsWith("pressure_plate")) return new float[]{0.6f, 6};
        if (path.endsWith("button")) return new float[]{0.3f, 3};
        if (path.endsWith("nugget")) return new float[]{0.1f, 1};
        if (path.endsWith("ingot") || path.endsWith("plate")) return new float[]{1.0f, 9};
        if (hasToken(path, "raw")) return new float[]{1.0f, 20};
        if (path.endsWith("ore")) return new float[]{1.5f, 20};
        if (path.endsWith("slab")) return new float[]{1.5f, 12};
        if (path.endsWith("stairs") || path.endsWith("wall") || path.endsWith("gate")) return new float[]{2.0f, 16};
        if (path.endsWith("trapdoor")) return new float[]{1.75f, 14};
        if (path.endsWith("door")) return new float[]{1.25f, 10};
        if (path.endsWith("bars") || path.endsWith("chain") || hasToken(path, "lantern")) return new float[]{0.9f, 8};
        if (hasToken(path, "bulb")) return new float[]{4.0f, 29};
        if (isTool(path)) return new float[]{3.0f, 15};
        if (path.endsWith("block")) return new float[]{2.0f, 16};
        return new float[]{3.0f, 22}; // bricks, cut, chiseled, grate, tiles, pillar, brazier, campfire, etc.
    }

    private static float[] ironHeal(String path) {
        if (path.endsWith("pressure_plate")) return new float[]{2.0f, 16};
        if (path.endsWith("button")) return new float[]{1.0f, 8};
        if (path.endsWith("nugget")) return new float[]{0.4f, 2};
        if (path.endsWith("ingot")) return new float[]{4.0f, 20};
        if (path.endsWith("plate") || path.endsWith("mesh")) return new float[]{4.0f, 20};
        if (hasToken(path, "raw")) return new float[]{2.0f, 20};
        if (path.endsWith("ore")) return new float[]{3.0f, 25};
        if (path.endsWith("slab")) return new float[]{2.0f, 16};
        if (path.endsWith("stairs") || path.endsWith("wall") || path.endsWith("gate")) return new float[]{4.0f, 24};
        if (path.endsWith("trapdoor")) return new float[]{10.0f, 50};
        if (path.endsWith("door")) return new float[]{6.0f, 30};
        if (path.endsWith("bars") || path.endsWith("chain")) return new float[]{3.0f, 15};
        if (hasToken(path, "large") && hasToken(path, "gear")) return new float[]{10.0f, 40};
        if (path.endsWith("gear")) return new float[]{5.0f, 25};
        if (isTool(path)) return new float[]{5.0f, 15};
        if (hasToken(path, "brick") || hasToken(path, "bricks") || hasToken(path, "plating")
                || hasToken(path, "tile") || hasToken(path, "tiles") || hasToken(path, "crate")) {
            return new float[]{4.0f, 24}; // decorative blocks - a few ingots' worth
        }
        if (path.endsWith("block")) return new float[]{25.0f, 125}; // storage block (9 ingots)
        return new float[]{4.0f, 20};
    }

    private static float[] tuffHeal(String path) {
        if (path.endsWith("slab")) return new float[]{2.0f, 16};
        if (path.endsWith("stairs") || path.endsWith("wall")) return new float[]{4.0f, 24};
        if (path.endsWith("button")) return new float[]{0.5f, 4};
        if (path.endsWith("pressure_plate")) return new float[]{1.0f, 10};
        if (hasToken(path, "brick") || hasToken(path, "bricks")) return new float[]{4.0f, 24};
        if (hasToken(path, "polished")) return new float[]{3.0f, 22};
        return new float[]{2.5f, 20}; // tiles, pillar, paving, carved, cracked, mossy, statue, plain
    }

    @SuppressWarnings("unchecked")
    private static Map<Item, Tuple<Float, Integer>> mapOf(Class<?> registry, String field) {
        try {
            return (Map<Item, Tuple<Float, Integer>>) registry.getField(field).get(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOG.warn("Could not access {}.{} ({}); skipping that golem diet.", AA_REGISTRY, field, e.toString());
            return null;
        }
    }
}
