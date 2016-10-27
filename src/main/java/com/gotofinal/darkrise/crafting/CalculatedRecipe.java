package com.gotofinal.darkrise.crafting;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import com.gotofinal.darkrise.economy.DarkRiseItems;
import com.gotofinal.darkrise.spigot.core.Vault;
import com.gotofinal.messages.api.messages.Message.MessageData;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CalculatedRecipe
{
    private final Recipe    recipe;
    private final ItemStack icon;

    private final boolean canCraft;

    CalculatedRecipe(Recipe recipe, ItemStack icon, boolean canCraft)
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
        DarkRiseItems ir = DarkRiseEconomy.getInstance().getItems();
        DarkRiseCrafting pl = DarkRiseCrafting.getInstance();

        StringBuilder lore = new StringBuilder(512);
        ItemStack result = recipe.getResult().getItemStack();
        List<String> resultLore = result.getItemMeta().getLore();
        if ((resultLore != null) && ! resultLore.isEmpty())
        {
            resultLore.forEach((str) -> lore.append(str).append('\n'));
            lore.append('\n');
        }
        boolean canCraft = true;

        String permissionLine;
        if (! Utils.hasCraftingPermission(player, recipe.name))
        {
            canCraft = false;
            permissionLine = pl.getMessageAsString("crafting.gui.learned.false", "crafting.gui.learned.false", new MessageData("recipe", recipe),
                                                   new MessageData("player", player));
        }
        else
        {
            permissionLine = pl.getMessageAsString("crafting.gui.learned.true", "crafting.gui.learned.true", new MessageData("recipe", recipe),
                                                   new MessageData("player", player));
        }
        String moneyLine;
        if (recipe.price != 0)
        {
            if (! Vault.canPay(player, recipe.price))
            {
                canCraft = false;
                moneyLine = pl.getMessageAsString("crafting.gui.money.false", "crafting.gui.money.false", new MessageData("recipe", recipe),
                                                  new MessageData("player", player));
            }
            else
            {
                moneyLine = pl.getMessageAsString("crafting.gui.money.true", "crafting.gui.money.true", new MessageData("recipe", recipe),
                                                  new MessageData("player", player));
            }
        }
        else
        {
            moneyLine = null;
        }

        String levelsLine;
        if (recipe.neededLevels != 0)
        {
            if (player.getLevel() < recipe.neededLevels)
            {
                canCraft = false;
                levelsLine = pl.getMessageAsString("crafting.gui.xpLevels.false", "crafting.gui.xpLevels.false", new MessageData("recipe", recipe),
                                                   new MessageData("player", player));
            }
            else
            {
                levelsLine = pl.getMessageAsString("crafting.gui.xpLevels.true", "crafting.gui.xpLevels.true", new MessageData("recipe", recipe),
                                                   new MessageData("player", player));
            }
        }
        else
        {
            levelsLine = null;
        }
        String xpLine;
        if (recipe.neededXp != 0)
        {
            if (ExperienceManager.getTotalExperience(player) < recipe.neededXp)
            {
                canCraft = false;
                xpLine = pl.getMessageAsString("crafting.gui.xp.false", "crafting.gui.xp.false", new MessageData("recipe", recipe),
                                               new MessageData("player", player));
            }
            else
            {
                xpLine = pl.getMessageAsString("crafting.gui.xp.true", "crafting.gui.xp.true", new MessageData("recipe", recipe),
                                               new MessageData("player", player));
            }
        }
        else
        {
            xpLine = null;
        }


        Map<ItemStack, Integer> eqItems = Recipe.getItems(items);




//        Map<String, Entry<DarkRiseItem, Integer>> eqItems = new LinkedHashMap<>(20);
//        for (ItemStack item : items)
//        {
//            DarkRiseItem riseItem = ir.getItemByStack(item);
//            if (riseItem == null)
//            {
//                continue;
//            }
//            eqItems.put(riseItem.getId().toLowerCase(), new SimpleEntry<>(riseItem, item.getAmount()));
//        }
        Collection<RecipeItem> localPattern = new HashSet<>(recipe.pattern);
        for (Iterator<RecipeItem> it = localPattern.iterator(); it.hasNext(); )
        {
            RecipeItem recipeItem = it.next();
            ItemStack recipeItemStack = recipeItem.getItemStack();
            ItemStack recipeItemStackOne = recipeItemStack.clone();
            recipeItemStackOne.setAmount(1);
            int eqAmount = eqItems.getOrDefault(recipeItemStackOne, - 1);
            if (eqAmount == -1)
            {
                canCraft = false;
                lore.append(pl.getMessageAsString(
                        (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.false" : "crafting.gui.ingredientSimple.false",
                        (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.false" : "crafting.gui.ingredientSimple.false",
                        new MessageData("amount", 0),
                        new MessageData("recipeItem", recipeItem), new MessageData("player", player),
                        new MessageData("recipe", recipe), new MessageData("item", recipeItemStack))).append('\n');
                continue;
            }
            int patternAmount = recipeItem.getAmount();
            if (eqAmount < patternAmount)
            {
                canCraft = false;
                lore.append(pl.getMessageAsString(
                        (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.false" : "crafting.gui.ingredientSimple.false",
                        (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.false" : "crafting.gui.ingredientSimple.false",
                        new MessageData("amount", eqAmount),
                        new MessageData("recipeItem", recipeItem), new MessageData("player", player),
                        new MessageData("recipe", recipe), new MessageData("item", recipeItemStack))).append('\n');
                continue;
            }
            if (eqAmount == patternAmount)
            {
                eqItems.remove(recipeItem.getItemStack());
            }
            int rest = eqAmount - patternAmount;
            eqItems.put(recipeItemStackOne, rest);
            it.remove();

            lore.append(pl.getMessageAsString((recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.true" : "crafting.gui.ingredientSimple.true",
                                              (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.true" : "crafting.gui.ingredientSimple.true",
                                              new MessageData("amount", eqAmount),
                                              new MessageData("recipeItem", recipeItem), new MessageData("player", player), new MessageData("recipe", recipe),
                                              new MessageData("item", recipeItemStack))).append('\n');
        }
        String canCraftLine;
        if (canCraft)
        {
            canCraftLine = pl.getMessageAsString("crafting.gui.canCraft.true", null, new MessageData("recipe", recipe), new MessageData("player", player));
        }
        else
        {
            canCraftLine = pl.getMessageAsString("crafting.gui.canCraft.false", null, new MessageData("recipe", recipe), new MessageData("player", player));
        }

        lore.append('\n');
        if (moneyLine != null)
        {
            lore.append(moneyLine).append('\n');
        }
        if (levelsLine != null)
        {
            lore.append(levelsLine).append('\n');
        }
        if (xpLine != null)
        {
            lore.append(xpLine).append('\n');
        }
        lore.append(permissionLine);
        if (canCraftLine != null)
        {
            lore.append('\n').append(canCraftLine);
        }

        ItemStack icon = result.clone();
        ItemMeta itemMeta = icon.getItemMeta();
        itemMeta.setLore(Arrays.asList(StringUtils.split(lore.toString(), '\n')));
        icon.setItemMeta(itemMeta);

        return new CalculatedRecipe(recipe, icon, canCraft);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (! (o instanceof CalculatedRecipe))
        {
            return false;
        }

        CalculatedRecipe that = (CalculatedRecipe) o;
        return new EqualsBuilder().append(this.recipe, that.recipe).append(this.icon, that.icon).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(this.recipe).append(this.icon).toHashCode();
    }
}
