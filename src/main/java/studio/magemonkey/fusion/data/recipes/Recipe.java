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
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.ProfessionConditions;
import studio.magemonkey.fusion.data.professions.ProfessionResults;
import studio.magemonkey.fusion.util.LevelFunction;
import studio.magemonkey.fusion.util.Utils;
import studio.magemonkey.risecore.legacy.util.DeserializationWorker;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
public class Recipe implements ConfigurationSerializable {

    @Getter
    private final CraftingTable table;

    @Setter
    private String name;

    @Setter
    private int craftingTime;
    @Setter
    private int craftingLimit = 0;
    @Setter
    private String category;

    private final ProfessionResults results;
    private final ProfessionConditions conditions;

    /* Things were recipes could be hidden */
    @Setter
    private Boolean hideNoPermission;
    @Setter
    private Boolean hideNoRank;
    @Setter
    private Boolean hideRecipeLimitReached;

    public Recipe(CraftingTable table, Map<String, Object> map) {
        this.table = table;
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.category = dw.getString("category");

        this.craftingTime = dw.getInt("craftingTime");
        this.craftingLimit = dw.getInt("craftingLimit");

        Map<String, Object> hiding = dw.getSection("hiding");
        this.hideNoPermission = (hiding != null && hiding.get("noPermission") != null) ? (boolean) hiding.get("noPermission") : null;
        this.hideNoRank = (hiding != null && hiding.get("noRank") != null) ? (boolean) hiding.get("noRank") : null;
        this.hideRecipeLimitReached = (hiding != null && hiding.get("recipeLimitReached") != null) ? (boolean) hiding.get("recipeLimitReached") : null;

        this.results = new ProfessionResults(name, dw);
        this.conditions = new ProfessionConditions(name, dw);
    }

    public Recipe(CraftingTable table, String name, String category, int craftingTime, int craftingLimit, ProfessionResults results, ProfessionConditions conditions, Boolean hideNoPermission, Boolean hideNoRank,Boolean hideLimitReached) {
        this.table = table;
        this.name = name;
        this.category = category;
        this.craftingTime = craftingTime;
        this.craftingLimit = craftingLimit;
        this.results = results;
        this.conditions = conditions;
        this.hideNoPermission = hideNoPermission;
        this.hideNoRank = hideNoRank;
        this.hideRecipeLimitReached = hideLimitReached;
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
            if (!Utils.hasCraftingPermission(p, getName())) {
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

    public List<ItemStack> getItemsToTake() {
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
                .append("craftingLimit", this.craftingLimit)
                .toString();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        SerializationBuilder builder = SerializationBuilder.start(6)
                .append("name", this.name)
                .append("craftingTime", this.craftingTime)
                .append("craftingLimit", this.craftingLimit);

        if(category != null) {
            builder.append("category", this.category);
        }

        Map<String, Object> hiding = new HashMap<>(3);
        if(hideNoPermission != null) hiding.put("noPermission", hideNoPermission);
        if(hideNoRank != null) hiding.put("noRank", hideNoRank);
        if(hideRecipeLimitReached != null) hiding.put("recipeLimitReached", hideRecipeLimitReached);

        if(!hiding.isEmpty()) {
            builder.append("hiding", hiding);
        }

        for(Entry<String, Object> entry : this.results.serialize().entrySet()) {
            builder.append(entry.getKey(), entry.getValue());
        }
        for(Entry<String, Object> entry : this.conditions.serialize().entrySet()) {
            builder.append(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    public static Recipe copy(Recipe recipe) {
        return new Recipe(recipe.getTable(), recipe.getName(), recipe.getCategory(), recipe.getCraftingTime(), recipe.getCraftingLimit(), ProfessionResults.copy(recipe.getResults()), ProfessionConditions.copy(recipe.getConditions()), recipe.getHideNoPermission(), recipe.getHideNoRank(), recipe.getHideRecipeLimitReached());
    }

    public boolean isHidden(Player player) {
        boolean isHidden = false;
        if(Cfg.hideRecipesNoPermission && !Utils.hasCraftingPermission(player, getName())) {
            isHidden = true;
            if(hideNoPermission != null) isHidden = hideNoPermission;
        } else if(!Utils.hasCraftingPermission(player, getName())){
            if(hideNoPermission != null) isHidden = hideNoPermission;
        }
        if(isHidden) return true;

        if(conditions.getRank() != null) {
            if (Cfg.hideRecipesNoPermission && !player.hasPermission("fusion.rank." + conditions.getRank())) {
                isHidden = true;
                if (hideNoPermission != null) isHidden = hideNoPermission;
            } else if (!Utils.hasCraftingPermission(player, getName())) {
                if (hideNoPermission != null) isHidden = hideNoPermission;
            }
            if(isHidden) return true;
        }

        FusionPlayer fusionPlayer = PlayerLoader.getPlayer(player);
        if(Cfg.hideRecipesLimitReached && fusionPlayer.hasRecipeLimitReached(this)) {
            isHidden = true;
            if(hideRecipeLimitReached != null) isHidden = hideRecipeLimitReached;
        } else if(fusionPlayer.hasRecipeLimitReached(this)) {
            if(hideRecipeLimitReached != null) isHidden = hideRecipeLimitReached;
        }
        return isHidden;
    }

    public String getRecipePath() {
        return table.getName() + "." + category + "." + name;
    }
}
