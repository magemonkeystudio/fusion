package studio.magemonkey.fusion.cfg.hooks;

import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;
import studio.magemonkey.fusion.Fusion;

import java.util.HashMap;
import java.util.Map;

public class DivinityService {

    public static DivinityCfg divinityCfg;
    public static Map<String, ItemGenEntry> ItemGenResults = new HashMap<>();

    public static boolean isCached(String namespace) {
        return ItemGenResults.containsKey(namespace);
    }

    public static boolean cache(String namespace) {
        ItemGeneratorManager.GeneratorItem item = Divinity.getInstance().getModuleCache().getTierManager().getItemById(namespace);
        if (item == null) {
            Fusion.getInstance().getLogger().warning("Failed to cache item " + namespace + " from Divinity as it does not exist.");
            return false;
        }
        ItemGenResults.put(namespace, new ItemGenEntry(item));
        return true;
    }

    public static void init() {
        ItemGenResults.clear();
        divinityCfg = new DivinityCfg();
        Fusion.getInstance().getLogger().info("Divinity Service loaded.");
    }
}
