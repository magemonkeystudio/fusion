package studio.magemonkey.fusion.gui.recipe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import studio.magemonkey.codex.util.DataUT;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityModuleItemType;
import studio.magemonkey.fusion.data.recipes.RecipeCustomItem;
import studio.magemonkey.fusion.data.recipes.RecipeItem;

import java.util.*;

/**
 * Immutable fingerprint for an ItemStack that matches CalculatedRecipe.isSimilar(...) logic.
 * <p>
 * Special handling for Divinity items (item_generator, gems, essences, runes): only the
 * item_id and level are compared, ignoring variable stats like lore or enchantments.
 * A divinityItemLevel of -1 acts as a wildcard ("any level") in matching — used for
 * recipe ingredients that do not specify a level constraint.
 */
public class IngredientFingerprint {
    private final Material                                          type;
    private final int                                               customModelData;
    private final String                                            displayName;
    private final List<String>                                      lore;
    private final Map<org.bukkit.enchantments.Enchantment, Integer> enchantments;
    private final boolean                                           unbreakable;
    private final int                                               durability;
    private final boolean                                           hasSocketFill;

    // --- Divinity-specific fields ---
    private final String divinityItemId;   // null if not a Divinity item
    private final int    divinityItemLevel; // -1 means "any level" (wildcard) or not a Divinity item

    private static final NamespacedKey DIVINITY_MODULE_KEY       = new NamespacedKey("divinity", "item_module");
    private static final NamespacedKey DIVINITY_ITEM_ID_KEY      = new NamespacedKey("divinity", "item_id");
    private static final NamespacedKey DIVINITY_ITEM_LEVEL_KEY   = new NamespacedKey("divinity", "item_level");
    private static final String        DIVINITY_MODULE_ITEMGEN   = "item_generator";
    private static final String        DIVINITY_MODULE_GEMS      = "gems";
    private static final String        DIVINITY_MODULE_ESSENCES  = "essences";
    private static final String        DIVINITY_MODULE_RUNES     = "runes";
    private static final String        DIVINITY_MODULE_EXTRACTOR = "extractor";

    public IngredientFingerprint(Material type,
                                 int customModelData,
                                 String displayName,
                                 List<String> lore,
                                 Map<org.bukkit.enchantments.Enchantment, Integer> enchantments,
                                 boolean unbreakable,
                                 int durability,
                                 boolean hasSocketFill,
                                 String divinityItemId,
                                 int divinityItemLevel) {
        this.type = type;
        this.customModelData = customModelData;
        this.displayName = (displayName == null ? "" : displayName);
        this.lore = (lore == null ? Collections.emptyList() : new ArrayList<>(lore));
        this.enchantments = (enchantments == null ? Collections.emptyMap() : new HashMap<>(enchantments));
        this.unbreakable = unbreakable;
        this.durability = durability;
        this.hasSocketFill = hasSocketFill;
        this.divinityItemId = divinityItemId;
        this.divinityItemLevel = divinityItemLevel;
    }

    public static boolean hasSocketFill(ItemStack item) {
        if (item == null) return false;
        return hasSocketFill(item.getItemMeta());
    }

