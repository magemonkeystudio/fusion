package studio.magemonkey.fusion.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import studio.magemonkey.divinity.api.DivinityAPI;

import java.util.*;

public class TabCacher {

    public static UUID GlobalUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final Map<UUID, TabCacher> PlayerTabs = new TreeMap<>();

    public final Map<String, List<String>> CachedTabs = new TreeMap<>();

    public static void cache(UUID uuid, String key, List<String> tabList) {
        PlayerTabs.putIfAbsent(uuid, new TabCacher());
        PlayerTabs.get(uuid).CachedTabs.put(key, tabList);
    }

    public static boolean isNotCached(UUID uuid, String key) {
        return !PlayerTabs.containsKey(uuid) || !PlayerTabs.get(uuid).CachedTabs.containsKey(key);
    }

    public static TabCacher get(UUID uuid) {
        return PlayerTabs.get(uuid);
    }

    public static void clearTabs(UUID uuid) {
        PlayerTabs.remove(uuid);
    }

    public static void clearTabs(UUID uuid, String key) {
        PlayerTabs.get(uuid).CachedTabs.remove(key);
    }

    public static List<String> getTabs(UUID uuid, String key, String arg) {
        List<String> entries = new ArrayList<>();
        if (isNotCached(uuid, key)) {
            for (Material material : Material.values()) {
                if (material.isAir()) continue;
                entries.add(material.toString().toLowerCase());
            }

            if (Bukkit.getPluginManager().isPluginEnabled("Divinity")) {
                DivinityAPI.getModuleManager().getCustomItemsManager().getItems().forEach((k) -> entries.add("DIVINITY_" + k.getId().toLowerCase()));
            }
            cache(uuid, key, entries);
        }

        List<String> cachedTabsCopy = new ArrayList<>(PlayerTabs.get(uuid).CachedTabs.get(key)); // Create a copy
        for (String entry : cachedTabsCopy) {
            if (entry.toLowerCase().startsWith(arg.toLowerCase())) {
                entries.add(entry);
            }
        }

        return entries;
    }

}
