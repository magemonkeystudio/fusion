package com.promcteam.fusion;

import com.promcteam.codex.CodexEngine;
import com.promcteam.codex.items.ItemType;
import com.promcteam.codex.items.exception.CodexItemException;
import com.promcteam.codex.items.providers.VanillaProvider;
import com.promcteam.risecore.legacy.util.DeserializationWorker;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Category implements ConfigurationSerializable {
    private final String             name;
    private final ItemType           iconItem;
    private final Collection<Recipe> recipes     = new ArrayList<>();
    private       InventoryPattern   pattern;
    private final int                order;
    private       boolean            hasPrevious = true;

    public Category(String name) {
        this.name = name;
        this.order = 0;
        this.iconItem = new VanillaProvider.VanillaItemType(Material.PAPER);
    }

    public Category(Map<String, Object> map) {
        DeserializationWorker dw = DeserializationWorker.start(map);
        name = dw.getString("name");
        order = dw.getInt("order");
        try {
            iconItem = CodexEngine.get().getItemManager().getItemType(dw.getString("icon"));
        } catch (CodexItemException e) {
            throw new RuntimeException(e);
        }

        if (iconItem == null) {
            Fusion.getInstance().getLogger().severe("Invalid category icon for: " + name);
        }

        pattern = dw.getSection("pattern") != null ? new InventoryPattern(dw.getSection("pattern")) : null;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        if (pattern != null)
            map.put("pattern", pattern.serialize());
        return map;
    }

    public InventoryPattern getPattern() {
        return pattern;
    }

    public void setPattern(InventoryPattern pattern) {
        this.pattern = pattern;
    }

    public String getName() {
        return name;
    }

    public Collection<Recipe> getRecipes() {
        return recipes;
    }

    public ItemType getIconItem() {
        return iconItem;
    }

    public int getOrder() {
        return order;
    }

    public void hasPrevious(boolean b) {
        this.hasPrevious = b;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }
}
