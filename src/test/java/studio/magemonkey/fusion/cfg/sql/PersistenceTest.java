package studio.magemonkey.fusion.cfg.sql;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.sql.tables.*;
import studio.magemonkey.fusion.data.player.PlayerRecipeLimit;
import studio.magemonkey.fusion.data.professions.Profession;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.CraftingReceipt;
import studio.magemonkey.fusion.data.recipes.Recipe;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersistenceTest {
    @TempDir Path directory;
    Connection connection;
    MockedStatic<SQLManager> sql;
    MockedStatic<Fusion> fusion;
    FusionQueuesSQL queues;
    FusionRecipeLimitsSQL limits;
    UUID uuid = UUID.randomUUID();
    Recipe recipe;
    Category category;

    @BeforeEach void open() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("fusion.db");
        connection = DriverManager.getConnection(url);
        sql = mockStatic(SQLManager.class);
        sql.when(SQLManager::connection).thenReturn(connection);
        sql.when(SQLManager::openConnection).thenAnswer(invocation -> DriverManager.getConnection(url));
        sql.when(SQLManager::getIdColumn).thenReturn("Id INTEGER PRIMARY KEY AUTOINCREMENT,");
        Fusion plugin = mock(Fusion.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        fusion = mockStatic(Fusion.class);
        fusion.when(Fusion::getInstance).thenReturn(plugin);
        queues = new FusionQueuesSQL();
        limits = new FusionRecipeLimitsSQL();
        sql.when(SQLManager::queues).thenReturn(queues);
        sql.when(SQLManager::recipeLimits).thenReturn(limits);
        recipe = mock(Recipe.class);
        when(recipe.getRecipePath()).thenReturn("smithing.iron_tools.pickaxe");
        when(recipe.getCraftingTime()).thenReturn(60);
        category = mock(Category.class);
        when(category.getName()).thenReturn("iron_tools");
        when(category.getRecipe("pickaxe")).thenReturn(recipe);
    }

    @AfterEach void close() throws Exception {
        if (sql != null) sql.close();
        if (fusion != null) fusion.close();
        if (connection != null) connection.close();
    }

    QueueItem item(int progress) {
        return new QueueItem(-1, "smithing", category, recipe, 1000L, progress);
    }

    @Test void repeatedSaveRetainsIdentityDurationTimestampAndPaidCost() throws Exception {
        QueueItem item = item(20);
        item.setPaidExpCost(17);
        assertTrue(queues.setQueueItem(uuid, item));
        long id = item.getId();
        assertTrue(id > 0);
        item.progressOffline(10);
        item.setTimestamp(9000L);
        assertTrue(queues.setQueueItem(uuid, item));
        when(recipe.getCraftingTime()).thenReturn(999); // config changed since enqueue
        var loaded = queues.getQueueItems(uuid, "smithing", category);
        assertEquals(1, loaded.size());
        QueueItem restored = loaded.get(0);
        assertEquals(id, restored.getId());
        assertEquals(60, restored.getCraftingTime());
        assertEquals(30, restored.getSavedSeconds());
        assertEquals(9000L, restored.getTimestamp());
        assertEquals(17, restored.getPaidExpCost());
        assertFalse(restored.isDone());
    }

    @Test void completedItemStaysCompletedAfterReload() {
        QueueItem item = item(60);
        assertTrue(queues.setQueueItem(uuid, item));
        assertTrue(queues.getQueueItems(uuid, "smithing", category).get(0).isDone());
    }

    @Test void receiptSurvivesReloadAndIsNotRebuiltFromEditedRecipe() {
        QueueItem queued = item(0);
        queued.setReceipt(new CraftingReceipt(java.util.List.of(), 13, 12.5));
        assertTrue(queues.setQueueItem(uuid, queued));
        CraftingReceipt loaded = queues.getQueueItems(uuid, "smithing", category).get(0).getReceipt();
        assertNotNull(loaded);
        assertEquals(13, loaded.getExperience());
        assertEquals(12.5, loaded.getMoney());
        assertTrue(loaded.getItems().isEmpty());
    }

    @Test void categorylessViewLoadsCanonicalRecipePaths() {
        assertTrue(queues.setQueueItem(uuid, item(0)));
        Category master = mock(Category.class);
        when(master.getName()).thenReturn("master");
        when(master.getRecipes()).thenReturn(new java.util.ArrayList<>(java.util.List.of(recipe)));
        assertEquals(1, queues.getQueueItems(uuid, "smithing", master).size());
    }

    @Test void queueMatchingDoesNotTreatUnderscoresAsWildcardsAndKeepsOrder() throws Exception {
        QueueItem first = item(0);
        QueueItem second = item(0);
        assertTrue(queues.setQueueItem(uuid, first));
        assertTrue(queues.setQueueItem(uuid, second));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO fusion_queues(UUID,RecipePath) VALUES ('" + uuid
                    + "','smithing.ironXtools.pickaxe')");
        }
        var loaded = queues.getQueueItems(uuid, "smithing", category);
        assertEquals(2, loaded.size());
        assertEquals(first.getId(), loaded.get(0).getId());
        assertEquals(second.getId(), loaded.get(1).getId());
    }

    @Test void limitsAreFreshAndBoundMutatorsPersistWithoutLogout() throws Exception {
        String path = recipe.getRecipePath();
        PlayerRecipeLimit old = limits.getRecipeLimit(uuid, path);
        limits.incrementLimit(uuid, path, 2, 0);
        old.incrementLimit(1); // must add to persisted 2 rather than overwrite with stale 1
        assertEquals(3, limits.getRecipeLimit(uuid, path).getLimit());
        limits.getRecipeLimit(uuid, path).resetLimit();
        assertEquals(0, limits.getRecipeLimit(uuid, path).getLimit());
        limits.incrementLimit(uuid, path, 1, 60);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE fusion_limits SET Timestamp=1");
        }
        assertEquals(0, limits.getRecipeLimit(uuid, path).getLimit());
        limits.incrementLimit(uuid, path, 1, 0);
        assertEquals(1, new FusionRecipeLimitsSQL().getRecipeLimit(uuid, path).getLimit());
    }

    @Test void claimAndLimitAreAtomicAndCannotBeRepeated() {
        when(recipe.getCraftingLimit()).thenReturn(1);
        QueueItem first = item(60);
        QueueItem second = item(60);
        assertTrue(queues.setQueueItem(uuid, first));
        assertTrue(queues.setQueueItem(uuid, second));
        assertTrue(queues.claimQueueItem(uuid, first));
        assertFalse(queues.claimQueueItem(uuid, first));
        assertFalse(queues.claimQueueItem(uuid, second));
        assertEquals(1, limits.getRecipeLimit(uuid, recipe.getRecipePath()).getLimit());
        assertEquals(second.getId(), queues.getQueueItems(uuid, "smithing", category).get(0).getId());
    }

    @Test void failedLimitWriteRollsBackQueueRemoval() throws Exception {
        when(recipe.getCraftingLimit()).thenReturn(1);
        QueueItem item = item(60);
        assertTrue(queues.setQueueItem(uuid, item));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE fusion_limits");
        }
        assertFalse(queues.claimQueueItem(uuid, item));
        assertEquals(1, queues.getQueueItems(uuid, "smithing", category).size());
    }

    @Test void unfinishedOrWrongOwnerCannotClaim() {
        QueueItem item = item(30);
        assertTrue(queues.setQueueItem(uuid, item));
        assertFalse(queues.claimQueueItem(uuid, item));
        assertFalse(queues.claimQueueItem(UUID.randomUUID(), item));
        assertEquals(1, queues.getQueueItems(uuid, "smithing", category).size());
    }

    @Test void newProfessionWritesUseLogicalIdentityAndAtomicExperience() {
        FusionProfessionsSQL professions = new FusionProfessionsSQL();
        sql.when(SQLManager::professions).thenReturn(professions);
        Profession profession = new Profession(-1, uuid, "smithing", 0, false, true);
        professions.setProfession(uuid, profession);
        profession.addExp(10);
        profession.setMastered(true);
        profession.addExp(20);
        Profession loaded = professions.getProfessions(uuid).get(0);
        assertEquals(30, loaded.getExp());
        assertTrue(loaded.isMastered());
    }

    @Test void offlineTimeIsDistributedOnceAndSavingDoesNotStopLiveQueue() {
        boolean previous = Cfg.updateQueueOffline;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ProfessionsCfg> config = mockStatic(ProfessionsCfg.class)) {
            Cfg.updateQueueOffline = true;
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            BukkitTask task = mock(BukkitTask.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskTimer(any(org.bukkit.plugin.Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(task);
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(uuid);
            QueueItem first = item(50);
            QueueItem second = item(0);
            long timestamp = System.currentTimeMillis() - 25000L;
            first.setTimestamp(timestamp);
            second.setTimestamp(timestamp);
            assertTrue(queues.setQueueItem(uuid, first));
            assertTrue(queues.setQueueItem(uuid, second));
            CraftingQueue queue = new CraftingQueue(player, "smithing", category);
            assertTrue(queue.getQueue().get(0).isDone());
            int progress = queue.getQueue().get(1).getSavedSeconds();
            assertTrue(progress >= 15 && progress < 20);
            queues.saveCraftingQueue(queue);
            verify(task, never()).cancel();
            CraftingQueue rejoined = new CraftingQueue(player, "smithing", category);
            assertTrue(rejoined.getQueue().get(1).getSavedSeconds() <= progress + 1);
        } finally {
            Cfg.updateQueueOffline = previous;
        }
    }

    @Test void offlineProgressCanBeDisabled() {
        boolean previous = Cfg.updateQueueOffline;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ProfessionsCfg> config = mockStatic(ProfessionsCfg.class)) {
            Cfg.updateQueueOffline = false;
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskTimer(any(org.bukkit.plugin.Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(mock(BukkitTask.class));
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(uuid);
            assertTrue(queues.setQueueItem(uuid, item(20)));
            CraftingQueue queue = new CraftingQueue(player, "smithing", category);
            assertEquals(20, queue.getQueue().get(0).getSavedSeconds());
            assertFalse(queue.getQueue().get(0).isDone());
        } finally {
            Cfg.updateQueueOffline = previous;
        }
    }
}
