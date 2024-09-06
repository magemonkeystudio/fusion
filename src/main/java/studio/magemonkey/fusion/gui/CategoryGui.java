package studio.magemonkey.fusion.gui;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.CraftingTable;

import java.util.*;

@Getter
public class CategoryGui implements Listener {

    private final Player player;
    private final CraftingTable table;

    private Inventory inventory;
    private final Map<Integer, RecipeGui> categories = new HashMap<>();


    public CategoryGui(Player player, CraftingTable table) {
        this.player = player;
        this.table = table;
        initialize();
    }

    private void initialize() {
        this.categories.clear();
        this.inventory = Bukkit.createInventory(null, table.getPattern().getInventorySize(), table.getInventoryName());
        Map<Character, ItemStack> items = table.getPattern().getItems();

        Set<Integer> recipeSlots = new HashSet<>();

        int i = 0;
        for (String row : table.getPattern().getPattern()) {
            for (char c : row.toCharArray()) {
                switch (c) {
                    // Mark the recipe slots to add categories here
                    case 'o', '=' -> recipeSlots.add(i);
                    // Prevent queues to be shown. This gui only shows categories on the recipe slots
                    case '-', '{', '}' -> {}
                    // On default use the pattern-items as usual
                    default -> {
                        if (items.containsKey(c)) {
                            inventory.setItem(i, items.get(c));
                        }
                    }
                }
                i++;
            }

            List<Category> categoryList = table.getCategories().values().stream().toList();
            int pos = 0;
            for (int slot : recipeSlots) {
                if (pos == categoryList.size()) {
                    break;
                }
                Category category = categoryList.get(pos);
                inventory.setItem(slot, category.getIconItem().create());
                categories.put(slot, new RecipeGui(player, table, category));
                pos++;
            }
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        if (categories.containsKey(slot)) {
            categories.get(slot).open(player);
        }
    }
}
