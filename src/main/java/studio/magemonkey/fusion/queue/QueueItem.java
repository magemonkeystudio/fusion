package studio.magemonkey.fusion.queue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.Recipe;
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

    private boolean startUpdate = false;
    private CraftingQueue craftingQueue;

    @Getter
    private int visualRemainingItemTime;

    public QueueItem(int id, String profession, Category category, @NotNull Recipe recipe, long timestamp, int savedSeconds) {
        this.id = id;
        this.profession = profession;
        this.category = category;
        this.recipe = recipe;
        this.timestamp = timestamp;
        this.savedSeconds = savedSeconds;
        this.visualRemainingItemTime = (recipe.getCooldown() - savedSeconds);
        // If the queue item shall not be working when player is offline, just instantly override the timestamp
        if (!Cfg.updateQueueOffline)
            this.timestamp = System.currentTimeMillis();
        update();
    }

    public void setCraftinQueue(CraftingQueue craftingQueue) {
        this.craftingQueue = craftingQueue;
    }

    public void update() {
        if(isDone()) return;
        if(this.craftingQueue != null) {
            this.visualRemainingItemTime = craftingQueue.getVisualRemainingTotalTime();
            int reconstructedCooldown = this.visualRemainingItemTime + savedSeconds;
            Bukkit.getConsoleSender().sendMessage("Reconstructed Cooldown: " + reconstructedCooldown + " Recipe Cooldown: " + recipe.getCooldown());
            Bukkit.getConsoleSender().sendMessage("Saved Seconds: " + savedSeconds + " Done: " + done);
            Bukkit.getConsoleSender().sendMessage("Index: " + craftingQueue.getQueue().indexOf(this) + " Size: " + craftingQueue.getQueue().size());

            if (reconstructedCooldown <= recipe.getCooldown()) {
                this.savedSeconds++;
                this.done = savedSeconds >= recipe.getCooldown();
                this.icon = ProfessionsCfg.getQueueItem(profession, this);
            }
        } else {
            this.icon = ProfessionsCfg.getQueueItem(profession, this);
        }
    }

    public void updateIcon() {
        this.icon = ProfessionsCfg.getQueueItem(profession, this);
    }

    public String getRecipePath() {
        return profession + "." + category.getName() + "." + recipe.getName();
    }
}
