package studio.magemonkey.fusion;

import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.cfg.PConfigManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CalculatedRecipe {
    private final Recipe    recipe;
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

    public static CalculatedRecipe create(Recipe recipe,
                                          Collection<ItemStack> items,
                                          Player player,
                                          CraftingTable craftingTable) {
        Fusion pl = Fusion.getInstance();

        StringBuilder lore       = new StringBuilder(512);
        ItemStack     result     = recipe.getResult().getItemStack();
        List<String>  resultLore = result.getItemMeta().getLore();

        if ((resultLore != null) && !resultLore.isEmpty()) {
            resultLore.forEach((str) -> lore.append(str).append('\n'));
            lore.append('\n');
        }

        boolean canCraft = true;

        //Rank line
        String rankLine = null;
        if (recipe.getRank() != null && !player.hasPermission("fusion.rank." + recipe.getRank())) {
            canCraft = false;
            rankLine = MessageUtil.getMessageAsString("fusion.gui.rank." + recipe.getRank(), null);
        }

        String permissionLine;
        if (!Utils.hasCraftingPermission(player, recipe.name)) {
            canCraft = false;
            permissionLine = MessageUtil.getMessageAsString("fusion.gui.learned.false",
                    "fusion.gui.learned.false",
                    new MessageData("recipe", recipe),
                    new MessageData("player", player));
        } else {
            permissionLine = MessageUtil.getMessageAsString("fusion.gui.learned.true",
                    "fusion.gui.learned.true",
                    new MessageData("recipe", recipe),
                    new MessageData("player", player));
        }

        String moneyLine = null;
        if (recipe.price != 0) {
            if (!CodexEngine.get().getVault().canPay(player, recipe.price)) {
                canCraft = false;
                moneyLine = MessageUtil.getMessageAsString("fusion.gui.money.false",
                        "fusion.gui.money.false",
                        new MessageData("recipe", recipe),
                        new MessageData("player", player));
            } else {
                moneyLine = MessageUtil.getMessageAsString("fusion.gui.money.true",
                        "fusion.gui.money.true",
                        new MessageData("recipe", recipe),
                        new MessageData("player", player));
            }
        }

        String levelsLine = null;
        if (recipe.neededLevels != 0) {
            if (LevelFunction.getLevel(player, craftingTable) < recipe.neededLevels) {
                canCraft = false;
                levelsLine = MessageUtil.getMessageAsString("fusion.gui.xpLevels.false",
                        "fusion.gui.xpLevels.false",
                        new MessageData("recipe", recipe),
                        new MessageData("player", player),
                        new MessageData("level", LevelFunction.getLevel(player, craftingTable)));
            } else {
                levelsLine = MessageUtil.getMessageAsString("fusion.gui.xpLevels.true",
                        "fusion.gui.xpLevels.true",
                        new MessageData("recipe", recipe),
                        new MessageData("player", player),
                        new MessageData("level", LevelFunction.getLevel(player, craftingTable)));
            }
        }

        String xpLine = null;
        if (recipe.neededXp != 0) {
            if (Fusion.getExperienceManager().getExperience(player, craftingTable) < recipe.neededXp) {
                canCraft = false;
                xpLine = MessageUtil.getMessageAsString("fusion.gui.xp.false",
                        "fusion.gui.xp.false",
                        new MessageData("recipe", recipe),
                        new MessageData("player", player));
            } else {
                xpLine = MessageUtil.getMessageAsString("fusion.gui.xp.true",
                        "fusion.gui.xp.true",
                        new MessageData("recipe", recipe),
                        new MessageData("player", player));
            }
        }

        String masteryLine = null;
        if (recipe.mastery) {
            if (!PConfigManager.hasMastery(player, craftingTable.getName())) {
                canCraft = false;
                masteryLine = MessageUtil.getMessageAsString("fusion.gui.mastery.false",
                        "fusion.gui.mastery.false",
                        new MessageData("recipe", recipe),
                        new MessageData("craftingTable", craftingTable));
            } else {
                masteryLine = MessageUtil.getMessageAsString("fusion.gui.mastery.true",
                        "fusion.gui.mastery.true",
                        new MessageData("recipe", recipe),
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
            RecipeItem recipeItem         = it.next();
            ItemStack  recipeItemStack    = recipeItem.getItemStack();
            ItemStack  recipeItemStackOne = recipeItemStack.clone();
            recipeItemStackOne.setAmount(1);
            int eqAmount = 0;
            for (Map.Entry<ItemStack, Integer> entry : eqItems.entrySet()) {
                ItemStack item = entry.getKey().clone();
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
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
                        (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.ingredient.false"
                                : "fusion.gui.ingredientSimple.false",
                        (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.ingredient.false"
                                : "fusion.gui.ingredientSimple.false",
                        new MessageData("amount", 0),
                        new MessageData("recipeItem", recipeItem), new MessageData("player", player),
                        new MessageData("recipe", recipe), new MessageData("item", recipeItemStack))).append('\n');
                continue;
            }
            int patternAmount = recipeItem.getAmount();
            if (eqAmount < patternAmount) {
                canCraft = false;
                lore.append(MessageUtil.getMessageAsString(
                        (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.ingredient.false"
                                : "fusion.gui.ingredientSimple.false",
                        (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.ingredient.false"
                                : "fusion.gui.ingredientSimple.false",
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
                    (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.ingredient.true"
                            : "fusion.gui.ingredientSimple.true",
                    (recipeItem instanceof RecipeEconomyItem) ? "fusion.gui.ingredient.true"
                            : "fusion.gui.ingredientSimple.true",
                    new MessageData("amount", eqAmount),
                    new MessageData("recipeItem", recipeItem),
                    new MessageData("player", player),
                    new MessageData("recipe", recipe),
                    new MessageData("item", recipeItemStack))).append('\n');
        }

        String canCraftLine;
        if (canCraft) {
            canCraftLine = MessageUtil.getMessageAsString("fusion.gui.canCraft.true",
                    null,
                    new MessageData("recipe", recipe),
                    new MessageData("player", player));
        } else {
            canCraftLine = MessageUtil.getMessageAsString("fusion.gui.canCraft.false",
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
        lore.append(permissionLine);

        if (rankLine != null) {
            lore.append('\n').append(rankLine);
        }

        if (canCraftLine != null) {
            lore.append('\n').append(canCraftLine);
        }

        ItemStack icon     = result.clone();
        ItemMeta  itemMeta = icon.getItemMeta();
        itemMeta.setLore(Arrays.asList(StringUtils.split(lore.toString(), '\n')));
        icon.setItemMeta(itemMeta);

        return new

                CalculatedRecipe(recipe, icon, canCraft);

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
        ItemMeta     meta     = item.getItemMeta();
        List<String> itemLore = meta.getLore();
        itemLore.removeIf(s -> org.apache.commons.lang3.StringUtils.contains(s, "Crafted by") || s.trim().equals("")
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
