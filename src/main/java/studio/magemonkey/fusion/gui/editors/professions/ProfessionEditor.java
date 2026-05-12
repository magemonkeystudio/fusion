package studio.magemonkey.fusion.gui.editors.professions;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.editors.EditorCriteria;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.commands.FusionEditorCommand;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.ProfessionGuiRegistry;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternEditor;
import studio.magemonkey.fusion.gui.editors.pattern.PatternItemsEditor;
import studio.magemonkey.fusion.gui.editors.professions.recipes.RecipeEditor;
import studio.magemonkey.fusion.util.InventoryUtils;
import studio.magemonkey.fusion.util.TabCacher;

@Getter
public class ProfessionEditor extends Editor implements Listener {

    private final Player        player;
    private final String        profession;
    private final CraftingTable table;

    private BukkitTask pendingSaveTask;

    private PatternItemsEditor patternItemsEditor;
    private PatternEditor      patternEditor;
    private PatternItemsEditor categoryPatternItemEditor;
    private PatternEditor      categoryPatternEditor;
    private CategoryEditor     categoryEditor;
    private RecipeEditor       recipeEditor;

    public ProfessionEditor(Player player, String profession) {
        super(null, EditorRegistry.getProfessionEditorCfg().getTitle(profession), 45);
        this.player = player;
        this.profession = profession;
        // Copy the table, deduplicating ItemGen pseudo-recipes (name::material) in one pass
        this.table = CraftingTable.copyForEditor(ProfessionsCfg.getTable(profession));
        setIcons(EditorRegistry.getProfessionEditorCfg().getIcons(table));

        initialize();
        Fusion.registerListener(this);
    }

    public void autoSave() {
        if (pendingSaveTask != null) {
            pendingSaveTask.cancel();
        }
        pendingSaveTask = Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), this::saveImmediate, 20L);
    }

    public void saveNow() {
        if (pendingSaveTask != null) {
            pendingSaveTask.cancel();
            pendingSaveTask = null;
        }
        saveImmediate();
    }

    private void saveImmediate() {
        pendingSaveTask = null;
        table.save(() -> {
            ProfessionsCfg.getMap().put(profession, table);
            ProfessionsCfg.getGuiMap().put(profession, new ProfessionGuiRegistry(profession));
            TabCacher.clearAllCaches("professions");
        });
    }

    private void initialize() {
        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        setItem(10, getIcons().get("name"));
        setItem(11, getIcons().get("icon"));
        setItem(12, getIcons().get("masteryUnlock"));
        setItem(13, getIcons().get("masteryCost"));
        setItem(14, getIcons().get("maxLevel"));
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
        Player  player     = (Player) event.getWhoClicked();
        boolean hasChanges = false;

        switch (event.getSlot()) {
            case 10 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.Profession_Edit_Name,
                    "/fusion-editor " + table.getName());
            case 11 -> FusionEditorCommand.suggestUsage(player,
                    EditorCriteria.Profession_Edit_Icon,
                    "/fusion-editor DIVINITY_" + table.getIconItem().getID());
            case 12 -> {
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
            case 13 -> {
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
            case 14 -> {
                int amount = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) {
                    table.setMaxLevel(table.getMaxLevel() + amount);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    if (table.getMaxLevel() == 0) return;
                    table.setMaxLevel(Math.max(table.getMaxLevel() - amount, 0));
                    hasChanges = true;
                }
            }
            case 16 -> {
                if (recipeEditor != null) recipeEditor.dispose();
                recipeEditor = new RecipeEditor(this, player, table);
                suppressCloseNav = true;
                recipeEditor.open(player);
            }
            case 28 -> {
                table.setUseCategories(!table.getUseCategories());
                hasChanges = true;
            }
            case 29 -> {
                if (patternItemsEditor != null) patternItemsEditor.dispose();
                patternItemsEditor = new PatternItemsEditor(this, player, table, false);
                suppressCloseNav = true;
                patternItemsEditor.open(player);
            }
            case 30 -> {
                if (patternEditor != null) patternEditor.dispose();
                patternEditor = new PatternEditor(this, player, table, false);
                suppressCloseNav = true;
                patternEditor.open(player);
            }
            case 32 -> {
                if (categoryEditor != null) categoryEditor.dispose();
                categoryEditor = new CategoryEditor(this, player, table);
                suppressCloseNav = true;
                categoryEditor.open(player);
            }
            case 33 -> {
                if (categoryPatternItemEditor != null) categoryPatternItemEditor.dispose();
                categoryPatternItemEditor = new PatternItemsEditor(this, player, table, true);
                suppressCloseNav = true;
                categoryPatternItemEditor.open(player);
            }
            case 34 -> {
                if (event.isLeftClick()) {
                    if (categoryPatternEditor != null) categoryPatternEditor.dispose();
                    categoryPatternEditor = new PatternEditor(this, player, table, true);
                    suppressCloseNav = true;
                    categoryPatternEditor.open(player);
                } else if (event.isRightClick()) {
                    if (table.getCatPattern() != null) {
                        table.getCatPattern().clear();
                    }
                }
            }
            case 44 -> {
                saveNow();
                suppressCloseNav = true;
                dispose();
                player.closeInventory();
                EditorRegistry.removeCurrentEditor(player);
                FusionEditorCommand.removeEditorCriteria(player.getUniqueId());
            }
        }

        if (hasChanges) {
            reload(true);
            autoSave();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != getInventory()) return;
        if (suppressCloseNav) {
            suppressCloseNav = false;
            return;
        }
        saveNow();
        EditorRegistry.removeCurrentEditor(player);
        FusionEditorCommand.removeEditorCriteria(player.getUniqueId());
        dispose();
    }

    public void reload(boolean open) {
        setIcons(EditorRegistry.getProfessionEditorCfg().getIcons(table));
        initialize();
        if (open) {
            suppressCloseNav = true;
            open(player);
        }
    }
}
