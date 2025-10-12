package studio.magemonkey.fusion.cfg.sql.tables;

import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.player.PlayerRecipeLimit;

import java.sql.PreparedStatement;
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
                        + "Id long,"
                        + "UUID varchar(36), "
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
    }

    public long getNextId() {
        try (PreparedStatement select = SQLManager.connection().prepareStatement("SELECT Count(Id) FROM " + Table)) {
            ResultSet result = select.executeQuery();
            if (result.next()) {
                return result.getLong(1);
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionRecipeLimitsSQL:getNextId] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return 0;
    }

    public Map<String, PlayerRecipeLimit> getRecipeLimits(UUID uuid) {
        Map<String, PlayerRecipeLimit> limits = new HashMap<>();
        try (PreparedStatement select = SQLManager.connection()
                .prepareStatement("SELECT * FROM " + Table + " WHERE UUID = ?")) {
            select.setString(1, uuid.toString());
            ResultSet result = select.executeQuery();
            while (result.next()) {
                String recipePath = result.getString("RecipePath");
                int    amount     = result.getInt("Amount");
                long   timestamp  = result.getLong("Timestamp");
                limits.put(recipePath, new PlayerRecipeLimit(recipePath, amount, timestamp));
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionRecipeLimitsSQL:getRecipeLimits] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
        return limits;
    }

    public void saveRecipeLimits(UUID uuid, Map<String, PlayerRecipeLimit> recipeLimits) {
        // Get all current limits from DB
        Map<String, PlayerRecipeLimit> currentLimits = getRecipeLimits(uuid);

        // Remove limits that are no longer present in the provided map
        for (String recipePath : currentLimits.keySet()) {
            if (!recipeLimits.containsKey(recipePath)) {
                PlayerRecipeLimit limit = currentLimits.get(recipePath);
                // Limits mit Timestamp = -1 nicht löschen, außer explizit entfernt
                if (limit.getCooldownTimestamp() != -1) {
                    try (PreparedStatement delete = SQLManager.connection().prepareStatement(
                            "DELETE FROM " + Table + " WHERE UUID = ? AND RecipePath = ?")) {
                        delete.setString(1, uuid.toString());
                        delete.setString(2, recipePath);
                        delete.execute();
                    } catch (SQLException e) {
                        Fusion.getInstance().getLogger().warning(
                                "[SQL:FusionRecipeLimitsSQL:saveRecipeLimits] Error at deletion: " + e.getMessage());
                    }
                }
            }
        }

        //  Update or Insert limits
        for (Map.Entry<String, PlayerRecipeLimit> entry : recipeLimits.entrySet()) {
            String recipePath = entry.getKey();
            PlayerRecipeLimit limit = entry.getValue();
            if (limit.getLimit() <= 0) {
                // Falls Limit <= 0, löschen (außer Timestamp = -1)
                if (limit.getCooldownTimestamp() != -1) {
                    try (PreparedStatement delete = SQLManager.connection().prepareStatement(
                            "DELETE FROM " + Table + " WHERE UUID = ? AND RecipePath = ?")) {
                        delete.setString(1, uuid.toString());
                        delete.setString(2, recipePath);
                        delete.execute();
                    } catch (SQLException e) {
                        Fusion.getInstance().getLogger().warning(
                                "[SQL:FusionRecipeLimitsSQL:saveRecipeLimits] Error at deletion: " + e.getMessage());
                    }
                }
                continue;
            }
            // Check if the limit already exists
            if (currentLimits.containsKey(recipePath)) {
                // Update
                try (PreparedStatement update = SQLManager.connection().prepareStatement(
                        "UPDATE " + Table + " SET Amount = ?, Timestamp = ? WHERE UUID = ? AND RecipePath = ?")) {
                    update.setInt(1, limit.getLimit());
                    update.setLong(2, limit.getCooldownTimestamp());
                    update.setString(3, uuid.toString());
                    update.setString(4, recipePath);
                    update.execute();
                } catch (SQLException e) {
                    Fusion.getInstance().getLogger().warning(
                            "[SQL:FusionRecipeLimitsSQL:saveRecipeLimits] Error at update: " + e.getMessage());
                }
            } else {
                // Insert
                try (PreparedStatement insert = SQLManager.connection().prepareStatement(
                        "INSERT INTO " + Table + "(Id, UUID, RecipePath, Amount, Timestamp) VALUES(?,?,?,?,?)")) {
                    insert.setLong(1, getNextId());
                    insert.setString(2, uuid.toString());
                    insert.setString(3, recipePath);
                    insert.setInt(4, limit.getLimit());
                    insert.setLong(5, limit.getCooldownTimestamp());
                    insert.execute();
                } catch (SQLException e) {
                    Fusion.getInstance().getLogger().warning(
                            "[SQL:FusionRecipeLimitsSQL:saveRecipeLimits] Error at insert: " + e.getMessage());
                }
            }
        }
    }
}
