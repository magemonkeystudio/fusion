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
        try (PreparedStatement delete = SQLManager.connection()
                .prepareStatement("DELETE FROM " + Table + " WHERE UUID = ?")) {
            delete.setString(1, uuid.toString());
            delete.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionRecipeLimitsSQL:saveRecipeLimits] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
        try (PreparedStatement insert = SQLManager.connection()
                .prepareStatement(
                        "INSERT INTO " + Table + "(Id, UUID, RecipePath, Amount, Timestamp) VALUES(?,?,?,?,?)")) {
            for (Map.Entry<String, PlayerRecipeLimit> entry : recipeLimits.entrySet()) {
                if (entry.getValue().getLimit() <= 0) continue;
                insert.setLong(1, getNextId());
                insert.setString(2, uuid.toString());
                insert.setString(3, entry.getKey());
                insert.setInt(4, entry.getValue().getLimit());
                insert.setLong(5, entry.getValue().getCooldownTimestamp());
                insert.execute();
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionRecipeLimitsSQL:saveRecipeLimits] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
    }
}
