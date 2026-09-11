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

    @Setter
    private          long      id;
    private          String    profession;
    private          Category  category;
    private @NonNull Recipe    recipe;
    private          ItemStack icon;
    @Setter
    private          long      timestamp;
    private          boolean   done;
    private          int       savedSeconds;
    // The duration charged at enqueue time, including player-specific modifiers.
    private          int       craftingTime;

    /** Experience charged when this queue item was created, for cancellation refunds. */
    @Setter
    private          int       paidExpCost;

    @Setter
    private CraftingReceipt receipt;

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
        this.craftingTime = recipe.getCraftingTime();
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
        this.craftingTime = recipe.getCraftingTime();
        this.timestamp = timestamp;
        this.savedSeconds = savedSeconds;
        this.visualRemainingItemTime = recipe.getCraftingTime() - savedSeconds;
        this.done = savedSeconds >= craftingTime;
    }

    public void restoreCraftingTime(int craftingTime) {
        this.craftingTime = Math.max(0, craftingTime);
        this.savedSeconds = Math.min(Math.max(0, savedSeconds), this.craftingTime);
        this.done = savedSeconds >= this.craftingTime;
        this.visualRemainingItemTime = this.craftingTime - savedSeconds;
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

    /** Compatibility refresh only; the queue service owns advancement. */
    public void update() {
        if (craftingQueue != null) QueueProgress.refreshTimes(craftingQueue.getQueue());
        updateIcon();
    }

    public int getRemainingSeconds() {
        return done ? 0 : Math.max(0, craftingTime - savedSeconds);
    }

    public void setVisualRemainingItemTime(int seconds) {
        visualRemainingItemTime = Math.max(0, seconds);
    }
    public void updateIcon() {
        this.icon = ProfessionsCfg.getQueueItem(profession, this);
    }

    public void markDone() {
        this.savedSeconds = craftingTime;
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
        int remaining = craftingTime - savedSeconds;
        if (offlineSeconds >= remaining) {
            // item has finished offline
            savedSeconds = craftingTime;
            done = true;
        } else {
            // item partially progressed offline
            savedSeconds += offlineSeconds;
        }
        // update the remaining time for the UI
        visualRemainingItemTime = craftingTime - savedSeconds;
    }
}
