package studio.magemonkey.fusion.data.professions;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.recipes.*;
import studio.magemonkey.fusion.util.ExperienceManager;
import studio.magemonkey.fusion.util.InvalidPatternItemException;
import studio.magemonkey.fusion.util.Utils;

import java.util.*;

@Getter
public class CalculatedProfession {

    private final ProfessionConditions conditions;
    private final ItemStack icon;

    private final boolean canJoin;

    public CalculatedProfession(ProfessionConditions conditions, ItemStack icon, boolean canJoin) {
        this.conditions = conditions;
        this.icon = icon;
        this.canJoin = canJoin;
    }

    public static CalculatedProfession create(ProfessionConditions conditions,
                                              Collection<ItemStack> items,
                                              Player player,
                                              CraftingTable craftingTable) throws InvalidPatternItemException {
        try {
            StringBuilder lore = new StringBuilder(512);
            ItemStack defaultIcon = craftingTable.getIconItem().create();
            List<String> resultLore = defaultIcon.getItemMeta().getLore();

            if ((resultLore != null) && !resultLore.isEmpty()) {
                resultLore.forEach((str) -> lore.append(str).append('\n'));
                lore.append('\n');
            }

            String requirementLine = MessageUtil.getMessageAsString("fusion.gui.professions.requirementLine", "fusion.gui.professions.requirementLine");
            if(!requirementLine.isEmpty())
                lore.append(requirementLine).append('\n');

            boolean canJoin = true;

            //Rank line
            String rankLine = null;
            if (conditions.getRank() != null && !player.hasPermission("fusion.rank." + conditions.getRank())) {
                canJoin = false;
                rankLine = MessageUtil.getMessageAsString("fusion.gui.professions.rank." + conditions.getRank(), null);
            }


            String permissionLine = MessageUtil.getMessageAsString("fusion.gui.professions.learned.true",
                    "fusion.gui.professions.learned.true",
                    new MessageData("profession", conditions.getProfession()),
                    new MessageData("player", player));
            /* TODO: Might consider to make professions controllable by permissions
            if (!Utils.hasCraftingUsePermission(player, craftingTable.getName())) {
                canCraft = false;
                permissionLine = MessageUtil.getMessageAsString("fusion.gui.professions.learned.false",
                        "fusion.gui.professions.learned.false",
                        new MessageData("profession", profession),
                        new MessageData("player", player));
            } else {
                permissionLine = MessageUtil.getMessageAsString("fusion.gui.professions.learned.true",
                        "fusion.gui.professions.learned.true",
                        new MessageData("profession", profession),
                        new MessageData("player", player));
            } */

            String moneyLine = null;
            if (conditions.getMoneyCost() != 0) {
                if (!CodexEngine.get().getVault().canPay(player, conditions.getMoneyCost())) {
                    canJoin = false;
                    moneyLine = MessageUtil.getMessageAsString("fusion.gui.professions.money.false",
                            "fusion.gui.professions.money.false",
                            new MessageData("profession", conditions.getProfession()),
                            new MessageData("price", conditions.getMoneyCost()),
                            new MessageData("costs.money", conditions.getMoneyCost()),
                            new MessageData("player.money", CodexEngine.get().getVault().getBalance(player)));
                } else {
                    moneyLine = MessageUtil.getMessageAsString("fusion.gui.professions.money.true",
                            "fusion.gui.professions.money.true",
                            new MessageData("profession", conditions.getProfession()),
                            new MessageData("price", conditions.getMoneyCost()),
                            new MessageData("costs.money", conditions.getMoneyCost()),
                            new MessageData("player.money", CodexEngine.get().getVault().getBalance(player)));
                }
            }

            String xpLine = null;
            if (conditions.getExpCost() != 0) {
                if (ExperienceManager.getTotalExperience(player) < conditions.getExpCost()) {
                    canJoin = false;
                    xpLine = MessageUtil.getMessageAsString("fusion.professions.exp.false",
                            "fusion.gui.professions.exp.false",
                            new MessageData("profession", conditions.getProfession()),
                            new MessageData("exp", conditions.getExpCost()),
                            new MessageData("costs.exp", conditions.getExpCost()),
                            new MessageData("player", player));
                } else {
                    xpLine = MessageUtil.getMessageAsString("fusion.gui.professions.exp.true",
                            "fusion.gui.professions.exp.true",
                            new MessageData("profession", conditions.getProfession()),
                            new MessageData("exp", conditions.getExpCost()),
                            new MessageData("costs.exp", conditions.getExpCost()),
                            new MessageData("player", player));
                }
            }

            // Removed professionLevel and mastery since those cannot be checked for the same profession

            List<Map.Entry<Boolean, String>> conditionLines = conditions.getConditionLines(player);
            for (Map.Entry<Boolean, String> entry : conditionLines) {
                if (!entry.getKey()) {
                    canJoin = false;
                    break;
                }
            }
            Map<ItemStack, Integer> eqItems = Recipe.getItems(items);

            Collection<RecipeItem> localPattern = new HashSet<>(conditions.getRequiredItems());
            for (Iterator<RecipeItem> it = localPattern.iterator(); it.hasNext(); ) {
                RecipeItem professionItem = it.next();
                ItemStack professionItemStack = professionItem.getItemStack();
                ItemStack professionItemStackOne = professionItemStack.clone();
                professionItemStackOne.setAmount(1);
                int eqAmount = 0;
                for (Map.Entry<ItemStack, Integer> entry : eqItems.entrySet()) {
                    ItemStack item = entry.getKey().clone();
                    if (professionItem instanceof RecipeEconomyItem && ((RecipeEconomyItem) professionItem).asItemType()
                            .isInstance(item)) {
                        eqAmount = entry.getValue();
                    } else if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                        item = item.clone();
                        if (CalculatedRecipe.isSimilar(professionItemStackOne, item)) {
                            eqAmount = entry.getValue();
                            break;
                        }
                    }
                }
                int patternAmount = professionItem.getAmount();
                if (eqAmount == -1) {
                    canJoin = false;
                    lore.append(MessageUtil.getMessageAsString(
                            (professionItem instanceof RecipeEconomyItem) ? "fusion.gui.professions.ingredient.false"
                                    : "fusion.gui.professions.ingredientSimple.false",
                            (professionItem instanceof RecipeEconomyItem) ? "fusion.gui.professions.ingredient.false"
                                    : "fusion.gui.professions.ingredientSimple.false",
                            new MessageData("amount", 0),
                            new MessageData("requiredAmount", patternAmount),
                            new MessageData("professionItem", professionItem), new MessageData("player", player),
                            new MessageData("profession", conditions.getProfession()),
                            new MessageData("item", Utils.getItemName(professionItemStack)))).append('\n');
                    continue;
                }
                if (eqAmount < patternAmount) {
                    canJoin = false;
                    lore.append(MessageUtil.getMessageAsString(
                            (professionItem instanceof RecipeEconomyItem) ? "fusion.gui.professions.ingredient.false"
                                    : "fusion.gui.professions.ingredientSimple.false",
                            (professionItem instanceof RecipeEconomyItem) ? "fusion.gui.professions.ingredient.false"
                                    : "fusion.gui.professions.ingredientSimple.false",
                            new MessageData("amount", eqAmount),
                            new MessageData("requiredAmount", patternAmount),
                            new MessageData("professionItem", professionItem), new MessageData("player", player),
                            new MessageData("profession", conditions.getProfession()),
                            new MessageData("item", Utils.getItemName(professionItemStack)))).append('\n');
                    continue;
                }
                if (eqAmount == patternAmount) {
                    eqItems.remove(professionItem.getItemStack());
                }
                int rest = eqAmount - patternAmount;
                eqItems.put(professionItemStackOne, rest);
                it.remove();

                lore.append(MessageUtil.getMessageAsString(
                        (professionItem instanceof RecipeEconomyItem) ? "fusion.gui.professions.ingredient.true"
                                : "fusion.gui.professions.ingredientSimple.true",
                        (professionItem instanceof RecipeEconomyItem) ? "fusion.gui.professions.ingredient.true"
                                : "fusion.gui.professions.ingredientSimple.true",
                        new MessageData("amount", eqAmount),
                        new MessageData("requiredAmount", patternAmount),
                        new MessageData("player", player),
                        new MessageData("profession", conditions.getProfession()),
                        new MessageData("item", Utils.getItemName(professionItemStack)))).append('\n');
            }

            String canJoinLine;
            if (canJoin) {
                canJoinLine = MessageUtil.getMessageAsString("fusion.gui.professions.canJoin.true",
                        null,
                        new MessageData("profession", conditions.getProfession()),
                        new MessageData("player", player));
            } else {
                canJoinLine = MessageUtil.getMessageAsString("fusion.gui.professions.canJoin.false",
                        null,
                        new MessageData("profession", conditions.getProfession()),
                        new MessageData("player", player));
            }

            lore.append('\n');
            if (moneyLine != null) {
                lore.append(moneyLine).append('\n');
            }
            if (xpLine != null) {
                lore.append(xpLine).append('\n');
            }

            if (!conditionLines.isEmpty()) {
                for (Map.Entry<Boolean, String> entry : conditionLines) {
                    lore.append('\n').append(entry.getValue());
                }
            }

            lore.append("\n").append(permissionLine);

            if (rankLine != null) {
                lore.append('\n').append(rankLine);
            }

            if (canJoinLine != null) {
                lore.append('\n').append(canJoinLine);
            }

            ItemStack icon = defaultIcon.clone();
            ItemMeta itemMeta = icon.getItemMeta();
            itemMeta.setLore(Arrays.asList(StringUtils.split(lore.toString(), '\n')));
            icon.setItemMeta(itemMeta);

            return new CalculatedProfession(conditions, icon, canJoin);
        } catch (Exception e) {
            Fusion.getInstance().error("The profession-item seems not to be recognized. Please check your setup on the following profession '" + craftingTable.getName() + "':");
            Fusion.getInstance().error("Pattern Items: ");
            for (String patternItem : conditions.getRequiredItemNames()) {
                Fusion.getInstance().error("- " + patternItem);
            }
            Fusion.getInstance().error("Error on creating CalculatedProfession: " + e.getMessage());
            throw new InvalidPatternItemException(e);
        }
    }
}
