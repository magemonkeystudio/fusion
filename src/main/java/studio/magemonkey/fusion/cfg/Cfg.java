package studio.magemonkey.fusion.cfg;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.legacy.item.ItemColors;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.InventoryPattern;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Cfg {
    public static String recursive = "floor(n+300^(n/7)^2)";
    public static String finalMod = "floor(x)/4";
    public static long dataSaveInterval = 12000;
    public static double forgetPenalty = 0.2d;
    public static boolean craftingQueue = true;

    static FileConfiguration getConfig() {
        File file = new File(Fusion.getInstance().getDataFolder(), "config.yml");
        FileConfiguration cfg = new YamlConfiguration();

        if (!file.exists()) {
            addDefs(cfg);
            file.getAbsoluteFile().getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Fusion.getInstance().getLogger().warning("Can't create config file: " + file);
                e.printStackTrace();
            }
            cfg.options().copyDefaults(true);
            try {
                cfg.save(file);
            } catch (IOException e) {
                Fusion.getInstance().getLogger().warning("Can't save config file: " + file);
                e.printStackTrace();
            }
        } else {
            try {
                cfg.load(file);
            } catch (Exception e) {
                Fusion.getInstance().getLogger().warning("Can't load config file: " + file);
                e.printStackTrace();
                return null;
            }
            addDefs(cfg);
        }

        return cfg;
    }

    private static void addDefs(FileConfiguration cfg) {
        cfg.addDefault("recursive_level_formula", recursive);
        cfg.addDefault("final_level_mod", finalMod);
        cfg.addDefault("data_save_interval", dataSaveInterval); //Auto save every 10 minutes
        cfg.addDefault("forget.penalty", forgetPenalty);
        cfg.addDefault("crafting_queue", craftingQueue);
    }

    public static void init() {
        File file = new File(Fusion.getInstance().getDataFolder(), "config.yml");
        FileConfiguration cfg = new YamlConfiguration();

        if (!file.exists()) {
            addDefs(cfg);
            file.getAbsoluteFile().getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Fusion.getInstance().getLogger().warning("Can't create config file: " + file);
                e.printStackTrace();
            }
            cfg.options().copyDefaults(true);
            try {
                cfg.save(file);
            } catch (IOException e) {
                Fusion.getInstance().getLogger().warning("Can't save config file: " + file);
                e.printStackTrace();
            }
        } else {
            try {
                cfg.load(file);
            } catch (Exception e) {
                Fusion.getInstance().getLogger().warning("Can't load config file: " + file + ":" + e.getMessage());
                return;
            }
            addDefs(cfg);
        }

        recursive = cfg.getString("recursive_level_formula");
        finalMod = cfg.getString("final_level_mod");
        dataSaveInterval = cfg.getLong("data_save_interval");
        forgetPenalty = cfg.getDouble("forget.penalty");
        craftingQueue = cfg.getBoolean("crafting_queue");

        migrateOldTypes(cfg);
    }

    public static void migrateOldTypes(FileConfiguration cfg) {
        List<Map<?, ?>> typesSection = cfg.getMapList("types");
        if (typesSection.isEmpty()) return;
        Fusion.getInstance().getLogger().warning("Found old types section in config.yml. Migrating...");
        typesSection.removeIf(typeData -> ProfessionsCfg.loadFrom((String) typeData.get("name"), (Map<String, Object>) typeData));
        cfg.set("types", typesSection);
        if (cfg.getMapList("types").isEmpty()) {
            cfg.set("types", null);
        }
        try {
            cfg.save(new File(Fusion.getInstance().getDataFolder(), "config.yml"));
        } catch (IOException e) {
            Fusion.getInstance().getLogger().warning("Can't save config file: " + e.getMessage());
        }
    }
}
