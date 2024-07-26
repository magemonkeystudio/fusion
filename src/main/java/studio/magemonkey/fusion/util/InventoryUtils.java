package studio.magemonkey.fusion.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtils {

    public static void fillInventory(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if(inventory.getItem(i) == null || inventory.getItem(i).getType().equals(Material.AIR))
                inventory.setItem(i, item);
        }
    }

    public static Inventory createFilledInventory(Player player, String title, int size, ItemStack fillItem) {
        Inventory inv = Bukkit.createInventory(player, size, title);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, fillItem);
        }
        return inv;
    }
}
