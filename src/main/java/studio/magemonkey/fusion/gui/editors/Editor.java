package studio.magemonkey.fusion.gui.editors;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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

    public Editor(Editor parentEditor, String title, int size) {
        this.parentEditor = parentEditor;
        this.title = title;
        this.size = size;

        this.inventory = Bukkit.createInventory(null, size, title);
        this.nestedInventories = new ArrayList<>();
    }

    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void openParent(Player player) {
        if (parentEditor != null) {
            parentEditor.open(player);
        }
    }
}
