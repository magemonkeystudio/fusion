package com.gotofinal.darkrise.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import com.gotofinal.darkrise.economy.DarkRiseItems;
import com.gotofinal.darkrise.spigot.core.Vault;
import com.gotofinal.darkrise.spigot.core.utils.DeserializationWorker;
import com.gotofinal.darkrise.spigot.core.utils.SerializationBuilder;
import com.gotofinal.darkrise.spigot.core.utils.cmds.DelayedCommand;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Recipe implements ConfigurationSerializable
{
    protected final String                 name;
    protected final Collection<RecipeItem> pattern;
    protected final RecipeItem             result;
    protected final double                 price;
    protected final int                    neededLevels;
    protected final int                    neededXp;
    protected final int                    xpGain;
    protected final boolean                mastery;
    protected final Collection<DelayedCommand> commands = new ArrayList<>();
    protected final String rank;

    public Recipe(Map<String, Object> map)
    {
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.result = RecipeItem.fromConfig(map.get("result"));
        this.pattern = dw.getStringList("pattern").stream().map(RecipeItem::fromConfig).collect(Collectors.toList());
        this.price = dw.getDouble("price", 0);
        this.neededLevels = dw.getInt("neededLevels", 0);
        this.neededXp = dw.getInt("neededXp", 0);
        this.xpGain = dw.getInt("xpGain", 0);
        this.mastery = dw.getBoolean("mastery");
        this.rank = dw.getString("rank");
        dw.deserializeCollection(this.commands, "commands", DelayedCommand.class);

        if(result == null) {
            throw new IllegalArgumentException("Invalid result: " + map.get("result"));
        }
    }

    public Recipe(String name, Collection<RecipeItem> pattern, RecipeEconomyItem result, double price, int neededLevels, int neededXp)
    {
        this.name = name;
        this.pattern = new ArrayList<>(pattern);
        this.result = result;
        this.price = price;
        this.neededLevels = neededLevels;
        this.neededXp = neededXp;
        this.xpGain = 0;
        this.mastery = false;
        this.rank = "";
    }

    public static Map<ItemStack, Integer> getItems(Collection<ItemStack> items)
    {
        Map<ItemStack, Integer> eqItems = new HashMap<>(20);
        for (ItemStack item : items)
        {
            int itemAmount = item.getAmount();
            item = item.clone();
            item.setAmount(1);
            Integer amount = eqItems.getOrDefault(item, 0);
            eqItems.put(item, amount + itemAmount);
        }
        return eqItems;
    }

    public static Map<ItemStack, Integer> getPattern(Collection<RecipeItem> items)
    {
        Map<ItemStack, Integer> localPattern = new HashMap<>(20);
        for (RecipeItem recipeItem : items)
        {
            ItemStack item = recipeItem.getItemStack();
            boolean added = false;

            int itemAmount = item.getAmount();
            item.setAmount(1);
            Integer amount = localPattern.getOrDefault(item, 0);
            localPattern.put(item, amount + itemAmount);
        }
        return localPattern;
    }

    public boolean isValid(Collection<ItemStack> items, Player p, CraftingTable craftingTable)
    {
        if (items.isEmpty())
        {
            return false;
        }
        if (p != null)
        {
            if (! Utils.hasCraftingPermission(p, this.name))
            {
                return false;
            }
            if (LevelFunction.getLevel(p, craftingTable) < this.neededLevels)
            {
                return false;
            }
            if (DarkRiseCrafting.getExperienceManager().getExperience(p, craftingTable) < this.neededXp)
            {
                return false;
            }
            if (! Vault.canPay(p, this.price))
            {
                return false;
            }
        }
        DarkRiseItems ir = DarkRiseEconomy.getInstance().getItems();
        Map<ItemStack, Integer> eqItems = getItems(items);
        Map<ItemStack, Integer> localPattern = getPattern(this.pattern);
        for (Iterator<Entry<ItemStack, Integer>> iterator = localPattern.entrySet().iterator(); iterator.hasNext(); )
        {
            Entry<ItemStack, Integer> patternEntry = iterator.next();
            int eqAmount = eqItems.getOrDefault(patternEntry.getKey(), - 1);
            if (eqAmount == - 1)
            {
                return false;
            }
            ItemStack eqEntry = patternEntry.getKey();
            int patternAmount = patternEntry.getValue();
            if (eqAmount < patternAmount)
            {
                return false;
            }
            if (eqAmount == patternAmount)
            {
                eqItems.remove(eqEntry);
            }
            int rest = eqAmount - patternAmount;
            eqItems.put(eqEntry, rest);
            iterator.remove();
        }
        return localPattern.isEmpty();
    }

    public RecipeItem getResult()
    {
        return this.result;
    }

    public String getName()
    {
        return this.name;
    }

    public double getPrice()
    {
        return this.price;
    }

    public int getNeededLevels()
    {
        return this.neededLevels;
    }

    public int getNeededXp()
    {
        return this.neededXp;
    }

    public int getXpGain()
    {
        return this.xpGain;
    }

    public Collection<DelayedCommand> getCommands()
    {
        return commands;
    }

    public Collection<RecipeItem> getPattern()
    {
        return this.pattern;
    }

    public Collection<ItemStack> getItemsToTake()
    {
        return this.pattern.stream().map(RecipeItem::getItemStack).collect(Collectors.toList());
    }

    public String getRank()
    {
        return rank;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (! (o instanceof Recipe))
        {
            return false;
        }
        Recipe recipe = (Recipe) o;
        return this.name.equals(recipe.name) && this.pattern.equals(recipe.pattern);
    }

    @Override
    public int hashCode()
    {
        int result = this.name.hashCode();
        result = (31 * result) + this.pattern.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("name", this.name).append("price", this.price)
                                                                          .append("pattern", this.pattern).append("result", this.result).toString();
    }

    @Override
    public Map<String, Object> serialize()
    {
        return SerializationBuilder.start(6).append("name", this.name).append("result", this.result.toConfig()).append("price", this.price)
                                   .append("neededXp", this.neededXp).append("neededLevels", this.neededLevels)
                                   .append("pattern", this.pattern.stream().map(RecipeItem::toConfig).collect(Collectors.toList())).build();
    }
}
