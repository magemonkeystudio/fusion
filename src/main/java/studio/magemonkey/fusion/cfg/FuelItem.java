package studio.magemonkey.fusion.cfg;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Represents one type of item that can be used as fuel in the server fuel tank.
 */
@Getter
public class FuelItem {

    private final String    id;             // config key (e.g. "coal")
    private final Material  material;       // item material
    private final int       fuelAmount;     // fuel added per item consumed
    private final Material  returnMaterial; // item returned after use (null = nothing)

    public FuelItem(String id, Material material, int fuelAmount, Material returnMaterial) {
        this.id             = id;
        this.material       = material;
        this.fuelAmount     = fuelAmount;
        this.returnMaterial = returnMaterial;
    }

    /**
     * Creates a display ItemStack for use in the FuelGUI.
     */
    public ItemStack createDisplayItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + id);
            meta.setLore(List.of(
                    ChatColor.GRAY  + "Fuel per item: " + ChatColor.WHITE + fuelAmount,
                    returnMaterial != null
                            ? ChatColor.GRAY + "Returns: " + ChatColor.WHITE + returnMaterial.name()
                            : ChatColor.DARK_GRAY + "No return item",
                    "",
                    ChatColor.GREEN + "Click to add fuel from inventory"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Returns true if the given ItemStack matches this fuel item's material.
     */
    public boolean matches(ItemStack stack) {
        return stack != null && stack.getType() == material;
    }
}
