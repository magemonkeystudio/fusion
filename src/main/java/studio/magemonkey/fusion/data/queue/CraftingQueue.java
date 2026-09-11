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
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Getter
public class CraftingQueue {

    private final Player                      player;
    private final String                      profession;
    private final Category                    category;
    private final List<QueueItem>             queue = new ArrayList<>();
    /** Legacy API only; live GUI bindings belong to each open view. */
    @Deprecated
    private final HashMap<Integer, QueueItem> queuedItems;
    private final BukkitTask                  queueTask;
    // Only failed/unacknowledged writes are retained for retry.
    private final Set<QueueItem> pendingCheckpoints = new HashSet<>();

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
                long offlineSeconds = Math.max(0L, (now - current.getTimestamp()) / 1000L);
                // apply offline progress sequentially
                for (QueueItem item : queue) {
                    if (offlineSeconds <= 0) {
                        break;
                    }
                    if (item.isDone()) {
                        continue;
                    }
                    int remaining = item.getCraftingTime() - item.getSavedSeconds();
                    int apply     = (int) Math.min(offlineSeconds, remaining);
                    item.progressOffline(apply);
                    offlineSeconds -= apply;
                }
            }
            // normalize timestamps after applying offline progress
            queue.forEach(item -> item.setTimestamp(now));
            SQLManager.queues().saveCraftingQueue(this);
        }

        visualRemainingTotalTime = QueueProgress.refreshTimes(queue);
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
                tick(System.currentTimeMillis());
            }
        }.runTaskTimer(Fusion.getInstance(), 0, 20L);
    }

    private long lastTick = System.currentTimeMillis();

    public void tick(long now) {
        long seconds = Math.max(0, (now - lastTick) / 1000L);
        if (seconds == 0) return;
        lastTick += seconds * 1000L;
        java.util.Map<QueueItem, Integer> before = new java.util.IdentityHashMap<>();
        queue.forEach(item -> before.put(item, item.getSavedSeconds()));
        QueueProgress.advance(queue, seconds);
        visualRemainingTotalTime = QueueProgress.refreshTimes(queue);
        queue.forEach(item -> {
            if (item.getSavedSeconds() != before.get(item)) {
                item.setTimestamp(now);
                pendingCheckpoints.add(item);
            }
        });
        queue.stream().filter(item -> !item.isDone()).findFirst().ifPresent(item -> {
            item.setTimestamp(now);
            pendingCheckpoints.add(item);
        });
        pendingCheckpoints.retainAll(queue);
        pendingCheckpoints.removeIf(item -> SQLManager.queues().setQueueItem(player.getUniqueId(), item));
    }
    public void addRecipe(Recipe recipe) {
        addRecipe(recipe, 0);
    }

    public boolean addRecipe(Recipe recipe, int paidExpCost) {
        QueueItem item = new QueueItem(-1, profession, category, recipe, System.currentTimeMillis(), 0);
        item.setPaidExpCost(Math.max(0, paidExpCost));
        return FusionAPI.getEventServices()
                .getQueueService()
                .addQueueItemAndReport(player, ProfessionsCfg.getTable(profession), this, item);
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
                        refund ? (item.getReceipt() == null ? item.getRecipe().getItemsToTake() : item.getReceipt().getItems()) : List.of());
    }

    public void cancelTask() {
        queueTask.cancel();
    }
}
