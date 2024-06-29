package studio.magemonkey.fusion.cfg;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import studio.magemonkey.fusion.Fusion;

import java.io.File;
import java.io.IOException;
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
                Fusion.getInstance().getLogger().warning("Can't create config file: " + file + ":" + e.getMessage());
            }
            cfg.options().copyDefaults(true);
            try {
                cfg.save(file);
            } catch (IOException e) {
                Fusion.getInstance().getLogger().warning("Can't save config file: " + file + ":" + e.getMessage());
            }
        } else {
            try {
                reload(cfg, file);
            } catch (Exception e) {
                Fusion.getInstance().getLogger().warning("Can't load config file: " + file + ":" + e.getMessage());
                return null;
            }
        }

        return cfg;
    }

    private static void addDefs(FileConfiguration cfg) {
        // addDefault did not add it to config, so here is a work around
        if(!cfg.isSet("recursive_level_formula")) cfg.set("recursive_level_formula", recursive);
        if(!cfg.isSet("final_level_mod")) cfg.set("final_level_mod", dataSaveInterval);
        if(!cfg.isSet("data_save_interval")) cfg.set("data_save_interval", dataSaveInterval);
        if(!cfg.isSet("forget.penalty")) cfg.set("forget.penalty", forgetPenalty);
        if(!cfg.isSet("crafting_queue")) cfg.set("crafting_queue", craftingQueue);
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
                Fusion.getInstance().getLogger().warning("Can't create config file: " + file + ":" + e.getMessage());
            }
            cfg.options().copyDefaults(true);
            try {
                cfg.save(file);
            } catch (IOException e) {
                Fusion.getInstance().getLogger().warning("Can't save config file: " + file + ":" + e.getMessage());
            }
        } else {
            try {
                reload(cfg, file);
            } catch (Exception e) {
                Fusion.getInstance().getLogger().warning("Can't load config file: " + file + ":" + e.getMessage());
                return;
            }
        }

        recursive = cfg.getString("recursive_level_formula");
        finalMod = cfg.getString("final_level_mod");
        dataSaveInterval = cfg.getLong("data_save_interval");
        forgetPenalty = cfg.getDouble("forget.penalty");
        craftingQueue = cfg.getBoolean("crafting_queue");

        migrateOldTypes(cfg);
    }

    private static void reload(FileConfiguration cfg, File file) {
        try {
            // Update the config from file
            cfg.load(file);
            // Add and save the defaults
            addDefs(cfg);
            cfg.save(file);
            // Load the config again
            cfg.load(file);
        } catch (Exception e) {
            Fusion.getInstance().getLogger().warning("Can't load config file: " + file + ":" + e.getMessage());
        }
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
