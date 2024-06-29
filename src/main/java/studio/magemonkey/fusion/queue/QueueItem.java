package studio.magemonkey.fusion.queue;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;

@Getter
@AllArgsConstructor
public class QueueItem {

    private String profession;
    private Category category;
    private @NonNull Recipe recipe;
    private ItemStack icon;
    private long timestamp;
    private int difference;
    private boolean done;

    public QueueItem(String profession, Category category, @NotNull Recipe recipe, long timestamp) {
        this.profession = profession;
        this.category = category;
        this.recipe = recipe;
        this.timestamp = timestamp;
        update();
    }

    public void update() {
        this.icon = ProfessionsCfg.getQueueItem(profession, this);

        // Get the difference of timestamp (long) and current time (long) in seconds (int)
        difference = (int) ((System.currentTimeMillis() - timestamp) / 1000);
        this.done = difference >= recipe.getCooldown();

    }
}
