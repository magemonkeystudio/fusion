package studio.magemonkey.fusion.cfg;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import studio.magemonkey.fusion.CraftingTable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.gui.CustomGUI;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ProfessionsCfg {

    @Getter
    private static final Map<String, CraftingTable> map = new HashMap<>(4);
    @Getter
    private static final Map<String, CustomGUI> guiMap = new HashMap<>(4);

    public static void init() {
        map.clear();
        guiMap.clear();
        File professionFolder = new File(Fusion.getInstance().getDataFolder(), "professions");
        if (!professionFolder.exists()) {
            professionFolder.mkdirs();
        }
        if(professionFolder.listFiles() == null) {
            Fusion.getInstance().getLogger().warning("There are no professions registered to load");
            return;
        }
        for (File file : professionFolder.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                FileConfiguration cfg = new YamlConfiguration();
                try {
                    cfg.load(file);
                    Map<String, Object> _map = cfg.getValues(false);
                    CraftingTable ct = new CraftingTable(_map);
                    map.put(ct.getName(), ct);


                } catch (Exception e) {
                    e.printStackTrace();
                    Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
                }
            }
        }

        for (Map.Entry<String, CraftingTable> entry : map.entrySet()) {
            String key = entry.getKey();
            CraftingTable value = entry.getValue();
            guiMap.put(key, new CustomGUI(key, value.getInventoryName(), value.getPattern()));
        }
    }

    public static CraftingTable getTable(String str) {
        return map.get(str.toLowerCase().trim());
    }

    public static CustomGUI getGUI(String str) {
        if (str == null) return null;
        return guiMap.get(str.toLowerCase().trim());
    }

    public static boolean migration(String key, Map<String, Object> map) {
        try {
            File professionFolder = new File(Fusion.getInstance().getDataFolder(), "professions");
            if (!professionFolder.exists()) {
                professionFolder.mkdirs();
            }

            File file = new File(professionFolder, key + ".yml");
            if(file.exists()) {
                Fusion.getInstance().getLogger().warning("Profession '" + key + "' was already migrated.");
                return true;
            }
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("name", key);
            cfg.set("icon", map.get("icon"));
            cfg.set("inventoryName", map.get("inventoryName"));
            cfg.set("useCategories", map.get("useCategories"));
            cfg.set("pattern", map.get("pattern"));
            cfg.set("masteryUnlock", map.get("masteryUnlock"));
            cfg.set("masteryFee", map.get("masteryFee"));
            cfg.set("fillItem", map.get("fillItem"));
            cfg.set("categories", map.get("categories"));
            cfg.set("recipes", map.get("recipes"));
            cfg.save(file);
            Fusion.getInstance().getLogger().warning("Profession '" + key + "' migrated successfully.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
            return false;
        }
    }
}
