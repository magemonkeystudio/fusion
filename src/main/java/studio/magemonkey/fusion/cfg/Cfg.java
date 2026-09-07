package studio.magemonkey.fusion.cfg;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.cfg.sql.DatabaseType;
import studio.magemonkey.fusion.commands.CommandMechanics;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.gui.BrowseGUI;

import java.io.File;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Cfg {
    public static String  recursive                = "floor(n+300^(n/7)^2)";
    public static String  finalMod                 = "floor(x)/4";
    public static long    dataSaveInterval         = 12000;
    public static double  forgetPenalty            = 0.2d;
    public static boolean craftingQueue            = true;
    public static int     finishedMessageInterval  = 300;
    public static boolean updateQueueOffline       = true;
    public static boolean showRequirementsOnBrowse = true;

    public static boolean hideRecipesNoPermission = false;
    public static boolean hideRecipesLimitReached = false;

    public static boolean useCustomFormula = true;

    public static String finishMessage = "&aYou have crafting items ready for pickup! ($<amount>)";

    public static List<NamespacedKey> disabledVanillaRecipes = new ArrayList<>();
    public static List<String> autoJoinProfessions = new ArrayList<>();

    // Fuel system
    public static int            fuelStart = 0;
    public static int            fuelMax   = 1000;
    public static List<FuelItem> fuelItems = new ArrayList<>();

    // No usage inside of Cfg, just used for default values. The actual values are stored in SQLManager.class
    private static final DatabaseType storageType     = DatabaseType.LOCAL;
    private static final String       storageHost     = "localhost";
    private static final int          storagePort     = 3306;
    private static final String       storageDatabase = "database";
    private static final String       storageUser     = "root";
    private static final String       storagePassword = "password";

    public static FileConfiguration getConfig() {
        File              file = new File(Fusion.getInstance().getDataFolder(), "config.yml");
        FileConfiguration cfg  = new YamlConfiguration();

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
        if (!cfg.isSet("recursive_level_formula")) cfg.set("recursive_level_formula", recursive);
        if (!cfg.isSet("final_level_mod")) cfg.set("final_level_mod", dataSaveInterval);
        if (!cfg.isSet("data_save_interval")) cfg.set("data_save_interval", dataSaveInterval);
        if (!cfg.isSet("forget.penalty")) cfg.set("forget.penalty", forgetPenalty);
        if (!cfg.isSet("crafting_queue")) cfg.set("crafting_queue", craftingQueue);
        if (!cfg.isSet("update_queue_offline")) cfg.set("update_queue_offline", updateQueueOffline);
        if (!cfg.isSet("finished_message")) cfg.set("finished_message", finishMessage);
        if (!cfg.isSet("finished_message_interval")) cfg.set("finished_message_interval", finishedMessageInterval);
        if (!cfg.isSet("showRequirementsOnBrowse")) cfg.set("showRequirementsOnBrowse", showRequirementsOnBrowse);
        if (!cfg.isSet("hideRecipesDefault.noPermission")) cfg.set("hideRecipesDefault.noPermission", hideRecipesNoPermission);
        if (!cfg.isSet("hideRecipesDefault.recipeLimitReached")) cfg.set("hideRecipesDefault.recipeLimitReached", hideRecipesLimitReached);

        if (!cfg.isSet("storage.type")) cfg.set("storage.type", storageType.name());
        if (!cfg.isSet("storage.host")) cfg.set("storage.host", storageHost);
        if (!cfg.isSet("storage.port")) cfg.set("storage.port", storagePort);
        if (!cfg.isSet("storage.database")) cfg.set("storage.database", storageDatabase);
        if (!cfg.isSet("storage.user")) cfg.set("storage.user", storageUser);
        if (!cfg.isSet("storage.password")) cfg.set("storage.password", storagePassword);

        if (!cfg.isSet("useCustomFormula")) cfg.set("useCustomFormula", useCustomFormula);
        if (!cfg.isSet("disabled_vanilla_recipes")) cfg.set("disabled_vanilla_recipes", disabledVanillaRecipes);
        if (!cfg.isSet("auto_join_professions")) cfg.set("auto_join_professions", autoJoinProfessions);

        // Fuel defaults
        if (!cfg.isSet("fuel.start")) cfg.set("fuel.start", 0);
        if (!cfg.isSet("fuel.max")) cfg.set("fuel.max", 1000);
        if (!cfg.isSet("fuel.items.coal.material")) cfg.set("fuel.items.coal.material", "COAL");
        if (!cfg.isSet("fuel.items.coal.amount")) cfg.set("fuel.items.coal.amount", 10);
        if (!cfg.isSet("fuel.items.coal.return")) cfg.set("fuel.items.coal.return", "null");
        if (!cfg.isSet("fuel.items.coal_block.material")) cfg.set("fuel.items.coal_block.material", "COAL_BLOCK");
        if (!cfg.isSet("fuel.items.coal_block.amount")) cfg.set("fuel.items.coal_block.amount", 90);
        if (!cfg.isSet("fuel.items.coal_block.return")) cfg.set("fuel.items.coal_block.return", "null");
        if (!cfg.isSet("fuel.items.blaze_rod.material")) cfg.set("fuel.items.blaze_rod.material", "BLAZE_ROD");
        if (!cfg.isSet("fuel.items.blaze_rod.amount")) cfg.set("fuel.items.blaze_rod.amount", 50);
        if (!cfg.isSet("fuel.items.blaze_rod.return")) cfg.set("fuel.items.blaze_rod.return", "BLAZE_POWDER");
    }

    public static void init() {
        FileConfiguration cfg = getConfig();
        if (cfg == null) {
            Fusion.getInstance().getLogger().warning("Can't load config file!");
            return;
        }

        recursive = cfg.getString("recursive_level_formula");
        finalMod = cfg.getString("final_level_mod");
        dataSaveInterval = cfg.getLong("data_save_interval");
        forgetPenalty = cfg.getDouble("forget.penalty");
        craftingQueue = cfg.getBoolean("crafting_queue");
        updateQueueOffline = cfg.getBoolean("update_queue_offline");
        finishedMessageInterval = cfg.getInt("finished_message_interval");
        finishMessage = cfg.getString("finished_message");
        showRequirementsOnBrowse = cfg.getBoolean("showRequirementsOnBrowse");
        hideRecipesNoPermission = cfg.getBoolean("hideRecipesDefault.noPermission");
        hideRecipesLimitReached = cfg.getBoolean("hideRecipesDefault.recipeLimitReached");

        useCustomFormula = cfg.getBoolean("useCustomFormula");
        List<Material> materials = cfg.getStringList("disabled_vanilla_recipes").stream().map(x -> Material.valueOf(x.toUpperCase())).toList();
        disabledVanillaRecipes = BukkitRecipeWrapper.getRecipeKeysForMaterials(materials);
        autoJoinProfessions = cfg.getStringList("auto_join_professions");

        // Load fuel config
        fuelStart = cfg.getInt("fuel.start", 0);
        fuelMax   = cfg.getInt("fuel.max", 1000);
        fuelItems = new ArrayList<>();
        if (cfg.isConfigurationSection("fuel.items")) {
            for (String key : cfg.getConfigurationSection("fuel.items").getKeys(false)) {
                String path = "fuel.items." + key;
                String matName = cfg.getString(path + ".material", "");
                int fuelAmount = cfg.getInt(path + ".amount", 0);
                String retName = cfg.getString(path + ".return", "null");

                Material mat;
                try {
                    mat = Material.valueOf(matName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Fusion.getInstance().getLogger().warning("Unknown material '" + matName + "' in fuel.items." + key);
                    continue;
                }
                Material ret = null;
                if (retName != null && !retName.equalsIgnoreCase("null") && !retName.isEmpty()) {
                    try {
                        ret = Material.valueOf(retName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        Fusion.getInstance().getLogger().warning("Unknown return material '" + retName + "' in fuel.items." + key);
                    }
                }
                fuelItems.add(new FuelItem(key, mat, fuelAmount, ret));
            }
        }

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

            BukkitRecipeWrapper.disableBukkitRecipes(disabledVanillaRecipes);
        } catch (Exception e) {
            Fusion.getInstance().getLogger().warning("Can't load config file: " + file + ":" + e.getMessage());
        }
    }

    public static void notifyForQueue(Player player, int amount) {
        player.sendMessage(finishMessage.replace("$<amount>", String.valueOf(amount)).replace("&", "§"));
    }

    public static void migrateOldTypes(FileConfiguration cfg) {
        List<Map<?, ?>> typesSection = cfg.getMapList("types");
        if (typesSection.isEmpty()) return;
        Fusion.getInstance().getLogger().warning("Found old types section in config.yml. Migrating...");
        typesSection.removeIf(typeData -> ProfessionsCfg.loadFrom((String) typeData.get("name"),
                (Map<String, Object>) typeData));
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

    public static boolean setDatabaseType(DatabaseType type) {
        FileConfiguration cfg = getConfig();
        if (cfg == null) {
            Fusion.getInstance().getLogger().warning("Can't load config file!");
            return false;
        }
        cfg.set("storage.type", type.name());
        try {
            cfg.save(new File(Fusion.getInstance().getDataFolder(), "config.yml"));
            return true;
        } catch (IOException e) {
            Fusion.getInstance().getLogger().warning("Can't save config file: " + e.getMessage());
            return false;
        }
    }

    public static void autoJoinProfessions(Player player) {
        FusionPlayer fp = PlayerLoader.getPlayer(player);
        for (String professionId : autoJoinProfessions) {
            if (fp.hasProfession(professionId) && fp.hasJoined(professionId)) continue;
            if (ProfessionsCfg.getTable(professionId) == null) continue;
            FusionAPI.getEventServices().getProfessionService().joinProfession(professionId, player, 0.0, 0);
        }
    }
}
