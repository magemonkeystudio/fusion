package studio.magemonkey.fusion.data.queue;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class CraftingQueue {

    private final Player                      player;
    private final String                      profession;
    private final Category                    category;
    private final List<QueueItem>             queue = new ArrayList<>();
    private final HashMap<Integer, QueueItem> queuedItems;
    private final BukkitTask                  queueTask;

    @Getter
    private int visualRemainingTotalTime = 0;

    public CraftingQueue(Player player, String profession, Category category) {
        this.player = player;
        this.profession = profession;
        this.category = category;
        this.queuedItems = new HashMap<>(20);

        // Load items from the database
        List<QueueItem> loaded = SQLManager.queues().getQueueItems(player.getUniqueId(), profession, category);
        queue.addAll(loaded);

        /*
         * If offline progression is enabled, distribute the offline time across the
         * queue sequentially.  All items are saved with the same timestamp when
         * saved, so use the first item's timestamp to calculate the offline duration.
         */
        if (Cfg.updateQueueOffline && !queue.isEmpty()) {
            long now = System.currentTimeMillis();
            // find the first unfinished item
            QueueItem current = queue.stream()
                    .filter(item -> !item.isDone())
                    .findFirst()
                    .orElse(null);
            if (current != null) {
                int offlineSeconds = (int) ((now - current.getTimestamp()) / 1000L);
                // apply offline progress sequentially
                for (QueueItem item : queue) {
                    if (offlineSeconds <= 0) {
                        break;
                    }
                    if (item.isDone()) {
                        continue;
                    }
                    int remaining = item.getRecipe().getCraftingTime() - item.getSavedSeconds();
                    int apply = Math.min(offlineSeconds, remaining);
                    item.progressOffline(apply);
                    offlineSeconds -= apply;
                }
            }
            // normalize timestamps after applying offline progress
            queue.forEach(item -> item.setTimestamp(now));
        }

        // Assign the queue and update the icons
        queue.forEach(entry -> {
            entry.setCraftinQueue(this);
            entry.updateIcon();
        });

        // Start the queue update task
        queueTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                visualRemainingTotalTime = 0;
                queue.forEach(item -> {
                    if (!item.isDone()) {
                        visualRemainingTotalTime += (item.getRecipe().getCraftingTime() - item.getSavedSeconds());
                        item.update();
                    }
                });
            }
        }.runTaskTimer(Fusion.getInstance(), 0, 20L);
    }

    public void addRecipe(Recipe recipe) {
        int[] limits = PlayerLoader.getPlayer(player.getUniqueId()).getQueueSizes(profession, category);
        int categoryLimit =
                PlayerUtil.getPermOption(player, "fusion.queue." + profession + "." + category.getName() + ".limit");
        int professionLimit = PlayerUtil.getPermOption(player, "fusion.queue." + profession + ".limit");
        int limit           = PlayerUtil.getPermOption(player, "fusion.queue.limit");

        if (categoryLimit > 0 && limits[0] >= categoryLimit) {
            CodexEngine.get().getMessageUtil().sendMessage("fusion.queue.fullCategory",
                    player,
                    new MessageData("limit", categoryLimit),
                    new MessageData("category", category.getName()),
                    new MessageData("profession", profession));
            return;
        } else if (professionLimit > 0 && limits[1] >= professionLimit) {
            CodexEngine.get().getMessageUtil().sendMessage("fusion.queue.fullProfession",
                    player,
                    new MessageData("limit", professionLimit),
                    new MessageData("profession", profession));
            return;
        } else if (limit > 0 && limits[2] >= limit) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.queue.fullGlobal", player, new MessageData("limit", limit));
            return;
        }

        QueueItem item = new QueueItem(-1, profession, category, recipe, System.currentTimeMillis(), 0);
        FusionAPI.getEventServices()
                .getQueueService()
                .addQueueItem(player, ProfessionsCfg.getTable(profession), this, item);
    }

    public void finishAllRecipes() {
        for (QueueItem item : queue) {
            Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), () -> finishRecipe(item), 1);
        }
    }

    public void finishRecipe(QueueItem item) {
        if (item.isDone()) {
            // TODO consider getting the item back from CodexEngine in case the icon is modified
            List<RecipeItem> resultItems = item.getRecipe().getResults().getItems();
            FusionAPI.getEventServices()
                    .getQueueService()
                    .finishQueueItem(player,
                            ProfessionsCfg.getTable(profession),
                            this,
                            item,
                            resultItems);
        }
    }

    public void removeRecipe(QueueItem item, boolean refund) {
        // As of today, refunding only happens when the crafting is not finished. Meaning !refund is our finish-parameter here
        // The opposite for !refund -> the recipe finished successfully
        FusionAPI.getEventServices()
                .getQueueService()
                .cancelQueueItem(player,
                        ProfessionsCfg.getTable(profession),
                        this,
                        item,
                        !refund,
                        refund,
                        refund ? item.getRecipe().getItemsToTake() : List.of());
    }

    public void cancelTask() {
        queueTask.cancel();
    }
}
