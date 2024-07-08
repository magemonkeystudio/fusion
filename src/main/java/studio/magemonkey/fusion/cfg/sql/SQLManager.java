package studio.magemonkey.fusion.cfg.sql;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.tables.FusionPlayersSQL;
import studio.magemonkey.fusion.cfg.sql.tables.FusionProfessionsSQL;
import studio.magemonkey.fusion.cfg.sql.tables.FusionQueuesSQL;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLManager {

    @Getter
    private static volatile Connection connection;

    private static FusionPlayersSQL fusionPlayersSQL;
    private static FusionProfessionsSQL fusionProfessionsSQL;
    private static FusionQueuesSQL fusionQueuesSQL;

    public static void init() {
        FileConfiguration cfg = Fusion.getInstance().getConfig();
        DatabaseType type = DatabaseType.valueOf(cfg.getString("storage.type", "LOCALE").toUpperCase());
        String host = cfg.getString("storage.host", "localhost");
        int port = cfg.getInt("storage.port", 3306);
        String database = cfg.getString("storage.database", "fusion");
        String user = cfg.getString("storage.user", "root");
        String password = cfg.getString("storage.password", "password");

        Fusion.getInstance().getLogger().info("Initializing SQLManager with type: " + type);

        switch (type) {
            case LOCALE:
                connection = getSQLiteConnection();
                break;
            case MYSQL:
                connection = getMySQLConnection(host, port, database, user, password);
                break;
            case MARIADB:
                connection = getMariaDBConnection(host, port, database, user, password);
                break;
        }

        if (connection == null) {
            Fusion.getInstance().getLogger().severe("Failed to initialize the Connection.");
        } else {
            fusionPlayersSQL = new FusionPlayersSQL();
            fusionProfessionsSQL = new FusionProfessionsSQL();
            fusionQueuesSQL = new FusionQueuesSQL();
            Fusion.getInstance().getLogger().info("Connection initialized successfully.");
        }
    }

    private static Connection getSQLiteConnection() {
        File databaseFile = new File(Fusion.getInstance().getDataFolder(), "database.db");
        databaseFile.getParentFile().mkdirs(); // Ensure the parent directories exist
        try {
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            Connection conn = DriverManager.getConnection(url);

            Fusion.getInstance().getLogger().info("SQLite connection created at: " + databaseFile.getAbsolutePath());
            return conn;
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().severe("Error creating SQLite connection: " + e.getMessage());
        }
        return null;
    }

    private static Connection getMySQLConnection(String host, int port, String database, String user, String password) {
        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            Connection conn = DriverManager.getConnection(url, user, password);
            Fusion.getInstance().getLogger().info("MySQL connection created for database: " + database);
            return conn;
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().severe("Error creating MySQL connection: " + e.getMessage());
        }
        return null;
    }

    private static Connection getMariaDBConnection(String host, int port, String database, String user, String password) {
        try {
            String url = "jdbc:mariadb://" + host + ":" + port + "/" + database;
            Connection conn = DriverManager.getConnection(url, user, password);
            Fusion.getInstance().getLogger().info("MariaDB connection created for database: " + database);
            return conn;
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().severe("Error creating MariaDB connection: " + e.getMessage());
        }
        return null;
    }

    public static Connection connection() {
        if (connection == null) {
            init();
        }
        if (connection != null) {
            return connection;
        } else {
            Fusion.getInstance().getLogger().severe("Connection is still null after initialization attempt.");
        }
        return null;
    }

    public static FusionPlayersSQL players() {
        if (fusionPlayersSQL == null) {
            fusionPlayersSQL = new FusionPlayersSQL();
        }
        return fusionPlayersSQL;
    }

    public static FusionProfessionsSQL professions() {
        if (fusionProfessionsSQL == null) {
            fusionProfessionsSQL = new FusionProfessionsSQL();
        }
        return fusionProfessionsSQL;
    }

    public static FusionQueuesSQL queues() {
        if (fusionQueuesSQL == null) {
            fusionQueuesSQL = new FusionQueuesSQL();
        }
        return fusionQueuesSQL;
    }
}
