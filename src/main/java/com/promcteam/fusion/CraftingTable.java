package com.promcteam.fusion;

import com.promcteam.codex.CodexEngine;
import com.promcteam.codex.items.ItemType;
import com.promcteam.codex.items.exception.MissingItemException;
import com.promcteam.codex.items.exception.MissingProviderException;
import com.promcteam.risecore.legacy.util.DeserializationWorker;
import com.promcteam.risecore.legacy.util.SerializationBuilder;
import com.promcteam.risecore.legacy.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class CraftingTable implements ConfigurationSerializable {
    private final String                          name;
    private final String                          inventoryName;
    private final InventoryPattern                pattern;
    private final InventoryPattern                catPattern;
    private final ItemStack                       fillItem;
    private final Map<String, Recipe>             recipes;
    private final ItemType                        iconItem;
    private       boolean                         useCategories = true;
    private final LinkedHashMap<String, Category> categories    = new LinkedHashMap<>();

    //Mastery!
    private final int masteryUnlock;
    private final int masteryFee;

    public CraftingTable(String name,
                         String inventoryName,
                         ItemType iconItem,
                         InventoryPattern pattern,
                         ItemStack fillItem,
                         int masteryUnlock,
                         int masteryFee,
                         Map<String, Recipe> recipes) {
        this.name = name;
        this.inventoryName = inventoryName;
        this.iconItem = iconItem;
        this.pattern = pattern;
        this.catPattern = null;
        this.recipes = recipes;
        this.fillItem = fillItem;
        this.masteryUnlock = masteryUnlock;
        this.masteryFee = masteryFee;
    }

    public CraftingTable(String name,
                         String inventoryName,
                         ItemType iconItem,
                         InventoryPattern pattern,
                         ItemStack fillItem,
                         int masteryUnlock,
                         int masteryFee) {
        this.name = name;
        this.inventoryName = inventoryName;
        this.iconItem = iconItem;
        this.pattern = pattern;
        this.catPattern = null;
        this.recipes = new LinkedHashMap<>(5);
        this.fillItem = fillItem;
        this.masteryUnlock = masteryUnlock;
        this.masteryFee = masteryFee;
    }

    @SuppressWarnings("unchecked")
    public CraftingTable(Map<String, Object> map) throws MissingProviderException, MissingItemException {
        this.recipes = new LinkedHashMap<>(5);
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.inventoryName = dw.getString("inventoryName");
        this.pattern = new InventoryPattern(dw.getSection("pattern"));
        this.catPattern =
                dw.getSection("categoryPattern") != null ? new InventoryPattern(dw.getSection("categoryPattern"))
                        : null;
        this.masteryUnlock = dw.getInt("masteryUnlock");
        this.masteryFee = dw.getInt("masteryFee");
        this.useCategories = dw.getBoolean("useCategories", true);
        this.iconItem = CodexEngine.get().getItemManager().getItemType(dw.getString("icon"));
        if (dw.getSection("fillItem") != null)
            this.fillItem = new ItemBuilder(dw.getSection("fillItem")).build();
        else
//            this.fillItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
            this.fillItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        Collection<Category> categoriesList = new ArrayList<>();
        dw.deserializeCollection(categoriesList, "categories", Category.class);
        categoriesList.stream()
                .filter(c -> c.getIconItem() != null)
                .forEach(c -> {
                    if (c.getPattern() == null)
                        c.setPattern(catPattern);
                    categories.put(c.getName(), c);
                });

        List<Map<?, ?>> recipesSection = dw.getList("recipes", new ArrayList<>(2));
        for (Map<?, ?> recipeData : recipesSection) {
            try {
                Recipe recipe = new Recipe((Map<String, Object>) recipeData);
                this.recipes.put(recipe.getName(), recipe);

                if (recipeData.containsKey("category") && recipeData.get("category") instanceof String) {
                    Category category = categories.get(recipeData.get("category"));

                    if (category == null) {
                        continue;
                    }

                    category.getRecipes().add(recipe);
                }
            } catch (Exception e) {
                Fusion.getInstance()
                        .error("Exception when reading config, Invalid entry in config of " + this.name
                                + " crafting table. Value: " + recipeData);
                e.printStackTrace();
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public String getInventoryName() {
        return this.inventoryName;
    }

    public ItemType getIconItem() {
        return iconItem;
    }

    public InventoryPattern getPattern() {
        return this.pattern;
    }

    public Map<String, Recipe> getRecipes() {
        return this.recipes;
    }

    public Map<String, Category> getCategories() {
        return categories;
    }

    public Recipe getRecipe(String str) {
        return this.recipes.get(str);
    }

    public ItemStack getFillItem() {
        return fillItem;
    }

    public void addRecipe(Recipe recipe) {
        this.recipes.put(recipe.getName(), recipe);
    }

    public int getMasteryUnlock() {
        return masteryUnlock;
    }

    public int getMasteryFee() {
        return masteryFee;
    }

    public boolean getUseCategories() {
        return useCategories;
    }

    public Collection<Recipe> getRecipes(Collection<ItemStack> items, Player p) {
        if (items.isEmpty()) {
            return new ArrayList<>(this.recipes.values());
        }
        return this.recipes.values()
                .stream()
                .unordered()
                .filter(r -> r.isValid(items, p, this))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> serialize() {
        return SerializationBuilder.start(4)
                .append("name", this.name)
                .append("icon", this.iconItem.getNamespacedID())
                .append("pattern", this.pattern.serialize())
                .append("categoryPattern", this.catPattern != null ? this.catPattern.serialize() : null)
                .append("inventoryName", this.inventoryName)
                .append("masteryUnlock", this.masteryUnlock)
                .append("masteryFee", this.masteryFee)
                .append("fillItem", ItemBuilder.newItem(fillItem).serialize())
                .append("useCategories", useCategories)
                .append("recipes", this.recipes.values().stream().map(Recipe::serialize).collect(Collectors.toList()))
                .build();
    }
}
