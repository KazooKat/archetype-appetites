package com.kazookat.archetypeappetites;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carnivore / chocolate-allergy diets are item tags, so they are handled purely by the data
 * files in this mod. The golem "metal eater" diets are NOT tags - Ancestral Archetypes keeps
 * them in Java {@code HashMap<Item, Tuple<Float,Integer>>} fields with per-item heal values, so
 * a datapack cannot reach them.
 *
 * <p>This initializer bridges that gap: at server start it reflects into Ancestral Archetypes'
 * {@code ArchetypeRegistry} maps and registers Polymania metals (PolyFactory copper / steel) with
 * heal values matched to the base mod's own numbers. Reflection means we need no compile-time
 * dependency on Ancestral Archetypes, and everything is wrapped so a missing mod is a no-op.
 */
public class ArchetypeAppetites implements ModInitializer {
    public static final String MOD_ID = "archetype_appetites";
    private static final Logger LOG = LoggerFactory.getLogger("ArchetypeAppetites");

    private static final String AA_REGISTRY = "net.borisshoes.ancestralarchetypes.ArchetypeRegistry";

    // itemId -> { healAmount, durationTicks }, mirroring Ancestral Archetypes' own values.
    private static final Map<String, float[]> COPPER_FOODS = new LinkedHashMap<>();
    private static final Map<String, float[]> IRON_FOODS = new LinkedHashMap<>();
    private static final Map<String, float[]> TUFF_FOODS = new LinkedHashMap<>();

    static {
        // --- copper_golem archetype (COPPER_FOODS): AA copper ingot = 1.0/9, raw copper = 1.0/20 ---
        COPPER_FOODS.put("polyfactory:copper_plate", new float[]{1.0f, 9});
        COPPER_FOODS.put("polyfactory:crushed_raw_copper", new float[]{1.0f, 20});

        // --- iron_golem archetype (IRON_FOODS): AA iron ingot = 4.0/20, raw = 2.0/20, nugget = 0.4/2 ---
        // Steel is treated as an iron-family alloy.
        IRON_FOODS.put("polyfactory:crushed_raw_iron", new float[]{2.0f, 20});
        IRON_FOODS.put("polyfactory:steel_nugget", new float[]{0.4f, 2});
        IRON_FOODS.put("polyfactory:steel_ingot", new float[]{4.0f, 20});
        IRON_FOODS.put("polyfactory:steel_plate", new float[]{4.0f, 20});
        IRON_FOODS.put("polyfactory:steel_mesh", new float[]{3.0f, 15});
        IRON_FOODS.put("polyfactory:steel_gear", new float[]{5.0f, 25});
        IRON_FOODS.put("polyfactory:large_steel_gear", new float[]{10.0f, 40});

        // --- tuff_golem archetype (TUFF_FOODS): no Polymania mod adds tuff variants yet ---
    }

    @Override
    public void onInitialize() {
        // SERVER_STARTING fires after datapacks/registries are ready and Ancestral Archetypes is loaded.
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

        int added = feed(registry, "COPPER_FOODS", COPPER_FOODS)
                + feed(registry, "IRON_FOODS", IRON_FOODS)
                + feed(registry, "TUFF_FOODS", TUFF_FOODS);

        LOG.info("Registered {} Polymania metal item(s) for copper/iron/tuff golem archetypes.", added);
    }

    @SuppressWarnings("unchecked")
    private static int feed(Class<?> registry, String fieldName, Map<String, float[]> entries) {
        if (entries.isEmpty()) {
            return 0;
        }

        final Map<Item, Tuple<Float, Integer>> map;
        try {
            map = (Map<Item, Tuple<Float, Integer>>) registry.getField(fieldName).get(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOG.warn("Could not access {}.{} ({}); skipping that golem diet.", AA_REGISTRY, fieldName, e.toString());
            return 0;
        }

        int added = 0;
        for (Map.Entry<String, float[]> entry : entries.entrySet()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.getKey()));
            if (item == Items.AIR) {
                continue; // that mod/item isn't installed
            }
            float[] value = entry.getValue();
            // putIfAbsent: never clobber a value Ancestral Archetypes already defined.
            if (map.putIfAbsent(item, new Tuple<>(value[0], (int) value[1])) == null) {
                added++;
            }
        }
        return added;
    }
}
