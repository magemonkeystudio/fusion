package studio.magemonkey.fusion.cfg;

import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.InventoryPattern;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class BrowseConfig {

    private static FileConfiguration config;
    private static File              file;

    private static final LinkedList<String>       professions = new LinkedList<>();
    private static final HashMap<String, Integer> profCosts   = new HashMap<>();

    private static String           browseName = ChatColor.DARK_AQUA + "Browse";
    private static ItemStack        browseFill;
    private static InventoryPattern browsePattern;

    private BrowseConfig() {
        load();
    }

    public static void load() {
        try {
            if (file == null || !file.exists()) {
                config = new YamlConfiguration();
                file = new File(Fusion.getInstance().getDataFolder(), "browse.yml");
                file.getParentFile().mkdirs();
                setDefaults();
                if (!file.exists()) {
                    file.createNewFile();
                    config.options().copyDefaults(true);
                    attemptPort();
                    config.save(file);
                    Fusion.getInstance().log.info("Created default browse.yml");
                }
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            config = yaml;

            setDefaults();
            readData();
            Fusion.getInstance().log.info("Successfully loaded browse.yml data");
        } catch (IOException e) {
            Fusion.getInstance().log.severe("Could not load browse.yml data");
            e.printStackTrace();
        }
    }

    public static LinkedList<String> getProfessions() {
        return professions;
    }

    private static void attemptPort() {
        FileConfiguration conf = Cfg.getConfig();
        boolean changed =
                conf.contains("browse.name") || conf.contains("browse.fillItem") || conf.contains("browse.pattern");

        if (conf.contains("browse.name"))
            config.set("name", conf.getString("browse.name"));

        if (conf.contains("browse.fillItem"))
            config.set("fillItem", conf.get("browse.fillItem"));

        if (conf.contains("browse.pattern"))
            config.set("pattern", conf.get("browse.pattern"));

        if (changed) {
            conf.set("browse", null);
            try {
                conf.save(new File(Fusion.getInstance().getDataFolder(), "config.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Fusion.getInstance().log.info("Successfully ported old browse info to new browse.yml");
        }
    }

    private static void setDefaults() {
        //Browse stuff -- Added in v1.01
        config.addDefault("name", "&3&lBrowse");
        config.addDefault("fillItem", ItemBuilder.newItem(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());

        HashMap<Character, ItemStack> browseItems = new HashMap<>();
        browseItems.put('0',
                ItemBuilder.newItem(Material.BIRCH_SIGN)
                        .name("&3Crafting Groups")
                        .insertLoreLine(0, ChatColor.GRAY + "Select a group to get started!")
                        .build());
        browseItems.put('1', ItemBuilder.newItem(Material.CYAN_STAINED_GLASS_PANE).name(" ").build());

        InventoryPattern browsePattern =
                new InventoryPattern(new java.lang.String[]{"111101111", "ooooooooo", "ooooooooo"}, browseItems);
        config.addDefault("pattern", browsePattern.serialize());
        config.addDefault("professions.smelting.cost", 10);
    }

    private static void readData() {
        professions.clear();
        profCosts.clear();
        browseName = config.getString("name");
        browseFill = config.getItemStack("fillItem");
        browsePattern = new InventoryPattern(config.getConfigurationSection("pattern").getValues(false));

        for (String prof : config.getConfigurationSection("professions").getValues(false).keySet()) {
            professions.add(prof.toLowerCase());

            if (config.contains("professions." + prof + ".cost"))
                profCosts.put(prof.toLowerCase(), config.getInt("professions." + prof + ".cost"));

            Fusion.getInstance().log.info("Loaded info for profession '" + prof + "'");
        }
    }

    public static String getBrowseName() {
        return browseName;
    }

    public static ItemStack getBrowseFill() {
        return browseFill;
    }

    public static InventoryPattern getBrowsePattern() {
        return browsePattern;
    }

    public static int getProfCost(String profession) {
        return profCosts.containsKey(profession.toLowerCase()) ? profCosts.get(profession.toLowerCase()) : 0;
    }

    public void saveConfig() {
//        config.set("guis", mastery);
//        config.set("autoCraft", autoCraft);
//        config.set("professions", professions);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
