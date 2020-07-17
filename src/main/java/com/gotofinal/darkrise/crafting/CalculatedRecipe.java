package com.gotofinal.darkrise.crafting;

import com.gotofinal.darkrise.crafting.cfg.PConfigManager;
import me.travja.darkrise.core.legacy.util.Vault;
import me.travja.darkrise.core.legacy.util.message.MessageData;
import me.travja.darkrise.core.legacy.util.message.MessageUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CalculatedRecipe {
    private final Recipe recipe;
    private final ItemStack icon;

    private final boolean canCraft;

    CalculatedRecipe(Recipe recipe, ItemStack icon, boolean canCraft) {
        this.recipe = recipe;
        this.icon = icon;
        this.canCraft = canCraft;
    }

    public Recipe getRecipe() {
        return this.recipe;
    }

    public ItemStack getIcon() {
        return this.icon;
    }

    public boolean isCanCraft() {
        return this.canCraft;
    }

    public static CalculatedRecipe create(Recipe recipe, Collection<ItemStack> items, Player player, CraftingTable craftingTable) {
        DarkRiseCrafting pl = DarkRiseCrafting.getInstance();

        StringBuilder lore = new StringBuilder(512);
        ItemStack result = recipe.getResult().getItemStack();
        List<String> resultLore = result.getItemMeta().getLore();

        if ((resultLore != null) && !resultLore.isEmpty()) {
            resultLore.forEach((str) -> lore.append(str).append('\n'));
            lore.append('\n');
        }

        boolean canCraft = true;

        //Rank line
        String rankLine = null;
        if (recipe.getRank() != null && !player.hasPermission("crafting.rank." + recipe.getRank())) {
            canCraft = false;
            rankLine = MessageUtil.getMessageAsString("crafting.gui.rank." + recipe.getRank(), null);
        }

        String permissionLine;
        if (!Utils.hasCraftingPermission(player, recipe.name)) {
            canCraft = false;
            permissionLine = MessageUtil.getMessageAsString("crafting.gui.learned.false", "crafting.gui.learned.false", new MessageData("recipe", recipe),
                    new MessageData("player", player));
        } else {
            permissionLine = MessageUtil.getMessageAsString("crafting.gui.learned.true", "crafting.gui.learned.true", new MessageData("recipe", recipe),
                    new MessageData("player", player));
        }

        String moneyLine = null;
        if (recipe.price != 0) {
            if (!Vault.canPay(player, recipe.price)) {
                canCraft = false;
                moneyLine = MessageUtil.getMessageAsString("crafting.gui.money.false", "crafting.gui.money.false", new MessageData("recipe", recipe),
                        new MessageData("player", player));
            } else {
                moneyLine = MessageUtil.getMessageAsString("crafting.gui.money.true", "crafting.gui.money.true", new MessageData("recipe", recipe),
                        new MessageData("player", player));
            }
        }

        String levelsLine = null;
        if (recipe.neededLevels != 0) {
            if (LevelFunction.getLevel(player, craftingTable) < recipe.neededLevels) {
                canCraft = false;
                levelsLine = MessageUtil.getMessageAsString("crafting.gui.xpLevels.false", "crafting.gui.xpLevels.false", new MessageData("recipe", recipe),
                        new MessageData("player", player), new MessageData("level", LevelFunction.getLevel(player, craftingTable)));
            } else {
                levelsLine = MessageUtil.getMessageAsString("crafting.gui.xpLevels.true", "crafting.gui.xpLevels.true", new MessageData("recipe", recipe),
                        new MessageData("player", player), new MessageData("level", LevelFunction.getLevel(player, craftingTable)));
            }
        }

        String xpLine = null;
        if (recipe.neededXp != 0) {
            if (DarkRiseCrafting.getExperienceManager().getExperience(player, craftingTable) < recipe.neededXp) {
                canCraft = false;
                xpLine = MessageUtil.getMessageAsString("crafting.gui.xp.false", "crafting.gui.xp.false", new MessageData("recipe", recipe),
                        new MessageData("player", player));
            } else {
                xpLine = MessageUtil.getMessageAsString("crafting.gui.xp.true", "crafting.gui.xp.true", new MessageData("recipe", recipe),
                        new MessageData("player", player));
            }
        }

        String masteryLine = null;
        if(recipe.mastery) {
            if(!PConfigManager.hasMastery(player, craftingTable.getName())) {
                canCraft = false;
                masteryLine = MessageUtil.getMessageAsString("crafting.gui.mastery.false", "crafting.gui.mastery.false", new MessageData("recipe", recipe),
                        new MessageData("craftingTable", craftingTable));
            } else {
                masteryLine = MessageUtil.getMessageAsString("crafting.gui.mastery.true", "crafting.gui.mastery.true", new MessageData("recipe", recipe),
                        new MessageData("craftingTable", craftingTable));
            }
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
        for (Iterator<RecipeItem> it = localPattern.iterator(); it.hasNext(); ) {
            RecipeItem recipeItem = it.next();
            ItemStack recipeItemStack = recipeItem.getItemStack();
            ItemStack recipeItemStackOne = recipeItemStack.clone();
            recipeItemStackOne.setAmount(1);
            int eqAmount = 0;
            for (Map.Entry<ItemStack, Integer> entry : eqItems.entrySet()) {
                ItemStack item = entry.getKey().clone();
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    item = item.clone();
                    ItemMeta meta = item.getItemMeta();
                    List<String> itemLore = meta.getLore();
                    itemLore.removeIf(s -> org.apache.commons.lang.StringUtils.contains(s, "Crafted by"));
                    meta.setLore(itemLore);
                    item.setItemMeta(meta);

                    if (item.isSimilar(recipeItemStackOne)) {
                        eqAmount = entry.getValue();
                        break;
                    }
                }
            }

            if (eqAmount == -1) {
                canCraft = false;
                lore.append(MessageUtil.getMessageAsString(
                        (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.false" : "crafting.gui.ingredientSimple.false",
                        (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.false" : "crafting.gui.ingredientSimple.false",
                        new MessageData("amount", 0),
                        new MessageData("recipeItem", recipeItem), new MessageData("player", player),
                        new MessageData("recipe", recipe), new MessageData("item", recipeItemStack))).append('\n');
                continue;
            }
            int patternAmount = recipeItem.getAmount();
            if (eqAmount < patternAmount) {
                canCraft = false;
                lore.append(MessageUtil.getMessageAsString(
                        (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.false" : "crafting.gui.ingredientSimple.false",
                        (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.false" : "crafting.gui.ingredientSimple.false",
                        new MessageData("amount", eqAmount),
                        new MessageData("recipeItem", recipeItem), new MessageData("player", player),
                        new MessageData("recipe", recipe), new MessageData("item", recipeItemStack))).append('\n');
                continue;
            }
            if (eqAmount == patternAmount) {
                eqItems.remove(recipeItem.getItemStack());
            }
            int rest = eqAmount - patternAmount;
            eqItems.put(recipeItemStackOne, rest);
            it.remove();

            lore.append(MessageUtil.getMessageAsString((recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.true" : "crafting.gui.ingredientSimple.true",
                    (recipeItem instanceof RecipeEconomyItem) ? "crafting.gui.ingredient.true" : "crafting.gui.ingredientSimple.true",
                    new MessageData("amount", eqAmount),
                    new MessageData("recipeItem", recipeItem), new MessageData("player", player), new MessageData("recipe", recipe),
                    new MessageData("item", recipeItemStack))).append('\n');
        }
        String canCraftLine;
        if (canCraft) {
            canCraftLine = MessageUtil.getMessageAsString("crafting.gui.canCraft.true", null, new MessageData("recipe", recipe), new MessageData("player", player));
        } else {
            canCraftLine = MessageUtil.getMessageAsString("crafting.gui.canCraft.false", null, new MessageData("recipe", recipe), new MessageData("player", player));
        }

        lore.append('\n');
        if (moneyLine != null) {
            lore.append(moneyLine).append('\n');
        }
        if (levelsLine != null) {
            lore.append(levelsLine).append('\n');
        }
        if (xpLine != null) {
            lore.append(xpLine).append('\n');
        }
        if(masteryLine != null) {
            lore.append(masteryLine).append('\n');
        }
        lore.append(permissionLine);

        if (rankLine != null) {
            lore.append('\n').append(rankLine);
        }

        if (canCraftLine != null) {
            lore.append('\n').append(canCraftLine);
        }

        ItemStack icon = result.clone();
        ItemMeta itemMeta = icon.getItemMeta();
        itemMeta.setLore(Arrays.asList(StringUtils.split(lore.toString(), '\n')));
        icon.setItemMeta(itemMeta);

        return new CalculatedRecipe(recipe, icon, canCraft);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CalculatedRecipe)) {
            return false;
        }

        CalculatedRecipe that = (CalculatedRecipe) o;
        return new EqualsBuilder().append(this.recipe, that.recipe).append(this.icon, that.icon).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(this.recipe).append(this.icon).toHashCode();
    }
}
