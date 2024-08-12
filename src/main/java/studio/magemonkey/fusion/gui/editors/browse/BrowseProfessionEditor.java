package studio.magemonkey.fusion.gui.editors.browse;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.cfg.professions.ProfessionConditions;
import studio.magemonkey.fusion.commands.EditorCriteria;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternItemsEditor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.ArrayList;

public class BrowseProfessionEditor extends Editor implements Listener {

    private final Player player;

    private ProfessionConditions conditions;

    public BrowseProfessionEditor(Editor editor, Player player, ProfessionConditions conditions) {
        super(editor, EditorRegistry.getBrowseProfessionCfg().getSubTitle(conditions.getProfession()),27);
        this.player = player;
        this.conditions = conditions;
        setIcons(EditorRegistry.getBrowseProfessionCfg().getSubIcons(conditions));
        initialize();
        Fusion.registerListener(this);
    }

    private void initialize() {
        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        setItem(10, getIcons().get("moneyCost"));
        setItem(12, getIcons().get("expCost"));
        setItem(14, getIcons().get("ingredients"));
        setItem(16, getIcons().get("conditions"));
        setItem(26, getIcons().get("back"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 10 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    conditions.setMoneyCost(conditions.getMoneyCost() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (conditions.getMoneyCost() == 0) return;
                    conditions.setMoneyCost(Math.max(conditions.getMoneyCost() - amount, 0));
                    hasChanges = true;
                }
            }
            case 12 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    conditions.setExpCost(conditions.getExpCost() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (conditions.getExpCost() == 0) return;
                    conditions.setExpCost(Math.max(conditions.getExpCost() - amount, 0));
                    hasChanges = true;
                }
            }
            case 14 -> FusionEditorCommand.suggestUsage(player, EditorCriteria.Browse_Profession_Add_Ingredients, "/fusion-editor <ingredient> <amount>");
            case 16 -> FusionEditorCommand.suggestUsage(player, EditorCriteria.Browse_Profession_Add_Conditions, "/fusion-editor <conditionKey> <conditionValue> <level>");
            case 26 -> {
                reload(false);
                ((BrowseProfessionsEditor) getParentEditor()).reload(true);
            }
        }

        if (hasChanges) {
            reload(true);
        }
    }

    private void reload(boolean open) {
        setIcons(EditorRegistry.getBrowseProfessionCfg().getSubIcons(conditions));
        initialize();
        if(open)
            open(player);
    }
}
