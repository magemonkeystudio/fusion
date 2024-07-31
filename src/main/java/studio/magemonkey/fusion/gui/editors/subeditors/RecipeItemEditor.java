package studio.magemonkey.fusion.gui.editors.subeditors;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.Recipe;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.util.InventoryUtils;

public class RecipeItemEditor extends Editor implements Listener {

    private final Player player;
    private final Recipe recipe;

    public RecipeItemEditor(Editor parentEditor, Player player, Recipe recipe) {
        super(parentEditor, EditorRegistry.getRecipeEditorCfg().getSubTitle(recipe.getName()), 54);
        this.player = player;
        this.recipe = recipe;
        setIcons(EditorRegistry.getRecipeEditorCfg().getSubIcons(recipe));

        initialize();
        Fusion.registerListener(this);
    }

    public void initialize() {
        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        setItem(10, getIcons().get("name"));
        setItem(11, getIcons().get("category"));
        setItem(12, getIcons().get("craftingTime"));
        setItem(14, getIcons().get("resultItem"));
        setItem(15, getIcons().get("professionExp"));
        setItem(16, getIcons().get("vanillaExp"));
        setItem(23, getIcons().get("commands"));

        setItem(28, getIcons().get("ingredients"));
        setItem(29, getIcons().get("moneyCost"));
        setItem(30, getIcons().get("expCost"));
        setItem(31, getIcons().get("professionLevel"));
        setItem(32, getIcons().get("mastery"));
        setItem(33, getIcons().get("conditions"));
        setItem(53, getIcons().get("back"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 10 -> {
            }
            case 11 -> {

            }
            case 12 -> {

            }
            case 16 -> {

            }
            case 28 -> {

            }
            case 29 -> {
            }
            case 30 -> {

            }
            case 32 -> {

            }
            case 33 -> {

            }
            case 34 -> {


            }
            case 53 -> {
                player.closeInventory();
                hasChanges = true;
            }
        }

        if (hasChanges) {

        }
    }
}
