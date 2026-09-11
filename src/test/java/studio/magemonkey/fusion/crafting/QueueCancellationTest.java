package studio.magemonkey.fusion.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.fusion.api.*;
import studio.magemonkey.fusion.api.events.QueueItemRemovedEvent;
import studio.magemonkey.fusion.api.events.services.QueueService;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.cfg.sql.tables.FusionQueuesSQL;
import studio.magemonkey.fusion.data.queue.*;
import studio.magemonkey.fusion.data.recipes.*;
import studio.magemonkey.fusion.data.recipes.Recipe;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueueCancellationTest {
    MockedStatic<Bukkit> bukkit;
    MockedStatic<FusionAPI> api;
    MockedStatic<ProfessionsCfg> config;
    MockedStatic<SQLManager> sql;
    MockedStatic<CodexEngine> codex;
    Player player;
    CraftingTable table;
    CraftingQueue queue;
    QueueItem item;
    FusionQueuesSQL repository;
    PluginManager plugins;
    CodexEngine engine;
    QueueService service = new QueueService();

    @BeforeEach void setup() {
        bukkit = mockStatic(Bukkit.class); api = mockStatic(FusionAPI.class);
        config = mockStatic(ProfessionsCfg.class); sql = mockStatic(SQLManager.class); codex = mockStatic(CodexEngine.class);
        plugins = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        table = mock(CraftingTable.class);
        when(table.getName()).thenReturn("smith");
        config.when(() -> ProfessionsCfg.getTable("smith")).thenReturn(table);
        PlayerManager manager = mock(PlayerManager.class);
        api.when(FusionAPI::getPlayerManager).thenReturn(manager);
        engine = mock(CodexEngine.class, RETURNS_DEEP_STUBS);
        codex.when(CodexEngine::get).thenReturn(engine);
        repository = mock(FusionQueuesSQL.class);
        sql.when(SQLManager::queues).thenReturn(repository);
        Recipe recipe = mock(Recipe.class, RETURNS_DEEP_STUBS);
        when(recipe.getCraftingTime()).thenReturn(30);
        when(recipe.getConditions().getMoneyCost()).thenReturn(999.0); // edited since charging
        item = new QueueItem(42, "smith", null, recipe, 1000, 0);
        item.setReceipt(new CraftingReceipt(List.of(), 7, 2.0));
        queue = mock(CraftingQueue.class);
        when(queue.getQueue()).thenReturn(new ArrayList<>(List.of(item)));
    }
    @AfterEach void close() { codex.close(); sql.close(); config.close(); api.close(); bukkit.close(); }

    @Test void cancelledCraftRefundsRecordedCostsOnce() {
        when(repository.removeQueueItem(item)).thenReturn(true);
        ItemStack actual = mock(ItemStack.class);
        when(actual.clone()).thenReturn(actual);
        when(actual.getAmount()).thenReturn(3);
        when(actual.getMaxStackSize()).thenReturn(64);
        var result = service.cancelQueueItemAndReport(player, table, queue, item, false, true, List.of(actual));
        assertEquals(CraftingResult.SUCCESS, result);
        verify(player).giveExp(7);
        verify(engine.getVault()).give(player, 2.0);
        verify(player.getInventory()).addItem(actual);
        assertEquals(CraftingResult.NOT_FOUND,
                service.cancelQueueItemAndReport(player, table, queue, item, false, true, List.of(actual)));
        verify(repository, times(1)).removeQueueItem(item);
        verify(player, times(1)).giveExp(7);
    }
    @Test void storageFailureKeepsCraftAndDoesNotRefund() {
        when(repository.removeQueueItem(item)).thenReturn(false);
        assertEquals(CraftingResult.STORAGE_FAILED,
                service.cancelQueueItemAndReport(player, table, queue, item, false, true, List.of()));
        assertTrue(queue.getQueue().contains(item));
        verify(player, never()).giveExp(anyInt());
        verify(engine.getVault(), never()).give(any(), anyDouble());
    }
    @Test void eventCancellationIsRespectedBeforeAnyStorageOrRefund() {
        doAnswer(invocation -> { ((QueueItemRemovedEvent) invocation.getArgument(0)).setCancelled(true); return null; })
                .when(plugins).callEvent(any(QueueItemRemovedEvent.class));
        assertEquals(CraftingResult.EVENT_CANCELLED,
                service.cancelQueueItemAndReport(player, table, queue, item, false, true, List.of()));
        verifyNoInteractions(repository);
        assertTrue(queue.getQueue().contains(item));
        verify(player, never()).giveExp(anyInt());
    }
}