    private static boolean hasSocketFill(ItemMeta meta) {
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equals("divinity")) continue;
            String k = key.getKey().toLowerCase();
            if (!k.startsWith("item_socket_gem_")
                    && !k.startsWith("item_socket_rune_")
                    && !k.startsWith("item_socket_essence_")) continue;
            String[] value = pdc.get(key, DataUT.STRING_ARRAY);
            if (value != null && value.length == 2 && !value[0].isEmpty() && !value[1].isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDivinityModule(String module) {
        return DIVINITY_MODULE_ITEMGEN.equalsIgnoreCase(module)
                || DIVINITY_MODULE_GEMS.equalsIgnoreCase(module)
                || DIVINITY_MODULE_ESSENCES.equalsIgnoreCase(module)
                || DIVINITY_MODULE_RUNES.equalsIgnoreCase(module)
                || DIVINITY_MODULE_EXTRACTOR.equalsIgnoreCase(module);
    }

    /**
     * Build an IngredientFingerprint by examining a live ItemStack from a player's inventory.
     */
    public static IngredientFingerprint of(ItemStack is) {
        Material mat  = is.getType();
        ItemMeta meta = is.getItemMeta();

        int                                               cmd         = 0;
        String                                            name        = "";
        List<String>                                      loreList    = Collections.emptyList();
        Map<org.bukkit.enchantments.Enchantment, Integer> enchantsMap = Collections.emptyMap();
        boolean                                           unbreak     = false;
        int                                               dmg         = 0;

        String divId    = null;
        int    divLevel = -1;

        if (meta != null) {
            if (meta.hasCustomModelData()) {
                cmd = meta.getCustomModelData();
            }
            if (meta.hasDisplayName()) {
                name = meta.getDisplayName();
            }
            if (meta.hasLore()) {
                loreList = new ArrayList<>(Objects.requireNonNull(meta.getLore()));
            }
            Map<org.bukkit.enchantments.Enchantment, Integer> raw = meta.getEnchants();
            if (!raw.isEmpty()) {
                enchantsMap = new HashMap<>(raw);
            }
            unbreak = meta.isUnbreakable();
            if (meta instanceof org.bukkit.inventory.meta.Damageable dmeta) {
                dmg = dmeta.getDamage();
            }

            // Detect Divinity items (item_generator, gems, essences, runes)
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String module = pdc.get(DIVINITY_MODULE_KEY, PersistentDataType.STRING);
            if (isDivinityModule(module)) {
                String itemId = pdc.get(DIVINITY_ITEM_ID_KEY, PersistentDataType.STRING);
                if (itemId != null && !itemId.isEmpty()) {
                    divId = itemId.toLowerCase();
                }
                Integer level = pdc.get(DIVINITY_ITEM_LEVEL_KEY, PersistentDataType.INTEGER);
                if (level != null) {
                    divLevel = level;
                }
            }
        }

        return new IngredientFingerprint(mat,
                cmd,
                name,
                loreList,
                enchantsMap,
                unbreak,
                dmg,
                hasSocketFill(meta),
                divId,
                divLevel);
    }

    /**
     * Build a fingerprint for a recipe ingredient requirement — avoids calling getItemStack()
     * on Divinity items (which would generate a random-level item when level=-1).
     * The resulting fingerprint uses level=-1 as a wildcard matching any actual item level.
     */
    public static IngredientFingerprint forRequired(RecipeItem required) {
        if (required instanceof RecipeCustomItem rci
                && rci.getItemType() instanceof DivinityModuleItemType dmt
                && dmt.getModuleItem() != null) {
            return new IngredientFingerprint(
                    Material.AIR, 0, "", Collections.emptyList(),
                    Collections.emptyMap(), false, 0, false,
                    dmt.getModuleItem().getId().toLowerCase(),
                    dmt.getLevel());
        }
        ItemStack single = required.getItemStack().clone();
        single.setAmount(1);
        return of(single);
    }

    /**
     * Sum all values in the map whose keys match this fingerprint.
     * Required when this fingerprint has divinityItemLevel=-1 (any level), since multiple
     * inventory entries with different specific levels all map to different HashMap buckets
     * under the old hashCode, but must all be counted.
     */
    public int sumMatchingEntries(Map<IngredientFingerprint, Integer> map) {
        if (divinityItemId != null && divinityItemLevel == -1) {
            int total = 0;
            for (Map.Entry<IngredientFingerprint, Integer> e : map.entrySet()) {
                IngredientFingerprint k = e.getKey();
                if (k.divinityItemId != null && k.divinityItemId.equals(this.divinityItemId)) {
                    total += e.getValue();
                }
            }
            return total;
        }
        return map.getOrDefault(this, 0);
    }

    /**
     * Drain `need` units from the map for entries matching this fingerprint.
     * Handles the wildcard (level=-1) case by draining across all matching specific-level entries.
     */
    public void drainFromMap(Map<IngredientFingerprint, Integer> map, int need) {
        if (divinityItemId != null && divinityItemLevel == -1) {
            for (Iterator<Map.Entry<IngredientFingerprint, Integer>> it = map.entrySet().iterator();
                 it.hasNext() && need > 0; ) {
                Map.Entry<IngredientFingerprint, Integer> e = it.next();
                IngredientFingerprint k = e.getKey();
                if (k.divinityItemId != null && k.divinityItemId.equals(this.divinityItemId)) {
                    int drain = Math.min(e.getValue(), need);
                    need -= drain;
                    if (e.getValue() - drain <= 0) {
                        it.remove();
                    } else {
                        e.setValue(e.getValue() - drain);
                    }
                }
            }
            return;
        }
        int cur = map.getOrDefault(this, 0);
        if (cur - need <= 0) {
            map.remove(this);
        } else {
            map.put(this, cur - need);
        }
    }

    public Material getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IngredientFingerprint that)) return false;

        // Specialized comparison for Divinity items
        if (this.divinityItemId != null && that.divinityItemId != null) {
            if (!this.divinityItemId.equals(that.divinityItemId)) return false;
            // -1 on either side = wildcard (no level constraint in recipe)
            if (this.divinityItemLevel == -1 || that.divinityItemLevel == -1) return true;
            return this.divinityItemLevel == that.divinityItemLevel;
        }

        // Fallback to original logic for non-Divinity items
        if (this.hasSocketFill || that.hasSocketFill) return false;
        return customModelData == that.customModelData &&
                unbreakable == that.unbreakable &&
                durability == that.durability &&
                type == that.type &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(lore, that.lore) &&
                Objects.equals(enchantments, that.enchantments);
    }

    @Override
    public int hashCode() {
        if (divinityItemId != null) {
            // Exclude level so that wildcard (-1) and specific levels hash to the same bucket.
            // equals() handles the level comparison correctly.
            return Objects.hash(divinityItemId);
        }
        return Objects.hash(type, customModelData, displayName, lore, enchantments, unbreakable, durability);
    }
}
