package studio.magemonkey.fusion.cfg.sql.tables;

import org.bukkit.OfflinePlayer;
import studio.magemonkey.fusion.Category;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.queue.CraftingQueue;
import studio.magemonkey.fusion.queue.QueueItem;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FusionQueuesSQL {

    private final String Table = "fusion_queues";

    public FusionQueuesSQL() {
        try (PreparedStatement create = SQLManager.connection().prepareStatement("CREATE TABLE IF NOT EXISTS " + Table + "("
                + "Id long,"
                + "UUID varchar(36), "
                + "RecipePath varchar(100),"
                + "Timestamp numeric,"
                + "CraftingTime numeric,"
                + "SavedSeconds numeric)")) {
            create.execute();
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().warning("[SQL:FusionQueuesSQL] Something went wrong with the sql-connection: " + e.getMessage());
        }
    }

    public long getNextId() {
        try (PreparedStatement search = SQLManager.connection().prepareStatement("SELECT MAX(Id) FROM " + Table)) {
            return search.executeQuery().getLong(1) + 1;
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().warning("[SQL:FusionQueuesSQL] Something went wrong with the sql-connection: " + e.getMessage());
        }
        return 0;
    }

    public boolean setQueueItem(OfflinePlayer player, QueueItem item) {
        if (item == null) return false;
        if (item.getId() == -1) {
            try (PreparedStatement insert = SQLManager.connection().prepareStatement("INSERT INTO " + Table + "(Id, UUID, RecipePath, Timestamp, CraftingTime, SavedSeconds) VALUES(?,?,?,?,?,?)")) {
                insert.setLong(1, getNextId());
                insert.setString(2, player.getUniqueId().toString());
                insert.setString(3, item.getRecipePath());
                insert.setLong(4, item.getTimestamp());
                insert.setLong(5, item.getRecipe().getCooldown());
                insert.setLong(6, item.getSavedSeconds());
                insert.execute();
                return true;
            } catch (SQLException e) {
                Fusion.getInstance().getLogger().warning("[SQL:FusionQueuesSQL] Something went wrong with the sql-connection: " + e.getMessage());
            }
        } else {
            try (PreparedStatement update = SQLManager.connection().prepareStatement("UPDATE " + Table + " SET SavedSeconds=? WHERE Id=?")) {
                update.setLong(1, item.getSavedSeconds());
                update.setLong(2, item.getId());
                update.execute();
                return true;
            } catch (SQLException e) {
                Fusion.getInstance().getLogger().warning("[SQL:FusionQueuesSQL] Something went wrong with the sql-connection: " + e.getMessage());
            }
        }
        return false;
    }

    public boolean removeQueueItem(QueueItem item) {
        try (PreparedStatement delete = SQLManager.connection().prepareStatement("DELETE FROM " + Table + " WHERE Id=?")) {
            delete.setLong(1, item.getId());
            delete.execute();
            return true;
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().warning("[SQL:FusionQueuesSQL] Something went wrong with the sql-connection: " + e.getMessage());
        }
        return false;
    }

    public List<QueueItem> getQueueItems(OfflinePlayer player, String profession, Category category) {
        List<QueueItem> entries = new ArrayList<>();
        try (PreparedStatement select = SQLManager.connection().prepareStatement("SELECT * FROM " + Table + " WHERE UUID=? AND RecipePath LIKE ?")) {
            select.setString(1, player.getUniqueId().toString());
            select.setString(2, "%" + profession + "." + category.getName() + "%");
            select.execute();
            return entries;
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().warning("[SQL:FusionQueuesSQL] Something went wrong with the sql-connection: " + e.getMessage());
        }
        return null;
    }

    public Map<String, CraftingQueue> getCraftingQueue(OfflinePlayer player) {
        return null;
    }

    public void saveCraftingQueue(CraftingQueue queue) {
        queue.cancelTask();
        for (QueueItem item : queue.getQueue()) {
            if(!setQueueItem(queue.getPlayer(), item)) {
                Fusion.getInstance().getLogger().warning("An instance of " + item.getRecipePath() + " could not be saved to the database: " + queue.getPlayer().getUniqueId());
            }
        }
    }
}
