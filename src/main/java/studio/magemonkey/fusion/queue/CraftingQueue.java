package studio.magemonkey.fusion.queue;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.*;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.player.PlayerLoader;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.*;

@Getter
public class CraftingQueue {

    private final Player player;
    private final String profession;
    private final Category category;
    private final List<QueueItem> queue = new ArrayList<>();
    private final HashMap<Integer, QueueItem> queuedItems;
    private final BukkitTask queueTask;

    public CraftingQueue(Player player, String profession, Category category) {
        this.player = player;
        this.profession = profession;
        this.category = category;
        this.queuedItems = new HashMap<>(20);
        queue.addAll(SQLManager.queues().getQueueItems(player, profession, category));

        queueTask = new BukkitRunnable() {
            @Override
            public void run() {
                queue.forEach(QueueItem::update);
            }
        }.runTaskTimer(Fusion.getInstance(), 0, 20L);
    }

    public void addRecipe(Recipe recipe) {
        int[] limits = PlayerLoader.getPlayer(player.getUniqueId()).getQueueSizes(profession, category);
        int categoryLimit = PlayerUtil.getPermOption(player, "fusion.queue." + profession + "." + category.getName() + ".limit");
        int professionLimit = PlayerUtil.getPermOption(player, "fusion.queue." + profession + ".limit");
        int limit = PlayerUtil.getPermOption(player, "fusion.queue.limit");

        if (categoryLimit > 0 && limits[0] >= categoryLimit) {
            MessageUtil.sendMessage("fusion.queue.fullCategory", player, new MessageData("limit", categoryLimit), new MessageData("category", category.getName()), new MessageData("profession", profession));
            return;
        } else if (professionLimit > 0 && limits[1] >= professionLimit) {
            MessageUtil.sendMessage("fusion.queue.fullProfession", player, new MessageData("limit", professionLimit), new MessageData("profession", profession));
            return;
        } else if (limit > 0 && limits[2] >= limit) {
            MessageUtil.sendMessage("fusion.queue.fullGlobal", player, new MessageData("limit", limit));
            return;
        }

        QueueItem item = new QueueItem(-1, profession, category, recipe, System.currentTimeMillis(), 0);
        queue.add(item);
    }


    public void finishRecipe(QueueItem item) {
        if (item.isDone()) {
            CodexEngine.get().getVault().take(this.player, item.getRecipe().getPrice());
            //Commands
            DelayedCommand.invoke(Fusion.getInstance(), player, item.getRecipe().getCommands());

            //Experience
            CraftingTable table = ProfessionsCfg.getTable(profession);

            if (item.getRecipe().getXpGain() > 0) {
                Fusion.getExperienceManager().getPlayerData(player).add(table, item.getRecipe().getXpGain());
            }

            RecipeItem recipeItem = item.getRecipe().getResult();
            ItemStack result = recipeItem.getItemStack();
            result.setAmount(recipeItem.getAmount());
            // If there is no space in the inventory, drop the items
            Collection<ItemStack> notAdded = player.getInventory().addItem(result).values();
            if (!notAdded.isEmpty()) {
                for (ItemStack _item : notAdded) {
                    Objects.requireNonNull(player.getLocation().getWorld()).dropItemNaturally(player.getLocation(), _item);
                }
            }
            removeRecipe(item, false);
        }
    }

    public void removeRecipe(QueueItem item, boolean refund) {
        if (refund) {
            Collection<ItemStack> refunds = item.getRecipe().getItemsToTake();
            for (ItemStack refundItem : refunds) {
                Collection<ItemStack> notAdded = player.getInventory().addItem(refundItem).values();
                if (!notAdded.isEmpty()) {
                    for (ItemStack _item : notAdded) {
                        Objects.requireNonNull(player.getLocation().getWorld()).dropItemNaturally(player.getLocation(), _item);
                    }
                }
            }
        }
        queue.remove(item);
        for (Map.Entry<Integer, QueueItem> entry : queuedItems.entrySet()) {
            if (entry.getValue().equals(item)) {
                queuedItems.remove(entry.getKey());
                break;
            }
        }
        SQLManager.queues().removeQueueItem(item);
    }

    public void cancelTask() {
        queueTask.cancel();
    }
}
