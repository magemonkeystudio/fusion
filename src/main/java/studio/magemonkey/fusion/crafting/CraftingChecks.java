package studio.magemonkey.fusion.crafting;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.recipes.*;
import studio.magemonkey.fusion.gui.recipe.IngredientFingerprint;
import studio.magemonkey.fusion.util.PlayerUtil;
import java.util.*;

public final class CraftingChecks {
    private CraftingChecks() { }
    public static Map<IngredientFingerprint, Integer> inventory(Player player) {
        Map<IngredientFingerprint, Integer> counts = new HashMap<>();
        for (ItemStack item : player.getInventory().getContents())
            if (item != null && item.getType() != Material.AIR)
                counts.merge(IngredientFingerprint.of(item), item.getAmount(), Integer::sum);
        return counts;
    }

    public static boolean canCraft(Player player, CraftingTable table, Recipe recipe, CraftingQueue queue) {
        try {
            if (!CalculatedRecipe.create(recipe, inventory(player), player, table).isCanCraft()) return false;
            var owner = PlayerLoader.getPlayer(player);
            if (owner.hasRecipeLimitReached(recipe)) return false;
            if (queue == null) return true;
            long queued = queue.getQueue().stream().filter(item -> item.getRecipePath().equals(recipe.getRecipePath())).count();
            if (recipe.getCraftingLimit() > 0
                    && queued + owner.getRecipeLimit(recipe).getLimit() >= recipe.getCraftingLimit()) return false;
            if (Cfg.instantCollect && recipe.getCraftingTime() <= 0) return true;
            int[] sizes = owner.getQueueSizes(table.getName(), queue.getCategory());
            String[] permissions = {"fusion.queue." + table.getName() + "." + queue.getCategory().getName() + ".limit",
                    "fusion.queue." + table.getName() + ".limit", "fusion.queue.limit"};
            for (int i = 0; i < permissions.length; i++) {
                int maximum = PlayerUtil.getPermOption(player, permissions[i]);
                if (maximum > 0 && sizes[i] >= maximum) return false;
            }
            return true;
        } catch (Exception exception) {
            studio.magemonkey.fusion.Fusion.getInstance().getLogger().warning("Cannot validate craft: " + exception.getMessage());
            return false;
        }
    }
}
