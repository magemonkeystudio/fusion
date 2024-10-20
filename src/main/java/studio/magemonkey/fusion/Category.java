package studio.magemonkey.fusion;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.items.ItemType;
import studio.magemonkey.codex.items.exception.CodexItemException;
import studio.magemonkey.codex.items.providers.VanillaProvider;
import studio.magemonkey.risecore.legacy.util.DeserializationWorker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Category implements ConfigurationSerializable {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private ItemType iconItem;
    @Getter
    private final Collection<Recipe> recipes = new ArrayList<>();
    @Setter
    @Getter
    private InventoryPattern pattern;
    @Getter
    @Setter
    private int order;
    private boolean hasPrevious = true;

    @Getter
    private final String iconName;

    public Category(String name) {
        this.name = name;
        this.order = 0;
        this.iconItem = new VanillaProvider.VanillaItemType(Material.PAPER);
        this.iconName = "PAPER";
    }

    public Category(String name, String iconName, InventoryPattern pattern, int order) {
        this.name = name;
        this.iconName = iconName;
        try {
            this.iconItem = CodexEngine.get().getItemManager().getItemType(iconName);
        } catch (CodexItemException e) {
            throw new RuntimeException(e);
        }
        this.pattern = pattern;
        this.order = order;
    }

    public Category(Map<String, Object> map) {
        DeserializationWorker dw = DeserializationWorker.start(map);
        name = dw.getString("name");
        order = dw.getInt("order");
        iconName = dw.getString("icon");
        try {
            iconItem = CodexEngine.get().getItemManager().getItemType(iconName);
        } catch (CodexItemException e) {
            throw new RuntimeException(e);
        }

        if (iconItem == null) {
            Fusion.getInstance().getLogger().severe("Invalid category icon for: " + name);
        }

        pattern = dw.getSection("pattern") != null ? new InventoryPattern(dw.getSection("pattern")) : null;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("order", order);
        map.put("icon", iconName);
        if (pattern != null)
            map.put("pattern", pattern.serialize());
        return map;
    }

    public void hasPrevious(boolean b) {
        this.hasPrevious = b;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }

    public Recipe getRecipe(String name) {
        for (Recipe recipe : recipes) {
            if (recipe.getName().equals(name)) {
                return recipe;
            }
        }
        return null;
    }

    public static Category copy(Category category) {
        return new Category(category.getName(), category.getIconName(), InventoryPattern.copy(category.getPattern()), category.getOrder());
    }
}
