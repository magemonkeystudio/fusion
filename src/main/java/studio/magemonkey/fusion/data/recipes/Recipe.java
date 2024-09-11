package studio.magemonkey.fusion.data.recipes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.ProfessionConditions;
import studio.magemonkey.fusion.data.professions.ProfessionResults;
import studio.magemonkey.fusion.util.LevelFunction;
import studio.magemonkey.fusion.util.Utils;
import studio.magemonkey.risecore.legacy.util.DeserializationWorker;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
public class Recipe implements ConfigurationSerializable {
    @Setter
    protected String name;

    @Setter
    protected int craftingTime;
    @Setter
    protected String category;

    protected final ProfessionResults results;
    protected final ProfessionConditions conditions;

    public Recipe(Map<String, Object> map) {
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.category = dw.getString("category");

        this.craftingTime = dw.getInt("craftingTime");
        this.results = new ProfessionResults(name, dw);
        this.conditions = new ProfessionConditions(name, dw);
    }

    public Recipe(String name, String category, int craftingTime, ProfessionResults results, ProfessionConditions conditions) {
        this.name = name;
        this.category = category;
        this.craftingTime = craftingTime;
        this.results = results;
        this.conditions = conditions;
    }

    public static Map<ItemStack, Integer> getItems(Collection<ItemStack> items) {
        Map<ItemStack, Integer> eqItems = new HashMap<>(20);
        for (ItemStack item : items) {
            int itemAmount = item.getAmount();
            item = item.clone();
            item.setAmount(1);
            Integer amount = eqItems.getOrDefault(item, 0);
            eqItems.put(item, amount + itemAmount);
        }
        return eqItems;
    }

    public static Map<ItemStack, Integer> getPattern(Collection<RecipeItem> items) {
        Map<ItemStack, Integer> localPattern = new HashMap<>(20);
        for (RecipeItem recipeItem : items) {
            ItemStack item = recipeItem.getItemStack();
            boolean added = false;

            int itemAmount = item.getAmount();
            item.setAmount(1);
            Integer amount = localPattern.getOrDefault(item, 0);
            localPattern.put(item, amount + itemAmount);
        }
        return localPattern;
    }

    public boolean isValid(Collection<ItemStack> items, Player p, CraftingTable craftingTable) {
        if (items.isEmpty()) {
            return false;
        }
        if (p != null) {
            if (!Utils.hasCraftingPermission(p, this.name)) {
                return false;
            }
            if (LevelFunction.getLevel(p, craftingTable) < this.conditions.getProfessionLevel()) {
                return false;
            }
            if (!this.conditions.isValid(PlayerLoader.getPlayer(p))) {
                return false;
            }
        }
        Map<ItemStack, Integer> eqItems = getItems(items);
        Map<ItemStack, Integer> localPattern = getPattern(this.conditions.getRequiredItems());
        for (Iterator<Entry<ItemStack, Integer>> iterator = localPattern.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<ItemStack, Integer> patternEntry = iterator.next();
            int eqAmount = eqItems.getOrDefault(patternEntry.getKey(), -1);
            if (eqAmount == -1) {
                return false;
            }
            ItemStack eqEntry = patternEntry.getKey();
            int patternAmount = patternEntry.getValue();
            if (eqAmount < patternAmount) {
                return false;
            }
            if (eqAmount == patternAmount) {
                eqItems.remove(eqEntry);
            }
            int rest = eqAmount - patternAmount;
            eqItems.put(eqEntry, rest);
            iterator.remove();
        }
        return localPattern.isEmpty();
    }

    public Collection<ItemStack> getItemsToTake() {
        return this.conditions.getRequiredItems().stream().map(RecipeItem::getItemStack).collect(Collectors.toList());
    }

    public double getMoneyCost() {
        return this.conditions.getMoneyCost();
    }

    public int getExpCost() {
        return this.conditions.getExpCost();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("name", this.name)
                .append("costs.money", this.conditions.getMoneyCost())
                .append("costs.experience", this.conditions.getExpCost())
                .append("costs.items", this.conditions.getRequiredItemNames())
                .append("results.item", this.getResults().getResultItem())
                .append("results.professionExp", this.getResults().getProfessionExp())
                .append("results.vanillaExp", this.getResults().getVanillaExp())
                .append("results.commands", this.getResults().getCommands())
                .append("craftingTime", this.craftingTime)
                .toString();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        SerializationBuilder builder = SerializationBuilder.start(6)
                .append("name", this.name)
                .append("craftingTime", this.craftingTime);

        if(category != null) {
            builder.append("category", this.category);
        }

        for(Map.Entry<String, Object> entry : this.results.serialize().entrySet()) {
            builder.append(entry.getKey(), entry.getValue());
        }
        for(Map.Entry<String, Object> entry : this.conditions.serialize().entrySet()) {
            builder.append(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    public static Recipe copy(Recipe recipe) {
        return new Recipe(recipe.getName(), recipe.getCategory(), recipe.getCraftingTime(), ProfessionResults.copy(recipe.getResults()), ProfessionConditions.copy(recipe.getConditions()));
    }

}
