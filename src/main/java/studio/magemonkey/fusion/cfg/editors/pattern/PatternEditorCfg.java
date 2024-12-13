package studio.magemonkey.fusion.cfg.editors.pattern;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatternEditorCfg {

    private final YamlParser config;

    public PatternEditorCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "Editors/pattern/PatternEditor.yml");
    }

    public String getTitle(String category) {
        String title = ChatUT.hexString(config.getString("title", "&2Pattern Simulation: $<category>"));
        title = title.replace(MessageUtil.getReplacement("category"), category);
        return ChatUT.hexString(title);
    }

    public Map<String, ItemStack> getIcons(CraftingTable table) {
        Map<String, ItemStack> icons = new HashMap<>();
        for (String icon : config.getConfigurationSection("icons").getKeys(false)) {
            if (icon.equalsIgnoreCase("patternItem")) continue;
            icons.put(icon, getIcon(table, icon));
        }
        return icons;
    }

    public Map<String, ItemStack> getIcons(BrowseEditor browseEditor) {
        Map<String, ItemStack> icons = new HashMap<>();
        for (String icon : config.getConfigurationSection("icons").getKeys(false)) {
            if (icon.equalsIgnoreCase("patternItem")) continue;
            icons.put(icon, getIcon(browseEditor, icon));
        }
        return icons;
    }

    public ItemStack getIcon(CraftingTable table, String icon) {
        Material material =
                Material.valueOf(config.getString("icons." + icon + ".material", "STONE").toUpperCase());
        int          amount      = config.getInt("icons." + icon + ".amount", 1);
        int          durability  = config.getInt("icons." + icon + ".durability", 0);
        boolean      unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String       name        = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore        = config.getStringList("icons." + icon + ".lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("name"), table.getInventoryName())
                    .replace(MessageUtil.getReplacement("profession"), table.getName())
                    .replace(MessageUtil.getReplacement("masteryUnlock"),
                            String.valueOf(table.getMasteryUnlock()))
                    .replace(MessageUtil.getReplacement("masteryCost"),
                            String.valueOf(table.getMasteryFee()))
                    .replace(MessageUtil.getReplacement("useCategories"),
                            String.valueOf(table.getUseCategories()))));
        }
        Map<Enchantment, Integer> enchants  = config.getEnchantmentSection("icons." + icon + ".enchants");
        List<ItemFlag>            flags     = config.getItemFlags("icons." + icon + ".flags");
        ItemFlag[]                itemFlags = flags.toArray(new ItemFlag[0]);
        return ItemBuilder.newItem(material)
                .amount(amount)
                .durability(durability)
                .unbreakable(unbreakable)
                .name(ChatUT.hexString(name))
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }

    public ItemStack getIcon(BrowseEditor browseEditor, String icon) {
        Material material =
                Material.valueOf(config.getString("icons." + icon + ".material", "STONE").toUpperCase());
        int          amount      = config.getInt("icons." + icon + ".amount", 1);
        int          durability  = config.getInt("icons." + icon + ".durability", 0);
        boolean      unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String       name        = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore        = config.getStringList("icons." + icon + ".lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("name"), browseEditor.getName())));
        }
        Map<Enchantment, Integer> enchants  = config.getEnchantmentSection("icons." + icon + ".enchants");
        List<ItemFlag>            flags     = config.getItemFlags("icons." + icon + ".flags");
        ItemFlag[]                itemFlags = flags.toArray(new ItemFlag[0]);
        return ItemBuilder.newItem(material)
                .amount(amount)
                .durability(durability)
                .unbreakable(unbreakable)
                .name(ChatUT.hexString(name))
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }

    public ItemStack getPatternItem(char c, ItemStack item) {
        String itemName = item.hasItemMeta() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String name = config.getString("icons.patternItem.name", "&9$<id>")
                .replace(MessageUtil.getReplacement("id"), String.valueOf(c))
                .replace(MessageUtil.getReplacement("pattern.name"), itemName);
        List<String> lore = config.getStringList("icons.patternItem.lore");

        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(MessageUtil.getReplacement("pattern.lore"))) {
                lore.remove(i);
                if (item.getItemMeta() == null || item.getItemMeta().getLore() == null) continue;
                int newLines = 1;
                for (String line : item.getItemMeta().getLore()) {
                    lore.add(i - 1 + newLines, ChatUT.hexString(line));
                    newLines++;
                }
                i += newLines;
                continue;
            }
            lore.set(i, ChatUT.hexString(lore.get(i)
                            .replace(MessageUtil.getReplacement("pattern.name"), itemName))
                    .replace(MessageUtil.getReplacement("id"), String.valueOf(c)));
        }
        return ItemBuilder.newItem(item)
                .name(name)
                .lore(lore)
                .build();
    }
}
