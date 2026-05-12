package studio.magemonkey.fusion.cfg;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.Fusion;

import java.io.File;
import java.io.IOException;

/**
 * Global per-server fuel counter.
 *
 * Configuration (config.yml):
 *   fuel.start   — initial fuel on first start
 *   fuel.max     — maximum capacity
 *   fuel.items   — items that can be used as fuel (loaded into Cfg.fuelItems)
 *
 * Persistence (fuel.yml):
 *   current      — current fuel level (updated live)
 */
public final class FuelManager {

    private static int current = 0;

    private FuelManager() {}

    // ──────────────────────────────────────────────
    // Initialisation — called from Fusion.reloadConfig()
    // ──────────────────────────────────────────────

    public static void init() {
        // Load current level from fuel.yml; use Cfg.fuelStart as default
        FileConfiguration saved = loadFuelFile();
        current = saved.getInt("current", Cfg.fuelStart);

        // Clamp to valid range from config
        if (current < 0) current = 0;
        if (current > Cfg.fuelMax) current = Cfg.fuelMax;
    }

    // ──────────────────────────────────────────────
    // fuel.yml helpers (stores only current level)
    // ──────────────────────────────────────────────

    private static File getFuelFile() {
        return new File(Fusion.getInstance().getDataFolder(), "fuel.yml");
    }

    private static FileConfiguration loadFuelFile() {
        File              file = getFuelFile();
        FileConfiguration cfg  = new YamlConfiguration();
        if (file.exists()) {
            try {
                cfg.load(file);
            } catch (Exception e) {
                Fusion.getInstance().getLogger().severe("Could not load fuel.yml: " + e.getMessage());
            }
        }
        return cfg;
    }

    public static void save() {
        File              file = getFuelFile();
        FileConfiguration cfg  = new YamlConfiguration();
        cfg.set("current", current);
        try {
            file.getParentFile().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            Fusion.getInstance().getLogger().severe("Could not save fuel.yml: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // API
    // ──────────────────────────────────────────────

    public static int getFuel() {
        return current;
    }

    public static int getMaxFuel() {
        return Cfg.fuelMax;
    }

    public static boolean hasFuel(int cost) {
        if (cost <= 0) return true;
        return current >= cost;
    }

    /**
     * Consumes {@code cost} fuel. Returns true if successful, false if not enough.
     */
    public static boolean consumeFuel(int cost) {
        if (cost <= 0) return true;
        if (current < cost) return false;
        current -= cost;
        save();
        return true;
    }

    /**
     * Adds up to {@code amount} fuel (capped at max). Returns the actual amount added.
     */
    public static int addFuel(int amount) {
        if (amount <= 0) return 0;
        int space = Cfg.fuelMax - current;
        int added = Math.min(amount, space);
        current += added;
        save();
        return added;
    }

    /**
     * Sets fuel directly, clamped to [0, max].
     */
    public static void setFuel(int amount) {
        current = Math.max(0, Math.min(amount, Cfg.fuelMax));
        save();
    }

    /**
     * Attempts to add fuel using one of the configured fuel items from the player's inventory.
     * Takes one matching item, adds its fuel amount, gives return item if configured.
     * Returns the FuelItem used, or null if the player had none of the given item.
     */
    public static FuelItem addFuelFromInventory(Player player, FuelItem fuelItem) {
        // Check inventory for a matching item
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (fuelItem.matches(stack)) {
                // Take 1 of the item
                if (stack.getAmount() == 1) {
                    player.getInventory().setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - 1);
                }

                // Add fuel
                addFuel(fuelItem.getFuelAmount());

                // Give return item
                if (fuelItem.getReturnMaterial() != null) {
                    player.getInventory().addItem(new ItemStack(fuelItem.getReturnMaterial(), 1))
                            .values()
                            .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
                }

                return fuelItem;
            }
        }
        return null; // player didn't have the item
    }
}
