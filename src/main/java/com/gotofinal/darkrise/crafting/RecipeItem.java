package com.gotofinal.darkrise.crafting;

import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import com.gotofinal.darkrise.economy.DarkRiseItem;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;

public class RecipeItem
{
    private final String itemName;
    private final int    amount;

    public RecipeItem(final String itemName, final int amount)
    {
        this.itemName = itemName;
        this.amount = amount;
    }

    public String getItemName()
    {
        return this.itemName;
    }

    public int getAmount()
    {
        return this.amount;
    }

    public DarkRiseItem asRiseItem()
    {
        return DarkRiseEconomy.getInstance().getItems().getItemById(this.itemName);
    }

    public ItemStack getItemStack()
    {
        final DarkRiseItem customItem = this.asRiseItem();
        if (customItem == null)
        {
            DarkRiseCrafting.getInstance().error("Can't find CustomItem named: " + this.itemName);
            DarkRiseCrafting.getInstance().error("Can't find CustomItem named: " + this.itemName);
            throw new NullPointerException("Can't find CustomItem named: " + this.itemName);
        }
        return customItem.getItem(this.amount);
    }

    public String toConfigString()
    {
        return this.itemName + ":" + this.amount;
    }

    public static RecipeItem fromConfigString(final String str)
    {
        try
        {
            final String[] srrs = StringUtils.split(str, ':');
            return new RecipeItem(srrs[0], Integer.parseInt(srrs[1]));
        }
        catch (final Exception e)
        {
            DarkRiseCrafting.getInstance().error("Error on loading configuration of RecipeItem: " + str + ", exception: " + e.getMessage() + ", more in console.");
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (! (o instanceof RecipeItem))
        {
            return false;
        }

        final RecipeItem that = (RecipeItem) o;

        return (this.amount == that.amount) && this.itemName.equals(that.itemName);

    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("itemName", this.itemName).append("amount", this.amount).toString();
    }

    @Override
    public int hashCode()
    {
        int result = this.itemName.hashCode();
        result = (31 * result) + this.amount;
        return result;
    }
}
