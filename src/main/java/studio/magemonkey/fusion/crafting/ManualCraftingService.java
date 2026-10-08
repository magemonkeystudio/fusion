package studio.magemonkey.fusion.crafting;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.queue.CraftingReceipt;
import studio.magemonkey.fusion.data.recipes.*;

/** One manual operation per session. Closing the session cancels its owned task. */
public final class ManualCraftingService {
    private final Player player;
    private final CraftingTable table;
    private final CraftingCostService costs = new CraftingCostService();
    private BukkitTask task;
    private CraftingReceipt receipt;
    private Recipe recipe;
    private long deadline;
    private int duration;
    private long generation;

    public ManualCraftingService(Player player, CraftingTable table) { this.player = player; this.table = table; }
    public boolean isRunning() { return receipt != null; }
    public Recipe getRecipe() { return recipe; }
    public int getDuration() { return duration; }
    public long getRemainingSeconds() { return isRunning() ? Math.max(0, (deadline - System.currentTimeMillis() + 999) / 1000) : 0; }

    public CraftingResult start(Recipe selected, Runnable changed) {
        boolean same = recipe != null && recipe.getRecipePath().equals(selected.getRecipePath());
        cancel();
        if (same) { changed.run(); return CraftingResult.SUCCESS; }
        if (!CraftingChecks.canCraft(player, table, selected, null)) return CraftingResult.REQUIREMENTS_NOT_MET;
        receipt = costs.charge(player, selected);
        if (receipt == null) return CraftingResult.REQUIREMENTS_NOT_MET;
        recipe = selected;
        duration = Math.max(0, (int) Math.round(selected.getCraftingTime()
                * (1 - Fusion.getInstance().getPlayerCooldown(player))));
        deadline = System.currentTimeMillis() + duration * 1000L;
        long operation = generation;
        task = Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), () -> {
            if (operation == generation) complete(changed);
        }, Math.max(1, duration * 20L));
        changed.run();
        return CraftingResult.SUCCESS;
    }

    private void complete(Runnable changed) {
        task = null;
        if (!isRunning()) return;
        if (!player.isOnline()) { cancel(); return; }
        Recipe completed = recipe;
        try {
            var owner = PlayerLoader.getPlayer(player);
            if (owner.hasRecipeLimitReached(completed)) {
                cancel();
                CraftingResult.REQUIREMENTS_NOT_MET.report(player);
                changed.run();
                return;
            }
            if (completed.getCraftingLimit() > 0) owner.incrementLimit(completed);
        } catch (RuntimeException exception) {
            cancel();
            CraftingResult.STORAGE_FAILED.report(player);
            changed.run();
            return;
        }
        receipt = null;
        recipe = null;
        new CraftingRewardService().give(player, table, completed, completed.getResults().getItems());
        changed.run();
    }

    public void cancel() {
        generation++;
        if (task != null) task.cancel();
        task = null;
        CraftingReceipt refund = receipt;
        receipt = null;
        recipe = null;
        if (refund != null) costs.refund(player, refund);
    }
}
