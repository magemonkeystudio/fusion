package studio.magemonkey.fusion.data.queue;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.Recipe;

import java.util.Objects;

@Getter
public class QueueItem {

    private          long      id;
    private          String    profession;
    private          Category  category;
    private @NonNull Recipe    recipe;
    private          ItemStack icon;
    @Setter
    private          long      timestamp;
    private          boolean   done;
    private          int       savedSeconds;

    /** Experience charged when this queue item was created, for cancellation refunds. */
    @Setter
    private          int       paidExpCost;

    private boolean       isRunning = false;
    private CraftingQueue craftingQueue;

    @Getter
    private int visualRemainingItemTime;

    /**
     * Retains the original public constructor shape for integrations compiled
     * against the queue API. Newly persisted costs are initialized separately.
     */
    public QueueItem(long id,
                     String profession,
                     Category category,
                     @NotNull Recipe recipe,
                     ItemStack icon,
                     long timestamp,
                     boolean done,
                     int savedSeconds,
                     boolean isRunning,
                     CraftingQueue craftingQueue,
                     int visualRemainingItemTime) {
        this.id = id;
        this.profession = profession;
        this.category = category;
        this.recipe = Objects.requireNonNull(recipe, "recipe");
        this.icon = icon;
        this.timestamp = timestamp;
        this.done = done;
        this.savedSeconds = savedSeconds;
        this.isRunning = isRunning;
        this.craftingQueue = craftingQueue;
        this.visualRemainingItemTime = visualRemainingItemTime;
    }

    public QueueItem(int id,
                     String profession,
                     Category category,
                     @NotNull Recipe recipe,
                     long timestamp,
                     int savedSeconds) {
        this.id = id;
        this.profession = profession;
        this.category = category;
        this.recipe = recipe;
        this.timestamp = timestamp;
        this.savedSeconds = savedSeconds;
        this.visualRemainingItemTime = recipe.getCraftingTime() - savedSeconds;
    }

    public QueueItem(int id,
                     String profession,
                     Category category,
                     @NotNull Recipe recipe) {
        this(id, profession, category, recipe, System.currentTimeMillis(), 0);
    }

    public void setCraftinQueue(CraftingQueue craftingQueue) {
        this.craftingQueue = craftingQueue;
    }

    public void update() {
        if (isDone()) return;
        if (this.craftingQueue != null) {
            this.visualRemainingItemTime = craftingQueue.getVisualRemainingTotalTime();
            int reconstructedCooldown = this.visualRemainingItemTime + savedSeconds;

            if (visualRemainingItemTime == recipe.getCraftingTime() + 1) return;

            if (reconstructedCooldown <= recipe.getCraftingTime()) {
                if (!isRunning) {
                    // Start the item
                    isRunning = true;
                    this.timestamp = System.currentTimeMillis();
                    return;
                }
                // Advance progress
                savedSeconds++;
                this.timestamp = System.currentTimeMillis();
                // Check if finished
                if (savedSeconds >= recipe.getCraftingTime()) {
                    done = true;
                    // Mark finish time to prevent future overcounting
                    this.timestamp = System.currentTimeMillis();
                }
                icon = ProfessionsCfg.getQueueItem(profession, this);
                if (savedSeconds > 0) {
                    visualRemainingItemTime--;
                }
            }
        } else {
            this.icon = ProfessionsCfg.getQueueItem(profession, this);
        }
    }

    public void updateIcon() {
        this.icon = ProfessionsCfg.getQueueItem(profession, this);
    }

    public void markDone() {
        this.done = true;
        this.visualRemainingItemTime = 0;
    }

    public String getRecipePath() {
        return recipe.getRecipePath();
    }

    public void progressOffline(int offlineSeconds) {
        if (done || offlineSeconds <= 0) {
            return;
        }
        int remaining = recipe.getCraftingTime() - savedSeconds;
        if (offlineSeconds >= remaining) {
            // item has finished offline
            savedSeconds = recipe.getCraftingTime();
            done = true;
        } else {
            // item partially progressed offline
            savedSeconds += offlineSeconds;
        }
        // update the remaining time for the UI
        visualRemainingItemTime = recipe.getCraftingTime() - savedSeconds;
    }
}
