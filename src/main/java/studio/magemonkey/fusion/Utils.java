package studio.magemonkey.fusion;

import org.bukkit.permissions.Permissible;

public final class Utils {
    private Utils() {
    }

    public static boolean hasCraftingPermission(Permissible permissible, String item) {
        return permissible.hasPermission("fusion.recipe." + item) || permissible.hasPermission("fusion.recipes") ||
                permissible.hasPermission("fusion.recipe.*");
    }

    public static boolean hasCraftingUsePermission(Permissible permissible, String item) {
        if (item == null) {
            return permissible.hasPermission("fusion.use") || permissible.hasPermission("fusion.use.*");
        }
        return permissible.hasPermission("fusion.use." + item) || permissible.hasPermission("fusion.use.*");
    }
}
