package com.gotofinal.darkrise;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.caversia.plugins.economy.model.CustomItem;
import com.caversia.plugins.economy.persistence.ItemsRepository;
import com.gotofinal.darkrise.core.Vault;
import com.gotofinal.messages.api.messages.Message.MessageData;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CalculatedRecipe
{
    private final Recipe    recipe;
    private final ItemStack icon;

    private final boolean canCraft;

    CalculatedRecipe(final Recipe recipe, final ItemStack icon, final boolean canCraft)
    {
        this.recipe = recipe;
        this.icon = icon;
        this.canCraft = canCraft;
    }

    public Recipe getRecipe()
    {
        return this.recipe;
    }

    public ItemStack getIcon()
    {
        return this.icon;
    }

    public boolean isCanCraft()
    {
        return this.canCraft;
    }

    public static CalculatedRecipe create(Recipe recipe, Collection<ItemStack> items, Player player)
    {
        final ItemsRepository ir = ItemsRepository.INSTANCE;
        final DarkRiseCrafting pl = DarkRiseCrafting.getInstance();

        final StringBuilder lore = new StringBuilder(512);
        final ItemStack result = recipe.getResult().getItemStack();
        final List<String> resultLore = result.getItemMeta().getLore();
        if ((resultLore != null) && ! resultLore.isEmpty())
        {
            resultLore.forEach((str) -> lore.append(str).append('\n'));
            lore.append('\n');
        }
        boolean canCraft = true;

        final String permissionLine;
        if (! player.hasPermission("crafting.recipe." + recipe.name) && ! player.hasPermission("crafting.recipes"))
        {
            canCraft = false;
            permissionLine = pl.getMessageAsString("gui.learned.false", "gui.learned.false", new MessageData("recipe", recipe), new MessageData("player", player));
        }
        else
        {
            permissionLine = pl.getMessageAsString("gui.learned.true", "gui.learned.true", new MessageData("recipe", recipe), new MessageData("player", player));
        }
        final String moneyLine;
        if (! Vault.canPay(player, recipe.price))
        {
            canCraft = false;
            moneyLine = pl.getMessageAsString("gui.money.false", "gui.money.false", new MessageData("recipe", recipe), new MessageData("player", player));
        }
        else
        {
            moneyLine = pl.getMessageAsString("gui.money.true", "gui.money.true", new MessageData("recipe", recipe), new MessageData("player", player));
        }


        final Map<String, Entry<CustomItem, Integer>> eqItems = new LinkedHashMap<>(20);
        for (final ItemStack item : items)
        {
            final Optional<CustomItem> itemOptional = ir.getItem(item);
            if (! itemOptional.isPresent())
            {
                continue;
            }
            final CustomItem customItem = itemOptional.get();
            eqItems.put(customItem.getName().toLowerCase(), new SimpleEntry<>(customItem, item.getAmount()));
        }
        final Map<String, RecipeItem> localPattern = new LinkedHashMap<>(20);
        for (final RecipeItem recipeItem : recipe.pattern)
        {
            localPattern.put(recipeItem.getItemName().toLowerCase(), recipeItem);
        }
        for (final Iterator<Entry<String, RecipeItem>> it = localPattern.entrySet().iterator(); it.hasNext(); )
        {
            final Entry<String, RecipeItem> patternEntry = it.next();
            final RecipeItem recipeItem = patternEntry.getValue();
            final ItemStack recipeItemStack = recipeItem.getItemStack();
            final Entry<CustomItem, Integer> eqEntry = eqItems.get(patternEntry.getKey().toLowerCase());
            if (eqEntry == null)
            {
                canCraft = false;
                lore.append(pl.getMessageAsString("gui.ingredient.false", "gui.ingredient.false", new MessageData("amount", 0), new MessageData("recipeItem", recipeItem), new MessageData("player", player), new MessageData("recipe", recipe), new MessageData("item", recipeItemStack))).append('\n');
                continue;
            }
            final int eqAmount = eqEntry.getValue();
            final int patternAmount = recipeItem.getAmount();
            if (eqAmount < patternAmount)
            {
                canCraft = false;
                lore.append(pl.getMessageAsString("gui.ingredient.false", "gui.ingredient.false", new MessageData("amount", eqAmount), new MessageData("recipeItem", recipeItem), new MessageData("player", player), new MessageData("recipe", recipe), new MessageData("item", recipeItemStack))).append('\n');
                continue;
            }
            if (eqAmount == patternAmount)
            {
                eqItems.remove(patternEntry.getKey().toLowerCase());
            }
            final int rest = eqAmount - patternAmount;
            eqEntry.setValue(rest);
            it.remove();

            lore.append(pl.getMessageAsString("gui.ingredient.true", "gui.ingredient.true", new MessageData("amount", eqAmount), new MessageData("recipeItem", recipeItem), new MessageData("player", player), new MessageData("recipe", recipe), new MessageData("item", recipeItemStack))).append('\n');
        }
        final String canCraftLine;
        if (canCraft)
        {
            canCraftLine = pl.getMessageAsString("gui.canCraft.true", null, new MessageData("recipe", recipe), new MessageData("player", player));
        }
        else
        {
            canCraftLine = pl.getMessageAsString("gui.canCraft.false", null, new MessageData("recipe", recipe), new MessageData("player", player));
        }

        lore.append('\n');
        lore.append(moneyLine).append('\n');
        lore.append(permissionLine);
        if (canCraftLine != null)
        {
            lore.append('\n').append(canCraftLine);
        }

        final ItemStack icon = result.clone();
        final ItemMeta itemMeta = icon.getItemMeta();
        itemMeta.setLore(Arrays.asList(StringUtils.split(lore.toString(), '\n')));
        icon.setItemMeta(itemMeta);

        return new CalculatedRecipe(recipe, icon, canCraft);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (! (o instanceof CalculatedRecipe))
        {
            return false;
        }

        final CalculatedRecipe that = (CalculatedRecipe) o;
        return new EqualsBuilder().append(this.recipe, that.recipe).append(this.icon, that.icon).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(this.recipe).append(this.icon).toHashCode();
    }
}
