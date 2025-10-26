package studio.magemonkey.fusion.cfg;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.util.ChatUT;

public class CraftingRequirementsCfg {

    private static YamlParser config;

    public static void init() {
        config = YamlParser.loadOrExtract(Fusion.getInstance(), "lang/CraftingRequirements.yml");
    }

    private static String getFulfilled() {
        return ChatUT.hexString(config.getString("fulfilled", "&a✓"));
    }

    private static String getUnfulfilled() {
        return ChatUT.hexString(config.getString("unfulfilled", "&c✗"));
    }

    /* Recipe-related crafting requirements */
    public static String getCraftingRequirementLine(String path) {
        return ChatUT.hexString(config.getString(path + ".requirementLine", "&7Crafting Requirements"));
    }

    public static String getCraftingConditionLine(String path) {
        return ChatUT.hexString(config.getString(path + ".conditionLine", "&7Crafting Conditions"));
    }

    public static String getBossBarTitle(ItemStack item) {
        String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName()
                : ChatUT.serialize(Component.translatable(item.getTranslationKey()));
        return ChatUT.hexString(config.getString("recipes.bossbar", "&5Crafting $<item>...")
                .replace(MessageUtil.getReplacement("item"), itemName));
    }

    public static String getLearned(String path, boolean fulfilled) {
        return ChatUT.hexString(config.getString(path + ".learned." + (fulfilled ? "true" : "false"),
                (fulfilled ? "&aLearned" : "&cNot Learned")));
    }

    public static String getMoney(String path, double money, double required) {
        boolean fulfilled = money >= required;
        String line = config.getString(path + ".money." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eMoney: &7(&a$$<money>&8/&7$$<required>&7)"
                        : "&6- &eMoney: &7(&c$$<money>&8/&7$$<required>&7)");
        line = line.replace("$<money>", String.valueOf(money)).replace("$<required>", String.valueOf(required));
        return ChatUT.hexString(line);
    }

    public static String getExp(String path, int experience, int required) {
        boolean fulfilled = experience >= required;
        String line = config.getString(path + ".exp." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eExperience: &7(&a<exp>&8/&7<required>&7)"
                        : "&6- &eExperience: &7(&c<exp>&8/&7<required>&7)");
        line = line.replace("$<exp>", String.valueOf(experience)).replace("$<required>", String.valueOf(required));
        return ChatUT.hexString(line);
    }

    public static String getProfessionLevel(String path, int level, int required) {
        boolean fulfilled = level >= required;
        String line = config.getString(path + ".professionLevel." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eLevel Needed: &7(&a<level>&8/&7<required>&7)"
                        : "&6- &eLevel Needed: &7(&c<level>&8/&7<required>&7)");
        line = line.replace("$<level>", String.valueOf(level)).replace("$<required>", String.valueOf(required));
        return ChatUT.hexString(line);
    }

    public static String getMastery(String path, boolean mastery, boolean required) {
        boolean fulfilled = mastery == required;
        String line = config.getString(path + ".mastery." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eMastery: &7(&a<mastery>&8/&7<required>&7)"
                        : "&6- &eMastery: &7(&c<mastery>&8/&7<required>&7)");
        line = line.replace("$<mastery>", String.valueOf(mastery)).replace("$<required>", String.valueOf(required));
        return ChatUT.hexString(line);
    }

    public static String getConditionLine(String path, String condition, String key, int level, int required) {
        boolean fulfilled = level >= required;
        String line = config.getString(path + ".conditions." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- e$<condition>&8: &7(&a<level>&8/&7<required>&7)"
                        : "&6- &e$<condition>&8: &7(&c$<level>&8/&7$<condition.level>&7)");
        line = line.replace("$<level>", String.valueOf(level)).replace("$<required>", String.valueOf(required));
        line = line.replace("$<condition>", getConditionVariation(path, condition, key));
        return ChatUT.hexString(line);
    }

    private static String getConditionVariation(String path, String condition, String key) {
        return config.getString(path + ".conditions.variations." + condition, "$<name>").replace("$<name>", key);
    }

    public static String getIngredientLine(String path, RecipeItem item, int amount, int required) {
        boolean fulfilled = amount >= required;
        String line = config.getString(path + ".ingredients." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &e$<item>&8: &7(&a<amount>&8/&7<required>&7)"
                        : "&6- &e$<item>&8: &7(&c<amount>&8/&7<required>&7)");
        line = line.replace("$<amount>", String.valueOf(amount)).replace("$<required>", String.valueOf(required));


        ItemStack _item    = item.getItemStack();
        String    itemName = VersionManager.getCompat().getItemName(_item);

        if (_item.hasItemMeta()) {
            line = getCustomHighlight(path) + line;
        }
        line = line.replace("$<item>", itemName);
        return ChatUT.hexString(line);
    }

    private static String getCustomHighlight(String path) {
        return config.getString(path + ".ingredients.highlightCustomItem", "");
    }

    /* Recipe-related crafting requirements */
    public static String getCanCraft(boolean fulfilled) {
        return ChatUT.hexString(config.getString("recipes.canCraft." + (fulfilled ? "true" : "false"),
                (fulfilled ? "&aYou can craft this item." : "&cYou can't craft this item.")));
    }

    public static String getLimit(String path, int limit, int maxLimit) {
        boolean fulfilled = limit < maxLimit;
        String line = config.getString(path + ".limit." + (fulfilled ? "true" : "false"),
                fulfilled ? "&6- &eLimit: &7(&a<limit>&8/&7<required>&7)"
                        : "&6- &eLimit: &7(&c<limit>&8/&7<required>&7)");
        line = line.replace("$<limit>", String.valueOf(limit)).replace("$<required>", String.valueOf(maxLimit));
        return ChatUT.hexString(line);
    }

    /* Profession-related crafting requirements */
    public static String getCanJoin(boolean fulfilled) {
        return ChatUT.hexString(config.getString("professions.canJoin." + (fulfilled ? "true" : "false"),
                (fulfilled ? "&aYou can join this profession." : "&cYou can't join this profession.")));
    }
}
