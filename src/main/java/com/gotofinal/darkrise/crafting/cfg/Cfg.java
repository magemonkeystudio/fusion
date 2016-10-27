package com.gotofinal.darkrise.crafting.cfg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gotofinal.darkrise.crafting.CraftingTable;
import com.gotofinal.darkrise.crafting.DarkRiseCrafting;
import com.gotofinal.darkrise.crafting.InventoryPattern;
import com.gotofinal.darkrise.crafting.Recipe;
import com.gotofinal.darkrise.crafting.RecipeCustomItem;
import com.gotofinal.darkrise.crafting.RecipeEconomyItem;
import com.gotofinal.darkrise.crafting.gui.CustomGUI;
import com.gotofinal.darkrise.spigot.core.utils.item.ItemBuilder;
import com.gotofinal.darkrise.spigot.core.utils.item.ItemColors;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import org.diorite.utils.collections.maps.CaseInsensitiveMap;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

public final class Cfg
{
    private static final Map<String, CraftingTable> map    = new CaseInsensitiveMap<>(4);
    private static final Map<String, CustomGUI>     guiMap = new CaseInsensitiveMap<>(4);

    private Cfg()
    {
    }

    public static CraftingTable getTable(String str)
    {
        return map.get(str);
    }

    public static CustomGUI getGUI(String str)
    {
        return guiMap.get(str);
    }

    public static Map<String, CraftingTable> getMap()
    {
        return map;
    }

    public static Map<String, CustomGUI> getGuiMap()
    {
        return guiMap;
    }

    private static void addDefs(FileConfiguration cfg)
    {
        Char2ObjectMap<ItemStack> items = new Char2ObjectOpenHashMap<>();
        items.put('0', ItemBuilder.newItem(Material.STAINED_GLASS_PANE).durability(ItemColors.BLACK).build());
        items.put('>', ItemBuilder.newItem(Material.BOOK).name("Next page").build());
        items.put('<', ItemBuilder.newItem(Material.BOOK).name("Prev page").build());
        InventoryPattern ip = new InventoryPattern(new String[]{"=========", "=========", "=========", "=========", "=========", "<0000000>"}, items);
        CraftingTable a = new CraftingTable("forge", "Forge inventory name", ip);

        a.addRecipe(new Recipe("test", Arrays.asList(new RecipeEconomyItem("testItem", 5), new RecipeCustomItem(new ItemStack(Material.COOKIE), 2, true)),
                               new RecipeEconomyItem("resultItem", 4), 0, 0, 0));
        CraftingTable b = new CraftingTable("craft", "Craft inventory name", ip);
        List<Map<String, Object>> list = new ArrayList<>(3);
        list.add(a.serialize());
        list.add(b.serialize());
        cfg.addDefault("types", list);
    }

    public static void init()
    {
        map.clear();
        guiMap.clear();
        File file = new File(DarkRiseCrafting.getInstance().getDataFolder(), "config.yml");
        FileConfiguration cfg;
        if (! file.exists())
        {
            cfg = new YamlConfiguration();
            addDefs(cfg);
            file.getAbsoluteFile().getParentFile().mkdirs();
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                DarkRiseCrafting.getInstance().getLogger().warning("Can't create config file: " + file);
                e.printStackTrace();
            }
            cfg.options().copyDefaults(true);
            try
            {
                cfg.save(file);
            }
            catch (IOException e)
            {
                DarkRiseCrafting.getInstance().getLogger().warning("Can't save config file: " + file);
                e.printStackTrace();
            }
        }
        else
        {
            cfg = new YamlConfiguration();
            try
            {
                cfg.load(file);
            }
            catch (Exception e)
            {
                DarkRiseCrafting.getInstance().getLogger().warning("Can't load config file: " + file);
                e.printStackTrace();
                return;
            }
            addDefs(cfg);
        }

        List<Map<?, ?>> typesSection = cfg.getMapList("types");
        for (Map<?, ?> typeData : typesSection)
        {
            CraftingTable ct = new CraftingTable((Map<String, Object>) typeData);
            map.put(ct.getName(), ct);
        }
        cfg.options().copyDefaults(true);
        try
        {
            cfg.save(file);
        }
        catch (IOException e)
        {
            DarkRiseCrafting.getInstance().getLogger().warning("Can't save config file: " + file);
            e.printStackTrace();
        }

        for (Entry<String, CraftingTable> entry : map.entrySet())
        {
            String key = entry.getKey();
            CraftingTable value = entry.getValue();
            guiMap.put(key, new CustomGUI(key, value.getInventoryName(), value.getPattern()));
        }
    }
}
