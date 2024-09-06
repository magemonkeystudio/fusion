package studio.magemonkey.fusion.gui;

import lombok.Getter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class ProfessionGuiRegistry {

    @Getter
    private final String profession;

    private final Map<UUID, CategoryGui> categoryGuis = new TreeMap<>();
    private final Map<UUID, RecipeGui> recipeGuis = new TreeMap<>();

    public ProfessionGuiRegistry(String profession) {
        this.profession = profession;
    }

    public void open(Player player) {
        CraftingTable table = ProfessionsCfg.getTable(profession);
        if(table.getUseCategories() && !table.getCategories().isEmpty()) {
            categoryGuis.put(player.getUniqueId(), new CategoryGui(player, table));
            categoryGuis.get(player.getUniqueId()).open(player);
        } else {
            recipeGuis.put(player.getUniqueId(), new RecipeGui(player, table, new Category("master", "PAPER", table.getPattern(), 1)));
            recipeGuis.get(player.getUniqueId()).open(player);
        }
    }

    public void closeAll() {
        for(CategoryGui gui : new ArrayList<>(categoryGuis.values())) {
            gui.getInventory().getViewers().forEach(HumanEntity::closeInventory);
            for(RecipeGui recipeGui : gui.getCategories().values()) {
                recipeGui.getInventory().getViewers().forEach(HumanEntity::closeInventory);
            }
        }
        for(RecipeGui gui : new ArrayList<>(recipeGuis.values())) {
            gui.getInventory().getViewers().forEach(HumanEntity::closeInventory);
        }
    }
}
