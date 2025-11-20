package studio.magemonkey.fusion.data.recipes;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.CraftingRequirementsCfg;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.player.PlayerRecipeLimit;
import studio.magemonkey.fusion.gui.recipe.IngredientFingerprint;
import studio.magemonkey.fusion.util.ExperienceManager;
import studio.magemonkey.fusion.util.InvalidPatternItemException;
import studio.magemonkey.fusion.util.Utils;

import java.util.*;

/**
 * A “calculated” recipe icon + canCraft flag. We build lore lines (ingredients, money, xp, etc.)
 * once per relevant change, and store the final Icon + canCraft in a single object.
 */
@Getter
public class CalculatedRecipe {
    private final Recipe    recipe;
    private final ItemStack icon;
    private final boolean   canCraft;

    public CalculatedRecipe(Recipe recipe, ItemStack icon, boolean canCraft) {
        this.recipe = recipe;
        this.icon = icon;
        this.canCraft = canCraft;
    }

    /**
     * Builds a CalculatedRecipe for the given Recipe and a snapshot of the player’s inventory counts.
     *
     * @param recipe        The Recipe to evaluate
     * @param invCounts     a {@link java.util.Map}&lt;{@link studio.magemonkey.fusion.gui.recipe.IngredientFingerprint}, {@link java.lang.Integer}&gt;
     *                     representing the count of each ingredient in the player’s inventory
     * @param player        The player who is crafting
     * @param craftingTable The CraftingTable (for level checks)
     * @return a new CalculatedRecipe containing:
     *   - recipe (underlying Recipe)
     *   - icon (cloned ItemStack with assembled lore)
     *   - canCraft (true if player meets all requirements)
     * @throws InvalidPatternItemException if building fails
     */
    public static CalculatedRecipe create(Recipe recipe,
                                          Map<IngredientFingerprint, Integer> invCounts,
                                          Player player,
                                          CraftingTable craftingTable) throws InvalidPatternItemException {
        try {
            StringBuilder lore = new StringBuilder(512);

            // Base icon (without lore)
            ItemStack    iconResult = recipe.getSettings().getRecipeItem().getItemStack();
            ItemMeta     baseMeta   = iconResult.getItemMeta();
            List<String> resultLore = (baseMeta == null) ? Collections.emptyList() : baseMeta.getLore();

            // Append resultLore if exists
            if (resultLore != null && !resultLore.isEmpty()) {
                for (String line : resultLore) {
                    lore.append(line).append('\n');
                }
                lore.append(" ").append('\n');
            }

            // 1) “Requirement” header
            String requirementLine = CraftingRequirementsCfg.getCraftingRequirementLine("recipes");
            if (!requirementLine.isEmpty()) {
                lore.append(requirementLine).append('\n');
            }

            boolean canCraft = true;

            // 2) Permission (learned) check
            String recipePermissionLine = null;
            if (!Utils.hasCraftingPermission(player, recipe.getName())) {
                canCraft = false;
            }
            recipePermissionLine = CraftingRequirementsCfg.getLearned(
                    "recipes",
                    Utils.hasCraftingPermission(player, recipe.getName())
            );

            // 3) Money cost
            String moneyLine = null;
            if (recipe.getConditions().getMoneyCost() != 0) {
                if (CodexEngine.get().getVault() == null ||
                        !CodexEngine.get().getVault().canPay(player, recipe.getConditions().getMoneyCost())) {
                    canCraft = false;
                }
                double balance = (CodexEngine.get().getVault() == null)
                        ? 0.0
                        : CodexEngine.get().getVault().getBalance(player);
                moneyLine = CraftingRequirementsCfg.getMoney(
                        "recipes",
                        (int) balance,
                        recipe.getConditions().getMoneyCost()
                );
            }

            // 4) XP cost
            String expLine = null;
            if (recipe.getConditions().getExpCost() != 0) {
                int totalExp = ExperienceManager.getTotalExperience(player);
                if (totalExp < recipe.getConditions().getExpCost()) {
                    canCraft = false;
                }
                expLine = CraftingRequirementsCfg.getExp(
                        "recipes",
                        totalExp,
                        recipe.getConditions().getExpCost()
                );
            }

            // 5) Profession level
            String levelsLine = null;
            if (recipe.getConditions().getProfessionLevel() != 0) {
                int profLevel = recipe.getTable().getLevelFunction().getLevel(player);
                if (profLevel < recipe.getConditions().getProfessionLevel()) {
                    canCraft = false;
                }
                levelsLine = CraftingRequirementsCfg.getProfessionLevel(
                        "recipes",
                        profLevel,
                        recipe.getConditions().getProfessionLevel()
                );
            }

            // 6) Mastery
            String masteryLine = null;
            if (recipe.getConditions().isMastery()) {
                boolean hasMastery = PlayerLoader.getPlayer(player)
                        .hasMastered(craftingTable.getName());
                if (!hasMastery) {
                    canCraft = false;
                }
                masteryLine = CraftingRequirementsCfg.getMastery(
                        "recipes",
                        hasMastery,
                        recipe.getConditions().isMastery()
                );
            }

            // 7) Crafting limit
            String limitLine = null;
            if (recipe.getCraftingLimit() > 0) {
                PlayerRecipeLimit limit = PlayerLoader.getPlayer(player).getRecipeLimit(recipe);
                if (limit.getLimit() > 0 &&
                        limit.getCooldownTimestamp() > 0 &&
                        !limit.hasCooldown()) {
                    limit.resetLimit();
                    Bukkit.getConsoleSender().sendMessage(
                            "§aResetting limit for " + player.getName() + " on " + recipe.getRecipePath()
                    );
                }
                if (limit.getLimit() >= recipe.getCraftingLimit()) {
                    canCraft = false;
                }
                limitLine = CraftingRequirementsCfg.getLimit(
                        "recipes",
                        limit.getLimit(),
                        recipe.getCraftingLimit()
                );
            }

            // 8) Custom condition lines
            List<Map.Entry<Boolean, String>> conditionLines =
                    recipe.getConditions().getConditionLines(player);
            for (Map.Entry<Boolean, String> entry : conditionLines) {
                if (!entry.getKey()) {
                    canCraft = false;
                    break;
                }
            }

            //
            // ─── 9) Ingredient check using invCounts ───
            //
            Collection<RecipeItem> localPattern = new LinkedHashSet<>(
                    recipe.getConditions().getRequiredItems()
            );

            for (Iterator<RecipeItem> it = localPattern.iterator(); it.hasNext(); ) {
                RecipeItem required = it.next();

                // We only compare a single “unit” for matching:
                ItemStack single = required.getItemStack().clone();
                single.setAmount(1);

                IngredientFingerprint reqKey = IngredientFingerprint.of(single);
                int                   have   = invCounts.getOrDefault(reqKey, 0);
                int                   need   = required.getAmount();

                if (have < need) {
                    canCraft = false;
                    lore.append(
                            CraftingRequirementsCfg.getIngredientLine("recipes", required, have, need)
                    ).append('\n');
                    continue;
                }

                // Subtract used quantity so overlapping items are handled correctly
                invCounts.put(reqKey, have - need);

                lore.append(
                        CraftingRequirementsCfg.getIngredientLine("recipes", required, have, need)
                ).append('\n');
            }

            // 10) Append summary lines
            String canCraftLine = CraftingRequirementsCfg.getCanCraft(canCraft);
            lore.append('\n');
            if (moneyLine != null) lore.append(moneyLine).append('\n');
            if (levelsLine != null) lore.append(levelsLine).append('\n');
            if (expLine != null) lore.append(expLine).append('\n');
            if (masteryLine != null) lore.append(masteryLine).append('\n');
            if (limitLine != null) lore.append(limitLine).append('\n');
            if (!conditionLines.isEmpty()) {
                String conditionLine = CraftingRequirementsCfg.getCraftingConditionLine("recipes");
                if (!conditionLine.isEmpty()) {
                    lore.append(conditionLine).append('\n');
                }
                for (Map.Entry<Boolean, String> e : conditionLines) {
                    lore.append('\n').append(e.getValue());
                }
            }
            lore.append('\n').append(recipePermissionLine);
            lore.append('\n').append(canCraftLine);

            // Build final icon + lore
            ItemStack icon = iconResult.clone();
            ItemMeta  im   = icon.getItemMeta();
            im.setLore(Arrays.asList(StringUtils.split(lore.toString(), '\n')));
            icon.setItemMeta(im);

            return new CalculatedRecipe(recipe, icon, canCraft);
        } catch (Exception e) {
            Fusion.getInstance().error(
                    "Error creating CalculatedRecipe for '" + recipe.getName() + "': " + e.getMessage()
            );
            throw new InvalidPatternItemException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalculatedRecipe that)) return false;
        return new EqualsBuilder()
                .append(this.recipe, that.recipe)
                .append(this.icon, that.icon)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.recipe)
                .append(this.icon)
                .toHashCode();
    }

    /**
     * Unchanged “isSimilar” from before—compares two ItemStacks in a relaxed manner.
     */
    public static boolean isSimilar(ItemStack is1, ItemStack is2) {
        if (is1.getType() != is2.getType()) return false;

        ItemMeta im1 = is1.getItemMeta();
        ItemMeta im2 = is2.getItemMeta();
        if ((im1 == null && im2 != null) || (im1 != null && im2 == null)) return false;
        if (im1 == null) return true;

        // Check for name
        if (im1.hasDisplayName()) {
            String displayName1 = im1.getDisplayName().trim();
            String displayName2 = im2.hasDisplayName() ? im2.getDisplayName().trim() : "";
            if (!displayName1.equals(displayName2)) return false;
        } else if (!im1.hasDisplayName() && im2.hasDisplayName()) {
            return false;
        }

        boolean isValid = true;
        // Check for lore
        if (im1.hasLore()) {
            List<String> lore1 = im1.getLore();
            List<String> lore2 = im2.hasLore() ? im2.getLore() : new ArrayList<>();
            if (lore1 == null || lore2 == null) {
                isValid = false;
            } else {
                if (lore1.isEmpty() || (lore1.size() != lore2.size())) {
                    isValid = false;
                } else {
                    for (int i = 0; i < lore1.size(); i++) {
                        if (!lore1.get(i).equals(lore2.get(i))) {
                            isValid = false;
                            break;
                        }
                    }
                }
            }
        }

        // Check for enchantments
        if (im1 instanceof EnchantmentStorageMeta storage1) {
            EnchantmentStorageMeta                            storage2 = (EnchantmentStorageMeta) im2;
            Map<org.bukkit.enchantments.Enchantment, Integer> ench1    = storage1.getStoredEnchants();
            Map<org.bukkit.enchantments.Enchantment, Integer> ench2    = storage2.getStoredEnchants();

            if (ench1.size() != ench2.size()) isValid = false;
            for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : ench1.entrySet()) {
                if (!ench2.containsKey(entry.getKey()) ||
                        !ench2.get(entry.getKey()).equals(entry.getValue())) {
                    isValid = false;
                }
            }
        } else {
            if (im1.hasEnchants()) {
                Map<org.bukkit.enchantments.Enchantment, Integer> ench1 = im1.getEnchants();
                Map<org.bukkit.enchantments.Enchantment, Integer> ench2 = im2.getEnchants();
                if (ench1.size() != ench2.size()) isValid = false;
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : ench1.entrySet()) {
                    if (!ench2.containsKey(entry.getKey()) ||
                            !ench2.get(entry.getKey()).equals(entry.getValue())) {
                        isValid = false;
                    }
                }
            }
        }

        // TODO make sure this works with Divinity's specific update logic. Until then, skip it.
        // Divinity Logic -> https://github.com/magemonkeystudio/divinity/blob/dev/src/main/java/studio/magemonkey/divinity/manager/listener/object/ItemUpdaterListener.java#L95
        /*if (!im1.getItemFlags().isEmpty()) {
            if (im1.getItemFlags().size() != im2.getItemFlags().size()) isValid = false;
            for (ItemFlag flag : im1.getItemFlags()) {
                if (!im2.getItemFlags().contains(flag)) isValid = false;
            }
        }*/

        // Check for custom model data
        if (im1.hasCustomModelData() && im2.hasCustomModelData()) {
            if (im1.getCustomModelData() != im2.getCustomModelData()) isValid = false;
        } else if (im1.hasCustomModelData() || im2.hasCustomModelData()) {
            isValid = false;
        }

        // Check if unbreakable
        if (im1.isUnbreakable()) {
            if (im2.isUnbreakable()) isValid = false;
        }

        // Check for durability if instanceof Damageable
        // TODO

        // Final fallback to Bukkit’s built‐in check
        return isValid || is1.isSimilar(is2);
    }
}
