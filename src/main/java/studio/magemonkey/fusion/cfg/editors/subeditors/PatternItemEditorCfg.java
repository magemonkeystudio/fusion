package studio.magemonkey.fusion.cfg.editors.subeditors;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatternItemEditorCfg {

    private final YamlParser config;

    public PatternItemEditorCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "/Editors/SubEditors/PatternItemEditor.yml");
    }

    public String getTitle() {
        return ChatUT.hexString(config.getString("title", "&2Pattern Item Editor"));
    }

    public Map<String, ItemStack> getIcons(CraftingTable table) {
        Map<String, ItemStack> icons = new HashMap<>();
        for(String icon : config.getConfigurationSection("icons").getKeys(false)) {
            if(icon.equalsIgnoreCase("patternItem")) continue;
            icons.put(icon, getIcon(table, icon));
        }
        return icons;
    }

    public ItemStack getIcon(CraftingTable table, String icon) {
        Material material = Material.valueOf(config.getString("icons." + icon + ".material", "STONE").toUpperCase());
        int amount = config.getInt("icons." + icon + ".amount", 1);
        int durability = config.getInt("icons." + icon + ".durability", 0);
        boolean unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String name = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore = config.getStringList("icons." + icon + ".lore");
        for(int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("name"), table.getInventoryName())
                    .replace(MessageUtil.getReplacement("profession"), table.getName())
                    .replace(MessageUtil.getReplacement("masteryUnlock"), String.valueOf(table.getMasteryUnlock()))
                    .replace(MessageUtil.getReplacement("masteryCost"), String.valueOf(table.getMasteryFee()))
                    .replace(MessageUtil.getReplacement("useCategories"), String.valueOf(table.getUseCategories()))));
        }
        Map<Enchantment, Integer> enchants = config.getEnchantmentSection("icons." + icon + ".enchants");
        List<ItemFlag> flags = config.getItemFlags("icons." + icon + ".flags");
        ItemFlag[] itemFlags = flags.toArray(new ItemFlag[0]);
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

    public ItemStack getPatternItemIcon(char c, ItemStack item) {
        String itemName = item.hasItemMeta() ? item.getItemMeta().getItemName() : item.getType().name();
        Material material = Material.valueOf(config.getString("icons.patternItem.material", "STONE").replace(MessageUtil.getReplacement("material"), item.getType().name().toUpperCase()).toUpperCase());
        int amount = config.getInt("icons.patternItem.amount", 1);
        int durability = config.getInt("icons.patternItem.durability", 0);
        boolean unbreakable = config.getBoolean("icon.patternItem.unbreakable", false);
        String name = config.getString("icons.patternItem.name", "$<id>")
                .replace(MessageUtil.getReplacement("id"), String.valueOf(c))
                .replace(MessageUtil.getReplacement("pattern.name"), itemName);
        List<String> lore = config.getStringList("icons.patternItem.lore");

        for(int i = 0; i < lore.size(); i++) {
            if(lore.get(i).contains(MessageUtil.getReplacement("pattern.lore"))) {
                lore.remove(i);
                if(item.getItemMeta() == null || item.getItemMeta().getLore() == null) continue;
                int newLines = 1;
                for(String line : item.getItemMeta().getLore()) {
                    lore.add(i - 1 + newLines, ChatUT.hexString(line));
                    newLines++;
                }
                i += newLines;
                continue;
            }
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("pattern.name"), itemName)
                    .replace(MessageUtil.getReplacement("pattern.id"), String.valueOf(c))));
        }
        Map<Enchantment, Integer> enchants = config.getEnchantmentSection("icons.patternItem.enchants");
        List<ItemFlag> flags = config.getItemFlags("icons.patternItem.flags");
        ItemFlag[] itemFlags = flags.toArray(new ItemFlag[0]);
        return ItemBuilder.newItem(material)
                .amount(amount)
                .durability(durability)
                .unbreakable(unbreakable)
                .name(name)
                .lore(lore)
                .enchant(enchants)
                .flag(itemFlags)
                .build();
    }
}
