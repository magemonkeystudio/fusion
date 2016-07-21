package com.gotofinal.darkrise.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gotofinal.darkrise.spigot.core.utils.DeserializationWorker;
import com.gotofinal.darkrise.spigot.core.utils.SerializationBuilder;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CraftingTable implements ConfigurationSerializable
{
    private final String              name;
    private final String              inventoryName;
    private final InventoryPattern    pattern;
    private final Map<String, Recipe> recipes;

    public CraftingTable(final String name, final String inventoryName, final InventoryPattern pattern, final Map<String, Recipe> recipes)
    {
        this.name = name;
        this.inventoryName = inventoryName;
        this.pattern = pattern;
        this.recipes = recipes;
    }

    public CraftingTable(final String name, final String inventoryName, final InventoryPattern pattern)
    {
        this.name = name;
        this.inventoryName = inventoryName;
        this.pattern = pattern;
        this.recipes = new LinkedHashMap<>(5);
    }

    @SuppressWarnings("unchecked")
    public CraftingTable(final Map<String, Object> map)
    {
        this.recipes = new LinkedHashMap<>(5);
        final DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.inventoryName = dw.getString("inventoryName");
        this.pattern = new InventoryPattern(dw.getSection("pattern"));
        final List<Map<?, ?>> recipesSection = dw.getList("recipes", new ArrayList<>(2));
        for (final Map<?, ?> recipeData : recipesSection)
        {
            try
            {
                final Recipe recipe = new Recipe((Map<String, Object>) recipeData);
                this.recipes.put(recipe.getName(), recipe);
            } catch (final Exception e)
            {
                DarkRiseCrafting.getInstance().error("Exception when reading config, Invalid entry in config of " + this.name + " crafting table. Value: " + recipeData);
                throw new RuntimeException(e);
            }
        }
    }

    public String getName()
    {
        return this.name;
    }

    public String getInventoryName()
    {
        return this.inventoryName;
    }

    public InventoryPattern getPattern()
    {
        return this.pattern;
    }

    public Map<String, Recipe> getRecipes()
    {
        return this.recipes;
    }

    public Recipe getRecipe(final String str)
    {
        return this.recipes.get(str);
    }

    public void addRecipe(final Recipe recipe)
    {
        this.recipes.put(recipe.getName(), recipe);
    }

    public Collection<Recipe> getRecipes(final Collection<ItemStack> items, final Player p)
    {
        if (items.isEmpty())
        {
            return new ArrayList<>(this.recipes.values());
        }
        return this.recipes.values().stream().unordered().filter(r -> r.isValid(items, p)).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> serialize()
    {
        return SerializationBuilder.start(4).append("name", this.name).append("pattern", this.pattern.serialize()).append("inventoryName", this.inventoryName).append("recipes", this.recipes.values().stream().map(Recipe::serialize).collect(Collectors.toList())).build();
    }
}
