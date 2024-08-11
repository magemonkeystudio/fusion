package studio.magemonkey.fusion.gui.editors.subeditors;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.Recipe;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.commands.EditorCriteria;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecipeItemEditor extends Editor implements Listener {

    private final Player player;
    @Getter
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
            case 10 ->
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Recipe_Edit_Name, "/fusion-editor <newName>");
            case 11 -> {
                List<String> categories = ((RecipeEditor) getParentEditor()).getTable().getCategoryList();
                String currentCategory = recipe.getCategory();
                int currentIndex = categories.indexOf(currentCategory);
                if (event.isLeftClick()) {
                    if(currentIndex == categories.size() - 1) {
                        recipe.setCategory(categories.get(0));
                    } else {
                        recipe.setCategory(categories.get(currentIndex + 1));
                    }
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (currentIndex == 0) {
                        recipe.setCategory(categories.get(categories.size() - 1));
                    } else {
                        recipe.setCategory(categories.get(currentIndex - 1));
                    }
                    hasChanges = true;
                }
            }
            case 12 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    recipe.setCraftingTime(recipe.getCraftingTime() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (recipe.getCraftingTime() == 0) return;
                    recipe.setCraftingTime(Math.max(recipe.getCraftingTime() - amount, 0));
                    hasChanges = true;
                }
            }
            case 14 -> FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Recipe_Edit_ResultItem, "/fusion-editor " + getRecipeName() + " " + getRecipeAmount());
            case 15 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    recipe.getResults().setProfessionExp(recipe.getResults().getProfessionExp() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (recipe.getResults().getProfessionExp() == 0) return;
                    recipe.getResults().setProfessionExp(Math.max(recipe.getResults().getProfessionExp() - amount, 0));
                    hasChanges = true;
                }
            }
            case 16 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    recipe.getResults().setVanillaExp(recipe.getResults().getVanillaExp() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (recipe.getResults().getVanillaExp() == 0) return;
                    recipe.getResults().setVanillaExp(Math.max(recipe.getResults().getVanillaExp() - amount, 0));
                    hasChanges = true;
                }
            }
            case 23 -> {
                if (event.isLeftClick())
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Recipe_Add_Commands, "/fusion-editor <caster> <delay> <command without />");
                else if (event.isRightClick()) {
                    if (recipe.getResults().getCommands().isEmpty())
                        return;
                    hasChanges = true;
                    recipe.getResults().getCommands().remove(recipe.getResults().getCommands().size() - 1);
                }
            }
            case 28 -> {
                if (event.isLeftClick())
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Recipe_Add_Ingredients, "/fusion-editor <ingredient> <amount>");
                else if (event.isRightClick()) {
                    if (recipe.getConditions().getRequiredItems().size() == 1) {
                        MessageUtil.sendMessage("editor.needMinimumOne", player);
                        FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Recipe_Edit_Ingredients, "/fusion-editor <ingredient> <amount>");
                        return;
                    }
                    hasChanges = true;
                    recipe.getConditions().getRequiredItems().remove(recipe.getConditions().getRequiredItems().size() - 1);
                }
            }
            case 29 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    recipe.getConditions().setMoneyCost(recipe.getConditions().getMoneyCost() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (recipe.getConditions().getMoneyCost() == 0) return;
                    recipe.getConditions().setMoneyCost(Math.max(recipe.getConditions().getMoneyCost() - amount, 0));
                    hasChanges = true;
                }
            }
            case 30 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    recipe.getConditions().setExpCost(recipe.getConditions().getExpCost() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (recipe.getConditions().getExpCost() == 0) return;
                    recipe.getConditions().setExpCost(Math.max(recipe.getConditions().getExpCost() - amount, 0));
                    hasChanges = true;
                }
            }
            case 31 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    recipe.getConditions().setProfessionLevel(recipe.getConditions().getProfessionLevel() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (recipe.getConditions().getProfessionLevel() == 0) return;
                    recipe.getConditions().setProfessionLevel(Math.max(recipe.getConditions().getProfessionLevel() - amount, 0));
                    hasChanges = true;
                }
            }
            case 32 -> {
                recipe.getConditions().setMastery(!recipe.getConditions().isMastery());
                hasChanges = true;
            }
            case 33 -> {
                if (event.isLeftClick())
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.Profession_Recipe_Add_Conditions, "/fusion-editor <conditionKey> <conditionValue> <level>");
                else if (event.isRightClick()) {
                    if (recipe.getConditions().getFullConditions().isEmpty())
                        return;
                    recipe.getConditions().removeLastCondition();
                    hasChanges = true;
                }
            }
            case 53 -> {
                reload(false);
                ((RecipeEditor) getParentEditor()).reload(true);
                return;
            }
        }

        if (hasChanges) {
            reload(true);
        }
    }

    public void reload(boolean open) {
        setIcons(EditorRegistry.getRecipeEditorCfg().getSubIcons(recipe));
        initialize();
        if (open)
            open(player);
    }

    public String getRecipeName() {
        return recipe.getResults().getResultName().split(":")[0];
    }

    public int getRecipeAmount() {
        return Integer.parseInt(recipe.getResults().getResultName().split(":")[1]);
    }
}
