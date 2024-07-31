package studio.magemonkey.fusion.gui.editors.subeditors;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.Recipe;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.*;

public class RecipeEditor extends Editor implements Listener {

    private final Player player;
    private final CraftingTable table;

    private RecipeItemEditor recipeItemEditor;

    private final HashMap<Inventory, HashMap<Integer, Recipe>> slots = new HashMap<>();

    public RecipeEditor(Editor parentEditor, Player player, CraftingTable table) {
        super(parentEditor, EditorRegistry.getRecipeEditorCfg().getTitle(), 54);
        this.player = player;
        this.table = table;
        setIcons(EditorRegistry.getRecipeEditorCfg().getIcons(table));

        initialize();
        Fusion.registerListener(this);
    }

    public void initialize() {
        slots.clear();
        getNestedInventories().clear();

        HashMap<Integer, Recipe> invSlots = new HashMap<>();
        List<Inventory> inventories = new ArrayList<>();
        Inventory inv = null;
        int invSlot = 0;
        Collection<Recipe> recipes = table.getRecipes().values();
        for (Recipe entry : recipes) {
            int invDex = invSlot % 36 + 9;
            if (invDex == 9) {
                if (inv != null)
                    inventories.add(inv);
                inv = InventoryUtils.createFilledInventory(null, EditorRegistry.getRecipeEditorCfg().getTitle(), 54, getIcons().get("fill"));
                inv.setItem(4, getIcons().get("add"));
                inv.setItem(48, getIcons().get("previous"));
                inv.setItem(50, getIcons().get("next"));
                inv.setItem(53, getIcons().get("back"));
                invSlots = new HashMap<>();
            }
            inv.setItem(invDex, EditorRegistry.getRecipeEditorCfg().getRecipeIcon(entry));
            invSlots.put(invDex, entry);
            slots.put(inv, invSlots);
            invSlot++;
        }
        inventories.add(inv == null ? InventoryUtils.createFilledInventory(null, EditorRegistry.getRecipeEditorCfg().getTitle(), 54, getIcons().get("fill")) : inv);
        setNestedInventories(inventories);
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        player.openInventory(getNestedInventories().get(page) != null ? getNestedInventories().get(page) : getNestedInventories().get(page - 1));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        int invdex = getNestedInventories().indexOf(event.getInventory());
        if (invdex < 0) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        int size = getNestedInventories().size();
        boolean hasChanges = false;

        if (event.getClickedInventory() == player.getInventory()) {
            ItemStack item = player.getInventory().getItem(event.getSlot());
            if (item == null || item.getType() == Material.AIR) return;

            // TODO setup a RecipeItemEditor
            hasChanges = true;
        } else {
            switch (event.getSlot()) {
                case 48 -> open(player, (size + invdex - 1) % size);
                case 50 -> open(player, (invdex + 1) % size);
                case 53 -> openParent(player);
                default -> {
                    if (slots.get(getNestedInventories().get(invdex)).get(slot) != null) {
                        Recipe entry = slots.get(getNestedInventories().get(invdex)).get(slot);
                        if(event.isLeftClick()) {
                            // TODO Configure recipe further
                            recipeItemEditor = new RecipeItemEditor(this, player, entry);
                            recipeItemEditor.open(player);
                        } else if(event.isRightClick()) {
                            table.getRecipes().remove(entry.getName());
                            hasChanges = true;
                        }
                    }
                }
            }
        }

        if (hasChanges) {
            table.save();
            setIcons(EditorRegistry.getPatternItemEditorCfg().getIcons(table));
            initialize();
            open(player);
        }

    }
}
