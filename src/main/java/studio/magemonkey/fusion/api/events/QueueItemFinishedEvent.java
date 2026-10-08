package studio.magemonkey.fusion.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.util.RecipeAmounts;

import java.util.List;

@Getter
public class QueueItemFinishedEvent extends FusionEvent {

    /**
     * The crafting queue
     */
    private final CraftingQueue    queue;
    /**
     * The queue item
     */
    private final QueueItem        queueItem;
    /**
     * The result item
     */
    @Setter
    private       List<RecipeItem> resultItems;
    /**
     * The number of recipe executions represented by this event.
     */
    private final int              recipeAmount;

    /**
     * Constructor for the QueueItemFinishedEvent
     *
     * @param professionName The name of the profession
     * @param player         The player that finished the item
     * @param queue          The crafting queue
     * @param queueItem      The queue item
     * @param resultItems    The result items
     */
    public QueueItemFinishedEvent(String professionName,
                                  Player player,
                                  CraftingQueue queue,
                                  QueueItem queueItem,
                                  List<RecipeItem> resultItems) {
        this(professionName, player, queue, queueItem, resultItems, 1);
    }

    public QueueItemFinishedEvent(String professionName,
                                  Player player,
                                  CraftingQueue queue,
                                  QueueItem queueItem,
                                  List<RecipeItem> resultItems,
                                  int recipeAmount) {
        super(professionName, ProfessionsCfg.getTable(professionName), player);
        this.queue = queue;
        this.queueItem = queueItem;
        this.resultItems = resultItems;
        this.recipeAmount = Math.max(0, recipeAmount);
    }

    public int getOutputAmount() {
        return RecipeAmounts.outputAmount(queueItem.getRecipe(), resultItems) * recipeAmount;
    }
}
