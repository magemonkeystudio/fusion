package studio.magemonkey.fusion.cfg.sql.tables;

import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.SQLManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class FusionPlayersSQL {

    private static final String Table = "fusion_players";

    // TODO Might have potential for further storage stuff
    public FusionPlayersSQL() {
        try (PreparedStatement create = SQLManager.connection().prepareStatement("CREATE TABLE IF NOT EXISTS " + Table + "("
                + "UUID varchar(36), "
                + "AutoCrafting boolean)")) {
            create.execute();
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().warning("[SQL:FusionPlayersSQL:FusionPlayersSQL] Something went wrong with the sql-connection: " + e.getMessage());
        }
    }

    public void setAutoCrafting(UUID uuid, boolean autoCrafting) {
        if(!hasPlayer(uuid))
            addPlayer(uuid);

        try (PreparedStatement update = SQLManager.connection().prepareStatement("UPDATE " + Table + " SET AutoCrafting=? WHERE UUID=?")) {
            update.setBoolean(1, autoCrafting);
            update.setString(2, uuid.toString());
            update.execute();
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().warning("[SQL:FusionPlayersSQL:setAutoCrafting] Something went wrong with the sql-connection: " + e.getMessage());
        }
    }

    public void addPlayer(UUID uuid) {
        try (PreparedStatement insert = SQLManager.connection().prepareStatement("INSERT INTO " + Table + "(UUID, AutoCrafting) VALUES(?,?)")) {
            insert.setString(1, uuid.toString());
            insert.setBoolean(2, false);
            insert.execute();
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().warning("[SQL:FusionPlayersSQL:addPlayer] Something went wrong with the sql-connection: " + e.getMessage());
        }
    }

    public boolean hasPlayer(UUID uuid) {
        try (PreparedStatement select = SQLManager.connection().prepareStatement("SELECT AutoCrafting FROM " + Table + " WHERE UUID=?")) {
            select.setString(1, uuid.toString());
            ResultSet result = select.executeQuery();
            if(result.next())
                return result.getBoolean("AutoCrafting");
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().warning("[SQL:FusionPlayersSQL:hasPlayer] Something went wrong with the sql-connection: " + e.getMessage());
        }
        return false;
    }
}
