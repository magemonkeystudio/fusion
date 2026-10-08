package studio.magemonkey.fusion.crafting;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.Test;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.data.player.*;
import studio.magemonkey.fusion.data.queue.CraftingReceipt;
import studio.magemonkey.fusion.data.recipes.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ManualCraftingServiceTest {
    @Test void cancelledSessionRefundsOnceAndOldCallbackCannotCompleteNewCraft() {
        CraftingReceipt receipt = new CraftingReceipt(List.of(), 4, 0);
        Player player = mock(Player.class);
        CraftingTable table = mock(CraftingTable.class);
        Recipe first = mock(Recipe.class), second = mock(Recipe.class);
        when(first.getRecipePath()).thenReturn("first"); when(second.getRecipePath()).thenReturn("second");
        when(first.getCraftingTime()).thenReturn(10); when(second.getCraftingTime()).thenReturn(10);
        try (var bukkit = mockStatic(Bukkit.class); var fusion = mockStatic(Fusion.class);
             var checks = mockStatic(CraftingChecks.class);
             var costs = mockConstruction(CraftingCostService.class, (mock, context) ->
                     when(mock.charge(any(), any())).thenReturn(receipt))) {
            Fusion plugin = mock(Fusion.class); fusion.when(Fusion::getInstance).thenReturn(plugin);
            checks.when(() -> CraftingChecks.canCraft(any(), any(), any(), isNull())).thenReturn(true);
            BukkitScheduler scheduler = mock(BukkitScheduler.class); bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            List<Runnable> callbacks = new ArrayList<>();
            BukkitTask task = mock(BukkitTask.class);
            when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(call -> {
                callbacks.add(call.getArgument(1)); return task;
            });
            ManualCraftingService service = new ManualCraftingService(player, table);
            assertEquals(CraftingResult.SUCCESS, service.start(first, () -> {}));
            service.cancel(); service.cancel();
            verify(costs.constructed().get(0), times(1)).refund(player, receipt);
            assertEquals(CraftingResult.SUCCESS, service.start(second, () -> {}));
            callbacks.get(0).run();
            assertTrue(service.isRunning());
            assertSame(second, service.getRecipe());
            service.cancel();
            assertFalse(service.isRunning());
            verify(costs.constructed().get(0), times(2)).refund(player, receipt);
        }
    }
    @Test void successfulCompletionDeliversOnceWithoutRefund() {
        Player player = mock(Player.class); when(player.isOnline()).thenReturn(true);
        CraftingTable table = mock(CraftingTable.class);
        Recipe recipe = mock(Recipe.class, RETURNS_DEEP_STUBS);
        when(recipe.getRecipePath()).thenReturn("smith.tools.pick");
        CraftingReceipt receipt = new CraftingReceipt(List.of(), 0, 0);
        try (var bukkit = mockStatic(Bukkit.class); var fusion = mockStatic(Fusion.class);
             var checks = mockStatic(CraftingChecks.class); var loader = mockStatic(PlayerLoader.class);
             var costs = mockConstruction(CraftingCostService.class, (mock, context) -> when(mock.charge(any(), any())).thenReturn(receipt));
             var rewards = mockConstruction(CraftingRewardService.class)) {
            Fusion plugin = mock(Fusion.class); fusion.when(Fusion::getInstance).thenReturn(plugin);
            loader.when(() -> PlayerLoader.getPlayer(player)).thenReturn(mock(FusionPlayer.class));
            checks.when(() -> CraftingChecks.canCraft(any(), any(), any(), isNull())).thenReturn(true);
            BukkitScheduler scheduler = mock(BukkitScheduler.class); bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            List<Runnable> callbacks = new ArrayList<>();
            when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(call -> {
                callbacks.add(call.getArgument(1)); return mock(BukkitTask.class);
            });
            ManualCraftingService service = new ManualCraftingService(player, table);
            service.start(recipe, () -> {});
            callbacks.get(0).run(); callbacks.get(0).run(); service.cancel();
            assertEquals(1, rewards.constructed().size());
            verify(rewards.constructed().get(0)).give(player, table, recipe, recipe.getResults().getItems());
            verify(costs.constructed().get(0), never()).refund(any(), any());
        }
    }
}
