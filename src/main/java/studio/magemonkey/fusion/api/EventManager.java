package studio.magemonkey.fusion.api;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.events.*;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.professions.Profession;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.util.ExperienceManager;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EventManager {

    /**
     * Call the ProfessionJoinEvent.
     * @param professionName The name of the profession.
     * @param player The player that wants to join the profession.
     * @param moneyCost The money cost to join the profession.
     * @param expCost The experience cost to join the profession.
     */
    public void callProfessionJoinEvent(String professionName, Player player, double moneyCost, int expCost) {
        ProfessionJoinEvent event = new ProfessionJoinEvent(professionName, player);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            event.getFusionPlayer().addProfession(new Profession(-1, player.getUniqueId(), professionName, 0, false, true));
            if (moneyCost > 0)
                CodexEngine.get().getVault().take(player, moneyCost);
            if (expCost > 0)
                ExperienceManager.setTotalExperience(player, (ExperienceManager.getTotalExperience(player) - expCost));

            MessageData[] data = {
                    new MessageData("profession", professionName),
                    new MessageData("costs.money", moneyCost),
                    new MessageData("costs.experience", expCost),
                    new MessageData("unlocked", event.getFusionPlayer().getJoinedProfessions().size()),
                    new MessageData("limit", PlayerUtil.getPermOption(player, "fusion.limit")),
                    new MessageData("bal", CodexEngine.get().getVault().getBalance(player))
            };

            MessageUtil.sendMessage("fusion.unlockedProfession", player, data);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }


    /**
     * Call the ProfessionLeaveEvent.
     * @param table The crafting table (profession) the player wants to leave.
     * @param player The player that wants to leave the profession.
     */
    public void callProfessionLeaveEvent(CraftingTable table, Player player) {
        ProfessionLeaveEvent event = new ProfessionLeaveEvent(table.getName(), player);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            event.getFusionPlayer().removeProfession(table);
            MessageUtil.sendMessage("fusion.forgotten",
                    player,
                    new MessageData("sender", player.getName()),
                    new MessageData("craftingTable", table));
        }
    }

    /**
     * Call the ProfessionGainXpEvent.
     * @param player The player that gains the experience.
     * @param table The crafting table (profession) the player gains experience in.
     * @param xp The amount of experience the player gains.
     */
    public void callProfessionGainXpEvent(Player player, CraftingTable table, double xp) {
        ProfessionGainXpEvent event = new ProfessionGainXpEvent(table.getName(), player, xp);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            FusionPlayer fusionPlayer = event.getFusionPlayer();
            int previousLevel = fusionPlayer.getProfession(table).getLevel();
            FusionAPI.getPlayerManager().getPlayer(player).addExperience(table.getName(), event.getGainedExp());
            int newLevel = fusionPlayer.getProfession(table).getLevel();
            if (newLevel != previousLevel) {
                callLevelUpEvent(player, table, previousLevel, newLevel);
            }
        }
    }

    /**
     * Call the ProfessionLevelUpEvent.
     * @param player The player that levels up.
     * @param table The crafting table (profession) the player levels up in.
     * @param previousLevel The previous level of the player.
     * @param newLevel The new level of the player.
     */
    public void callLevelUpEvent(Player player, CraftingTable table, int previousLevel, int newLevel) {
        ProfessionLevelUpEvent event = new ProfessionLevelUpEvent(table.getName(), player, previousLevel, newLevel);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            event.getFusionPlayer().getProfession(table).setLevel(newLevel);
            MessageUtil.sendMessage("fusion.levelup",
                    player,
                    new MessageData("craftingTable", table),
                    new MessageData("level", newLevel));
        }
    }

    /**
     * Call the ProfessionMasteryEvent.
     * @param professionName The name of the profession.
     * @param player The player that masters the profession.
     * @param isMastered If the player has mastered the profession.
     */
    public void callProfessionMasteryEvent(String professionName, Player player, boolean isMastered) {
        ProfessionMasteryEvent event = new ProfessionMasteryEvent(professionName, player, isMastered);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            event.getFusionPlayer().getProfession(professionName).setMastered(isMastered);
            if (event.isHasMastered()) {
                MessageUtil.sendMessage("fusion.mastered", player, new MessageData("sender", player), new MessageData("craftingTable", event.getCraftingTable()));
            }
        }
    }

    /**
     * Call the QueueItemAddedEvent.
     * @param player The player that adds the item to the queue.
     * @param table The crafting table (profession) the player is using.
     * @param queue The crafting queue the player is using.
     * @param item The queue item that is added to the queue.
     */
    public void callQueueItemAddedEvent(Player player, CraftingTable table, CraftingQueue queue, QueueItem item) {
        QueueItemAddedEvent event = new QueueItemAddedEvent(table.getName(), player, queue, item);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            item.setCraftinQueue(event.getQueue());
            event.getQueue().getQueue().add(item);
        }
    }

    /**
     * Call the QueueItemCanceledEvent.
     * @param player The player that cancels the item in the queue.
     * @param table The crafting table (profession) the player is using.
     * @param queue The crafting queue the player is using.
     * @param item The queue item that is canceled.
     * @param finished If the item is finished.
     * @param refunded If the item ingredients will be refunded.
     * @param refundItems The items that will be refunded in case `refunded=true`.
     */
    public void callQueueItemCanceledEvent(Player player, CraftingTable table, CraftingQueue queue, QueueItem item, boolean finished, boolean refunded, List<ItemStack> refundItems) {
        QueueItemRemovedEvent event = new QueueItemRemovedEvent(table.getName(), player, queue, item, finished, refunded, refundItems);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (event.isRefunded()) {
                CodexEngine.get().getVault().give(event.getPlayer(), item.getRecipe().getConditions().getMoneyCost());

                Collection<ItemStack> refunds = event.getRefundedItems();
                for (ItemStack refundItem : refunds) {
                    // If those are not stacked natively, we need to give them one by one
                    if(refundItem.getMaxStackSize() < refundItem.getAmount()) {
                        for (int i = 0; i < refundItem.getAmount(); i++) {
                            ItemStack singleItem = refundItem.clone();
                            singleItem.setAmount(1);
                            Collection<ItemStack> notAdded = player.getInventory().addItem(singleItem).values();
                            if (!notAdded.isEmpty()) {
                                for (ItemStack _item : notAdded) {
                                    Objects.requireNonNull(player.getLocation().getWorld())
                                            .dropItemNaturally(player.getLocation(), _item);
                                }
                            }
                        }
                    } else {
                        Collection<ItemStack> notAdded = player.getInventory().addItem(refundItem).values();
                        if (!notAdded.isEmpty()) {
                            for (ItemStack _item : notAdded) {
                                Objects.requireNonNull(player.getLocation().getWorld())
                                        .dropItemNaturally(player.getLocation(), _item);
                            }
                        }
                    }
                }
            }
            event.getQueue().getQueue().remove(item);
            for (Map.Entry<Integer, QueueItem> entry : event.getQueue().getQueuedItems().entrySet()) {
                if (entry.getValue().equals(item)) {
                    event.getQueue().getQueuedItems().remove(entry.getKey());
                    break;
                }
            }
            if (!SQLManager.queues().removeQueueItem(item)) {
                Fusion.getInstance().getLogger().warning("Failed to remove queue item from SQL");
            }
        }
    }

    /**
     * Call the QueueItemFinishedEvent.
     * @param player The player that finishes the item in the queue.
     * @param table The crafting table (profession) the player is using.
     * @param queue The crafting queue the player is using.
     * @param item The queue item that is finished.
     * @param resultItem The result item of the queue item.
     * @param resultAmount The amount of the result item.
     */
    public void callQueueItemFinishedEvent(Player player, CraftingTable table, CraftingQueue queue, QueueItem item, ItemStack resultItem, int resultAmount) {
        QueueItemFinishedEvent event = new QueueItemFinishedEvent(table.getName(), player, queue, item, resultItem, resultAmount);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if(event.getFusionPlayer().hasRecipeLimitReached(event.getQueueItem().getRecipe())) {
                callQueueItemCanceledEvent(player, table, queue, item, false, true, event.getQueueItem().getRecipe().getItemsToTake());
                event.setCancelled(true);
                MessageUtil.sendMessage("fusion.error.recipeLimitReached", player,
                        new MessageData("recipe", event.getQueueItem().getRecipe().getName()),
                        new MessageData("amount", event.getFusionPlayer().getRecipeLimit(event.getQueueItem().getRecipe())),
                        new MessageData("recipe.limit", event.getQueueItem().getRecipe().getCraftingLimit()),
                        new MessageData("limit", event.getQueueItem().getRecipe().getCraftingLimit()));
                return;
            }
            //Commands
            DelayedCommand.invoke(Fusion.getInstance(), player, item.getRecipe().getResults().getCommands());
            //Experience

            if (item.getRecipe().getResults().getProfessionExp() > 0) {
                FusionAPI.getEventManager().callProfessionGainXpEvent(player, event.getCraftingTable(), event.getQueueItem().getRecipe().getResults().getProfessionExp());
            }
            if(item.getRecipe().getResults().getVanillaExp() > 0) {
                player.giveExp(event.getQueueItem().getRecipe().getResults().getVanillaExp());
            }

            ItemStack  result     = event.getResultItem();
            result.setAmount(event.getResultAmount());
            // If there is no space in the inventory, drop the items
            Collection<ItemStack> notAdded = player.getInventory().addItem(result).values();
            if (!notAdded.isEmpty()) {
                for (ItemStack _item : notAdded) {
                    Objects.requireNonNull(player.getLocation().getWorld()).dropItemNaturally(player.getLocation(), _item);
                }
            }
            if(event.getQueueItem().getRecipe().getCraftingLimit() > 0)
                event.getFusionPlayer().incrementLimit(event.getQueueItem().getRecipe());

            event.getQueue().removeRecipe(event.getQueueItem(), false);
        }
    }

}
