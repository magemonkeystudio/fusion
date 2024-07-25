package studio.magemonkey.fusion.gui.editors;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

@Getter
public class Editor {

    private final String title;
    private final int size;
    @Setter
    private Map<String, ItemStack> icons;

    @Setter
    private InventoryCallback inventoryCallback;

    private final Inventory inventory;

    public Editor(String title, int size) {
        this.title = title;
        this.size = size;

        this.inventory = Bukkit.createInventory(null, size, title);
    }

    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}
