package studio.magemonkey.fusion.util;

import org.bukkit.permissions.Permissible;

public final class Utils {

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

    public static String getFormattedTime(int seconds) {
        int hours   = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs    = seconds % 60;

        // format it properly so when hours are 0 it just show minutes and seconds. If minutes are 0 it just shows seconds
        if (hours == 0) {
            if (minutes == 0) {
                return secs > 0 ? secs + "s" : "Finished";
            }
            return minutes + "m " + secs + "s";
        }
        return hours + "h " + minutes + "m " + secs + "s";
    }
}
