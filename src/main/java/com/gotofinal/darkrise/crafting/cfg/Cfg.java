package com.gotofinal.darkrise.crafting.cfg;

import com.gotofinal.darkrise.crafting.*;
import com.gotofinal.darkrise.crafting.gui.CustomGUI;
import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import me.travja.darkrise.core.legacy.util.item.ItemBuilder;
import me.travja.darkrise.core.legacy.util.item.ItemColors;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public final class Cfg {
    private static final Map<String, CraftingTable> map = new HashMap<>(4);
    private static final Map<String, CustomGUI> guiMap = new HashMap<>(4);
    public static String recursive = "floor(n+300^(n/7)^2)";
    public static String finalMod = "floor(x)/4";

    private static String browseName = ChatColor.DARK_AQUA + "Browse";
    private static ItemStack browseFill;
    private static InventoryPattern browsePattern;

    private Cfg() {
    }

    public static CraftingTable getTable(String str) {
        return map.get(str.toLowerCase().trim());
    }

    public static CustomGUI getGUI(String str) {
        return guiMap.get(str.toLowerCase().trim());
    }

    public static Map<String, CraftingTable> getMap() {
        return map;
    }

    public static Map<String, CustomGUI> getGuiMap() {
        return guiMap;
    }

    public static String getBrowseName() {
        return browseName;
    }

    public static ItemStack getBrowseFill() {
        return browseFill;
    }

    public static InventoryPattern getBrowsePattern() {
        if (browsePattern == null) {

            File file = new File(DarkRiseCrafting.getInstance().getDataFolder(), "config.yml");
            FileConfiguration cfg = new YamlConfiguration();
            try {
                cfg.load(file);
            } catch (Exception e) {
                DarkRiseCrafting.getInstance().getLogger().warning("Can't load config file: " + file);
                e.printStackTrace();
                return null;
            }

            browsePattern = cfg.getSerializable("browse.pattern", InventoryPattern.class);
        }

        return browsePattern;
    }

    private static void addDefs(FileConfiguration cfg) {
        cfg.addDefault("recursive_level_formula", recursive);
        cfg.addDefault("final_level_mod", finalMod);
        cfg.addDefault("browse.name", "&3&lBrowse");
        cfg.addDefault("browse.fillItem", ItemBuilder.newItem(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());

        //Browse stuff -- Added in v1.01
        HashMap<Character, ItemStack> browseItems = new HashMap<>();
        browseItems.put('0', ItemBuilder.newItem(Material.BIRCH_SIGN).name(ChatColor.DARK_AQUA + "Crafting Groups").insertLoreLine(0, ChatColor.GRAY + "Select a group to get started!").build());
        browseItems.put('1', ItemBuilder.newItem(Material.CYAN_STAINED_GLASS_PANE).name(" ").build());

        InventoryPattern browsePattern = new InventoryPattern(new String[]{"111101111", "ooooooooo", "ooooooooo"}, browseItems);
        cfg.addDefault("browse.pattern", browsePattern.serialize());


        HashMap<Character, ItemStack> items = new HashMap<>();
        items.put('0', ItemBuilder.newItem(Material.STONE).durability(ItemColors.BLACK).build());
        items.put('>', ItemBuilder.newItem(Material.BOOK).name("Next page").build());
        items.put('<', ItemBuilder.newItem(Material.BOOK).name("Prev page").build());
        InventoryPattern ip = new InventoryPattern(new String[]{"=========", "=========", "=========", "=========", "=========", "<0000000>"}, items);
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE/*, 1, (short) 15*/);
        CraftingTable a = new CraftingTable("forge", "Forge inventory name", DarkRiseEconomy.getItemsRegistry().getItems().iterator().next(), ip, item/*new ItemStack(Material.BLACK_STAINED_GLASS_PANE)*/, 0, 0);

        a.addRecipe(new Recipe("test", Arrays.asList(new RecipeEconomyItem("testItem", 5), new RecipeCustomItem(new ItemStack(Material.COOKIE), 2, true)),
                new RecipeEconomyItem("resultItem", 4), 0, 0, 0));
        CraftingTable b = new CraftingTable("craft", "Craft inventory name", DarkRiseEconomy.getItemsRegistry().getItems().iterator().next(), ip, item/*new ItemStack(Material.BLACK_STAINED_GLASS_PANE)*/, 0, 0);
        List<Map<String, Object>> list = new ArrayList<>(3);
        list.add(a.serialize());
        list.add(b.serialize());
        cfg.addDefault("types", list);
    }

    public static void init() {
        map.clear();
        guiMap.clear();
        File file = new File(DarkRiseCrafting.getInstance().getDataFolder(), "config.yml");
        FileConfiguration cfg;
        if (!file.exists()) {
            cfg = new YamlConfiguration();
            addDefs(cfg);
            file.getAbsoluteFile().getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                DarkRiseCrafting.getInstance().getLogger().warning("Can't create config file: " + file);
                e.printStackTrace();
            }
            cfg.options().copyDefaults(true);
            try {
                cfg.save(file);
            } catch (IOException e) {
                DarkRiseCrafting.getInstance().getLogger().warning("Can't save config file: " + file);
                e.printStackTrace();
            }
        } else {
            cfg = new YamlConfiguration();
            try {
                cfg.load(file);
            } catch (Exception e) {
                DarkRiseCrafting.getInstance().getLogger().warning("Can't load config file: " + file);
                e.printStackTrace();
                return;
            }
            addDefs(cfg);
        }

        recursive = cfg.getString("recursive_level_formula");
        finalMod = cfg.getString("final_level_mod");


        browseName = cfg.getString("browse.name");
        browseFill = cfg.getItemStack("browse.fillItem");
        browsePattern = new InventoryPattern(cfg.getConfigurationSection("browse.pattern").getValues(false));


        List<Map<?, ?>> typesSection = cfg.getMapList("types");
        for (Map<?, ?> typeData : typesSection) {
            //noinspection unchecked
            CraftingTable ct = new CraftingTable((Map<String, Object>) typeData);
            map.put(ct.getName(), ct);
        }
        cfg.options().copyDefaults(true);
        try {
            cfg.save(file);
        } catch (IOException e) {
            DarkRiseCrafting.getInstance().getLogger().warning("Can't save config file: " + file);
            e.printStackTrace();
        }

        for (Entry<String, CraftingTable> entry : map.entrySet()) {
            String key = entry.getKey();
            CraftingTable value = entry.getValue();
            guiMap.put(key, new CustomGUI(key, value.getInventoryName(), value.getPattern()));
        }
    }
}
