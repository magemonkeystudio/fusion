package studio.magemonkey.fusion;

import studio.magemonkey.codex.legacy.item.ItemBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.inventory.ItemStack;

public class RecipeCustomItem implements RecipeItem {
    private final ItemStack item;
    private final int       amount;
    private final boolean   simple;

    public RecipeCustomItem(ItemStack item, int amount, boolean simple) {
        this.item = item;
        item.setAmount(1);
        this.amount = amount;
        this.simple = simple;
    }

    @Override
    public int getAmount() {
        return this.amount;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack clone = this.item.clone();
        clone.setAmount(clone.getAmount() * this.amount);
        return clone;
    }

    @Override
    public Object toConfig() {
        if (this.simple) {
            return CUSTOM_PREFIX + this.item.getType().name() + ":" + this.item.getDurability() + (this.amount != 0 ? (
                    ":" + this.amount) : "");
        }
        return ItemBuilder.newItem(this.item).serialize();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecipeCustomItem)) {
            return false;
        }

        RecipeCustomItem that = (RecipeCustomItem) o;

        return (this.amount == that.amount) && this.item.equals(that.item);

    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("item", ItemBuilder.newItem(this.item).toString())
                .append("amount", this.amount).toString();
    }

    @Override
    public int hashCode() {
        int result = this.item.hashCode();
        result = (31 * result) + this.amount;
        return result;
    }
}
