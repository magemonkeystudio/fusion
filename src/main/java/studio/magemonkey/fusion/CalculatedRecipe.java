package studio.magemonkey.fusion;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.cfg.player.PlayerLoader;
import studio.magemonkey.fusion.util.Utils;

import java.util.*;

@Getter
public class CalculatedRecipe {
    private final Recipe recipe;

    private final ItemStack icon;
    private final boolean canCraft;

    CalculatedRecipe(Recipe recipe, ItemStack icon, boolean canCraft) {
        this.recipe = recipe;
        this.icon = icon;
        this.canCraft = canCraft;
    }

    public static CalculatedRecipe create(Recipe recipe,
                                          Collection<ItemStack> items,
                                          Player player,
                                          CraftingTable craftingTable) throws InvalidPatternItemException {
        try {
            Fusion pl = Fusion.getInstance();

            StringBuilder lore = new StringBuilder(512);
            ItemStack result = recipe.getResults().getResultItem().getItemStack();
            List<String> resultLore = result.getItemMeta().getLore();

            if ((resultLore != null) && !resultLore.isEmpty()) {
                resultLore.forEach((str) -> lore.append(str).append('\n'));
                lore.append('\n');
            }

            boolean canCraft = true;

            //Rank line
            String rankLine = null;
            if (recipe.getConditions().getRank() != null && !player.hasPermission("fusion.rank." + recipe.getConditions().getRank())) {
                canCraft = false;
                rankLine = MessageUtil.getMessageAsString("fusion.gui.recipes.rank." + recipe.getConditions().getRank(), null);
            }

            String permissionLine;
            if (!Utils.hasCraftingPermission(player, recipe.name)) {
                canCraft = false;
                permissionLine = MessageUtil.getMessageAsString("fusion.gui.recipes.learned.false",
                        "fusion.gui.recipes.learned.false",
                        new MessageData("recipe", recipe),
                        new MessageData("player", player));
            } else {
                permissionLine = MessageUtil.getMessageAsString("fusion.gui.recipes.learned.true",
                        "fusion.gui.recipes.learned.true",
                        new MessageData("recipe", recipe),
                        new MessageData("player", player));
            }

            String moneyLine = null;
            if (recipe.getConditions().getMoneyCost() != 0) {
                if (!CodexEngine.get().getVault().canPay(player, recipe.getConditions().getMoneyCost())) {
                    canCraft = false;
                    moneyLine = MessageUtil.getMessageAsString("fusion.gui.recipes.money.false",
                            "fusion.gui.recipes.money.false",
                            new MessageData("recipe", recipe),
                            new MessageData("price", recipe.getConditions().getMoneyCost()),
                            new MessageData("costs.money", recipe.getConditions().getMoneyCost()),
                            new MessageData("player.money", CodexEngine.get().getVault().getBalance(player)));
                } else {
                    moneyLine = MessageUtil.getMessageAsString("fusion.gui.recipes.money.true",
                            "fusion.gui.recipes.money.true",
                            new MessageData("recipe", recipe),
                            new MessageData("price", recipe.getConditions().getMoneyCost()),
                            new MessageData("costs.money", recipe.getConditions().getMoneyCost()),
                            new MessageData("player.money", CodexEngine.get().getVault().getBalance(player)));
                }
            }

            String xpLine = null;
            if (recipe.getConditions().getExpCost() != 0) {
                if (PlayerLoader.getPlayer(player.getUniqueId()).getExperience(craftingTable) < recipe.getConditions().getExpCost()) {
                    canCraft = false;
                    xpLine = MessageUtil.getMessageAsString("fusion.gui.recipes.xp.false",
                            "fusion.gui.recipes.exp.false",
                            new MessageData("recipe", recipe),
                            new MessageData("exp", recipe.getConditions().getExpCost()),
                            new MessageData("costs.exp", recipe.getConditions().getExpCost()),
                            new MessageData("player", player));
                } else {
                    xpLine = MessageUtil.getMessageAsString("fusion.gui.recipes.xp.true",
                            "fusion.gui.recipes.exp.true",
                            new MessageData("recipe", recipe),
                            new MessageData("exp", recipe.getConditions().getExpCost()),
                            new MessageData("costs.exp", recipe.getConditions().getExpCost()),
                            new MessageData("player", player));
                }
            }

            String levelsLine = null;
            if (recipe.getConditions().getProfessionLevel() != 0) {
                if (LevelFunction.getLevel(player, craftingTable) < recipe.getConditions().getProfessionLevel()) {
                    canCraft = false;
                    levelsLine = MessageUtil.getMessageAsString("fusion.gui.recipes.professionLevel.false",
                            "fusion.gui.recipes.professionLevel.false",
                            new MessageData("recipe", recipe),
                            new MessageData("player", player),
                            new MessageData("level", LevelFunction.getLevel(player, craftingTable)),
                            new MessageData("conditions.professionLevel", LevelFunction.getLevel(player, craftingTable)));
                } else {
                    levelsLine = MessageUtil.getMessageAsString("fusion.gui.recipes.professionLevel.true",
                            "fusion.gui.recipes.professionLevel.true",
                            new MessageData("recipe", recipe),
                            new MessageData("player", player),
                            new MessageData("level", LevelFunction.getLevel(player, craftingTable)),
                            new MessageData("conditions.professionLevel", LevelFunction.getLevel(player, craftingTable)));
                }
            }

            String masteryLine = null;
            if (recipe.getConditions().isMastery()) {
                if (!PlayerLoader.getPlayer(player).hasMastered(craftingTable.getName())) {
                    canCraft = false;
                    masteryLine = MessageUtil.getMessageAsString("fusion.gui.recipes.mastery.false",
                            "fusion.gui.recipes.mastery.false",
                            new MessageData("recipe", recipe),
                            new MessageData("craftingTable", craftingTable));
                } else {
                    masteryLine = MessageUtil.getMessageAsString("fusion.gui.recipes.mastery.true",
                            "fusion.gui.recipes.mastery.true",
                            new MessageData("recipe", recipe),
                            new MessageData("craftingTable", craftingTable));
                }
            }

            List<Map.Entry<Boolean, String>> conditionLines = recipe.getConditions().getConditionLines(player);
            for (Map.Entry<Boolean, String> entry : conditionLines) {
                if (!entry.getKey()) {
                    canCraft = false;
                    break;
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
            Collection<RecipeItem> localPattern = new HashSet<>(recipe.getConditions().getRequiredItems());
            for (Iterator<RecipeItem> it = localPattern.iterator(); it.hasNext(); ) {
                RecipeItem recipeItem = it.next();
                ItemStack recipeItemStack = recipeItem.getItemStack();
                ItemStack recipeItemStackOne = recipeItemStack.clone();
                recipeItemStackOne.setAmount(1);
                int eqAmount = 0;
                for (Map.Entry<ItemStack, Integer> entry : eqItems.entrySet()) {
                    ItemStack item = entry.getKey().clone();
                    if (recipeItem instanceof RecipeEconomyItem && ((RecipeEconomyItem) recipeItem).asItemType()
                            .isInstance(item)) {
                        eqAmount = entry.getValue();
                    } else if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                        item = item.clone();
//                    ItemMeta meta = item.getItemMeta();
//                    List<String> itemLore = meta.getLore();
//                    itemLore.removeIf(s -> org.apache.commons.lang3.StringUtils.contains(s, "Crafted by"));
//                    meta.setLore(itemLore);
//                    item.setItemMeta(meta);

//                    if (item.isSimilar(recipeItemStackOne)) {
                        if (CalculatedRecipe.isSimilar(recipeItemStackOne, item)) {
                            eqAmount = entry.getValue();
                            break;
                        }
                    }
                }

                if (eqAmount == -1) {
                    canCraft = false;
                    lore.append(MessageUtil.getMessageAsString(
                            (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.recipes.ingredient.false"
                                    : "fusion.gui.recipes.ingredientSimple.false",
                            (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.recipes.ingredient.false"
                                    : "fusion.gui.recipes.ingredientSimple.false",
                            new MessageData("amount", 0),
                            new MessageData("recipeItem", recipeItem), new MessageData("player", player),
                            new MessageData("recipe", recipe), new MessageData("item", recipeItemStack))).append('\n');
                    continue;
                }
                int patternAmount = recipeItem.getAmount();
                if (eqAmount < patternAmount) {
                    canCraft = false;
                    lore.append(MessageUtil.getMessageAsString(
                            (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.recipes.ingredient.false"
                                    : "fusion.gui.recipes.ingredientSimple.false",
                            (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.recipes.ingredient.false"
                                    : "fusion.gui.recipes.ingredientSimple.false",
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

                lore.append(MessageUtil.getMessageAsString(
                        (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.recipes.ingredient.true"
                                : "fusion.gui.recipes.ingredientSimple.true",
                        (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.recipes.ingredient.true"
                                : "fusion.gui.recipes.ingredientSimple.true",
                        new MessageData("amount", eqAmount),
                        new MessageData("recipeItem", recipeItem),
                        new MessageData("player", player),
                        new MessageData("recipe", recipe),
                        new MessageData("item", recipeItemStack))).append('\n');
            }

            String canCraftLine;
            if (canCraft) {
                canCraftLine = MessageUtil.getMessageAsString("fusion.gui.recipes.canCraft.true",
                        null,
                        new MessageData("recipe", recipe),
                        new MessageData("player", player));
            } else {
                canCraftLine = MessageUtil.getMessageAsString("fusion.gui.recipes.canCraft.false",
                        null,
                        new MessageData("recipe", recipe),
                        new MessageData("player", player));
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
            if (masteryLine != null) {
                lore.append(masteryLine).append('\n');
            }

            if(!conditionLines.isEmpty())
            {
                for (Map.Entry<Boolean, String> entry : conditionLines) {
                    lore.append('\n').append(entry.getValue());
                }
            }

            lore.append("\n").append(permissionLine);

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
        } catch (Exception e) {
            Fusion.getInstance().error("The recipe-item seems not to be recognized. Please check your setup on the following recipe '" + recipe.getName() + "':");
            Fusion.getInstance().error("Result: " + recipe.getResults().getResultName());
            Fusion.getInstance().error("Pattern Items: ");
            for (String patternItem : recipe.getConditions().getRequiredItemNames()) {
                Fusion.getInstance().error("- " + patternItem);
            }
            Fusion.getInstance().error("Error on creating CalculatedRecipe: " + e.getMessage());
            throw new InvalidPatternItemException(e);
        }
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

    public static boolean isSimilar(ItemStack is1, ItemStack is2) {

        //More relaxed comparison
        if (is1.getType() != is2.getType())
            return false;

        if (is1.hasItemMeta() && is2.hasItemMeta()) {
            ItemMeta im1 = is1.getItemMeta();
            ItemMeta im2 = is2.getItemMeta();
            if (im1.hasDisplayName() && im2.hasDisplayName())
                return im1.getDisplayName().equals(im2.getDisplayName());
        }

        return is1.isSimilar(is2);

        //More Strict comparison
        /*ItemStack recipeItemStackOne = is1.clone();
        recipeItemStackOne.setAmount(1);
        trimLore(recipeItemStackOne);

        ItemStack item = is2.clone();
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            item = item.clone();
            trimLore(item);

            return item.isSimilar(recipeItemStackOne);
        }
        return false;*/
    }

    public static void trimLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> itemLore = meta.getLore();
        if (itemLore == null) return;
        itemLore.removeIf(s -> org.apache.commons.lang3.StringUtils.contains(s, "Crafted by") || s.trim().isEmpty()
                || org.apache.commons.lang3.StringUtils.contains(s, "Craft Requirements")
                || org.apache.commons.lang3.StringUtils.contains(s, "Item")
                || org.apache.commons.lang3.StringUtils.contains(s, "Level Needed")
                || org.apache.commons.lang3.StringUtils.contains(s, "Mastery"));
        meta.setLore(itemLore);
        item.setItemMeta(meta);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(this.recipe).append(this.icon).toHashCode();
    }
}
