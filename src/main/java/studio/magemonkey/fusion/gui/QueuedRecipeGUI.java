package studio.magemonkey.fusion.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.crafting.*;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.queue.*;
import studio.magemonkey.fusion.data.recipes.*;
import studio.magemonkey.fusion.gui.recipe.SlotRole;
import java.util.*;

public final class QueuedRecipeGUI extends RecipeGui {
    private final QueueCraftingService service = new QueueCraftingService();
    private final Map<Integer, Long> queueBindings = new HashMap<>();
    private CraftingQueue queue;
    private int queuePage;

    public QueuedRecipeGUI(Player player, CraftingTable table, Category category) { super(player, table, category); }
    @Override protected void onOpen() {
        queue = PlayerLoader.getPlayer(player).getQueue(table.getName(), category);
    }
    @Override protected void onClose() { queueBindings.clear(); queue = null; }
    @Override protected void onCraft(Recipe recipe, boolean bulk) {
        int maximum = bulk ? 1024 : 1;
        for (int i = 0; i < maximum; i++) {
            CraftingResult result = service.enqueue(player, table, queue, recipe);
            if (result != CraftingResult.SUCCESS) {
                if (i == 0) result.report(player);
                break;
            }
        }
    }
    @Override protected void renderDynamic() {
        if (queue == null) return;
        QueueProgress.refreshTimes(queue.getQueue());
        List<QueueItem> items = List.copyOf(queue.getQueue());
        List<Integer> slots = layout.slots(SlotRole.QUEUE_LIST);
        queuePage = clampPage(queuePage, items.size(), slots.size());
        queueBindings.clear();
        for (int offset = 0; offset < slots.size(); offset++) {
            int index = queuePage * slots.size() + offset;
            int slot = slots.get(offset);
            if (index < items.size()) {
                QueueItem item = items.get(index);
                queueBindings.put(slot, item.getId());
                renderer.set(inventory, slot, ProfessionsCfg.getQueueItem(name, item));
            } else renderer.set(inventory, slot, ProfessionsCfg.getQueueSlot(name));
        }
        for (int slot : layout.slots(SlotRole.PREVIOUS_QUEUE_PAGE))
            renderer.set(inventory, slot, queuePage == 0 ? table.getFillItem() : decorations.get(layout.symbol(slot)));
        for (int slot : layout.slots(SlotRole.NEXT_QUEUE_PAGE))
            renderer.set(inventory, slot, queuePage >= clampPage(Integer.MAX_VALUE, items.size(), slots.size())
                    ? table.getFillItem() : decorations.get(layout.symbol(slot)));
    }
    @Override protected void onModeClick(int slot, InventoryClickEvent event) {
        if (layout.role(slot) == SlotRole.PREVIOUS_QUEUE_PAGE) { queuePage = Math.max(0, queuePage - 1); return; }
        if (layout.role(slot) == SlotRole.NEXT_QUEUE_PAGE) { queuePage++; return; }
        Long id = queueBindings.get(slot);
        if (id == null) return;
        QueueItem item = service.find(queue, id);
        if (item == null) { CraftingResult.NOT_FOUND.report(player); return; }
        if (!item.isDone()) service.cancel(player, table, queue, id).report(player);
        else if (event.isRightClick()) {
            for (QueueItem completed : List.copyOf(queue.getQueue()))
                if (completed.isDone()) service.collect(player, table, queue, completed.getId()).report(player);
        } else service.collect(player, table, queue, id).report(player);
    }
    @Override protected MessageData[] viewData() {
        List<MessageData> data = new ArrayList<>(Arrays.asList(super.viewData()));
        data.add(new MessageData("queue_done", queue == null ? 0 : queue.getQueue().stream().filter(QueueItem::isDone).count()));
        data.add(new MessageData("queue_size", queue == null ? 0 : queue.getQueue().size()));
        data.add(new MessageData("queue_time", queue == null ? 0 : QueueProgress.refreshTimes(queue.getQueue())));
        return data.toArray(MessageData[]::new);
    }
}
