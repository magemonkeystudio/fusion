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
        try (PreparedStatement create = SQLManager.connection()
                .prepareStatement("CREATE TABLE IF NOT EXISTS " + Table + "("
                        + "UUID varchar(36), "
                        + "AutoCrafting boolean, "
                        + "Locked boolean)")) {
            create.execute();

            boolean lockedColumnAdded = alterIfLockedNotExistent();
            if (lockedColumnAdded) {
                Fusion.getInstance()
                        .getLogger()
                        .info("[SQL:FusionPlayersSQL:FusionPlayersSQL] Added 'Locked' column to 'fusion_players' table.");
            }
            
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionPlayersSQL:FusionPlayersSQL] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
    }

    public void setAutoCrafting(UUID uuid, boolean autoCrafting) {
        addPlayer(uuid);
        try (PreparedStatement update = SQLManager.connection()
                .prepareStatement("UPDATE " + Table + " SET AutoCrafting=? WHERE UUID=?")) {
            update.setBoolean(1, autoCrafting);
            update.setString(2, uuid.toString());
            update.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionPlayersSQL:setAutoCrafting] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
    }

    public void setLocked(UUID uuid, boolean locked) {
        addPlayer(uuid);
        try (PreparedStatement update = SQLManager.connection()
                .prepareStatement("UPDATE " + Table + " SET Locked=? WHERE UUID=?")) {
            update.setBoolean(1, locked);
            update.setString(2, uuid.toString());
            update.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionPlayersSQL:setLocked] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
    }

    public boolean isLocked(UUID uuid) {
        try (PreparedStatement select = SQLManager.connection()
                .prepareStatement("SELECT Locked FROM " + Table + " WHERE UUID=?")) {
            select.setString(1, uuid.toString());
            ResultSet result = select.executeQuery();
            if (result.next())
                return result.getBoolean("Locked");
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionPlayersSQL:isLocked] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return false;
    }

    public void addPlayer(UUID uuid) {
        if (hasPlayer(uuid))
            return;
        try (PreparedStatement insert = SQLManager.connection()
                .prepareStatement("INSERT INTO " + Table + "(UUID, AutoCrafting, Locked) VALUES(?,?,?)")) {
            insert.setString(1, uuid.toString());
            insert.setBoolean(2, false);
            insert.setBoolean(3, false);
            insert.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionPlayersSQL:addPlayer] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
    }

    public boolean hasPlayer(UUID uuid) {
        try (PreparedStatement select = SQLManager.connection()
                .prepareStatement("SELECT * FROM " + Table + " WHERE UUID=?")) {
            select.setString(1, uuid.toString());
            ResultSet result = select.executeQuery();
            if (result.next())
                return true;
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionPlayersSQL:hasPlayer] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return false;
    }

    public boolean isAutoCrafting(UUID uuid) {
        try (PreparedStatement select = SQLManager.connection()
                .prepareStatement("SELECT AutoCrafting FROM " + Table + " WHERE UUID=?")) {
            select.setString(1, uuid.toString());
            ResultSet result = select.executeQuery();
            if (result.next())
                return result.getBoolean("AutoCrafting");
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionPlayersSQL:isAutoCrafting] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return false;
    }

    public boolean alterIfLockedNotExistent() {
        try (PreparedStatement select = SQLManager.connection()
                .prepareStatement("SELECT Locked FROM " + Table + " LIMIT 1")) {
            ResultSet result = select.executeQuery();
            if (result.next())
                return false;
        } catch (SQLException e) {
            // Column does not exist, we need to add it
            try (PreparedStatement alter = SQLManager.connection()
                    .prepareStatement("ALTER TABLE " + Table + " ADD COLUMN Locked boolean DEFAULT false")) {
                alter.execute();
                return true;
            } catch (SQLException ex) {
                Fusion.getInstance()
                        .getLogger()
                        .warning("[SQL:FusionPlayersSQL:alterIfLockedNotExistent] Something went wrong with the sql-connection: "
                                + ex.getMessage());
            }
        }
        return false;
    }
}
