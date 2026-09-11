package studio.magemonkey.fusion.cfg.sql.tables;

import org.bukkit.entity.Player;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class FusionQueuesSQL {

    private final String Table = "fusion_queues";

    public FusionQueuesSQL() {
        try (PreparedStatement create = SQLManager.connection()
                .prepareStatement("CREATE TABLE IF NOT EXISTS " + Table + "("
                        + SQLManager.getIdColumn() + " UUID varchar(36), "
                        + "RecipePath varchar(100),"
                        + "CraftingTime numeric,"
                        + "SavedSeconds numeric,"
                        + "PaidExpCost numeric DEFAULT 0,"
                        + "Timestamp BIGINT)")) {
            create.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionQueuesSQL:FusionQueuesSQL] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        // Existing installations need the new persisted queue-cost column as well.
        try (PreparedStatement alter = SQLManager.connection()
                .prepareStatement("ALTER TABLE " + Table + " ADD COLUMN PaidExpCost numeric DEFAULT 0")) {
            alter.execute();
        } catch (SQLException ignored) {
            // The column already exists on current installations.
        }
        try (PreparedStatement alter = SQLManager.connection().prepareStatement(
                "ALTER TABLE " + Table + " ADD COLUMN Receipt TEXT")) {
            alter.execute();
        } catch (SQLException ignored) { /* Already migrated. Insert fails safely if unavailable. */ }
        SQLManager.ensureIndex(Table, "fusion_queues_player", "UUID, Id");
    }

    public boolean setQueueItem(UUID uuid, QueueItem item) {
        if (item == null) return false;
        if (item.getId() == -1) {
            try (PreparedStatement insert = SQLManager.connection()
                    .prepareStatement("INSERT INTO " + Table
                            + "(UUID, RecipePath, Timestamp, CraftingTime, SavedSeconds, PaidExpCost, Receipt) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, uuid.toString());
                insert.setString(2, item.getRecipePath());
                insert.setLong(3, item.getTimestamp());
                insert.setLong(4, item.getCraftingTime());
                insert.setLong(5, item.getSavedSeconds());
                insert.setInt(6, item.getPaidExpCost());
                insert.setString(7, item.getReceipt() == null ? null : item.getReceipt().encode());
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Queue insert returned no ID");
                    item.setId(keys.getLong(1));
                }
                return true;
            } catch (SQLException e) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("[SQL:FusionQueuesSQL:setQueueItem] Something went wrong with the sql-connection: "
                                + e.getMessage());
            }
        } else {
            try (PreparedStatement update = SQLManager.connection()
                    .prepareStatement("UPDATE " + Table + " SET SavedSeconds=?, PaidExpCost=?, Timestamp=? WHERE Id=? AND UUID=?")) {
                update.setLong(1, item.getSavedSeconds());
                update.setInt(2, item.getPaidExpCost());
                update.setLong(3, item.getTimestamp());
                update.setLong(4, item.getId());
                update.setString(5, uuid.toString());
                return update.executeUpdate() == 1;
            } catch (SQLException e) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("[SQL:FusionQueuesSQL:setQueueItem] Something went wrong with the sql-connection: "
                                + e.getMessage());
            }
        }
        return false;
    }

    public boolean removeQueueItem(QueueItem item) {
        try (PreparedStatement delete = SQLManager.connection()
                .prepareStatement("DELETE FROM " + Table + " WHERE Id=?")) {
            delete.setLong(1, item.getId());
            return delete.executeUpdate() == 1;
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionQueuesSQL:removeQueueItem] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return false;
    }

    public boolean claimQueueItem(UUID uuid, QueueItem item) {
        try (Connection connection = SQLManager.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM " + Table + " WHERE Id=? AND UUID=? AND SavedSeconds>=CraftingTime")) {
                delete.setLong(1, item.getId());
                delete.setString(2, uuid.toString());
                if (delete.executeUpdate() != 1 || !SQLManager.recipeLimits().consumeLimit(connection,
                        uuid, item.getRecipePath(), item.getRecipe().getCraftingLimit(),
                        item.getRecipe().getCraftingLimitCooldown())) {
                    connection.rollback();
                    return false;
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException | RuntimeException e) {
            Fusion.getInstance().getLogger().warning("Failed to claim queue item " + item.getId() + ": " + e.getMessage());
            return false;
        }
    }

    public List<QueueItem> getQueueItems(UUID uuid, String profession, Category category) {
        List<QueueItem> entries = new ArrayList<>();
        String          sql     = "SELECT * FROM " + Table + " WHERE UUID=? ORDER BY Id";

        try (PreparedStatement select = SQLManager.connection().prepareStatement(sql)) {
            select.setString(1, uuid.toString());
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    String prefix = profession + "." + (category.getName().equals("master") ? "" : category.getName() + ".");
                    String path = result.getString("RecipePath");
                    if (path == null || !path.startsWith(prefix)) continue;
                    String recipeStr = path.substring(prefix.length());
                    Recipe recipe = category.getName().equals("master")
                            ? category.getRecipes().stream().filter(candidate -> candidate.getRecipePath().equals(path)).findFirst().orElse(null)
                            : category.getRecipe(recipeStr);

                    if (recipe == null) {
                        Fusion.getInstance()
                                .getLogger()
                                .warning("Unable to locate recipe: " + recipeStr + " for " + profession + "."
                                        + category.getName() + " while loading queue for " + uuid + ". Does it exist?");
                        List<String> availableRecipes =
                                category.getRecipes().stream().map(r -> r.getName().split("::")[0]).toList();
                        Fusion.getInstance().getLogger().info("Available recipes are " + availableRecipes);
                        continue;
                    }

                    QueueItem queueItem = new QueueItem(
                            result.getInt("Id"),
                            profession,
                            category,
                            recipe,
                            result.getLong("Timestamp"),
                            result.getInt("SavedSeconds")
                    );
                    queueItem.setPaidExpCost(result.getInt("PaidExpCost"));
                    queueItem.setReceipt(studio.magemonkey.fusion.data.queue.CraftingReceipt.decode(result.getString("Receipt")));
                    queueItem.setId(result.getLong("Id"));
                    queueItem.restoreCraftingTime(result.getInt("CraftingTime"));
                    entries.add(queueItem);
                }
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionQueuesSQL:getQueueItems] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return entries;
    }

    public Map<String, CraftingQueue> getCraftingQueues(Player player) {
        Map<String, CraftingQueue> entries = new HashMap<>();
        for (Map.Entry<String, CraftingTable> entry : ProfessionsCfg.getMap().entrySet()) {
            String profession = entry.getKey();
            Collection<Category> categories = entry.getValue().getCategories().values();
            if (!entry.getValue().getUseCategories() || categories.isEmpty()) {
                Category master = new Category("master", "PAPER", entry.getValue().getRecipePattern(), 1);
                master.getRecipes().addAll(entry.getValue().getRecipes().values());
                categories = List.of(master);
            }
            for (Category category : categories) {
                String path = profession + "." + category.getName();
                if (entries.containsKey(path)) continue;
                entries.putIfAbsent(path, new CraftingQueue(player, profession, category));
            }
        }
        return entries;
    }

    public void saveCraftingQueue(CraftingQueue queue) {
        for (QueueItem item : queue.getQueue()) {
            if (!setQueueItem(queue.getPlayer().getUniqueId(), item)) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("An instance of " + item.getRecipePath() + " could not be saved to the database: "
                                + queue.getPlayer().getUniqueId());
            }
        }
    }
}
