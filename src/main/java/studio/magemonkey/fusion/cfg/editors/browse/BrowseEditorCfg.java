package studio.magemonkey.fusion.cfg.editors.browse;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowseEditorCfg {

    private final YamlParser config;

    public BrowseEditorCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "Editors/browse/BrowseEditor.yml");
    }

    public String getTitle() {
        return ChatUT.hexString(config.getString("title", "Browse Gui Editor"));
    }

    public Map<String, ItemStack> getIcons(BrowseEditor browseEditor) {
        Map<String, ItemStack> icons = new HashMap<>();
        for(String icon : config.getConfigurationSection("icons").getKeys(false)) {
            icons.put(icon, getIcon(browseEditor, icon));
        }
        return icons;
    }

    public ItemStack getIcon(BrowseEditor browseEditor, String icon) {
        Material material = Material.valueOf(config.getString("icons." + icon + ".material", "STONE"));
        int amount = config.getInt("icons." + icon + ".amount", 1);
        int durability = config.getInt("icons." + icon + ".durability", 0);
        boolean unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String name = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore = config.getStringList("icons." + icon + ".lore");
        for(int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatUT.hexString(lore.get(i).replace(MessageUtil.getReplacement("name"), browseEditor.getName())));
        }
        Map<Enchantment, Integer> enchants = config.getEnchantmentSection("icons." + icon + ".enchants");
        List<ItemFlag> flags = config.getItemFlags("icons." + icon + ".flags");
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
