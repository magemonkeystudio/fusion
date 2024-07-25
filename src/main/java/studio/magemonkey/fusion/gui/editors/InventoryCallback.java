package studio.magemonkey.fusion.gui.editors;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public interface InventoryCallback {
    void onInventoryClick(InventoryClickEvent event);
    void onInventoryClose(InventoryCloseEvent event);
}
