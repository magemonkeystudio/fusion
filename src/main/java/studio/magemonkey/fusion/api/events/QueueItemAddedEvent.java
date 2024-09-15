package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;

@Getter
public class QueueItemAddedEvent extends FusionEvent {

    /**
     * The crafting queue
     */
    private final CraftingQueue queue;
    /**
     * The queue item
     */
    private final QueueItem queueItem;

    /**
     * Constructor for the QueueItemAddedEvent
     *
     * @param professionName The name of the profession
     * @param player The player that added the item to the queue
     * @param queue The crafting queue
     * @param queueItem The queue item
     */
    public QueueItemAddedEvent(String professionName, Player player, CraftingQueue queue, QueueItem queueItem) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.queue = queue;
        this.queueItem = queueItem;
    }
}
