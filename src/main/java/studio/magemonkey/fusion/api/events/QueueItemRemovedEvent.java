package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;

import java.util.List;

@Getter
public class QueueItemRemovedEvent extends FusionEvent {

    /**
     * The crafting queue
     */
    private final CraftingQueue   queue;
    /**
     * The queue item
     */
    private final QueueItem       queueItem;
    /**
     * Whether the item was finished
     */
    private final boolean         finished;
    /**
     * Whether the item was refunded
     */
    private final boolean         refunded;
    /**
     * The refunded items in case `refunded` is `true`
     */
    @Setter
    private       List<ItemStack> refundedItems;

    /**
     * Constructor for the QueueItemRemovedEvent
     *
     * @param professionName The name of the profession
     * @param player The player that removed the item from the queue
     * @param queue The crafting queue
     * @param queueItem The queue item
     * @param finished Whether the item was finished
     * @param refunded Whether the item was refunded
     * @param refundedItems The refunded items in case `refunded` is `true`
     */
    public QueueItemRemovedEvent(String professionName,
                                 Player player,
                                 CraftingQueue queue,
                                 QueueItem queueItem,
                                 boolean finished,
                                 boolean refunded,
                                 List<ItemStack> refundedItems) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.queue = queue;
        this.queueItem = queueItem;
        this.finished = finished;
        this.refunded = refunded;
        this.refundedItems = refundedItems;
    }
}
