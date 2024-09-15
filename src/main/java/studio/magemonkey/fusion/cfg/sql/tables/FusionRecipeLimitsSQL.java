package studio.magemonkey.fusion.cfg.sql.tables;

import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.player.FusionPlayer;

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
                        + "Amount numeric)")) {
            create.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionRecipeLimitsSQL:FusionRecipeLimitsSQL] Something went wrong with the sql-connection: "
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

    public void incrementLimits(FusionPlayer player) {

    }

    public int getLimit(UUID uuid, String recipePath) {
        try (PreparedStatement select = SQLManager.connection().prepareStatement("SELECT * FROM " + Table + " WHERE UUID = ? AND RecipePath = ?")) {
            select.setString(1, uuid.toString());
            select.setString(2, recipePath);
            ResultSet result = select.executeQuery();
            if (result.next()) {
                return result.getInt("Amount");
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionRecipeLimitsSQL:getLimit] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return 0;
    }

    public void setLimit(UUID uuid, String recipePath, int limit) {
        try (PreparedStatement select = SQLManager.connection().prepareStatement("SELECT * FROM " + Table + " WHERE UUID = ? AND RecipePath = ?")) {
            select.setString(1, uuid.toString());
            select.setString(2, recipePath);
            ResultSet result = select.executeQuery();
            if (result.next()) {
                try (PreparedStatement update = SQLManager.connection().prepareStatement("UPDATE " + Table + " SET Amount = ? WHERE UUID = ? AND RecipePath = ?")) {
                    update.setInt(1, limit);
                    update.setString(2, uuid.toString());
                    update.setString(3, recipePath);
                    update.execute();
                }
            } else {
                try (PreparedStatement insert = SQLManager.connection().prepareStatement("INSERT INTO " + Table + "(Id, UUID, RecipePath, Amount) VALUES(?,?,?,?)")) {
                    insert.setLong(1, getNextId());
                    insert.setString(2, uuid.toString());
                    insert.setString(3, recipePath);
                    insert.setInt(4, limit);
                    insert.execute();
                }
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionRecipeLimitsSQL:setLimit] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
    }

    public Map<String, Integer> getRecipeLimits(UUID uuid) {
        Map<String, Integer> limits = new HashMap<>();
        try (PreparedStatement select = SQLManager.connection().prepareStatement("SELECT * FROM " + Table + " WHERE UUID = ?")) {
            select.setString(1, uuid.toString());
            ResultSet result = select.executeQuery();
            while (result.next()) {
                limits.put(result.getString("RecipePath"), result.getInt("Amount"));
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionRecipeLimitsSQL:getRecipeLimits] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return limits;
    }

    public void saveRecipeLimits(UUID uuid, Map<String, Integer> recipeLimits) {
        try (PreparedStatement delete = SQLManager.connection().prepareStatement("DELETE FROM " + Table)) {
            delete.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionRecipeLimitsSQL:saveRecipeLimits] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        try (PreparedStatement insert = SQLManager.connection().prepareStatement("INSERT INTO " + Table + "(Id, UUID, RecipePath, Amount) VALUES(?,?,?,?)")) {
            for (Map.Entry<String, Integer> entry : recipeLimits.entrySet()) {
                insert.setLong(1, getNextId());
                insert.setString(2, uuid.toString());
                insert.setString(3, entry.getKey());
                insert.setInt(4, entry.getValue());
                insert.execute();
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionRecipeLimitsSQL:saveRecipeLimits] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
    }
}
