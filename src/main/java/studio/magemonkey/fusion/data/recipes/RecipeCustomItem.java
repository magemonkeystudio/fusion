package studio.magemonkey.fusion.data.recipes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityRecipeMeta;

import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
public class RecipeCustomItem implements RecipeItem {
    // Only ONE of these 3 should be set
    private final ItemType           item;
    private final ItemBuilder        builder;
    private final DivinityRecipeMeta meta;

    @Getter
    private final int     amount;
    private final boolean simple;

    /** Original config key for DIV_ module items (e.g. "DIV_ITEMGEN_sword~level:5").
     *  Excluded from equals/hashCode to preserve logical item identity. */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final String originalKey;

    /** Cached result of getItemStack() — avoids re-generating DIV_ITEMGEN random stats on every render.
     *  Naturally invalidated when config reloads (new RecipeCustomItem instances are created). */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private transient ItemStack displayCache;

    public RecipeCustomItem(@NotNull ItemType item, int amount, boolean simple) {
        this(item, amount, simple, null);
    }

    public RecipeCustomItem(@NotNull ItemType item, int amount, boolean simple, @Nullable String originalKey) {
        this.item = item;
        this.builder = null;
        this.meta = null;
        this.amount = amount;
        this.simple = simple;
        this.originalKey = originalKey;
    }

    /** Returns the underlying {@link ItemType}, or {@code null} if this item was created from an
     *  {@link ItemBuilder} or a {@link DivinityRecipeMeta}. */
    @Nullable
    public ItemType getItemType() {
        return this.item;
    }

    public RecipeCustomItem(@NotNull ItemBuilder item, int amount, boolean simple) {
        this.item = null;
        this.builder = item;
        this.meta = null;
        this.amount = amount;
        this.simple = simple;
        this.originalKey = null;
    }

    public RecipeCustomItem(@NotNull DivinityRecipeMeta meta) {
        this.item = null;
        this.builder = null;
        this.meta = meta;
        this.amount = meta.getAmount();
        this.simple = false;
        this.originalKey = null;
    }

    @Override
    public ItemStack getItemStack() {
        if (displayCache != null) return displayCache.clone();
        ItemStack built;
        if (this.item != null) {
            built = this.item.create();
        } else if (this.builder != null) {
            built = this.builder.build();
        } else {
            built = this.meta.generateItem();
        }
        built.setAmount(built.getAmount() * this.amount);
        displayCache = built.clone();
        return built;
    }

    @Override
    public Object toConfig() {
        // For DIV_ module items the full config string (including amount) is stored in originalKey.
        if (this.originalKey != null) {
            return this.originalKey;
        }

        ItemStack it = null;
        if (this.item != null) {
            it = this.item.create();
        } else if (this.builder != null) {
            it = this.builder.build();
        } else {
            it = this.meta.generateItem();
        }

        if (this.simple) {
            return CUSTOM_PREFIX + it.getType().name() + ":" + it.getDurability()
                    + (this.amount != 0 ? (":" + this.amount) : "");
        }

        ItemBuilder builder = ItemBuilder.newItem(it);
        if (it.getType() == Material.ENCHANTED_BOOK) {
            // Reapply enchants to the item, if it's an enchanted book
            if (it.getItemMeta() instanceof EnchantmentStorageMeta storage) {
                builder.clearEnchants();
                Map<Enchantment, Integer> enchants = new LinkedHashMap<>(storage.getStoredEnchants());
                builder.enchant(enchants);
            }
        }

        return builder.serialize();
    }
}
