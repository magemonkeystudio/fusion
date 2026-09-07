package studio.magemonkey.fusion.data.recipes;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * A recipe ingredient that accepts any material belonging to a Bukkit {@link Tag}.
 * YAML format: {@code TAG_<tagname>:<amount>}, e.g. {@code TAG_PLANKS:4}.
 * Tag names match Minecraft namespaced-key keys (lowercase, underscores).
 */
public class RecipeTagItem implements RecipeItem {
    private final Tag<Material> tag;
    private final String        tagKey;
    private final int           amount;

    public RecipeTagItem(Tag<Material> tag, String tagKey, int amount) {
        this.tag    = tag;
        this.tagKey = tagKey;
        this.amount = amount;
    }

    public Tag<Material> getTag() {
        return tag;
    }

    public String getTagKey() {
        return tagKey;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public ItemStack getItemStack() {
        Set<Material> values = tag.getValues();
        Material rep = values.isEmpty() ? Material.OAK_PLANKS : values.iterator().next();
        return new ItemStack(rep, amount);
    }

    @Override
    public Object toConfig() {
        return "TAG_" + tagKey + ":" + amount;
    }
}
