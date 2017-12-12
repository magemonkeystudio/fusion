package com.gotofinal.darkrise.crafting;

import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import com.gotofinal.darkrise.economy.DarkRiseItem;
import com.gotofinal.darkrise.economy.DarkRiseItems;
import com.gotofinal.darkrise.spigot.core.utils.DeserializationWorker;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Category implements ConfigurationSerializable
{
    private final String name;
    private final DarkRiseItem iconItem;
    private final Collection<Recipe> recipes = new ArrayList<>();
    private final int order;

    public Category(Map<String, Object> map)
    {
        DeserializationWorker dw = DeserializationWorker.start(map);
        name = dw.getString("name");
        order = dw.getInt("order");
        iconItem = DarkRiseEconomy.getInstance().getItems().getItemById(dw.getString("icon"));
    }

    @Override
    public Map<String, Object> serialize()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        return map;
    }

    public String getName()
    {
        return name;
    }

    public Collection<Recipe> getRecipes()
    {
        return recipes;
    }

    public DarkRiseItem getIconItem()
    {
        return iconItem;
    }

    public int getOrder()
    {
        return order;
    }
}
