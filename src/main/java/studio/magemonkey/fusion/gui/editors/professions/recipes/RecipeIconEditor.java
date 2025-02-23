package studio.magemonkey.fusion.gui.editors.professions.recipes;

import lombok.Getter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.editors.EditorCriteria;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternItemsEditor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.ArrayList;

@Getter
public class RecipeIconEditor extends Editor implements Listener {

    private final Player player;
    private final Recipe recipe;

    public RecipeIconEditor(Editor parentEditor, Player player, Recipe recipe) {
        super(parentEditor, EditorRegistry.getRecipeIconEditorCfg().getTitle(), 45);
        this.player = player;
        this.recipe = recipe;

        setIcons(EditorRegistry.getRecipeIconEditorCfg().getIcons(recipe));
        initialize();
        Fusion.registerListener(this);
    }

    private void initialize() {
        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        setItem(10, getIcons().get("name"));
        setItem(11, getIcons().get("lore"));
        //setItem(13, getIcons().get("patternItem"));
        setItem(15, getIcons().get("customModelData"));
        setItem(16, getIcons().get("commands"));
        setItem(29, getIcons().get("color"));
        setItem(30, getIcons().get("unbreakable"));
        setItem(32, getIcons().get("enchants"));
        setItem(33, getIcons().get("flags"));
        setItem(44, getIcons().get("back"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        Player  player     = (Player) event.getWhoClicked();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 10 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.RecipeIcon_Edit_Name,
                    "/fusion-editor " + recipe.getSettings().getName());
            case 11 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.RecipeIcon_Edit_Lore, "/fusion-editor <lore>");
                } else {
                    if (recipe.getSettings().getLore().isEmpty()) {
                        return;
                    }
                    recipe.getSettings().getLore().remove(recipe.getSettings().getLore().size() - 1);
                    hasChanges = true;
                }
            }
            /*case 13 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.Pattern_Edit_Pattern,
                    "/fusion-editor " + builder.getMaterial().getNamespacedID() + " " + builder.getAmount());*/
            case 15 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    recipe.getSettings().setCustomModelData(recipe.getSettings().getCustomModelData() + amount);
                } else if (event.isRightClick()) {
                    if (recipe.getSettings().getCustomModelData() == 0) return;
                    recipe.getSettings().setCustomModelData(Math.max(recipe.getSettings().getCustomModelData() - amount, 0));
                }
                hasChanges = true;
            }
            case 16 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.RecipeIcon_Add_Commands, "/fusion-editor <caster> <delay> <command without />");
                } else {
                    if (recipe.getSettings().getCommandsOnClick().isEmpty()) {
                        return;
                    }
                    DelayedCommand command = new ArrayList<>(recipe.getSettings().getCommandsOnClick()).get(recipe.getSettings().getCommandsOnClick().size() - 1);
                    recipe.getSettings().getCommandsOnClick().remove(command);
                    hasChanges = true;
                }
            }
            case 26 -> {
                reload(false);
                ((PatternItemsEditor) getParentEditor()).reload(true);
                return;
            }
            case 29 -> {
                FusionEditorCommand.suggestUsage(player,
                        EditorCriteria.RecipeIcon_Edit_Color,
                        "/fusion-editor " + recipe.getSettings().getColor());
                hasChanges = true;
            }
            case 30 -> {
                recipe.getSettings().setUnbreakable(!recipe.getSettings().isUnbreakable());
                hasChanges = true;
            }
            case 32 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player,
                            EditorCriteria.RecipeIcon_Add_Enchants,
                            "/fusion-editor <enchantment> [level]");
                } else if (event.isRightClick()) {
                    if (recipe.getSettings().getEnchantments().isEmpty()) {
                        return;
                    }
                    // Get last entry and remove it
                    Enchantment lastEnchantment =
                            new ArrayList<>(recipe.getSettings().getEnchantments().keySet()).get(recipe.getSettings().getEnchantments().size() - 1);
                    recipe.getSettings().getEnchantments().remove(lastEnchantment);
                    hasChanges = true;
                }
            }
            case 33 -> {
                if (event.isLeftClick()) {
                    FusionEditorCommand.suggestUsage(player, EditorCriteria.RecipeIcon_Add_Flags, "/fusion-editor <flag>");
                } else if (event.isRightClick()) {
                    if (recipe.getSettings().getFlags().isEmpty()) {
                        return;
                    }
                    // Remove the last flag from the set
                    ItemFlag lastFlag = new ArrayList<>(recipe.getSettings().getFlags()).get(recipe.getSettings().getFlags().size() - 1);
                    recipe.getSettings().getFlags().remove(lastFlag);
                    hasChanges = true;
                }
            }
            case 44 -> {
                getParentEditor().open(player);
                return;
            }
        }

        if (hasChanges) {
            reload(true);
        }
    }

    public void reload(boolean open) {
        setIcons(EditorRegistry.getRecipeIconEditorCfg().getIcons(recipe));
        initialize();
        if (open)
            open(player);
    }

    public void addCommand(DelayedCommand command) {
        recipe.getSettings().getCommandsOnClick().add(command);
    }
}
