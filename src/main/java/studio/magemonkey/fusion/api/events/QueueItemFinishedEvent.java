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

    /**
     * The crafting queue
     */
    private final CraftingQueue queue;
    /**
     * The queue item
     */
    private final QueueItem queueItem;
    /**
     * The result item
     */
    @Setter
    private ItemStack resultItem;
    /**
     * The amount of the result item
     */
    @Setter
    private int resultAmount;

    /**
     * Constructor for the QueueItemFinishedEvent
     *
     * @param professionName The name of the profession
     * @param player The player that finished the item
     * @param queue The crafting queue
     * @param queueItem The queue item
     * @param resultItem The result item
     * @param resultAmount The amount of the result item
     */
    public QueueItemFinishedEvent(String professionName, Player player, CraftingQueue queue, QueueItem queueItem, ItemStack resultItem, int resultAmount) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.queue = queue;
        this.queueItem = queueItem;
        this.resultItem = resultItem;
        this.resultAmount = resultAmount;
    }
}
