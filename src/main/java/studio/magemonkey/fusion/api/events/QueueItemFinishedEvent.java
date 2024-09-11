package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.queue.CraftingQueue;
import studio.magemonkey.fusion.queue.QueueItem;

@Getter
public class QueueItemFinishedEvent extends FusionEvent {

    private final CraftingQueue queue;
    private final QueueItem queueItem;
    @Setter
    private ItemStack resultItem;
    @Setter
    private int resultAmount;

    public QueueItemFinishedEvent(String professionName, Player player, CraftingQueue queue, QueueItem queueItem, ItemStack resultItem, int resultAmount) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.queue = queue;
        this.queueItem = queueItem;
        this.resultItem = resultItem;
        this.resultAmount = resultAmount;
    }
}
