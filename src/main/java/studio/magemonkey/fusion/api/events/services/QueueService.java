package studio.magemonkey.fusion.api.events.services;

import studio.magemonkey.fusion.crafting.CraftingResult;
import studio.magemonkey.fusion.crafting.CraftingCostService;
import studio.magemonkey.fusion.crafting.CraftingRewardService;
import studio.magemonkey.fusion.data.queue.CraftingReceipt;
import studio.magemonkey.fusion.data.queue.QueueProgress;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.api.events.QueueItemAddedEvent;
import studio.magemonkey.fusion.api.events.QueueItemFinishedEvent;
import studio.magemonkey.fusion.api.events.QueueItemRemovedEvent;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QueueService {

    /**
     * Call the QueueItemAddedEvent.
     *
     * @param player The player that adds the item to the queue.
     * @param table  The crafting table (profession) the player is using.
     * @param queue  The crafting queue the player is using.
     * @param item   The queue item that is added to the queue.
     */
    public void addQueueItem(Player player, CraftingTable table, CraftingQueue queue, QueueItem item) {
        addQueueItemAndReport(player, table, queue, item);
    }

    public boolean addQueueItemAndReport(Player player, CraftingTable table, CraftingQueue queue, QueueItem item) {
        return addQueueItemResult(player, table, queue, item) == CraftingResult.SUCCESS;
    }

    public CraftingResult addQueueItemResult(
            Player player, CraftingTable table, CraftingQueue queue, QueueItem item) {
        QueueItemAddedEvent event = new QueueItemAddedEvent(table.getName(), player, queue, item);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            item.setCraftinQueue(event.getQueue());
            if (!SQLManager.queues().setQueueItem(player.getUniqueId(), item)) return CraftingResult.STORAGE_FAILED;
            event.getQueue().getQueue().add(item);
            if (Cfg.instantCollect && item.getRecipe().getCraftingTime() <= 0) {
                item.markDone();
                event.getQueue().finishRecipe(item);
            }
            return CraftingResult.SUCCESS;
        }
        return CraftingResult.EVENT_CANCELLED;
    }

    /**
     * Call the QueueItemCanceledEvent.
     *
     * @param player      The player that cancels the item in the queue.
     * @param table       The crafting table (profession) the player is using.
     * @param queue       The crafting queue the player is using.
     * @param item        The queue item that is canceled.
     * @param finished    If the item is finished.
     * @param refunded    If the item ingredients will be refunded.
     * @param refundItems The items that will be refunded in case `refunded=true`.
     */
    public void cancelQueueItem(Player player,
                                CraftingTable table,
                                CraftingQueue queue,
                                QueueItem item,
                                boolean finished,
                                boolean refunded,
                                List<ItemStack> refundItems) {
        cancelQueueItemAndReport(player, table, queue, item, finished, refunded, refundItems);
    }

    public CraftingResult cancelQueueItemAndReport(
            Player player, CraftingTable table, CraftingQueue queue, QueueItem item,
            boolean finished, boolean refunded, List<ItemStack> refundItems) {
        if (!queue.getQueue().contains(item)) return CraftingResult.NOT_FOUND;
        QueueItemRemovedEvent event =
                new QueueItemRemovedEvent(table.getName(), player, queue, item, finished, refunded, refundItems);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            double refundMoney = item.getReceipt() == null ? item.getRecipe().getConditions().getMoneyCost() : item.getReceipt().getMoney();
            if (refunded && refundMoney > 0 && CodexEngine.get().getVault() == null) return CraftingResult.REQUIREMENTS_NOT_MET;
            boolean removed = finished
                    ? SQLManager.queues().claimQueueItem(player.getUniqueId(), item)
                    : SQLManager.queues().removeQueueItem(item);
            if (!removed) return CraftingResult.STORAGE_FAILED;
            if (event.isRefunded()) {
                var receipt = item.getReceipt();
                int xp = receipt == null ? item.getPaidExpCost() : receipt.getExperience();
                double money = receipt == null ? item.getRecipe().getConditions().getMoneyCost() : receipt.getMoney();
                var items = event.getRefundedItems() == null ? List.<ItemStack>of() : event.getRefundedItems();
                new CraftingCostService().refund(player,
                        new CraftingReceipt(items, xp, money));
            }
            event.getQueue().getQueue().remove(item);
            QueueProgress.refreshTimes(queue.getQueue());
            return CraftingResult.SUCCESS;
        }
        return CraftingResult.EVENT_CANCELLED;
    }
    /**
     * Call the QueueItemFinishedEvent.
     *
     * @param player      The player that finishes the item in the queue.
     * @param table       The crafting table (profession) the player is using.
     * @param queue       The crafting queue the player is using.
     * @param item        The queue item that is finished.
     * @param resultItems The result items of the queue item.
     */
    public void finishQueueItem(Player player,
                                CraftingTable table,
                                CraftingQueue queue,
                                QueueItem item,
                                List<RecipeItem> resultItems) {
        finishQueueItemAndReport(player, table, queue, item, resultItems);
    }

    public CraftingResult finishQueueItemAndReport(
            Player player, CraftingTable table, CraftingQueue queue, QueueItem item, List<RecipeItem> resultItems) {
        if (!queue.getQueue().contains(item)) return CraftingResult.NOT_FOUND;
        if (!item.isDone()) return CraftingResult.NOT_READY;
        QueueItemFinishedEvent event =
                new QueueItemFinishedEvent(table.getName(), player, queue, item, resultItems);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (event.getFusionPlayer().hasRecipeLimitReached(event.getQueueItem().getRecipe())) {
                cancelQueueItem(player,
                        table,
                        queue,
                        item,
                        false,
                        true,
                        item.getReceipt() == null ? item.getRecipe().getItemsToTake() : item.getReceipt().getItems());
                event.setCancelled(true);
                CodexEngine.get().getMessageUtil().sendMessage("fusion.error.recipeLimitReached", player,
                        new MessageData("recipe", event.getQueueItem().getRecipe().getName()),
                        new MessageData("amount",
                                event.getFusionPlayer().getRecipeLimit(event.getQueueItem().getRecipe()).getLimit()),
                        new MessageData("recipe.limit", event.getQueueItem().getRecipe().getCraftingLimit()),
                        new MessageData("limit", event.getQueueItem().getRecipe().getCraftingLimit()));
                return CraftingResult.REQUIREMENTS_NOT_MET;
            }
            // Claim the persisted row before delivering anything. A failed or cancelled
            // removal must not deliver a second reward on the next click/rejoin.
            var removal = cancelQueueItemAndReport(player, table, queue, item, true, false, List.of());
            if (removal != CraftingResult.SUCCESS) return removal;
            new CraftingRewardService().give(player, table, item.getRecipe(), event.getResultItems());
            return CraftingResult.SUCCESS;
        }
        return CraftingResult.EVENT_CANCELLED;
    }
}
