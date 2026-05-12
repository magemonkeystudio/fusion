package studio.magemonkey.fusion.gui.editors.professions.recipes;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.editors.EditorCriteria;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.professions.ProfessionEditor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeEditor extends Editor implements Listener {

    private final Player        player;
    @Getter
    private final CraftingTable table;

    @Getter
    private RecipeItemEditor recipeItemEditor;

    private final HashMap<Inventory, HashMap<Integer, Recipe>> slots = new HashMap<>();
    private List<Recipe>       recipeList     = new ArrayList<>();
    private final Set<Integer> populatedPages = new HashSet<>();

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
        populatedPages.clear();
        for (Inventory old : getNestedInventories()) {
            EditorRegistry.unregisterInventory(old);
        }
        getNestedInventories().clear();

        recipeList = new ArrayList<>(table.getRecipes().values());

        int             pageCount   = recipeList.isEmpty() ? 1 : (int) Math.ceil(recipeList.size() / 36.0);
        List<Inventory> inventories = new ArrayList<>();
        for (int p = 0; p < pageCount; p++) {
            Inventory inv = InventoryUtils.createFilledInventory(null,
                    EditorRegistry.getRecipeEditorCfg().getTitle(),
                    54,
                    getIcons().get("fill"));
            inv.setItem(4, getIcons().get("add"));
            inv.setItem(48, getIcons().get("previous"));
            inv.setItem(50, getIcons().get("next"));
            inv.setItem(53, getIcons().get("back"));
            inventories.add(inv);
        }
        setNestedInventories(inventories);
        for (Inventory nested : inventories) {
            EditorRegistry.registerInventory(nested, this);
        }
    }

    private void populatePage(int page) {
        if (page < 0 || page >= getNestedInventories().size()) return;
        Inventory                inv      = getNestedInventories().get(page);
        HashMap<Integer, Recipe> invSlots = new HashMap<>();
        int                      start    = page * 36;
        int                      end      = Math.min(start + 36, recipeList.size());
        for (int i = start; i < end; i++) {
            Recipe entry  = recipeList.get(i);
            int    invDex = (i - start) + 9;
            inv.setItem(invDex, EditorRegistry.getRecipeEditorCfg().getRecipeIcon(entry));
            invSlots.put(invDex, entry);
        }
        slots.put(inv, invSlots);
        populatedPages.add(page);
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<Inventory> invs     = getNestedInventories();
        int             safePage = (page >= 0 && page < invs.size()) ? page : invs.size() - 1;
        if (!populatedPages.contains(safePage)) {
            populatePage(safePage);
        }
        suppressCloseNav = true;
        player.openInventory(invs.get(safePage));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        int invdex = getNestedInventories().indexOf(event.getInventory());
        if (invdex < 0) return;
        event.setCancelled(true);
        Player  player     = (Player) event.getWhoClicked();
        int     slot       = event.getSlot();
        int     size       = getNestedInventories().size();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 4 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.Profession_Recipe_Add,
                    "/fusion-editor <recipeName> <resultItem> <amount>");
            case 48 -> open(player, (size + invdex - 1) % size);
            case 50 -> open(player, (invdex + 1) % size);
            case 53 -> openParent(player);
            default -> {
                if (slots.containsKey(getNestedInventories().get(invdex))
                        && slots.get(getNestedInventories().get(invdex)).containsKey(slot)) {
                    Recipe entry = slots.get(getNestedInventories().get(invdex)).get(slot);
                    if (!event.isShiftClick()) {
                        if (event.isLeftClick()) {
                            recipeItemEditor = new RecipeItemEditor(this, player, entry);
                            suppressCloseNav = true;
                            recipeItemEditor.open(player);
                        } else if (event.isRightClick()) {
                            table.getRecipes().remove(entry.getName());
                            hasChanges = true;
                        }
                    } else {
                        Recipe recipe = table.getRecipes().get(entry.getName());
                        if (event.isLeftClick()) {
                            // Put the Recipe one more to the left in the Map
                            table.moveEntry(recipe, -1);
                            hasChanges = true;
                        } else if (event.isRightClick()) {
                            // Put the Recipe one more to the right in the Map
                            table.moveEntry(recipe, 1);
                            hasChanges = true;
                        }
                    }
                }
            }
        }

        if (hasChanges) {
            reload(true);
            Editor root = getRootEditor();
            if (root instanceof ProfessionEditor) ((ProfessionEditor) root).autoSave();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!getNestedInventories().contains(event.getInventory())) return;
        if (suppressCloseNav) {
            suppressCloseNav = false;
            return;
        }
        // Fix B: bail if editor was already closed via command (prevents reopening after closeAllEditors)
        Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), () -> {
            if (EditorRegistry.getCurrentEditor(player) == null) return;
            openParent(player);
        }, 1);
    }

    public void reload(boolean open) {
        setIcons(EditorRegistry.getRecipeEditorCfg().getIcons(table));
        initialize();
        if (open)
            open(player);
    }
}
