package com.kazookat.archetypeappetites;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carnivore / chocolate-allergy diets are item tags, handled entirely by this mod's data files.
 * The golem "metal eater" diets are NOT tags - Ancestral Archetypes stores them in Java
 * {@code HashMap<Item, Tuple<Float,Integer>>} fields with per-item heal values, so a datapack
 * cannot reach them.
 *
 * <p>This initializer bridges that gap at server start, via reflection (no compile-time dependency
 * on Ancestral Archetypes; a safe no-op if it is absent). It feeds the golem maps two ways:
 * <ol>
 *   <li><b>By convention tag</b> - every copper/iron item any mod tags into the standard
 *       {@code c:ingots/*}, {@code c:nuggets/*}, {@code c:raw_materials/*}, {@code c:ores/*} and
 *       {@code c:storage_blocks/*} sub-tags, with heal values scaled by the item's form.</li>
 *   <li><b>By explicit id</b> - modded metal items that don't sit in those sub-tags
 *       (e.g. PolyFactory plates, gears, crushed materials and the steel family).</li>
 * </ol>
 * Heal values mirror Ancestral Archetypes' own numbers, and existing entries are never overwritten.
 */
public class ArchetypeAppetites implements ModInitializer {
    public static final String MOD_ID = "archetype_appetites";
    private static final Logger LOG = LoggerFactory.getLogger("ArchetypeAppetites");
    private static final String AA_REGISTRY = "net.borisshoes.ancestralarchetypes.ArchetypeRegistry";

    // Explicit modded metals NOT covered by the metal-specific convention sub-tags.
    // itemId -> { healAmount, durationTicks }.
    private static final Map<String, float[]> COPPER_ITEMS = new LinkedHashMap<>();
    private static final Map<String, float[]> IRON_ITEMS = new LinkedHashMap<>();

    static {
        // PolyFactory copper-family (AA: copper ingot 1.0/9, raw copper 1.0/20).
        COPPER_ITEMS.put("polyfactory:copper_plate", new float[]{1.0f, 9});
        COPPER_ITEMS.put("polyfactory:crushed_raw_copper", new float[]{1.0f, 20});

        // PolyFactory iron / steel-family, steel treated as an iron alloy (AA: iron ingot 4.0/20, nugget 0.4/2).
        IRON_ITEMS.put("polyfactory:crushed_raw_iron", new float[]{2.0f, 20});
        IRON_ITEMS.put("polyfactory:steel_nugget", new float[]{0.4f, 2});
        IRON_ITEMS.put("polyfactory:steel_ingot", new float[]{4.0f, 20});
        IRON_ITEMS.put("polyfactory:steel_plate", new float[]{4.0f, 20});
        IRON_ITEMS.put("polyfactory:steel_mesh", new float[]{3.0f, 15});
        IRON_ITEMS.put("polyfactory:steel_gear", new float[]{5.0f, 25});
        IRON_ITEMS.put("polyfactory:large_steel_gear", new float[]{10.0f, 40});
    }

    /** A copper/iron convention sub-tag mapped to an Ancestral Archetypes map + heal value. */
    private record TagRule(String field, String tag, float heal, int duration) {}

    private static final List<TagRule> TAG_RULES = List.of(
            // copper_golem (COPPER_FOODS)
            new TagRule("COPPER_FOODS", "c:ingots/copper", 1.0f, 9),
            new TagRule("COPPER_FOODS", "c:nuggets/copper", 0.1f, 1),
            new TagRule("COPPER_FOODS", "c:raw_materials/copper", 1.0f, 20),
            new TagRule("COPPER_FOODS", "c:ores/copper", 1.5f, 20),
            new TagRule("COPPER_FOODS", "c:storage_blocks/copper", 9.0f, 80),
            // iron_golem (IRON_FOODS)
            new TagRule("IRON_FOODS", "c:ingots/iron", 4.0f, 20),
            new TagRule("IRON_FOODS", "c:nuggets/iron", 0.4f, 2),
            new TagRule("IRON_FOODS", "c:raw_materials/iron", 2.0f, 20),
            new TagRule("IRON_FOODS", "c:ores/iron", 3.0f, 25),
            new TagRule("IRON_FOODS", "c:storage_blocks/iron", 25.0f, 125)
    );

    @Override
    public void onInitialize() {
        // SERVER_STARTING fires after datapacks/tags are loaded and Ancestral Archetypes is initialised.
        ServerLifecycleEvents.SERVER_STARTING.register(this::applyGolemFoods);
    }

    private void applyGolemFoods(MinecraftServer server) {
        final Class<?> registry;
        try {
            registry = Class.forName(AA_REGISTRY);
        } catch (ClassNotFoundException e) {
            LOG.info("Ancestral Archetypes not present - skipping golem metal-food patch.");
            return;
        }

        int added = 0;
        added += feedById(registry, "COPPER_FOODS", COPPER_ITEMS);
        added += feedById(registry, "IRON_FOODS", IRON_ITEMS);

        HolderLookup.RegistryLookup<Item> items = server.registryAccess().lookupOrThrow(Registries.ITEM);
        for (TagRule rule : TAG_RULES) {
            added += feedByTag(registry, items, rule);
        }

        LOG.info("Registered {} modded metal item(s) for copper/iron/tuff golem archetypes.", added);
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

    private static int feedById(Class<?> registry, String field, Map<String, float[]> entries) {
        if (entries.isEmpty()) {
            return 0;
        }
        Map<Item, Tuple<Float, Integer>> map = mapOf(registry, field);
        if (map == null) {
            return 0;
        }
        int added = 0;
        for (Map.Entry<String, float[]> entry : entries.entrySet()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.getKey()));
            if (item == Items.AIR) {
                continue; // that mod/item isn't installed
            }
            float[] v = entry.getValue();
            if (map.putIfAbsent(item, new Tuple<>(v[0], (int) v[1])) == null) {
                added++;
            }
        }
        return added;
    }

    private static int feedByTag(Class<?> registry, HolderLookup.RegistryLookup<Item> items, TagRule rule) {
        Map<Item, Tuple<Float, Integer>> map = mapOf(registry, rule.field());
        if (map == null) {
            return 0;
        }
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(rule.tag()));
        var holders = items.get(tag);
        if (holders.isEmpty()) {
            return 0; // no mod contributes to this tag
        }
        int added = 0;
        for (Holder<Item> holder : holders.get()) {
            Item item = holder.value();
            if (item == Items.AIR) {
                continue;
            }
            if (map.putIfAbsent(item, new Tuple<>(rule.heal(), rule.duration())) == null) {
                added++;
            }
        }
        return added;
    }
}
