package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.queue.CraftingQueue;
import studio.magemonkey.fusion.queue.QueueItem;

@Getter
public class QueueItemAddedEvent extends FusionEvent {

    private final CraftingQueue queue;
    private final QueueItem queueItem;

    public QueueItemAddedEvent(String professionName, Player player, CraftingQueue queue, QueueItem queueItem) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.queue = queue;
        this.queueItem = queueItem;
    }
}
