package studio.magemonkey.fusion.util;

import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static void moveEntry(LinkedHashMap<String, Object> map, String key, int offset) {
        // Ensure the offset is either -1 (left) or 1 (right)
        if (offset != -1 && offset != 1) {
            throw new IllegalArgumentException("Offset must be -1 or 1");
        }

        List<Map.Entry<String, Object>> entries = new ArrayList<>(map.entrySet());
        int index = -1;

        // Find the index of the current entry
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(key)) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return; // Key not found, do nothing
        }

        // Calculate the new index
        int newIndex = index + offset;

        // Check if the new index is within bounds
        if (newIndex < 0 || newIndex >= entries.size()) {
            return; // New index out of bounds, do nothing
        }

        // Remove and reinsert the entry at the new position
        Map.Entry<String, Object> entry = entries.remove(index);
        entries.add(newIndex, entry);

        // Clear the original map and reinsert the entries in the new order
        map.clear();
        for (Map.Entry<String, Object> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
    }
}
