package studio.magemonkey.fusion.api.events.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.api.PlayerManager;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.cfg.sql.tables.FusionQueuesSQL;
import studio.magemonkey.fusion.crafting.CraftingResult;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueueServiceInstantCollectTest {
    MockedStatic<Bukkit> bukkit;
    MockedStatic<FusionAPI> api;
    MockedStatic<SQLManager> sql;
    boolean previousInstantCollect;

    @BeforeEach void setup() {
        previousInstantCollect = Cfg.instantCollect;
        Cfg.instantCollect = true;
        bukkit = mockStatic(Bukkit.class);
        api = mockStatic(FusionAPI.class);
        sql = mockStatic(SQLManager.class);
        PluginManager manager = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
        PlayerManager players = mock(PlayerManager.class);
        FusionPlayer owner = mock(FusionPlayer.class);
        when(players.getPlayer(any(Player.class))).thenReturn(owner);
        api.when(FusionAPI::getPlayerManager).thenReturn(players);
    }

    @AfterEach void close() {
        Cfg.instantCollect = previousInstantCollect;
        if (sql != null) sql.close();
        if (api != null) api.close();
        if (bukkit != null) bukkit.close();
    }

    @Test void instantCollectReturnsFinalizationResult() {
        Player player = mock(Player.class);
        CraftingTable table = mock(CraftingTable.class);
        when(table.getName()).thenReturn("smith");
        CraftingQueue queue = mock(CraftingQueue.class);
        when(queue.getQueue()).thenReturn(new ArrayList<>());
        Recipe recipe = mock(Recipe.class, RETURNS_DEEP_STUBS);
        when(recipe.getCraftingTime()).thenReturn(0);
        List<RecipeItem> result = List.of(mock(RecipeItem.class));
        when(recipe.getResults().getItems()).thenReturn(result);
        QueueItem item = mock(QueueItem.class);
        when(item.getRecipe()).thenReturn(recipe);

        FusionQueuesSQL queues = mock(FusionQueuesSQL.class);
        when(queues.setQueueItem(any(), eq(item))).thenReturn(true);
        sql.when(SQLManager::queues).thenReturn(queues);

        QueueService service = spy(new QueueService());
        doReturn(CraftingResult.REQUIREMENTS_NOT_MET).when(service)
                .finishQueueItemAndReport(player, table, queue, item, result);

        assertEquals(CraftingResult.REQUIREMENTS_NOT_MET, service.addQueueItemResult(player, table, queue, item));
        verify(service).finishQueueItemAndReport(player, table, queue, item, result);
    }
}
