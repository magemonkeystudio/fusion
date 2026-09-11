package studio.magemonkey.fusion.crafting;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.fusion.data.queue.CraftingReceipt;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.gui.recipe.IngredientFingerprint;
import studio.magemonkey.fusion.util.ExperienceManager;
import java.util.*;

public final class CraftingCostService {
    public CraftingReceipt charge(Player player, Recipe recipe) {
        int xp = Math.max(0, recipe.getConditions().getExpCost());
        double money = Math.max(0, recipe.getConditions().getMoneyCost());
        var vault = CodexEngine.get().getVault();
        if (ExperienceManager.getTotalExperience(player) < xp
                || (money > 0 && (vault == null || !vault.canPay(player, money)))) return null;
        List<ItemStack> removed = takeItems(player, recipe.getItemsToTake());
        if (removed == null) return null;
        if (money > 0) vault.take(player, money);
        if (xp > 0) player.giveExp(-xp);
        return new CraftingReceipt(removed, xp, money);
    }

    public List<ItemStack> takeItems(Player player, Collection<ItemStack> requirements) {
        List<ItemStack> removed = new ArrayList<>();
        var inventory = player.getInventory();
        for (ItemStack required : requirements) {
            if (required == null || required.getType() == Material.AIR) continue;
            int needed = required.getAmount();
            var fingerprint = IngredientFingerprint.of(required);
            for (int slot = 0; slot < inventory.getSize() && needed > 0; slot++) {
                ItemStack present = inventory.getItem(slot);
                if (present == null || present.getType() == Material.AIR
                        || !fingerprint.equals(IngredientFingerprint.of(present))) continue;
                int count = Math.min(needed, present.getAmount());
                ItemStack actual = present.clone();
                actual.setAmount(count);
                removed.add(actual);
                ItemStack rest = present.clone();
                rest.setAmount(present.getAmount() - count);
                inventory.setItem(slot, rest.getAmount() == 0 ? null : rest);
                needed -= count;
            }
            if (needed > 0) { giveItems(player, removed); return null; }
        }
        return removed;
    }

    public void refund(Player player, CraftingReceipt receipt) {
        giveItems(player, receipt.getItems());
        if (receipt.getExperience() > 0) player.giveExp(receipt.getExperience());
        if (receipt.getMoney() > 0) CodexEngine.get().getVault().give(player, receipt.getMoney());
    }

    public void giveItems(Player player, Collection<ItemStack> items) {
        for (ItemStack item : items) {
            int remaining = item.getAmount();
            while (remaining > 0) {
                ItemStack part = item.clone();
                part.setAmount(Math.min(remaining, Math.max(1, item.getMaxStackSize())));
                remaining -= part.getAmount();
                player.getInventory().addItem(part).values().forEach(
                        overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
            }
        }
    }
}
