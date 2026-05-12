package studio.magemonkey.fusion.gui.editors;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class Editor {

    private final Editor                 parentEditor;
    private final String                 title;
    private final int                    size;
    @Setter
    private       Map<String, ItemStack> icons;

    private final Inventory       inventory;
    @Setter
    private       List<Inventory> nestedInventories;

    protected boolean suppressCloseNav = false;

    public Editor(Editor parentEditor, String title, int size) {
        this.parentEditor = parentEditor;
        this.title = title;
        this.size = size;

        this.inventory = Bukkit.createInventory(null, size, title);
        this.nestedInventories = new ArrayList<>();
        EditorRegistry.registerInventory(this.inventory, this);
    }

    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void openParent(Player player) {
        if (parentEditor != null) {
            suppressCloseNav = true;
            dispose();
            parentEditor.open(player);
        }
    }

    public void suppressNextClose() {
        this.suppressCloseNav = true;
    }

    /**
     * Unregisters this editor's event listeners and removes its inventories from the registry.
     * Call this when navigating away from this editor permanently.
     */
    public void dispose() {
        if (this instanceof Listener listener) {
            HandlerList.unregisterAll(listener);
        }
        EditorRegistry.unregisterInventory(this.inventory);
        for (Inventory nested : nestedInventories) {
            EditorRegistry.unregisterInventory(nested);
        }
    }

    public Editor getRootEditor() {
        Editor current = this;
        while (current.getParentEditor() != null) {
            current = current.getParentEditor();
        }
        return current;
    }
}
