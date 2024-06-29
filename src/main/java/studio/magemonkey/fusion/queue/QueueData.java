package studio.magemonkey.fusion.queue;

import org.bukkit.inventory.ItemStack;

public class QueueData {

    private final ItemStack icon;
    private final int cooldown;
    private final long remainingTime;

    public QueueData(ItemStack icon, int cooldown, long remainingTime) {
        this.icon = icon;
        this.cooldown = cooldown;
        this.remainingTime = remainingTime;
    }
}
