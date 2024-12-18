package studio.magemonkey.fusion.data.recipes;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.exception.MissingItemException;
import studio.magemonkey.codex.api.items.exception.MissingProviderException;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.hooks.HookType;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityRecipeMeta;

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
            ItemStack item   = new ItemStack(Material.STONE);
            int       amount = 1;

            if (obj instanceof String args) {
                if (args.toUpperCase().startsWith("VANILLA_")) {
                    String material = args.split("_", 2)[1];
                    amount = Integer.parseInt(material.split(":")[1]);
                    material = material.split(":")[0];

                    Material mat = Material.matchMaterial(material);
                    if (mat == null) {
                        Fusion.getInstance()
                                .getLogger()
                                .warning("Invalid material found in configuration: " + material);
                        return null;
                    }
                    result = new RecipeCustomItem(ItemBuilder.newItem(mat).build(), amount, false);
                } else if (args.toUpperCase().startsWith("DIVINITY_")) {
                    if (!Fusion.getHookManager().isHooked(HookType.Divinity)) {
                        Fusion.getInstance()
                                .getLogger()
                                .warning(
                                        "Divinity is not hooked, but a Divinity item was found in the configuration. Skipping...");
                        return null;
                    }
                    int level = -1;

                    // DIVINITY_<module>_<item_id>
                    String[] divinityArgs = args.split("_", 3);

                    // Get the module
                    String module = divinityArgs[1];
                    // Get the item_id
                    String namespace = divinityArgs[2].split(":")[0];

                    // Get the item args without the first position
                    String[] itemArgs = new String[divinityArgs[2].split(":").length - 1];
                    if (divinityArgs[2].split(":").length - 1 >= 0)
                        System.arraycopy(divinityArgs[2].split(":"),
                                1,
                                itemArgs,
                                0,
                                divinityArgs[2].split(":").length - 1);

                    switch (module.toLowerCase()) {
                        case "arrows", "ar" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            }

                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getArrowManager()
                                    .getItemById(namespace)
                                    .create(level);
                        }
                        case "consumables", "co" -> {

                        }
                        case "customitems", "ci" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            }
                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getCustomItemsManager()
                                    .getItemById(namespace)
                                    .create();
                        }
                        case "dismantle", "di" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            } else if (itemArgs.length == 2) {
                                level = Integer.parseInt(itemArgs[0]);
                                amount = Integer.parseInt(itemArgs[1]);
                            }

                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getResolveManager()
                                    .getItemById(namespace)
                                    .create(level);
                        }
                        case "essences", "es" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            } else if (itemArgs.length == 2) {
                                level = Integer.parseInt(itemArgs[0]);
                                amount = Integer.parseInt(itemArgs[1]);
                            }

                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getEssenceManager()
                                    .getItemById(namespace)
                                    .create(level);
                        }
                        case "extractor", "ex" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            } else if (itemArgs.length == 2) {
                                level = Integer.parseInt(itemArgs[0]);
                                amount = Integer.parseInt(itemArgs[1]);
                            }

                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getExtractManager()
                                    .getItemById(namespace)
                                    .create(level);
                        }
                        case "fortify", "fo" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            } else if (itemArgs.length == 2) {
                                level = Integer.parseInt(itemArgs[0]);
                                amount = Integer.parseInt(itemArgs[1]);
                            }

                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getFortifyManager()
                                    .getItemById(namespace)
                                    .create(level);
                        }
                        case "gems", "ge" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            } else if (itemArgs.length == 2) {
                                level = Integer.parseInt(itemArgs[0]);
                                amount = Integer.parseInt(itemArgs[1]);
                            }

                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getGemManager()
                                    .getItemById(namespace)
                                    .create(level);
                        }
                        case "identify", "id" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            } else if (itemArgs.length == 2) {
                                level = Integer.parseInt(itemArgs[0]);
                                amount = Integer.parseInt(itemArgs[1]);
                            }

                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getIdentifyManager()
                                    .getItemById(namespace)
                                    .create(level);
                        }
                        case "itemgenerator", "itemgen", "it" -> {
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

                            if (material == null) {
                                item = Divinity.getInstance()
                                        .getModuleCache()
                                        .getTierManager()
                                        .getItemById(namespace)
                                        .create(level);
                            } else {
                                try {
                                    item = Divinity.getInstance()
                                            .getModuleCache()
                                            .getTierManager()
                                            .getItemById(namespace)
                                            .create(level,
                                                    0,
                                                    CodexEngine.get()
                                                            .getItemManager()
                                                            .getItemType(material.toString()));
                                } catch (Exception e) {
                                    Fusion.getInstance()
                                            .getLogger()
                                            .warning("Invalid material found for Divinity-ItemGenerator: " + material);
                                    return null;
                                }
                            }
                        }
                        case "magicdust", "ma" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            } else if (itemArgs.length == 2) {
                                level = Integer.parseInt(itemArgs[0]);
                                amount = Integer.parseInt(itemArgs[1]);
                            }

                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getMagicDustManager()
                                    .getItemById(namespace)
                                    .create(level);
                        }
                        case "runes", "ru" -> {
                            if (itemArgs.length == 1) {
                                amount = Integer.parseInt(itemArgs[0]);
                            } else if (itemArgs.length == 2) {
                                level = Integer.parseInt(itemArgs[0]);
                                amount = Integer.parseInt(itemArgs[1]);
                            }
                            item = Divinity.getInstance()
                                    .getModuleCache()
                                    .getRuneManager()
                                    .getItemById(namespace)
                                    .create(level);
                        }
                    }

                    result = new RecipeCustomItem(item, amount, false);
                } else if (args.toUpperCase().startsWith("ORAXEN_")) {
                    if (!Fusion.getHookManager().isHooked(HookType.Oraxen)) {
                        Fusion.getInstance()
                                .getLogger()
                                .warning("Oraxen is not hooked, but an Oraxen item was found in the configuration. Skipping...");
                        return null;
                    }
                    String[] oraxenArgs = args.split(":");
                    String   namespace  = oraxenArgs[0];

                    if (oraxenArgs.length > 1) {
                        amount = Integer.parseInt(oraxenArgs[1]);
                    }

                    try {
                        ItemType type = CodexEngine.get().getItemManager().getItemType(namespace);

                        if(type != null) {
                            item = type.create();
                            Fusion.getInstance()
                                    .getLogger()
                                    .warning("Oraxen item " + namespace + " found: " + ItemBuilder.newItem(item).build().getItemMeta().getDisplayName() + ItemBuilder.newItem(item).getName());
                        }

                    } catch (MissingProviderException | MissingItemException e) {
                        Fusion.getInstance().getLogger().warning("Invalid Oraxen item found in configuration: " + namespace);
                    }
                    result = new RecipeCustomItem(item, amount, false);
                }
                else {
                    String[] itemArgs   = args.split(":");
                    Material mat        = Material.matchMaterial(itemArgs[0]);
                    int      durability = -1;

                    if (itemArgs.length > 1) {
                        amount = Integer.parseInt(itemArgs[1]);
                        if (itemArgs.length > 2) {
                            durability = Integer.parseInt(itemArgs[2]);
                        }
                    }


                    if (durability == -1) {
                        result = new RecipeCustomItem(ItemBuilder.newItem(mat).build(), amount, false);
                    } else {
                        result = new RecipeCustomItem(ItemBuilder.newItem(mat).durability(durability).build(),
                                amount,
                                false);
                    }
                }


                String  str    = obj.toString();
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
                        ItemType          itemType          = recipeEconomyItem.asItemType();
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
            Fusion.getInstance()
                    .error("Error on loading configuration of RecipeItem: " + obj + ", exception: " + e.getMessage()
                            + ", more in console.");
            throw new RuntimeException(e);
        }
        return result;
    }

    static RecipeItem fromDivinityRecipeMeta(DivinityRecipeMeta meta) {
        return new RecipeCustomItem(meta.generateItem(), meta.getAmount(), false);
    }
}
