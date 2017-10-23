package com.gotofinal.darkrise.crafting;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gotofinal.darkrise.spigot.core.utils.DeserializationWorker;
import com.gotofinal.darkrise.spigot.core.utils.SerializationBuilder;
import com.gotofinal.darkrise.spigot.core.utils.cmds.DelayedCommand;
import com.gotofinal.darkrise.spigot.core.utils.item.ItemBuilder;

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
    private final Char2ObjectMap<Collection<DelayedCommand>> commands = new Char2ObjectOpenHashMap<>();

    public InventoryPattern(String[] pattern, Char2ObjectMap<ItemStack> items)
    {
        this.pattern = pattern;
        this.items = items;
    }

    @SuppressWarnings("unchecked")
    public InventoryPattern(Map<String, Object> map)
    {
        DeserializationWorker dw = DeserializationWorker.start(map);
        List<String> temp = dw.getStringList("pattern");
        this.pattern = temp.toArray(new String[temp.size()]);
        this.items = new Char2ObjectOpenHashMap<>();
        DeserializationWorker itemsTemp = DeserializationWorker.start(dw.getSection("items", new HashMap<>(2)));
        for (String entry : itemsTemp.getMap().keySet())
        {
            this.items.put(entry.charAt(0), new ItemBuilder(itemsTemp.getSection(entry)).build());
        }

        final DeserializationWorker commandsTemp = DeserializationWorker.start(dw.getSection("commands", new HashMap<>(2)));
        for (final String entry : commandsTemp.getMap().keySet())
        {
            this.commands.put(entry.charAt(0), commandsTemp.deserializeCollection(new ArrayList<>(5), entry, DelayedCommand.class));
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

    public Collection<DelayedCommand> getCommands(char c)
    {
            return this.commands.get(c);
    }

    public Character getSlot(int slot)
    {
        return pattern[slot / 9].charAt(slot % 9);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("pattern", this.pattern).append("items", this.items).toString();
    }

    @Override
    public Map<String, Object> serialize()
    {
        //noinspection Convert2MethodRef,RedundantCast eclipse...,
        return SerializationBuilder.start(2)
                                   .append("pattern", this.pattern)
                                   .appendMap("commands", this.commands)
                                   .append("items", this.items.entrySet().stream().map(e -> new SimpleEntry<>(e.getKey().toString(), ItemBuilder.newItem(e.getValue()).serialize())).collect(Collectors.toMap((stringMapSimpleEntry) -> ((SimpleEntry<String, Map<String, Object>>) stringMapSimpleEntry).getKey(), (stringMapSimpleEntry1) -> ((SimpleEntry<String, Map<String, Object>>) stringMapSimpleEntry1).getValue()))).build();
    }
}
