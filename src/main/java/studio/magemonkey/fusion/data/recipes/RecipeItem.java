package studio.magemonkey.fusion.data.recipes;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.exception.MissingItemException;
import studio.magemonkey.codex.api.items.exception.MissingProviderException;
import studio.magemonkey.codex.items.CodexItemManager;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.hooks.HookType;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityModuleItemType;
import studio.magemonkey.fusion.cfg.hooks.divinity.DivinityRecipeMeta;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface RecipeItem {
    String  CUSTOM_PREFIX   = "@ ";
    // The negative lookahead (?!\\d+(?:[^\\w]|$)) in the namespace group prevents it from
    // consuming "itemname:" when the following part is a pure integer (the :amount suffix).
    Pattern divinityPattern =
            Pattern.compile("DIVINITY_([\\w-]+:(?!\\d+(?:[^\\w]|$)))?([^:~]+)\\b((~level:(\\d+))|(~material:([\\w-]+)\\b)){0,2}");

    int getAmount();

    ItemStack getItemStack();

    Object toConfig();

    /** Returns true if the item string starts with a DIV_ module prefix (case-insensitive). */
    static boolean isDivModulePrefix(String item) {
        String u = item.toUpperCase();
        return u.startsWith("DIV_ITEMGEN_") || u.startsWith("DIV_GEM_")
                || u.startsWith("DIV_ESSENCE_") || u.startsWith("DIV_RUNE_")
                || u.startsWith("DIV_EXTRACTOR_") || u.startsWith("DIV_ARROW_")
                || u.startsWith("DIV_CONSUMABLE_") || u.startsWith("DIV_CUSTOM_")
                || u.startsWith("DIV_FORTIFY_") || u.startsWith("DIV_IDENTIFY_")
                || u.startsWith("DIV_DUST_") || u.startsWith("DIV_REPAIR_")
                || u.startsWith("DIV_DISMANTLE_") || u.startsWith("DIV_REFINE_");
    }

    /**
     * Validates a DIV_ module item by name (no :amount suffix expected).
     * Strips optional ~level:N and params before lookup.
     */
    static boolean isValidDivModuleItem(String item) {
        if (!isDivModulePrefix(item)) return false;
        if (!Fusion.getInstance().getHookManager().isHooked(HookType.Divinity)) return false;
        try {
            String base  = item.replaceAll("~level:\\d+", "").replaceAll("~material:\\w+", "").trim();
            String upper = base.toUpperCase();
            // Strip any :params suffix (amount, level, type)
            int modEnd  = upper.indexOf('_', 4);
            int idStart = (modEnd >= 0) ? modEnd + 1 : base.length();
            int colon   = base.indexOf(':', idStart);
            if (colon >= 0) base = base.substring(0, colon);
            return parseDivModuleItem(base, base, -1, 1, null) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    static RecipeItem fromConfig(Object obj) {
        if (obj instanceof Map) {
            //noinspection unchecked
            return getItemBuilder((Map<String, Object>) obj);
        }

        int        amount = 1;
        RecipeItem result = null;

        if (obj instanceof String itemString) {
            // Handle TAG_<name>:<amount> — matches any material in the given Bukkit Tag
            if (itemString.toUpperCase().startsWith("TAG_")) {
                String[] parts   = itemString.split(":", 2);
                String   tagName = parts[0].substring(4).toLowerCase();
                int      amt     = 1;
                if (parts.length > 1) {
                    try { amt = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                }
                Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(tagName), Material.class);
                if (tag == null) {
                    tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(tagName), Material.class);
                }
                if (tag == null) {
                    throw new RuntimeException("Unknown material tag: \"" + tagName
                            + "\". Check https://minecraft.wiki/w/Tag for valid tag names.");
                }
                return new RecipeTagItem(tag, tagName, amt);
            }


            // Handle DIV_ module prefixes (DIV_ITEMGEN_, DIV_GEM_, DIV_CUSTOM_, etc.)
            if (isDivModulePrefix(itemString)
                    && Fusion.getInstance().getHookManager().isHooked(HookType.Divinity)) {
                int      amt          = 1;
                int      lvl          = -1;
                ItemType materialType = null;
                String   base         = itemString;
                String   upper        = base.toUpperCase();

                // --- Backward compat: ~level:LVL:AMT format (e.g. DIV_ITEMGEN_name~level:5:3) ---
                int levelIdx = base.indexOf("~level:");
                if (levelIdx >= 0) {
                    String afterLevel = base.substring(levelIdx + 7);
                    int    nextColon  = afterLevel.indexOf(':');
                    if (nextColon >= 0) {
                        try { lvl = Integer.parseInt(afterLevel.substring(0, nextColon)); } catch (NumberFormatException ignored) {}
                        int lastColon = base.lastIndexOf(':');
                        try { amt = Integer.parseInt(base.substring(lastColon + 1)); } catch (NumberFormatException ignored) {}
                    } else {
                        try { lvl = Integer.parseInt(afterLevel); } catch (NumberFormatException ignored) {}
                    }
                    base = base.substring(0, levelIdx);
                } else {
                    // --- New format: DIV_MODULE_ID:AMT[:LVL[:TYPE]] ---
                    int modEnd  = upper.indexOf('_', 4);  // '_' between module type and ID
                    int idStart = (modEnd >= 0) ? modEnd + 1 : base.length();
                    int firstColon = base.indexOf(':', idStart);
                    if (firstColon >= 0) {
                        String id     = base.substring(idStart, firstColon);
                        String params = base.substring(firstColon + 1);

                        // Backward compat: AMT;MINLEVEL (semicolon, old ITEMGEN format)
                        int semiIdx = params.indexOf(';');
                        if (semiIdx >= 0) {
                            try { lvl = Integer.parseInt(params.substring(semiIdx + 1)); } catch (NumberFormatException ignored) {}
                            params = params.substring(0, semiIdx);
                        }

                        String[] parts = params.split(":", -1);
                        // parts[0] = amount
                        try { amt = Integer.parseInt(parts[0]); } catch (NumberFormatException ignored) {}

                        boolean isItemGen = upper.startsWith("DIV_ITEMGEN_");
                        boolean isCustom  = upper.startsWith("DIV_CUSTOM_");

                        if (!isCustom && lvl < 0 && parts.length >= 2) {
                            if (isItemGen) {
                                // Last non-numeric part is material type
                                int endIdx = parts.length - 1;
                                if (!isNumericOrRange(parts[endIdx]) && endIdx >= 1) {
                                    try {
                                        materialType = CodexEngine.get().getItemManager().getItemType(parts[endIdx]);
                                    } catch (Exception ignored) {}
                                    endIdx--;
                                }
                                if (endIdx >= 1) {
                                    lvl = parseFirstNumber(joinRange(parts, 1, endIdx + 1));
                                }
                            } else {
                                lvl = parseFirstNumber(joinRange(parts, 1, parts.length));
                            }
                        }

                        base = base.substring(0, idStart) + id;
                    }
                    // else: no colon — base stays as DIV_MODULE_ID with default params
                }

                result = parseDivModuleItem(itemString, base, lvl, amt, materialType);
                if (result != null) return result;
                throw new RuntimeException("DIV_ module item not found: " + base
                        + ". Verify the id exists in your Divinity configuration.");
            }

            CodexItemManager items = CodexEngine.get().getItemManager();
            try {
                Matcher divMatcher = divinityPattern.matcher(itemString);
                String  itemKey;
                if (divMatcher.find()) {
                    itemKey = divMatcher.group(0);
                } else {
                    itemKey = itemString.split(":")[0];
                }
                ItemType type = items.getItemType(itemKey);
                if (type != null) {
                    // The amount *should* be the last part of the string, after the colon.
                    // We'll replace the Divinity format with a blank string to get rid of possible
                    // ~level:X conflicts so we can be sure that the amount is the last part.
                    itemString = itemString.replace(itemKey, "");
                    String[] amountSplit = StringUtils.split(itemString, ':');
                    if (amountSplit.length > 0) {
                        try {
                            amount = Integer.parseInt(amountSplit[amountSplit.length - 1]);
                        } catch (NumberFormatException e) {
                            Fusion.getInstance()
                                    .getLogger()
                                    .warning("Invalid amount found in configuration: "
                                            + amountSplit[amountSplit.length - 1] + " for item " + itemKey);
                        }
                    }

                    result = new RecipeCustomItem(type, amount, false);
                }
            } catch (MissingProviderException | MissingItemException e) {
                throw new RuntimeException(e);
            }

            if (result == null) {
                result = buildInternalItem(obj);
            }
        }

        return result;
    }

    /**
     * Looks up the DIV_ module item and wraps it in a RecipeCustomItem.
     *
     * @param configKey original config string (stored as-is in YAML via toConfig())
     * @param base      clean item ID string, e.g. "DIV_ITEMGEN_sword" (no params)
     * @param level     item level (-1 = random/unspecified)
     * @param amount    stack amount
     * @param material  optional material type for ITEMGEN (null = random)
     */
    private static @Nullable RecipeItem parseDivModuleItem(String configKey, String base, int level, int amount, @Nullable ItemType material) {
        String upper = base.toUpperCase();
        if (upper.startsWith("DIV_ITEMGEN_")) {
            String  name   = base.substring("DIV_ITEMGEN_".length());
            boolean noEnch = upper.endsWith("_NOENCH");
            if (noEnch) name = name.substring(0, name.length() - "_NOENCH".length());
            var item = Divinity.getInstance().getModuleCache().getTierManager().getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_ITEMGEN item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, material, noEnch), amount, false, configKey);
        } else if (upper.startsWith("DIV_GEM_")) {
            String name = base.substring("DIV_GEM_".length());
            var item = Divinity.getInstance().getModuleCache().getGemManager().getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_GEM item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_ESSENCE_")) {
            String name = base.substring("DIV_ESSENCE_".length());
            var item = Divinity.getInstance().getModuleCache().getEssenceManager().getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_ESSENCE item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_RUNE_")) {
            String name = base.substring("DIV_RUNE_".length());
            var item = Divinity.getInstance().getModuleCache().getRuneManager().getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_RUNE item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_EXTRACTOR_")) {
            String name = base.substring("DIV_EXTRACTOR_".length());
            var mgr = Divinity.getInstance().getModuleCache().getExtractManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_EXTRACTOR used but Extractor module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_EXTRACTOR item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_ARROW_")) {
            String name = base.substring("DIV_ARROW_".length());
            var mgr = Divinity.getInstance().getModuleCache().getArrowManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_ARROW used but Arrow module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_ARROW item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_CONSUMABLE_")) {
            String name = base.substring("DIV_CONSUMABLE_".length());
            var mgr = Divinity.getInstance().getModuleCache().getConsumablesManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_CONSUMABLE used but Consumables module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_CONSUMABLE item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_CUSTOM_")) {
            String name = base.substring("DIV_CUSTOM_".length());
            var mgr = Divinity.getInstance().getModuleCache().getCustomItemsManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_CUSTOM used but CustomItems module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_CUSTOM item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_FORTIFY_")) {
            String name = base.substring("DIV_FORTIFY_".length());
            var mgr = Divinity.getInstance().getModuleCache().getFortifyManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_FORTIFY used but Fortify module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_FORTIFY item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_IDENTIFY_")) {
            String name = base.substring("DIV_IDENTIFY_".length());
            var mgr = Divinity.getInstance().getModuleCache().getIdentifyManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_IDENTIFY used but Identify module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_IDENTIFY item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_DUST_")) {
            String name = base.substring("DIV_DUST_".length());
            var mgr = Divinity.getInstance().getModuleCache().getMagicDustManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_DUST used but MagicDust module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_DUST item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_REPAIR_")) {
            String name = base.substring("DIV_REPAIR_".length());
            var mgr = Divinity.getInstance().getModuleCache().getRepairManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_REPAIR used but Repair module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_REPAIR item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_DISMANTLE_")) {
            String name = base.substring("DIV_DISMANTLE_".length());
            var mgr = Divinity.getInstance().getModuleCache().getResolveManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_DISMANTLE used but Dismantle module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_DISMANTLE item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        } else if (upper.startsWith("DIV_REFINE_")) {
            String name = base.substring("DIV_REFINE_".length());
            var mgr = Divinity.getInstance().getModuleCache().getRefineManager();
            if (mgr == null) {
                Fusion.getInstance().getLogger().warning("DIV_REFINE used but Refine module is not loaded.");
                return null;
            }
            var item = mgr.getItemById(name.toLowerCase());
            if (item == null) {
                Fusion.getInstance().getLogger().warning("DIV_REFINE item not found: " + name);
                return null;
            }
            return new RecipeCustomItem(new DivinityModuleItemType(item, level, null, false), amount, false, configKey);
        }
        return null;
    }

    private static boolean isNumericOrRange(String s) {
        if (s == null || s.isEmpty()) return false;
        for (String p : s.split(":")) {
            try { Integer.parseInt(p.trim()); } catch (NumberFormatException e) { return false; }
        }
        return true;
    }

    private static int parseFirstNumber(String s) {
        if (s == null || s.isEmpty()) return -1;
        try { return Integer.parseInt(s.split(":")[0]); } catch (NumberFormatException e) { return -1; }
    }

    private static String joinRange(String[] parts, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to && i < parts.length; i++) {
            if (sb.length() > 0) sb.append(':');
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private static @Nullable RecipeItem buildInternalItem(Object obj) {
        RecipeItem result = null;
        String     str    = obj.toString();
        boolean    custom = str.startsWith(CUSTOM_PREFIX);
        if (custom) {
            str = str.substring(CUSTOM_PREFIX.length());
        }

        String[] srrs = StringUtils.split(str, ':');
        if (srrs.length == 0) {
            return null;
        }

        if (custom) {
            Material mat = Material.matchMaterial(srrs[0]);
            if (mat == null) {
                Fusion.getInstance().getLogger().warning("Invalid material found in configuration: " + srrs[0]);
                return null;
            }

            ItemBuilder itemBuilder = ItemBuilder.newItem(mat);

            if (srrs.length > 1) {
                try {
                    itemBuilder.durability(Integer.parseInt(srrs[1]));
                } catch (NumberFormatException e) {
                    Fusion.getInstance().error("Durability is invalid (" + srrs[1] + ") for material " + mat.name());
                }
            }

            if (srrs.length > 2) {
                try {
                    itemBuilder.amount(Integer.parseInt(srrs[2]));
                } catch (NumberFormatException e) {
                    Fusion.getInstance().error("Amount is invalid (" + srrs[2] + ") for material " + mat.name());
                }
            }
            result = new RecipeCustomItem(itemBuilder, 1, true);
        } else if (srrs.length == 2) {
            try {
                RecipeEconomyItem recipeEconomyItem = new RecipeEconomyItem(srrs[0], Integer.parseInt(srrs[1]));
                ItemType          itemType          = recipeEconomyItem.asItemType();
                if (itemType != null) {
                    result = recipeEconomyItem;
                }
            } catch (Exception e) {
                Fusion.getInstance()
                        .error("Error on loading configuration of RecipeItem: " + obj + ", exception: " + e.getMessage()
                                + ", more in console.");
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private static @NotNull RecipeItem getItemBuilder(Map<String, Object> obj) {
        ItemBuilder itemBuilder = new ItemBuilder(obj);
        return new RecipeCustomItem(itemBuilder, itemBuilder.getAmount(), false);
    }

    static RecipeItem fromDivinityRecipeMeta(DivinityRecipeMeta meta) {
        return new RecipeCustomItem(meta);
    }
}
