package studio.magemonkey.fusion;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.util.SerializationBuilder;
import studio.magemonkey.fusion.cfg.player.PlayerLoader;
import studio.magemonkey.risecore.legacy.util.DeserializationWorker;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@EqualsAndHashCode
public class Recipe implements ConfigurationSerializable {
    @Getter
    protected final String                     name;
    protected final LinkedList<RecipeItem>     pattern;
    @Getter
    protected final RecipeItem                 result;
    @Getter
    protected final double                     price;
    @Getter
    protected final int                        neededLevels;
    @Getter
    protected final int                        neededXp;
    @Getter
    protected final int                        xpGain;
    @Getter
    protected final boolean                    mastery;
    @Getter
    protected final Collection<DelayedCommand> commands = new ArrayList<>();
    @Getter
    protected final String                     rank;

    @Getter
    protected final int cooldown;

    public Recipe(Map<String, Object> map) {
        DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.result = RecipeItem.fromConfig(map.get("result"));
//        this.pattern = dw.getStringList("pattern").stream().map(RecipeItem::fromConfig).collect(Collectors.toList());
        this.pattern = dw.getStringList("pattern")
                .stream()
                .map(RecipeItem::fromConfig)
                .collect(Collectors.toCollection(LinkedList::new));
        this.price = dw.getDouble("price", 0);
        this.neededLevels = dw.getInt("neededLevels", 0);
        this.neededXp = dw.getInt("neededXp", 0);
        this.xpGain = dw.getInt("xpGain", 0);
        this.mastery = dw.getBoolean("mastery");
        this.rank = dw.getString("rank");
        this.cooldown = dw.getInt("cooldown");
        dw.deserializeCollection(this.commands, "commands", DelayedCommand.class);

        if (result == null) {
            throw new IllegalArgumentException("Invalid result: " + map.get("result"));
        }
    }

    public Recipe(String name,
                  Collection<RecipeItem> pattern,
                  RecipeEconomyItem result,
                  double price,
                  int neededLevels,
                  int neededXp) {
        this.name = name;
        this.pattern = new LinkedList<>(pattern);
        this.result = result;
        this.price = price;
        this.neededLevels = neededLevels;
        this.neededXp = neededXp;
        this.xpGain = 0;
        this.mastery = false;
        this.rank = "";
        this.cooldown = 0;
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
            if (LevelFunction.getLevel(p, craftingTable) < this.neededLevels) {
                return false;
            }
            if (PlayerLoader.getPlayer(p.getUniqueId()).getExperience(craftingTable) < this.neededXp) {
                return false;
            }
            if (!CodexEngine.get().getVault().canPay(p, this.price)) {
                return false;
            }
        }
        Map<ItemStack, Integer> eqItems      = getItems(items);
        Map<ItemStack, Integer> localPattern = getPattern(this.pattern);
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

    public Collection<RecipeItem> getPattern() {
        return this.pattern;
    }

    public Collection<ItemStack> getItemsToTake() {
        return this.pattern.stream().map(RecipeItem::getItemStack).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("name", this.name)
                .append("price", this.price)
                .append("pattern", this.pattern)
                .append("result", this.result)
                .append("cooldown", this.cooldown)
                .toString();
    }

    @Override
    public Map<String, Object> serialize() {
        return SerializationBuilder.start(6)
                .append("name", this.name)
                .append("result", this.result.toConfig())
                .append("price", this.price)
                .append("neededXp", this.neededXp)
                .append("neededLevels", this.neededLevels)
                .append("xpGain", this.xpGain)
                .append("pattern", this.pattern.stream().map(RecipeItem::toConfig).collect(Collectors.toList()))
                .append("cooldown", this.cooldown)
                .append("commands", this.commands)
                .build();
    }
}
