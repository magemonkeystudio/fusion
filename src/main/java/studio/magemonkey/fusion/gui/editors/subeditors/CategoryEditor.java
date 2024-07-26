package studio.magemonkey.fusion.gui.editors.subeditors;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.util.InventoryUtils;

import java.util.*;

public class CategoryEditor extends Editor implements Listener {

    private final Player player;
    private final CraftingTable table;

    private final Map<Integer, Category> slots = new HashMap<>();

    public CategoryEditor(Editor parentEditor, Player player, CraftingTable table) {
        super(parentEditor, EditorRegistry.getCategoryEditorCfg().getTitle(), 54);
        this.player = player;
        this.table = table;
        setIcons(EditorRegistry.getCategoryEditorCfg().getIcons(table));
        initialize();
        Fusion.registerListener(this);
    }

    public void initialize() {
        slots.clear();

        InventoryUtils.fillInventory(getInventory(), getIcons().get("fill"));
        int slot = 9;
        for (Map.Entry<String, Category> entry : table.getCategories().entrySet()) {
            ItemStack icon = entry.getValue().getIconItem().create();
            if (icon.getType().isAir()) continue;
            slots.put(slot, entry.getValue());
            slot++;
        }

        setItem(4, getIcons().get("add"));
        setItem(53, getIcons().get("back"));

        for (int i = 9; i < 45; i++) {
            if (slots.containsKey(i)) {
                setItem(i, EditorRegistry.getCategoryEditorCfg().getCategoryItemIcon(slots.get(i)));
            } else {
                setItem(i, new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != getInventory()) return;
        boolean hasChanges = false;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == getInventory()) {
            event.setCancelled(true);
            switch (event.getSlot()) {
                case 4 -> {
                    // TODO Command to add new category
                }
                case 53 -> openParent(player);
                default -> {
                    if (slots.containsKey(event.getSlot())) {
                        if (event.isLeftClick()) {
                            // TODO Edit the category through command
                            hasChanges = true;
                        } else if(event.isRightClick()) {
                            // TODO Remove the category through command
                            table.getCategories().remove(slots.get(event.getSlot()).getName());
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
