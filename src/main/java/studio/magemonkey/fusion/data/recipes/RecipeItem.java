package studio.magemonkey.fusion.data.recipes;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import studio.magemonkey.codex.items.ItemType;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.fusion.Fusion;

import java.util.LinkedHashMap;
import java.util.Map;

public interface RecipeItem {
    String CUSTOM_PREFIX = "@ ";

    int getAmount();

    ItemStack getItemStack();

    Object toConfig();

    static RecipeItem fromConfig(Object obj) {
        RecipeItem result = null;
        if (obj instanceof String) {
            String str = obj.toString();
            boolean custom = str.startsWith(CUSTOM_PREFIX);
            if (custom) {
                str = str.substring(CUSTOM_PREFIX.length());
            }
            String[] srrs = StringUtils.split(str, ':');
            if (srrs.length == 0) {
                return null;
            }
            if (!custom && (srrs.length == 2)) {
                try {
                    RecipeEconomyItem recipeEconomyItem = new RecipeEconomyItem(srrs[0], Integer.parseInt(srrs[1]));
                    ItemType itemType = recipeEconomyItem.asItemType();
                    if (itemType != null) {
                        result = recipeEconomyItem;
                    }
                } catch (Exception e) {
                    Fusion.getInstance()
                            .error("Error on loading configuration of RecipeItem: " + obj + ", exception: "
                                    + e.getMessage() + ", more in console.");
                    throw new RuntimeException(e);
                }
            }
            if (result == null) {
                try {
                    Material mat = Material.matchMaterial(srrs[0]);
                    if (mat == null) {
                        return null;
                    }
                    int type = 0;
                    int amount = 1;
                    if (srrs.length > 1) {
                        type = Integer.parseInt(srrs[1]);
                    }
                    if (srrs.length > 2) {
                        amount = Integer.parseInt(srrs[2]);
                    }
                    ItemBuilder builder = ItemBuilder.newItem(mat).durability(type).amount(amount);
                    result = new RecipeCustomItem(builder.build(), builder.getAmount(), true);
                } catch (Exception e) {
                    Fusion.getInstance()
                            .error("Error on loading configuration of RecipeItem: " + obj + ", exception: "
                                    + e.getMessage() + ", more in console.");
                    throw new RuntimeException(e);
                }
            }
        } else if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            ItemBuilder itemBuilder = new ItemBuilder(((Map<String, Object>) obj));

            // In case we face an enchantment book, properly handle the enchantments
            // in a seperated meta before handling the item later on
            if (itemBuilder.getMaterial() == Material.ENCHANTED_BOOK) {
                Map<Enchantment, Integer> enchantments = new LinkedHashMap<>(itemBuilder.getEnchants());
                itemBuilder.clearEnchants();
                ItemStack item = itemBuilder.build().clone();
                EnchantmentStorageMeta storage = (EnchantmentStorageMeta) item.getItemMeta();
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    storage.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                }
                item.setItemMeta(storage);
                result = new RecipeCustomItem(item, itemBuilder.getAmount(), false);
            } else {
                result = new RecipeCustomItem(itemBuilder.build(), itemBuilder.getAmount(), false);
            }
        }
        return result;
    }
}
