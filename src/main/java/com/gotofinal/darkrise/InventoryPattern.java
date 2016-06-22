package com.gotofinal.darkrise;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.gotofinal.darkrise.core.utils.DeserializationWorker;
import com.gotofinal.darkrise.core.utils.SerializationBuilder;
import com.gotofinal.darkrise.core.utils.item.ItemBuilder;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

public class InventoryPattern implements ConfigurationSerializable
{
    private final String[]                  pattern; // _ for ingredients, = for result.
    private final Char2ObjectMap<ItemStack> items;

    public InventoryPattern(final String[] pattern, final Char2ObjectMap<ItemStack> items)
    {
        this.pattern = pattern;
        this.items = items;
    }

    @SuppressWarnings("unchecked")
    public InventoryPattern(final Map<String, Object> map)
    {
        final DeserializationWorker dw = DeserializationWorker.start(map);
        final List<String> temp = dw.getStringList("pattern");
        this.pattern = temp.toArray(new String[temp.size()]);
        this.items = new Char2ObjectOpenHashMap<>();
        final DeserializationWorker itemsTemp = DeserializationWorker.start(dw.getSection("items", new HashMap<>(2)));
        for (final String entry : itemsTemp.getMap().keySet())
        {
            this.items.put(entry.charAt(0), new ItemBuilder(itemsTemp.getSection(entry)).build());
        }
    }

    public String[] getPattern()
    {
        return this.pattern;
    }

    public Char2ObjectMap<ItemStack> getItems()
    {
        return this.items;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("pattern", this.pattern).append("items", this.items).toString();
    }

    @Override
    public Map<String, Object> serialize()
    {
        return SerializationBuilder.start(2).append("pattern", this.pattern).append("items", this.items.entrySet().stream().map(e -> new SimpleEntry<>(e.getKey().toString(), ItemBuilder.newItem(e.getValue()).serialize())).collect(Collectors.toMap(Entry::getKey, Entry::getValue))).build();
    }
}
