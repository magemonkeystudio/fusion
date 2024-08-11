package studio.magemonkey.fusion.gui.editors.subeditors.pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.InventoryPattern;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.gui.editors.Editor;

import java.util.HashMap;
import java.util.Map;

public class PatternEditor extends Editor implements Listener {

    private final Player player;
    private final CraftingTable table;
    private InventoryPattern pattern;

    private boolean isCategoryPattern;

    private final Map<Integer, Character> slots = new HashMap<>();

    public PatternEditor(Editor parentEditor, Player player, CraftingTable table, boolean isCategoryPattern) {
        super(parentEditor, EditorRegistry.getPatternEditorCfg().getTitle(isCategoryPattern ? "Category" : "Master"), (isCategoryPattern && table.getCatPattern() != null) ? table.getCatPattern().getInventorySize() : table.getPattern().getInventorySize());
        this.player = player;
        this.table = table;
        this.isCategoryPattern = isCategoryPattern;
        setIcons(EditorRegistry.getPatternEditorCfg().getIcons(table));
        this.pattern = isCategoryPattern ? table.getCatPattern() : table.getPattern();
        if (isCategoryPattern && this.pattern == null) {
            table.setCatPattern(InventoryPattern.copy(table.getPattern()));
            this.pattern = table.getCatPattern();
            MessageUtil.sendMessage("editor.defaultCategoryPattern", player);
        }

        initialize();
        Fusion.registerListener(this);
    }

    public void initialize() {
        slots.clear();
        int i = 0;
        for (String key : pattern.getPattern()) {
            for (char c : key.toCharArray()) {
                slots.put(i, c);
                if (c == 'o') {
                    setItem(i, getIcons().get("recipeSlot"));
                    i++;
                    continue;
                } else if (pattern.getItems().get(c) != null && !pattern.getItems().get(c).getType().isAir()) {
                    setItem(i, EditorRegistry.getPatternEditorCfg().getPatternItem(c, pattern.getItems().get(c)));
                }
                i++;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        event.setCancelled(true);
        boolean hasChanges = false;
        if (event.getClickedInventory() == getInventory()) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (slots.containsKey(slot)) {
                if (event.isLeftClick()) {
                    char cycled = pattern.getCycledCharacter(pattern.getSlot(slot), true);
                    pattern.replaceSlot(event.getSlot(), cycled);
                    hasChanges = true;
                } else if (event.isRightClick()) {
                    char cycled = pattern.getCycledCharacter(pattern.getSlot(slot), false);
                    pattern.replaceSlot(event.getSlot(), cycled);
                    hasChanges = true;
                }
            }
        }
        if (hasChanges) {
            setIcons(EditorRegistry.getPatternEditorCfg().getIcons(table));
            initialize();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != getInventory()) return;
        Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), () -> openParent(player), 1);
    }
}
