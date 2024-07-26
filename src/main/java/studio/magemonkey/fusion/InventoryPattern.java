package studio.magemonkey.fusion;

import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.legacy.item.ItemBuilder;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.risecore.legacy.util.DeserializationWorker;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.stream.Collectors;

public class InventoryPattern implements ConfigurationSerializable {
    @Getter
    private final String[]                                       pattern; // _ for ingredients, = for result.
    @Getter
    private final HashMap<Character, ItemStack>                  items;
    private final HashMap<Character, Collection<DelayedCommand>> commands          = new HashMap<>();
    @Getter
    private final List<Character>                                closeOnClickSlots = new ArrayList<>();

    public InventoryPattern(String[] pattern, HashMap<Character, ItemStack> items) {
        this.pattern = pattern;
        this.items = items;
    }

    public InventoryPattern(Map<String, Object> map) {

        DeserializationWorker dw   = DeserializationWorker.start(map);
        List<String>          temp = dw.getStringList("pattern");
        this.pattern = temp.toArray(new String[0]);
        this.items = new HashMap<>();
        DeserializationWorker itemsTemp = DeserializationWorker.start(dw.getSection("items", new HashMap<>(2)));
        for (String entry : itemsTemp.getMap().keySet()) {
            if (entry.contains("."))
                continue;

            Map<String, Object> section = itemsTemp.getSection(entry);
            this.items.put(entry.charAt(0), new ItemBuilder(section).build());

            if (section.containsKey("closeonclick") && (boolean) section.get("closeonclick")) {
                closeOnClickSlots.add(entry.charAt(0));
            }
        }
        if (dw.getSection("items.queue-items.-") != null)
            this.items.put('-', new ItemBuilder(dw.getSection("items.queue-items.-")).build());

        final DeserializationWorker commandsTemp =
                DeserializationWorker.start(dw.getSection("commands", new HashMap<>(2)));
        for (final String entry : commandsTemp.getMap().keySet()) {
            this.commands.put(entry.charAt(0),
                    commandsTemp.deserializeCollection(new ArrayList<>(5), entry, DelayedCommand.class));
        }

        // Required for editor purposes
        items.put('o', null);
    }

    public Collection<DelayedCommand> getCommands(char c) {
        return this.commands.get(c);
    }

    public Character getSlot(int slot) {
        if (slot / 9 >= pattern.length)
            return ' ';
        return pattern[slot / 9].charAt(slot % 9);
    }

    public void replaceSlot(int slot, char c) {
        if (slot / 9 >= pattern.length)
            return;
        StringBuilder sb = new StringBuilder(pattern[slot / 9]);
        sb.setCharAt(slot % 9, c);
        pattern[slot / 9] = sb.toString();
    }

    public Character getCycledCharacter(char c, boolean forward) {
        List<Map.Entry<Character, ItemStack>> entries = getItemsAsList();
        int size = entries.size();

        for (int i = 0; i < size; i++) {
            if (entries.get(i).getKey() == c) {
                if (forward) {
                    return entries.get((i + 1) % size).getKey();
                } else {
                    return entries.get((i - 1 + size) % size).getKey();
                }
            }
        }

        // If character 'c' is not found, return the first or last item based on 'forward' flag
        return forward ? entries.get(0).getKey() : entries.get(size - 1).getKey();
    }

    public int getInventorySize() {
        return pattern.length * 9;
    }

    public List<Map.Entry<Character, ItemStack>> getItemsAsList() {
        return items.entrySet().stream().map(e -> new SimpleEntry<>(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("pattern", this.pattern)
                .append("items", this.items)
                .toString();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        // noinspection RedundantCast eclipse...
        return SerializationBuilder.start(2)
                .append("pattern", this.pattern)
                .appendMap("commands", this.commands)
                .append("items",
                        this.items.entrySet()
                                .stream()
                                .map(e -> new SimpleEntry<>(e.getKey().toString(),
                                        ItemBuilder.newItem(e.getValue()).serialize()))
                                .collect(Collectors.toMap((stringMapSimpleEntry) -> ((SimpleEntry<String, Map<String, Object>>) stringMapSimpleEntry).getKey(),
                                        (stringMapSimpleEntry1) -> ((SimpleEntry<String, Map<String, Object>>) stringMapSimpleEntry1).getValue())))
                .build();
    }

    public void clear() {
        // Replace all items that are not 'o', '-', '<', '>', '{', '}', 'f' with 'f'
        for(int i = 0; i < pattern.length; i++) {
            StringBuilder sb = new StringBuilder(pattern[i]);
            for(int j = 0; j < sb.length(); j++) {
                char c = sb.charAt(j);
                if(c != 'o' && c != '-' && c != '<' && c != '>' && c != '{' && c != '}' && c != 'f') {
                    sb.setCharAt(j, 'f');
                }
            }
            pattern[i] = sb.toString();
        }
    }

    public static InventoryPattern copy(InventoryPattern pattern) {
        if(pattern == null) return null;
        InventoryPattern _pattern = new InventoryPattern(pattern.pattern, new HashMap<>(pattern.items));
        _pattern.commands.putAll(pattern.commands);
        _pattern.closeOnClickSlots.addAll(pattern.closeOnClickSlots);
        return _pattern;
    }
}
