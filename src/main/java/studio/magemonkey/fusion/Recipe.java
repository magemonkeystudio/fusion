package studio.magemonkey.fusion;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.cfg.player.PlayerLoader;
import studio.magemonkey.fusion.cfg.professions.ProfessionConditions;
import studio.magemonkey.fusion.cfg.professions.ProfessionResults;
import studio.magemonkey.fusion.util.Utils;
import studio.magemonkey.risecore.legacy.util.DeserializationWorker;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@EqualsAndHashCode
public class Recipe implements ConfigurationSerializable {
    @Getter
    protected final String                     name;
    @Getter
    protected final Collection<DelayedCommand> commands = new ArrayList<>();
    @Getter
    protected final String                     rank;

    @Getter
    protected final int craftingTime;

    @Getter
    protected final String category;
    @Getter
    protected final ProfessionResults results;
    @Getter
    protected final ProfessionConditions conditions;

    public Recipe(Map<String, Object> map) {
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.category = dw.getString("category");

        this.rank = dw.getString("rank");
        this.craftingTime = dw.getInt("craftingTime");
        dw.deserializeCollection(this.commands, "commands", DelayedCommand.class);
        this.results = new ProfessionResults(name, dw);
        this.conditions = new ProfessionConditions(name, dw);
    }

    public Recipe(String name,
                  Collection<RecipeItem> pattern,
                  RecipeEconomyItem result,
                  double moneyCost,
                  int neededLevels,
                  int experienceCost,
                  String category, ProfessionResults results, ProfessionConditions conditions) {
        this.name = name;
        this.category = category;
        this.results = results;
        this.conditions = conditions;
        this.rank = "";
        this.craftingTime = 0;
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
            ItemStack item  = recipeItem.getItemStack();
            boolean   added = false;

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
            if(!this.conditions.isValid(PlayerLoader.getPlayer(p))) {
                return false;
            }
        }
        Map<ItemStack, Integer> eqItems      = getItems(items);
        Map<ItemStack, Integer> localPattern = getPattern(this.conditions.getRequiredItems());
        for (Iterator<Entry<ItemStack, Integer>> iterator = localPattern.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<ItemStack, Integer> patternEntry = iterator.next();
            int                       eqAmount     = eqItems.getOrDefault(patternEntry.getKey(), -1);
            if (eqAmount == -1) {
                return false;
            }
            ItemStack eqEntry       = patternEntry.getKey();
            int       patternAmount = patternEntry.getValue();
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
        return SerializationBuilder.start(6)
                .append("name", this.name)
                .append("craftingTime", this.craftingTime)

                .append("result.professionExp", this.results.getProfessionExp())
                .append("result.vanillaExp", this.results.getVanillaExp())
                .append("result.items", this.results.getResultItem().toConfig())
                .append("result.commands", this.results.getCommands().stream().map(DelayedCommand::serialize).collect(Collectors.toList()))

                .append("costs.money", this.conditions.getMoneyCost())
                .append("costs.experience", this.conditions.getExpCost())
                .append("costs.items", this.conditions.getRequiredItems().stream().map(RecipeItem::toConfig).collect(Collectors.toList()))
                .build();
    }

    /* TODO: Refactoring and Auto-Migration
    - price -> costs.money
    - neededXp -> costs.experience
    - cooldown -> craftingTime
     */

}
