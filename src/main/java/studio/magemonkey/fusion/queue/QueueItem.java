package studio.magemonkey.fusion.queue;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

@Getter
@AllArgsConstructor
public class QueueItem {

    private long id;
    private String profession;
    private Category category;
    private @NonNull Recipe recipe;
    private ItemStack icon;
    private long timestamp;
    private boolean done;
    private int savedSeconds;

    public QueueItem(int id, String profession, Category category, @NotNull Recipe recipe, long timestamp, int savedSeconds) {
        this.id = id;
        this.profession = profession;
        this.category = category;
        this.recipe = recipe;
        this.timestamp = timestamp;
        this.savedSeconds = savedSeconds;

        // If the queue item shall not be working when player is offline, just instantly override the timestamp
        if(!Cfg.updateQueueOffline)
            this.timestamp = System.currentTimeMillis();
        update();
    }

    public void update() {
        // Get the difference of timestamp (long) and current time (long) in seconds (int)
        this.savedSeconds += (int) ((System.currentTimeMillis() - timestamp) / 1000);
        this.timestamp = System.currentTimeMillis();
        this.done = savedSeconds >= recipe.getCooldown();
        this.icon = ProfessionsCfg.getQueueItem(profession, this);
    }

    public String getRecipePath() {
        return profession + "." + category.getName() + "." + recipe.getName();
    }
}
