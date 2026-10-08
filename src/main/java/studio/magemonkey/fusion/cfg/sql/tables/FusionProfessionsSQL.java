package studio.magemonkey.fusion.cfg.sql.tables;

import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.professions.Profession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FusionProfessionsSQL {

    private final String Table = "fusion_professions";

    public FusionProfessionsSQL() {
        try (PreparedStatement create = SQLManager.connection()
                .prepareStatement("CREATE TABLE IF NOT EXISTS " + Table + "("
                        + SQLManager.getIdColumn() + " UUID varchar(36), "
                        + "Profession varchar(100),"
                        + "Experience numeric,"
                        + "Mastered boolean,"
                        + "Joined boolean)")) {
            create.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionProfessionsSQL:FusionProfessionsSQL] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
        SQLManager.ensureIndex(Table, "fusion_professions_player_name", "UUID, Profession");
    }

    public void setProfession(UUID uuid, Profession profession) {
        if (hasProfession(uuid, profession.getName())) {
            updateProfession(profession);
        } else {
            addProfession(profession);
        }
    }

    public void addProfession(Profession profession) {
        try (PreparedStatement insert = SQLManager.connection()
                .prepareStatement("INSERT INTO " + Table
                        + "(UUID, Profession, Experience, Mastered, Joined) VALUES(?,?,?,?,?)")) {
            insert.setString(1, profession.getUuid().toString());
            insert.setString(2, profession.getName());
            insert.setDouble(3, profession.getExp());
            insert.setBoolean(4, profession.isMastered());
            insert.setBoolean(5, profession.isJoined());
            insert.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionProfessionsSQL:addProfession] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
    }

    public void updateProfession(Profession profession) {
        try (PreparedStatement update = SQLManager.connection()
                .prepareStatement("UPDATE " + Table + " SET Experience=?, Mastered=?, Joined=? WHERE UUID=? AND Profession=?")) {
            update.setDouble(1, profession.getExp());
            update.setBoolean(2, profession.isMastered());
            update.setBoolean(3, profession.isJoined());
            update.setString(4, profession.getUuid().toString());
            update.setString(5, profession.getName());
            update.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionProfessionsSQL:updateProfession] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
    }

    public void incrementExperience(Profession profession, long amount) {
        if (!hasProfession(profession.getUuid(), profession.getName())) addProfession(profession);
        try (PreparedStatement update = SQLManager.connection().prepareStatement(
                "UPDATE " + Table + " SET Experience=Experience+? WHERE UUID=? AND Profession=?")) {
            update.setLong(1, amount);
            update.setString(2, profession.getUuid().toString());
            update.setString(3, profession.getName());
            update.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update profession experience", e);
        }
    }

    public void setValue(Profession profession, String column, Object value) {
        if (!java.util.Set.of("Experience", "Mastered", "Joined").contains(column))
            throw new IllegalArgumentException("Unknown profession column: " + column);
        if (!hasProfession(profession.getUuid(), profession.getName())) addProfession(profession);
        try (PreparedStatement update = SQLManager.connection().prepareStatement(
                "UPDATE " + Table + " SET " + column + "=? WHERE UUID=? AND Profession=?")) {
            update.setObject(1, value);
            update.setString(2, profession.getUuid().toString());
            update.setString(3, profession.getName());
            update.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update profession " + column, e);
        }
    }

    public boolean hasProfession(UUID uuid, String profession) {
        try (PreparedStatement select = SQLManager.connection()
                .prepareStatement("SELECT * FROM " + Table + " WHERE UUID=? AND Profession=?")) {
            select.setString(1, uuid.toString());
            select.setString(2, profession);
            return select.executeQuery().next();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning("[SQL:FusionProfessionsSQL:hasProfession] Something went wrong with the sql-connection: "
                            + e.getMessage());
        }
        return false;
    }

    public void removeProfession(String uuid, String profession) {
        try (PreparedStatement delete = SQLManager.connection()
                .prepareStatement("DELETE FROM " + Table + " WHERE UUID=? AND Profession=?")) {
            delete.setString(1, uuid);
            delete.setString(2, profession);
            delete.execute();
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .warning(
                            "[SQL:FusionProfessionsSQL:removeProfession] Something went wrong with the sql-connection: "
                                    + e.getMessage());
        }
    }

    public List<Profession> getProfessions(UUID uuid) {
        return readProfessions(uuid, null);
    }

    public List<Profession> getProfessions(UUID uuid, boolean joined) {
        return readProfessions(uuid, joined);
    }

    public Profession getProfession(UUID uuid, String name) {
        try (PreparedStatement select = SQLManager.connection().prepareStatement(
                "SELECT * FROM " + Table + " WHERE UUID=? AND Profession=?")) {
            select.setString(1, uuid.toString());
            select.setString(2, name);
            try (ResultSet result = select.executeQuery()) {
                return result.next() ? new Profession(result.getLong("Id"), uuid, name,
                        result.getLong("Experience"), result.getBoolean("Mastered"), result.getBoolean("Joined")) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read profession " + name, e);
        }
    }

    private List<Profession> readProfessions(UUID uuid, Boolean joined) {
        List<Profession> entries = new ArrayList<>();
        try (PreparedStatement select = SQLManager.connection()
                .prepareStatement("SELECT * FROM " + Table + " WHERE UUID=?" + (joined == null ? "" : " AND Joined=?"))) {
            select.setString(1, uuid.toString());
            if (joined != null) select.setBoolean(2, joined);
            try (ResultSet result = select.executeQuery()) {
            while (result.next()) {
                long    id         = result.getLong("Id");
                String  profession = result.getString("Profession");
                long    exp        = result.getLong("Experience");
                boolean mastered   = result.getBoolean("Mastered");
                boolean joined1    = result.getBoolean("Joined");
                entries.add(new Profession(id, uuid, profession, exp, mastered, joined1));
            }
            }
            return entries;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read professions", e);
        }
    }
}
