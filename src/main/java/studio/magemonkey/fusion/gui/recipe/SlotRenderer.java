package studio.magemonkey.fusion.gui.recipe;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.Objects;

/** Never erase a slot just to repaint it; send only actual item changes. */
public final class SlotRenderer {
    public void set(Inventory inventory, int slot, ItemStack desired) {
        if (!Objects.equals(inventory.getItem(slot), desired))
            inventory.setItem(slot, desired == null ? null : desired.clone());
    }
}
