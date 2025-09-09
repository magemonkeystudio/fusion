package studio.magemonkey.fusion.cfg.editors.professions;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.YamlParser;
import studio.magemonkey.fusion.data.professions.ProfessionSettings;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.util.ChatUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeIconEditorCfg {

    private final YamlParser config;

    public RecipeIconEditorCfg() {
        this.config = YamlParser.loadOrExtract(Fusion.getInstance(), "Editors/professions/RecipeIconEditor.yml");
    }

    public String getTitle() {
        return ChatUT.hexString(config.getString("title", "&2Recipe Icon Editor"));
    }

    public Map<String, ItemStack> getIcons(Recipe recipe) {
        Map<String, ItemStack> icons = new HashMap<>();
        for (String icon : config.getConfigurationSection("icons").getKeys(false)) {
            icons.put(icon, getIcon(recipe, icon));
        }
        return icons;
    }

    public ItemStack getIcon(Recipe recipe, String icon) {
        Material     material    =
                Material.valueOf(config.getString("icons." + icon + ".material", "STONE").toUpperCase());
        int          amount      = config.getInt("icons." + icon + ".amount", 1);
        int          durability  = config.getInt("icons." + icon + ".durability", 0);
        boolean      unbreakable = config.getBoolean("icon." + icon + ".unbreakable", false);
        String       name        = config.getString("icons." + icon + ".name", "&cInvalid Item: &4" + icon);
        List<String> lore        = config.getStringList("icons." + icon + ".lore");

        ProfessionSettings settings = recipe.getSettings();
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(MessageUtil.getReplacement("lore"))) {
                lore.remove(i);
                if (settings.getLore().isEmpty()) continue;
                int newLines = 1;
                for (String line : settings.getLore()) {
                    lore.add(i - 1 + newLines, ChatUT.hexString(line));
                    newLines++;
                }
                i += newLines;
                continue;
            } else if (lore.get(i).contains(MessageUtil.getReplacement("commands"))) {
                lore.remove(i);
                int newLines = 1;

                for (DelayedCommand line : settings.getCommandsOnClick()) {
                    lore.add(i - 1 + newLines,
                            config.getString("subEditor.icons.commands.commandPrefix", "&7- &a$<command>")
                                    .replace(MessageUtil.getReplacement("command"),
                                            line.getCmd())
                                    .replace(MessageUtil.getReplacement("delay"),
                                            String.valueOf(line.getDelay()))
                                    .replace(MessageUtil.getReplacement("caster"),
                                            line.getAs().name())
                    );
                    newLines++;
                }
                i += newLines;
                continue;
            } else if (lore.get(i).contains(MessageUtil.getReplacement("enchants"))) {
                lore.remove(i);
                int newLines = 1;
                for (Enchantment enchant : settings.getEnchantments().keySet()) {
                    lore.add(i - 1 + newLines,
                            config.getString("subEditor.icons.enchants.enchantPrefix", "&7- &a$<enchant> $<level>")
                                    .replace(MessageUtil.getReplacement("enchant"),
                                            enchant.getKey().getKey())
                                    .replace(MessageUtil.getReplacement("level"),
                                            String.valueOf(settings.getEnchantments().get(enchant)))
                    );
                    newLines++;
                }
                i += newLines;
                continue;
            } else if (lore.get(i).contains(MessageUtil.getReplacement("flags"))) {
                lore.remove(i);
                int newLines = 1;
                for (ItemFlag flag : settings.getFlags()) {
                    lore.add(i - 1 + newLines, config.getString("subEditor.icons.flags.flagPrefix", "&7- &a$<flag>")
                            .replace(MessageUtil.getReplacement("flag"),
                                    flag.name().toLowerCase())
                    );
                    newLines++;
                }
                i += newLines;
                continue;
            }
            lore.set(i, ChatUT.hexString(lore.get(i)
                    .replace(MessageUtil.getReplacement("name"), settings.getName() != null ? settings.getName() : "")
                    .replace(MessageUtil.getReplacement("customModelData"),
                            String.valueOf(settings.getCustomModelData()))
                    .replace(MessageUtil.getReplacement("unbreakable"), String.valueOf(settings.isUnbreakable()))
                    .replace(MessageUtil.getReplacement("color"),
                            settings.getColor() != null ? settings.getColor() : "")
                    .replace(MessageUtil.getReplacement("cancelDrop"), String.valueOf(settings.isCancelDrop()))));
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
}
