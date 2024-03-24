package com.promcteam.fusion.cfg;

import com.promcteam.fusion.CraftingTable;
import com.promcteam.fusion.InventoryPattern;
import com.promcteam.fusion.Fusion;
import com.promcteam.fusion.gui.CustomGUI;
import com.promcteam.codex.items.exception.ProItemException;
import com.promcteam.codex.items.providers.VanillaProvider;
import com.promcteam.risecore.legacy.util.item.ItemBuilder;
import com.promcteam.risecore.legacy.util.item.ItemColors;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class Cfg {
    private static final Map<String, CraftingTable> map              = new HashMap<>(4);
    private static final Map<String, CustomGUI>     guiMap           = new HashMap<>(4);
    public static        String                     recursive        = "floor(n+300^(n/7)^2)";
    public static        String                     finalMod         = "floor(x)/4";
    public static        long                       dataSaveInterval = 12000;
    public static        double                     forgetPenalty    = 0.2d;

    private Cfg() {
    }

    public static CraftingTable getTable(String str) {
        return map.get(str.toLowerCase().trim());
    }

    public static CustomGUI getGUI(String str) {
        if (str == null) return null;
        return guiMap.get(str.toLowerCase().trim());
    }

    public static Map<String, CraftingTable> getMap() {
        return map;
    }

    public static Map<String, CustomGUI> getGuiMap() {
        return guiMap;
    }

    static FileConfiguration getConfig() {
        File              file = new File(Fusion.getInstance().getDataFolder(), "config.yml");
        FileConfiguration cfg  = new YamlConfiguration();

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

        HashMap<Character, ItemStack> items = new HashMap<>();
        items.put('0', ItemBuilder.newItem(Material.STONE).durability(ItemColors.BLACK).build());
        items.put('>', ItemBuilder.newItem(Material.BOOK).name("Next page").build());
        items.put('<', ItemBuilder.newItem(Material.BOOK).name("Prev page").build());
        InventoryPattern ip   =
                new InventoryPattern(new String[]{"=========", "=========", "=========", "=========", "=========", "<0000000>"},
                        items);
        ItemStack        item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE/*, 1, (short) 15*/);
//        CraftingTable a = new CraftingTable("forge", "Forge inventory name", DarkRiseEconomy.getItemsRegistry().getItems().iterator().next(), ip, item/*new ItemStack(Material.BLACK_STAINED_GLASS_PANE)*/, 0, 0);
//
//        a.addRecipe(new Recipe("test",
//                Arrays.asList(new RecipeEconomyItem("testItem", 5),
//                        new RecipeCustomItem(new ItemStack(Material.COOKIE), 2, true)),
//                new RecipeEconomyItem("resultItem", 4), 0, 0, 0));
        CraftingTable             b    = new CraftingTable("craft",
                "Craft inventory name",
                new VanillaProvider.VanillaItemType(Material.PAPER),
                ip,
                item/*new ItemStack(Material.BLACK_STAINED_GLASS_PANE)*/,
                0,
                0);
        List<Map<String, Object>> list = new ArrayList<>(3);
//        list.add(a.serialize());
        list.add(b.serialize());
        cfg.addDefault("types", list);
    }

    public static void init() {
        map.clear();
        guiMap.clear();
        File              file = new File(Fusion.getInstance().getDataFolder(), "config.yml");
        FileConfiguration cfg  = new YamlConfiguration();

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
                return;
            }
            addDefs(cfg);
        }

        recursive = cfg.getString("recursive_level_formula");
        finalMod = cfg.getString("final_level_mod");
        dataSaveInterval = cfg.getLong("data_save_interval");
        forgetPenalty = cfg.getDouble("forget.penalty");


        List<Map<?, ?>> typesSection = cfg.getMapList("types");
        for (Map<?, ?> typeData : typesSection) {
            try {
                //noinspection unchecked
                CraftingTable ct = new CraftingTable((Map<String, Object>) typeData);
                map.put(ct.getName(), ct);
            } catch (ProItemException e) {
                Fusion.getInstance().getLogger().warning("Can't load crafting table: " + e.getMessage());
            }
        }
        cfg.options().copyDefaults(true);
        try {
            cfg.save(file);
        } catch (IOException e) {
            Fusion.getInstance().getLogger().warning("Can't save config file: " + file);
            e.printStackTrace();
        }

        for (Entry<String, CraftingTable> entry : map.entrySet()) {
            String        key   = entry.getKey();
            CraftingTable value = entry.getValue();
            guiMap.put(key, new CustomGUI(key, value.getInventoryName(), value.getPattern()));
        }
    }
}
