package studio.magemonkey.fusion.gui.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Immutable fingerprint for an ItemStack that matches CalculatedRecipe.isSimilar(...) logic.
 * <p>
 * We compare:
 * - Material
 * - customModelData (if present)
 * - displayName (if present)
 * - lore lines (if present)
 * - all enchantments (if present)
 * - unbreakable flag
 * - durability (if Damageable)
 */
public class IngredientFingerprint {
    private final Material                                          type;
    private final int                                               customModelData;
    private final String                                            displayName;
    private final List<String>                                      lore;
    private final Map<org.bukkit.enchantments.Enchantment, Integer> enchantments;
    private final boolean                                           unbreakable;
    private final int                                               durability;

    public IngredientFingerprint(Material type,
                                 int customModelData,
                                 String displayName,
                                 List<String> lore,
                                 Map<org.bukkit.enchantments.Enchantment, Integer> enchantments,
                                 boolean unbreakable,
                                 int durability) {
        this.type = type;
        this.customModelData = customModelData;
        this.displayName = (displayName == null ? "" : displayName);
        this.lore = (lore == null ? Collections.emptyList() : new ArrayList<>(lore));
        this.enchantments = (enchantments == null ? Collections.emptyMap() : new HashMap<>(enchantments));
        this.unbreakable = unbreakable;
        this.durability = durability;
    }

    /**
     * Build an IngredientFingerprint by examining a live ItemStack.
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
        }

        return new IngredientFingerprint(mat, cmd, name, loreList, enchantsMap, unbreak, dmg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IngredientFingerprint that)) return false;
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
        return Objects.hash(type, customModelData, displayName, lore, enchantments, unbreakable, durability);
    }
}
