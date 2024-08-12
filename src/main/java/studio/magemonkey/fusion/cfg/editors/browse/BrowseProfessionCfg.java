package studio.magemonkey.fusion.cfg.editors.browse;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.cfg.professions.ProfessionConditions;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowseProfessionCfg {

    private final YamlParser config;

    public BrowseProfessionCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "Editors/browse/BrowseProfessionEditor.yml");
    }

    public String getTitle() {
        return ChatUT.hexString(config.getString("title", "&2Professions in browse gui"));
    }

    public Map<String, ItemStack> getIcons(BrowseEditor browseEditor) {
        Map<String, ItemStack> icons = new HashMap<>();
        for(String icon : config.getConfigurationSection("icons").getKeys(false)) {
            if(icon.equalsIgnoreCase("recipeItem")) continue;
            icons.put(icon, getIcon(browseEditor, icon));
        }
        return icons;
    }

    public ItemStack getIcon(BrowseEditor browseEditor, String icon) {
        Material material = Material.valueOf(config.getString("icons." + icon + ".material", "STONE").toUpperCase());
        int amount = config.getInt("icons." + icon + ".amount", 1);
        int durability = config.getInt("icons." + icon + ".durability", 0);
        boolean unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String name = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore = config.getStringList("icons." + icon + ".lore");
        for(int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("name"), browseEditor.getName())));
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

    public ItemStack getProfessionIcon(ProfessionConditions conditions) {
        CraftingTable table = ProfessionsCfg.getMap().get(conditions.getProfession());
        ItemStack result = table.getIconItem().create();
        String name = config.getString("icons.professionItem.name", "&9$<name>")
                .replace(MessageUtil.getReplacement("id"), conditions.getProfession())
                .replace(MessageUtil.getReplacement("name"), table.getInventoryName());
        List<String> lore = config.getStringList("icons.professionItem.lore");

        for(int i = 0; i < lore.size(); i++) {
            if(lore.get(i).contains(MessageUtil.getReplacement("conditions"))) {
                // TODO Full Translation of Conditions
                lore.remove(i);
                int newLines = 1;
                for(Map.Entry<String, Map<String, Integer>> entry : conditions.getFullConditions().entrySet()) {
                    for(Map.Entry<String, Integer> condition : entry.getValue().entrySet()) {
                        lore.add(i - 1 + newLines, config.getString("icons.professionItem.conditionPrefix", "&7- &a$<condition>")
                                .replace(MessageUtil.getReplacement("condition"), getIconConditionFormat(entry.getKey(), condition.getKey(), condition.getValue())));
                        newLines++;
                    }
                }
                i += newLines;
                continue;
            }
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("id"), conditions.getProfession())
                    .replace(MessageUtil.getReplacement("name"), ProfessionsCfg.getMap().get(conditions.getProfession()).getInventoryName())
                    .replace(MessageUtil.getReplacement("costs.money"), String.valueOf(conditions.getMoneyCost()))
                    .replace(MessageUtil.getReplacement("costs.exp"), String.valueOf(conditions.getExpCost()))
            ));
        }

        return ItemBuilder.newItem(result)
                .name(name)
                .lore(lore)
                .build();
    }

    private String getIconConditionFormat(String condition, String key, int value) {
        return config.getString("icons.professionItem.conditionFormatting." + condition, key + ": " + value)
                .replace(MessageUtil.getReplacement("condition.name"), key)
                .replace(MessageUtil.getReplacement("condition.amount"), String.valueOf(value));
    }

    private String getSubIconConditionFormat(String condition, String key, int value) {
        return config.getString("subEditor.icons.conditions.conditionFormatting." + condition, key + ": " + value)
                .replace(MessageUtil.getReplacement("condition.name"), key)
                .replace(MessageUtil.getReplacement("condition.amount"), String.valueOf(value));
    }
}
