package com.gotofinal.darkrise.crafting;

import me.travja.darkrise.core.legacy.util.DeserializationWorker;
import me.travja.darkrise.core.legacy.util.SerializationBuilder;
import me.travja.darkrise.core.legacy.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class CraftingTable implements ConfigurationSerializable {
    private final String name;
    private final String inventoryName;
    private final InventoryPattern pattern;
    private final ItemStack fillItem;
    private final Map<String, Recipe> recipes;
    private final LinkedHashMap<String, Category> categories = new LinkedHashMap<>();

    //Mastery!
    private final int masteryUnlock;
    private final int masteryFee;

    public CraftingTable(String name, String inventoryName, InventoryPattern pattern, ItemStack fillItem, int masteryUnlock, int masteryFee, Map<String, Recipe> recipes) {
        this.name = name;
        this.inventoryName = inventoryName;
        this.pattern = pattern;
        this.recipes = recipes;
        this.fillItem = fillItem;
        this.masteryUnlock = masteryUnlock;
        this.masteryFee = masteryFee;
    }

    public CraftingTable(String name, String inventoryName, InventoryPattern pattern, ItemStack fillItem, int masteryUnlock, int masteryFee) {
        this.name = name;
        this.inventoryName = inventoryName;
        this.pattern = pattern;
        this.recipes = new LinkedHashMap<>(5);
        this.fillItem = fillItem;
        this.masteryUnlock = masteryUnlock;
        this.masteryFee = masteryFee;
    }

    @SuppressWarnings("unchecked")
    public CraftingTable(Map<String, Object> map) {
        this.recipes = new LinkedHashMap<>(5);
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.inventoryName = dw.getString("inventoryName");
        this.pattern = new InventoryPattern(dw.getSection("pattern"));
        this.masteryUnlock = dw.getInt("masteryUnlock");
        this.masteryFee = dw.getInt("masteryFee");
        if (dw.getSection("fillItem") != null)
            this.fillItem = new ItemBuilder(dw.getSection("fillItem")).build();
        else
            this.fillItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
//            this.fillItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        Collection<Category> categoriesList = new ArrayList<>();
        dw.deserializeCollection(categoriesList, "categories", Category.class);
        categoriesList.stream()
                .filter(c -> c.getIconItem() != null)
                .forEach(c -> categories.put(c.getName(), c));

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
                DarkRiseCrafting.getInstance().error("Exception when reading config, Invalid entry in config of " + this.name + " crafting table. Value: " + recipeData);
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

    public Collection<Recipe> getRecipes(Collection<ItemStack> items, Player p) {
        if (items.isEmpty()) {
            return new ArrayList<>(this.recipes.values());
        }
        return this.recipes.values().stream().unordered().filter(r -> r.isValid(items, p, this)).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> serialize() {
        return SerializationBuilder.start(4)
                .append("name", this.name)
                .append("pattern", this.pattern.serialize())
                .append("inventoryName", this.inventoryName)
                .append("masteryUnlock", this.masteryUnlock)
                .append("masteryFee", this.masteryFee)
                .append("fillItem", ItemBuilder.newItem(fillItem).serialize())
                .append("recipes", this.recipes.values().stream().map(Recipe::serialize).collect(Collectors.toList())).build();
    }
}
