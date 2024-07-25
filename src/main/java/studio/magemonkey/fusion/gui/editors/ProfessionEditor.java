package studio.magemonkey.fusion.gui.editors;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.gui.editors.subeditors.PatternItemEditor;
import studio.magemonkey.fusion.util.InventoryUtils;

@Getter
public class ProfessionEditor extends Editor implements Listener {

    private final Player player;
    private final String profession;
    private final CraftingTable table;

    private PatternItemEditor patternItemEditor;
    private PatternItemEditor categoryPatternItemEditor;

    public ProfessionEditor(Player player, String profession) {
        super(EditorRegistry.getProfessionEditorCfg().getTitle(profession), 45);
        this.player = player;
        this.profession = profession;
        // Copy the table to prevent changes to the original table while people do crafting
        this.table = CraftingTable.copy(ProfessionsCfg.getTable(profession));
        setIcons(EditorRegistry.getProfessionEditorCfg().getIcons(table));

        Fusion.registerListener(this);
        initialize();
    }

    private void initialize() {
        InventoryUtils.fillInventory(getInventory(), new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        setItem(10, getIcons().get("name"));
        setItem(11, getIcons().get("masteryUnlock"));
        setItem(12, getIcons().get("masteryCost"));
        setItem(16, getIcons().get("recipes"));
        setItem(28, getIcons().get("useCategories"));
        setItem(29, getIcons().get("patternItems"));
        setItem(30, getIcons().get("pattern"));

        setItem(32, getIcons().get("categories"));
        setItem(33, getIcons().get("categoryPatternItems"));
        setItem(34, getIcons().get("categoryPattern"));
        setItem(44, getIcons().get("back"));
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
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    table.setMasteryUnlock(table.getMasteryUnlock() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (table.getMasteryUnlock() == 0) return;
                    table.setMasteryUnlock(Math.max(table.getMasteryUnlock() - amount, 0));
                    hasChanges = true;
                }
            }
            case 12 -> {
                int amount = event.isShiftClick() ? 100 : 1;
                if (event.isLeftClick()) {
                    table.setMasteryFee(table.getMasteryFee() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (table.getMasteryFee() == 0) return;
                    table.setMasteryFee(Math.max(table.getMasteryFee() - amount, 0));
                    hasChanges = true;
                }
            }
            case 17 -> {
            }
            case 28 -> {
                table.setUseCategories(!table.getUseCategories());
                hasChanges = true;
            }
            case 29 -> {
                if (patternItemEditor == null)
                    patternItemEditor = new PatternItemEditor(player, table, false);
                patternItemEditor.open(player);
            }
            case 30 -> {
            }
            case 31 -> {
            }
            case 32 -> {
            }
            case 33 -> {
            }
            case 34 -> {

                if (categoryPatternItemEditor == null)
                    categoryPatternItemEditor = new PatternItemEditor(player, table, true);
                categoryPatternItemEditor.open(player);
            }
            case 44 -> player.closeInventory();
        }

        if (hasChanges) {
            table.save();
            setIcons(EditorRegistry.getProfessionEditorCfg().getIcons(table));
            initialize();
            open(player);
        }
    }
}
