package studio.magemonkey.fusion.cfg.sql.tables;

import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.player.PlayerRecipeLimit;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FusionRecipeLimitsSQL {

    private final String Table = "fusion_limits";

    public FusionRecipeLimitsSQL() {
        try (PreparedStatement create = SQLManager.connection()
                .prepareStatement("CREATE TABLE IF NOT EXISTS " + Table + "("
                        + SQLManager.getIdColumn() + " UUID varchar(36), "
                        + "RecipePath varchar(100),"
                        + "Amount numeric,"
                        + "Timestamp BIGINT)")) {
            create.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionRecipeLimitsSQL:FusionRecipeLimitsSQL] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
        SQLManager.ensureIndex(Table, "fusion_limits_player_recipe", "UUID, RecipePath");
    }

    /** Fresh database state; expired cooldowns are reset regardless of which UI is open. */
    public synchronized PlayerRecipeLimit getRecipeLimit(UUID uuid, String path) {
        try {
            expire(uuid, path);
            try (PreparedStatement select = SQLManager.connection().prepareStatement(
                    "SELECT Amount, Timestamp FROM " + Table + " WHERE UUID=? AND RecipePath=? ORDER BY Id")) {
                select.setString(1, uuid.toString());
                select.setString(2, path);
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) return new PlayerRecipeLimit(uuid, path,
                            result.getInt("Amount"), result.getLong("Timestamp"));
                }
            }
            return new PlayerRecipeLimit(uuid, path, 0, -1);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read recipe limit for " + uuid + "/" + path, e);
        }
    }

    private void expire(UUID uuid, String path) throws SQLException {
        expire(SQLManager.connection(), uuid, path);
    }

    private void expire(Connection connection, UUID uuid, String path) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM " + Table + " WHERE UUID=? AND RecipePath=? AND Timestamp>0 AND Timestamp<=?")) {
            delete.setString(1, uuid.toString());
            delete.setString(2, path);
            delete.setLong(3, System.currentTimeMillis());
            delete.executeUpdate();
        }
    }

    /** Increment in SQL, never overwrite an amount from a session snapshot. */
    public synchronized void incrementLimit(UUID uuid, String path, int amount, int cooldownSeconds) {
        try {
            expire(uuid, path);
            ensureRow(uuid, path);
            String sql = "UPDATE " + Table + " SET Amount=Amount+?"
                    + (cooldownSeconds > 0 ? ", Timestamp=?" : "") + " WHERE UUID=? AND RecipePath=?";
            try (PreparedStatement update = SQLManager.connection().prepareStatement(sql)) {
                int index = 1;
                update.setInt(index++, amount);
                if (cooldownSeconds > 0)
                    update.setLong(index++, System.currentTimeMillis() + cooldownSeconds * 1000L);
                update.setString(index++, uuid.toString());
                update.setString(index, path);
                update.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot increment recipe limit for " + uuid + "/" + path, e);
        }
    }

    private void ensureRow(UUID uuid, String path) throws SQLException {
        ensureRow(SQLManager.connection(), uuid, path);
    }

    private void ensureRow(Connection connection, UUID uuid, String path) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO " + Table + " (UUID, RecipePath, Amount, Timestamp) "
                        + "SELECT ?, ?, 0, -1 WHERE NOT EXISTS (SELECT 1 FROM " + Table
                        + " WHERE UUID=? AND RecipePath=?)")) {
            insert.setString(1, uuid.toString());
            insert.setString(2, path);
            insert.setString(3, uuid.toString());
            insert.setString(4, path);
            insert.executeUpdate();
        }
    }

    /** Used inside the queue-claim transaction so a failed limit write cannot lose a craft. */
    public boolean consumeLimit(Connection connection, UUID uuid, String path, int maximum,
                                int cooldownSeconds) throws SQLException {
        if (maximum <= 0) return true;
        expire(connection, uuid, path);
        ensureRow(connection, uuid, path);
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE " + Table + " SET Amount=Amount+1, Timestamp=? WHERE UUID=? AND RecipePath=? AND Amount<?")) {
            update.setLong(1, cooldownSeconds > 0 ? System.currentTimeMillis() + cooldownSeconds * 1000L : -1L);
            update.setString(2, uuid.toString());
            update.setString(3, path);
            update.setInt(4, maximum);
            return update.executeUpdate() == 1;
        }
    }

    public synchronized void resetLimit(UUID uuid, String path) {
        try (PreparedStatement delete = SQLManager.connection().prepareStatement(
                "DELETE FROM " + Table + " WHERE UUID=? AND RecipePath=?")) {
            delete.setString(1, uuid.toString());
            delete.setString(2, path);
            delete.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot reset recipe limit", e);
        }
    }

    public synchronized void updateCooldown(UUID uuid, String path, long timestamp) {
        try {
            ensureRow(uuid, path);
            try (PreparedStatement update = SQLManager.connection().prepareStatement(
                    "UPDATE " + Table + " SET Timestamp=? WHERE UUID=? AND RecipePath=?")) {
                update.setLong(1, timestamp);
                update.setString(2, uuid.toString());
                update.setString(3, path);
                update.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update recipe cooldown", e);
        }
    }

    public synchronized Map<String, PlayerRecipeLimit> getRecipeLimits(UUID uuid) {
        Map<String, PlayerRecipeLimit> limits = new HashMap<>();
        try (PreparedStatement select = SQLManager.connection().prepareStatement(
                "SELECT * FROM " + Table + " WHERE UUID=?")) {
            select.setString(1, uuid.toString());
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    String path = result.getString("RecipePath");
                    limits.put(path, new PlayerRecipeLimit(uuid, path,
                            result.getInt("Amount"), result.getLong("Timestamp")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read recipe limits", e);
        }
        return limits;
    }

    /** Explicit import API only. Normal player saves must not replay limit snapshots. */
    public synchronized void saveRecipeLimits(UUID uuid, Map<String, PlayerRecipeLimit> limits) {
        for (Map.Entry<String, PlayerRecipeLimit> entry : limits.entrySet()) {
            try {
                ensureRow(uuid, entry.getKey());
                try (PreparedStatement update = SQLManager.connection().prepareStatement(
                        "UPDATE " + Table + " SET Amount=?, Timestamp=? WHERE UUID=? AND RecipePath=?")) {
                    update.setInt(1, entry.getValue().getLimit());
                    update.setLong(2, entry.getValue().getCooldownTimestamp());
                    update.setString(3, uuid.toString());
                    update.setString(4, entry.getKey());
                    update.executeUpdate();
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Cannot import recipe limit", e);
            }
        }
    }
}
