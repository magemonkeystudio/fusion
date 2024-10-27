package studio.magemonkey.fusion.data.recipes;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import scala.Int;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.items.ItemType;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.divinity.Divinity;

import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.hooks.DivinityRecipeMeta;
import studio.magemonkey.fusion.cfg.hooks.HookType;

import java.util.LinkedHashMap;
import java.util.Map;

public interface RecipeItem {
    String CUSTOM_PREFIX = "@ ";

    int getAmount();

    ItemStack getItemStack();

    Object toConfig();

    static RecipeItem fromConfig(Object obj) {
        RecipeItem result = null;

        try {
            ItemStack item = new ItemStack(Material.STONE);
            int amount = 1;

            if (obj instanceof String args) {
                args = args.toUpperCase();
                if (args.startsWith("VANILLA_")) {
                    String material = args.split("_", 2)[1];
                    amount = Integer.parseInt(material.split(":")[1]);
                    material = material.split(":")[0];

                    Material mat = Material.matchMaterial(material);
                    if (mat == null) {
                        Fusion.getInstance().getLogger().warning("Invalid material found in configuration: " + material);
                        return null;
                    }
                    result = new RecipeCustomItem(ItemBuilder.newItem(mat).build(), amount, false);
                } else if (args.startsWith("DIVINITY_")) {
                    if (!Fusion.getHookManager().isHooked(HookType.Divinity)) {
                        Fusion.getInstance().getLogger().warning("Divinity is not hooked, but a Divinity item was found in the configuration. Skipping...");
                        return null;
                    }

                    // DIVINITY_<module>_<item_id>
                    String[] divinityArgs = args.split("_", 3);

                    // Get the module
                    String module = divinityArgs[1];
                    // Get the item_id
                    String namespace = divinityArgs[2].split(":")[0];

                    // Get the item args without the first position
                    String[] itemArgs = new String[divinityArgs[2].split(":").length - 1];
                    for(int i = 1; i < divinityArgs[2].split(":").length; i++) {
                        itemArgs[i - 1] = divinityArgs[2].split(":")[i];
                    }

                    switch (module.toLowerCase()) {
                        case "arrows", "ar" -> {
                            int level = -1;

                            if(itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            } else if(itemArgs.length == 2) {
                                level = Integer.parseInt(itemArgs[0]);
                                amount = Integer.parseInt(itemArgs[1]);
                            }

                            item = Divinity.getInstance().getModuleCache().getArrowManager().getItemById(namespace).create(level);
                            item.setAmount(amount);
                        }
                        case "consumables", "co" -> {

                        }
                        case "customitems", "ci" -> item = Divinity.getInstance().getModuleCache().getCustomItemsManager().getItemById(namespace).create();
                        case "dismantle", "di" -> {
                        }
                        case "essences", "es" -> {
                        }
                        case "extractor", "ex" -> {
                        }
                        case "fortify", "fo" -> {
                        }
                        case "gems", "ge" -> {
                        }
                        case "identify", "id" -> {
                        }
                        case "itemgenerator", "itemgen", "it" -> {
                            int level = -1;
                            Material material = null;

                            switch (itemArgs.length) {
                                case 1 -> amount = Integer.parseInt(itemArgs[0]);
                                case 2 -> {
                                    level = Integer.parseInt(itemArgs[0]);
                                    amount = Integer.parseInt(itemArgs[1]);
                                }
                                case 3 -> {
                                    level = Integer.parseInt(itemArgs[0]);
                                    amount = Integer.parseInt(itemArgs[1]);
                                    material = Material.matchMaterial(itemArgs[2]);
                                }
                            }

                            if(material == null) {
                                item = Divinity.getInstance().getModuleCache().getTierManager().getItemById(namespace).create(level);
                            } else {
                                try {
                                    item = Divinity.getInstance().getModuleCache().getTierManager().getItemById(namespace).create(level, 0, CodexEngine.get().getItemManager().getItemType(material.toString()));
                                } catch (Exception e) {
                                    Fusion.getInstance().getLogger().warning("Invalid material found for Divinity-ItemGenerator: " + material);
                                    return null;
                                }
                            }
                        }
                        case "magicdust", "ma" -> {
                        }
                        case "money", "mo" -> {
                        }
                        case "runes", "ru" -> {
                        }
                        case "sets", "se" -> {
                        }
                    }

                    result = new RecipeCustomItem(item, amount, false);
                } else {
                    String[] itemArgs = args.split(":");
                    Material mat = Material.matchMaterial(itemArgs[0]);
                    int durability = -1;

                    if(itemArgs.length > 1) {
                        amount = Integer.parseInt(itemArgs[1]);
                        if (itemArgs.length > 2) {
                            durability = Integer.parseInt(itemArgs[2]);
                        }
                    }


                    if(durability == -1) {
                        result = new RecipeCustomItem(ItemBuilder.newItem(mat).build(), amount, false);
                    } else {
                        result = new RecipeCustomItem(ItemBuilder.newItem(mat).durability(durability).build(), amount, false);
                    }
                }


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
            } else if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                ItemBuilder itemBuilder = new ItemBuilder(((Map<String, Object>) obj));

                // In case we face an enchantment book, properly handle the enchantments
                // in a seperated meta before handling the item later on
                if (itemBuilder.getMaterial() == Material.ENCHANTED_BOOK) {
                    Map<Enchantment, Integer> enchantments = new LinkedHashMap<>(itemBuilder.getEnchants());
                    itemBuilder.clearEnchants();
                    item = itemBuilder.build().clone();
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
        } catch (NumberFormatException e) {
            Fusion.getInstance().error("Error on loading configuration of RecipeItem: " + obj + ", exception: " + e.getMessage() + ", more in console.");
            throw new RuntimeException(e);
        }
        return result;
    }

    static RecipeItem fromDivinityRecipeMeta(DivinityRecipeMeta meta) {
        return new RecipeCustomItem(meta.generateItem(), meta.getAmount(), false);
    }
}
