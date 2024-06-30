package studio.magemonkey.fusion.queue;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.Recipe;
import studio.magemonkey.fusion.RecipeItem;
import studio.magemonkey.fusion.cfg.PConfigManager;
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
        queue.addAll(PConfigManager.getPlayerConfig(player).getQueueItems(profession, category));

        queueTask = new BukkitRunnable() {
            @Override
            public void run() {
                queue.forEach(QueueItem::update);
            }
        }.runTaskTimer(Fusion.getInstance(), 0, 20L);
    }

    public void addRecipe(Recipe recipe) {
        int[] limits = PConfigManager.getPlayerConfig(player).getQueueSizes(profession, category.getName());
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

        QueueItem item = new QueueItem(profession, category, recipe, System.currentTimeMillis());
        queue.add(item);
        PConfigManager.getPlayerConfig(player).addQueueItem(item);
    }


    public void finishRecipe(QueueItem item) {
        if (item.isDone()) {
            RecipeItem recipeItem = item.getRecipe().getResult();
            ItemStack result = recipeItem.getItemStack();
            result.setAmount(recipeItem.getAmount());
            // TODO Checks if the inventory is full to drop the items instead
            player.getInventory().addItem(result);
            removeRecipe(item, false);
        }
    }

    public void removeRecipe(QueueItem item, boolean refund) {
        if (refund) {
            Collection<ItemStack> refunds = item.getRecipe().getItemsToTake();
            for (ItemStack refundItem : refunds) {
                player.getInventory().addItem(refundItem);
            }
        }
        PConfigManager.getPlayerConfig(player).removeQueueItem(item);
        queue.remove(item);
        for (Map.Entry<Integer, QueueItem> entry : queuedItems.entrySet()) {
            if (entry.getValue().equals(item)) {
                queuedItems.remove(entry.getKey());
                break;
            }
        }
    }
}
