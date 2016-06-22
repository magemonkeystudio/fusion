package com.gotofinal.darkrise.cfg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gotofinal.darkrise.CraftingTable;
import com.gotofinal.darkrise.DarkRiseCrafting;
import com.gotofinal.darkrise.InventoryPattern;
import com.gotofinal.darkrise.Recipe;
import com.gotofinal.darkrise.RecipeItem;
import com.gotofinal.darkrise.core.utils.item.ItemBuilder;
import com.gotofinal.darkrise.core.utils.item.ItemColors;
import com.gotofinal.darkrise.gui.CustomGUI;

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

    public static CraftingTable getTable(final String str)
    {
        return map.get(str);
    }

    public static CustomGUI getGUI(final String str)
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

    private static void addDefs(final FileConfiguration cfg)
    {
        final Char2ObjectMap<ItemStack> items = new Char2ObjectOpenHashMap<>();
        items.put('0', ItemBuilder.newItem(Material.STAINED_GLASS_PANE).durability(ItemColors.BLACK).build());
        items.put('>', ItemBuilder.newItem(Material.BOOK).name("Next page").build());
        items.put('<', ItemBuilder.newItem(Material.BOOK).name("Prev page").build());
        final InventoryPattern ip = new InventoryPattern(new String[]{"=========", "=========", "=========", "=========", "=========", "<0000000>"}, items);
        final CraftingTable a = new CraftingTable("forge", "Forge inventory name", ip);

        a.addRecipe(new Recipe("test", Collections.singletonList(new RecipeItem("testItem", 5)), new RecipeItem("resultItem", 4), 0));
        final CraftingTable b = new CraftingTable("craft", "Craft inventory name", ip);
        final List<Map<String, Object>> list = new ArrayList<>(3);
        list.add(a.serialize());
        list.add(b.serialize());
        cfg.addDefault("types", list);
    }

    public static void init()
    {
        map.clear();
        guiMap.clear();
        final File file = new File(DarkRiseCrafting.getInstance().getDataFolder(), "config.yml");
        final FileConfiguration cfg;
        if (! file.exists())
        {
            cfg = new YamlConfiguration();
            addDefs(cfg);
            file.getAbsoluteFile().getParentFile().mkdirs();
            try
            {
                file.createNewFile();
            } catch (final IOException e)
            {
                DarkRiseCrafting.getInstance().getLogger().warning("Can't create config file: " + file);
                e.printStackTrace();
            }
            cfg.options().copyDefaults(true);
            try
            {
                cfg.save(file);
            } catch (final IOException e)
            {
                DarkRiseCrafting.getInstance().getLogger().warning("Can't save config file: " + file);
                e.printStackTrace();
            }
        }
        else
        {
            cfg = YamlConfiguration.loadConfiguration(file);
            addDefs(cfg);
        }

        final List<Map<?, ?>> typesSection = cfg.getMapList("types");
        for (final Map<?, ?> typeData : typesSection)
        {
            final CraftingTable ct = new CraftingTable((Map<String, Object>) typeData);
            map.put(ct.getName(), ct);
        }
        cfg.options().copyDefaults(true);
        try
        {
            cfg.save(file);
        } catch (final IOException e)
        {
            DarkRiseCrafting.getInstance().getLogger().warning("Can't save config file: " + file);
            e.printStackTrace();
        }

        for (final Entry<String, CraftingTable> entry : map.entrySet())
        {
            final String key = entry.getKey();
            final CraftingTable value = entry.getValue();
            guiMap.put(key, new CustomGUI(key, value.getInventoryName(), value.getPattern()));
        }
    }
}
