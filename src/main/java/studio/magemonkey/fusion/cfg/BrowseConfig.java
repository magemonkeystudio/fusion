package studio.magemonkey.fusion.cfg;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.professions.ProfessionConditions;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@SuppressWarnings("all")
public class BrowseConfig implements ConfigurationSerializable {

    private static FileConfiguration config;
    private static File              file;

    @Getter
    @Setter
    private static final LinkedList<String>                    professions          = new LinkedList<>();
    @Getter
    @Setter
    private static final HashMap<String, ProfessionConditions> professionConditions = new HashMap<>();

    @Getter
    @Setter
    private static String           browseName = ChatColor.DARK_AQUA + "Browse";
    @Getter
    @Setter
    private static ItemStack        browseFill;
    @Getter
    @Setter
    private static InventoryPattern browsePattern;

    @Getter
    @Setter
    private static boolean loreConditionsEnabled = true;

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
            Fusion.getInstance().log.severe("Could not load browse.yml data: " + e.getMessage());
        }
    }

    private static void attemptPort() {
        FileConfiguration conf = Cfg.getConfig();
        boolean changed =
                conf.contains("browse.name") || conf.contains("browse.fillItem") || conf.contains("browse.pattern");

        if (conf.contains("browse.name"))
            config.set("name", conf.getString("browse.name"));

        if (conf.contains("browse.fillItem"))
            config.set("pattern.items.fillItem", conf.get("browse.fillItem"));

        if (conf.contains("browse.pattern"))
            config.set("pattern", conf.get("browse.pattern"));

        if (changed) {
            conf.set("browse", null);
            try {
                conf.save(new File(Fusion.getInstance().getDataFolder(), "config.yml"));
            } catch (IOException e) {
                Fusion.getInstance().log.severe("Could not load browse.yml data: " + e.getMessage());
                e.printStackTrace();
            }
            Fusion.getInstance().log.info("Successfully ported old browse info to new browse.yml");
        }
    }

    private static void setDefaults() {
        //Browse stuff -- Added in v1.01
        config.addDefault("name", "&3&lBrowse");
        config.addDefault("pattern.items.fillItem",
                ItemBuilder.newItem(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());

        HashMap<Character, ItemStack> browseItems = new HashMap<>();
        browseItems.put('0',
                ItemBuilder.newItem(Material.BIRCH_SIGN)
                        .name("&3Crafting Groups")
                        .insertLoreLine(0, ChatColor.GRAY + "Select a profession")
                        .build());
        browseItems.put('1', ItemBuilder.newItem(Material.CYAN_STAINED_GLASS_PANE).name(" ").build());

        InventoryPattern browsePattern =
                new InventoryPattern(new java.lang.String[]{"111101111", "ooooooooo", "ooooooooo"}, browseItems);
        config.addDefault("pattern", browsePattern.serialize());

        config.addDefault("professions.armor_smithing.costs", Map.of("exp", 10));
        config.addDefault("professions.armor_smithing.conditions", Map.of());
        config.addDefault("professions.weapon_smithing.costs", Map.of("money", 5));
        config.addDefault("professions.weapon_smithing.conditions", Map.of());
    }

    private static void readData() {
        professions.clear();
        professionConditions.clear();
        browseName = config.getString("name");
        browseFill = config.getItemStack("pattern.items.fillItem");
        browsePattern = new InventoryPattern(config.getConfigurationSection("pattern").getValues(false));

        for (String prof : config.getConfigurationSection("professions").getValues(false).keySet()) {
            professions.add(prof.toLowerCase());
            professionConditions.put(prof.toLowerCase(),
                    new ProfessionConditions(prof, config.getConfigurationSection("professions." + prof)));
            Fusion.getInstance().log.info("Loaded info for profession '" + prof + "'");
        }
    }

    public static ProfessionConditions getProfessionConditions(String profession) {
        return professionConditions.getOrDefault(profession.toLowerCase(), null);
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

    public static void save(BrowseEditor editor) {
        Map<String, Object> professionsMap = new LinkedHashMap<>();
        for (ProfessionConditions conditions : editor.getProfessionConditions().values()) {
            professionsMap.put(conditions.getProfession(), conditions.serialize());
        }
        config.set("name", editor.getName());
        config.set("pattern", editor.getBrowsePattern().serialize());
        config.set("professions", professionsMap);
        try {
            config.save(file);
            config = YamlConfiguration.loadConfiguration(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> professionsMap = new LinkedHashMap<>();
        for (ProfessionConditions conditions : professionConditions.values()) {
            professionsMap.put(conditions.getProfession(), conditions.serialize());
        }
        return SerializationBuilder.start(2)
                .append("name", browseName)
                .append("pattern", browsePattern.serialize())
                .append("professions", professionsMap)
                .build();
    }
}
