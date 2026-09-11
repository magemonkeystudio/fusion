package studio.magemonkey.fusion.crafting;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.data.queue.*;
import studio.magemonkey.fusion.data.recipes.*;

/** Application actions used by the queue GUI; no inventory slot state. */
public final class QueueCraftingService {
    private final CraftingCostService costs = new CraftingCostService();

    public CraftingResult enqueue(Player player, CraftingTable table, CraftingQueue queue, Recipe recipe) {
        if (!CraftingChecks.canCraft(player, table, recipe, queue)) return CraftingResult.REQUIREMENTS_NOT_MET;
        CraftingReceipt receipt = costs.charge(player, recipe);
        if (receipt == null) return CraftingResult.REQUIREMENTS_NOT_MET;
        QueueItem item = new QueueItem(-1, table.getName(), queue.getCategory(), recipe, System.currentTimeMillis(), 0);
        item.setReceipt(receipt);
        item.setPaidExpCost(receipt.getExperience());
        boolean added = false;
        try {
            // Validate encoding before publishing or accepting the craft.
            receipt.encode();
            CraftingResult result = FusionAPI.getEventServices().getQueueService().addQueueItemResult(player, table, queue, item);
            added = result == CraftingResult.SUCCESS;
            QueueProgress.refreshTimes(queue.getQueue());
            return result;
        } catch (RuntimeException exception) {
            studio.magemonkey.fusion.Fusion.getInstance().getLogger().warning("Cannot enqueue craft: " + exception.getMessage());
            return CraftingResult.STORAGE_FAILED;
        } finally {
            if (!added && item.getId() == -1) costs.refund(player, receipt);
        }
    }

    public CraftingResult cancel(Player player, CraftingTable table, CraftingQueue queue, long id) {
        QueueItem item = find(queue, id);
        if (item == null) return CraftingResult.NOT_FOUND;
        return FusionAPI.getEventServices().getQueueService().cancelQueueItemAndReport(player, table, queue,
                item, false, true, item.getReceipt() == null ? item.getRecipe().getItemsToTake() : item.getReceipt().getItems());
    }

    public CraftingResult collect(Player player, CraftingTable table, CraftingQueue queue, long id) {
        QueueItem item = find(queue, id);
        if (item == null) return CraftingResult.NOT_FOUND;
        if (!item.isDone()) return CraftingResult.NOT_READY;
        return FusionAPI.getEventServices().getQueueService().finishQueueItemAndReport(
                player, table, queue, item, item.getRecipe().getResults().getItems());
    }

    public QueueItem find(CraftingQueue queue, long id) {
        return queue.getQueue().stream().filter(item -> item.getId() == id).findFirst().orElse(null);
    }
}
