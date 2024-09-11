package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.queue.CraftingQueue;
import studio.magemonkey.fusion.queue.QueueItem;

import java.util.List;

@Getter
public class QueueItemRemovedEvent extends FusionEvent {

    private final CraftingQueue queue;
    private final QueueItem queueItem;
    private final boolean finished;
    private final boolean refunded;
    @Setter
    private List<ItemStack> refundedItems;

    public QueueItemRemovedEvent(String professionName, Player player, CraftingQueue queue, QueueItem queueItem, boolean finished, boolean refunded, List<ItemStack> refundedItems) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.queue = queue;
        this.queueItem = queueItem;
        this.finished = finished;
        this.refunded = refunded;
        this.refundedItems = refundedItems;
    }
}
