package studio.magemonkey.fusion.gui;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.CraftingTable;

public final class RecipeGuiFactory {
    private RecipeGuiFactory() { }
    public static RecipeGui create(Player player, CraftingTable table, Category category) {
        return Cfg.craftingQueue ? new QueuedRecipeGUI(player, table, category) : new ManualRecipeGUI(player, table, category);
    }
}
