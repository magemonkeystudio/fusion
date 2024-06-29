package studio.magemonkey.fusion.queue;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.Recipe;
import studio.magemonkey.fusion.RecipeItem;
import studio.magemonkey.fusion.cfg.PConfigManager;

import java.util.*;

@Getter
public class CraftingQueue {

    private Player player;
    private String profession;
    private Category category;
    private final List<QueueItem> queue = new ArrayList<>();
    private final BukkitTask queueTask;

    public CraftingQueue(Player player, String profession, Category category) {
        this.player = player;
        this.profession = profession;
        this.category = category;
        queue.addAll(PConfigManager.getPlayerConfig(player).getQueueItems(profession, category));
        queueTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (QueueItem item : queue) {
                    item.update();
                    if (item.isDone()) {
                        player.sendMessage("Your item is done!");
                        //finishRecipe(item);
                        // Here is potential to notify the player when his item is done (would require some time between the messages)
                        continue;
                    }
                }
            }
        }.runTaskTimer(Fusion.getInstance(), 0, 20L);
    }

    public void addRecipe(Recipe recipe) {
        QueueItem item = new QueueItem(profession, category, recipe, System.currentTimeMillis());
        queue.add(item);
        PConfigManager.getPlayerConfig(player).addQueueItem(item);
    }


    public void finishRecipe(QueueItem item) {
        if(item.isDone()) {
            RecipeItem result = item.getRecipe().getResult();
            result.getItemStack().setAmount(result.getAmount());
            // TODO Checks if the inventory is full to drop the items instead
            player.getInventory().addItem(result.getItemStack());
            queue.remove(item);
            PConfigManager.getPlayerConfig(player).removeQueueItem(item);
        }
    }

    private QueueItem getQueueItemByTimestamp(long timestamp) {
        for (QueueItem item : queue) {
            if (item.getTimestamp() == timestamp) {
                return item;
            }
        }
        return null;
    }
}
