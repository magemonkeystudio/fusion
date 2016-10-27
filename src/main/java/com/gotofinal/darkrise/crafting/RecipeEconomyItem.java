package com.gotofinal.darkrise.crafting;

import com.gotofinal.darkrise.economy.DarkRiseEconomy;
import com.gotofinal.darkrise.economy.DarkRiseItem;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.inventory.ItemStack;

public class RecipeEconomyItem implements RecipeItem
{
    private final String itemName;
    private final int    amount;

    public RecipeEconomyItem(String itemName, int amount)
    {
        this.itemName = itemName;
        this.amount = amount;
    }

    public String getItemName()
    {
        return this.itemName;
    }

    @Override
    public int getAmount()
    {
        return this.amount;
    }

    public DarkRiseItem asRiseItem()
    {
        return DarkRiseEconomy.getInstance().getItems().getItemById(this.itemName);
    }

    @Override
    public ItemStack getItemStack()
    {
        DarkRiseItem customItem = this.asRiseItem();
        if (customItem == null)
        {
            DarkRiseCrafting.getInstance().error("Can't find CustomItem named: " + this.itemName);
            DarkRiseCrafting.getInstance().error("Can't find CustomItem named: " + this.itemName);
            throw new NullPointerException("Can't find CustomItem named: " + this.itemName);
        }
        return customItem.getItem(this.amount);
    }

    @Override
    public String toConfig()
    {
        return this.itemName + ":" + this.amount;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (! (o instanceof RecipeEconomyItem))
        {
            return false;
        }

        RecipeEconomyItem that = (RecipeEconomyItem) o;

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
