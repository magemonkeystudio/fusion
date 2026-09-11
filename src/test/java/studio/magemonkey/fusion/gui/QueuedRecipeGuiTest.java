package studio.magemonkey.fusion.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.player.*;
import studio.magemonkey.fusion.data.professions.pattern.*;
import studio.magemonkey.fusion.data.queue.*;
import studio.magemonkey.fusion.data.recipes.*;
import studio.magemonkey.fusion.data.recipes.Recipe;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueuedRecipeGuiTest {
    private Inventory inventory() {
        Inventory inventory = mock(Inventory.class);
        Map<Integer, ItemStack> slots = new HashMap<>();
        when(inventory.getSize()).thenReturn(9);
        when(inventory.getItem(anyInt())).thenAnswer(call -> slots.get(call.getArgument(0)));
        doAnswer(call -> { slots.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(inventory).setItem(anyInt(), any());
        return inventory;
    }
    @Test void visibleCountdownChangesAndSeparateViewsDoNotShareSlotBindings() {
        try (var bukkit = mockStatic(Bukkit.class); var loader = mockStatic(PlayerLoader.class);
             var config = mockStatic(ProfessionsCfg.class)) {
            Player player = mock(Player.class);
            FusionPlayer owner = mock(FusionPlayer.class);
            loader.when(() -> PlayerLoader.getPlayer(player)).thenReturn(owner);
            CraftingTable table = mock(CraftingTable.class);
            when(table.getName()).thenReturn("smith");
            when(table.getInventoryName()).thenReturn("Smith");
            InventoryPattern firstPattern = new InventoryPattern(new String[]{"o-fffffff"}, new HashMap<>());
            InventoryPattern secondPattern = new InventoryPattern(new String[]{"offffff-f"}, new HashMap<>());
            when(table.getRecipePattern()).thenReturn(firstPattern);
            Category firstCategory = mock(Category.class), secondCategory = mock(Category.class);
            when(firstCategory.getName()).thenReturn("tools"); when(secondCategory.getName()).thenReturn("tools");
            when(firstCategory.getPattern()).thenReturn(firstPattern); when(secondCategory.getPattern()).thenReturn(secondPattern);
            Inventory firstInventory = inventory(), secondInventory = inventory();
            bukkit.when(() -> Bukkit.createInventory(isNull(), eq(9), eq("Smith"))).thenReturn(firstInventory, secondInventory);
            CraftingQueue queue = mock(CraftingQueue.class);
            when(owner.getQueue(eq("smith"), any())).thenReturn(queue);
            Recipe recipe = mock(Recipe.class);
            when(recipe.getCraftingTime()).thenReturn(10);
            QueueItem item = new QueueItem(99, "smith", firstCategory, recipe, 1000, 0);
            when(queue.getQueue()).thenReturn(new ArrayList<>(List.of(item)));
            Map<Integer, ItemStack> icons = new HashMap<>();
            config.when(() -> ProfessionsCfg.getQueueItem(eq("smith"), any())).thenAnswer(call -> {
                int seconds = ((QueueItem) call.getArgument(1)).getVisualRemainingItemTime();
                return icons.computeIfAbsent(seconds, ignored -> {
                    ItemStack icon = mock(ItemStack.class); when(icon.clone()).thenReturn(icon); return icon;
                });
            });
            QueuedRecipeGUI first = new QueuedRecipeGUI(player, table, firstCategory);
            QueuedRecipeGUI second = new QueuedRecipeGUI(player, table, secondCategory);
            first.onOpen(); second.onOpen();
            first.renderDynamic(); second.renderDynamic();
            assertSame(icons.get(10), firstInventory.getItem(1));
            assertSame(icons.get(10), secondInventory.getItem(7));
            clearInvocations(firstInventory);
            first.renderDynamic();
            verify(firstInventory, never()).setItem(anyInt(), any());
            QueueProgress.advance(queue.getQueue(), 1);
            first.renderDynamic(); second.renderDynamic();
            assertSame(icons.get(9), firstInventory.getItem(1));
            assertSame(icons.get(9), secondInventory.getItem(7));
            verify(queue, never()).getQueuedItems();
        }
    }
}
