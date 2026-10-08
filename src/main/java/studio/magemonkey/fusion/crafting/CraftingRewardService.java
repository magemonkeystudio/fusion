package studio.magemonkey.fusion.crafting;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.data.recipes.*;
import studio.magemonkey.fusion.util.PlayerUtil;
import java.util.*;

public final class CraftingRewardService {
    public void give(Player player, CraftingTable table, Recipe recipe, List<RecipeItem> results) {
        CraftingCostService items = new CraftingCostService();
        if (!recipe.getResults().hasCommandsOrItems()) {
            ItemStack result = recipe.getDivinityRecipeMeta() == null
                    ? recipe.getSettings().getRecipeItem().getItemStack().clone()
                    : recipe.getDivinityRecipeMeta().generateItem();
            if (player.hasPermission("fusion.craftedby." + recipe.getName())) {
                var meta = result.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add("§f - §eCrafted by: §f" + player.getName());
                    meta.setLore(lore);
                    result.setItemMeta(meta);
                }
            }
            items.giveItems(player, List.of(result));
        } else {
            for (RecipeItem result : results) {
                ItemStack stack = result.getItemStack();
                if (stack != null) items.giveItems(player, List.of(stack));
            }
            DelayedCommand.invoke(Fusion.getInstance(), player, recipe.getResults().getCommands());
        }
        long xp = recipe.getResults().getProfessionExp();
        xp += (long) (xp * PlayerUtil.getProfessionExpBonusThroughPermissions(player, table.getName()));
        if (xp > 0) FusionAPI.getEventServices().getProfessionService().giveProfessionExp(player, table, xp);
        if (recipe.getResults().getVanillaExp() > 0) player.giveExp(recipe.getResults().getVanillaExp());
    }
}
