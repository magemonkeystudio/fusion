package studio.magemonkey.fusion.cfg.sql;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.sql.tables.FusionPlayersSQL;
import studio.magemonkey.fusion.cfg.sql.tables.FusionProfessionsSQL;
import studio.magemonkey.fusion.cfg.sql.tables.FusionQueuesSQL;

import java.io.File;
import java.sql.*;

public class SQLManager {

    @Getter
    private static volatile Connection connection;

    private static FusionPlayersSQL     fusionPlayersSQL;
    private static FusionProfessionsSQL fusionProfessionsSQL;
    private static FusionQueuesSQL      fusionQueuesSQL;

    private static String host;
    private static int    port;
    private static String database;
    private static String user;
    private static String password;

    public static void init() {
        FileConfiguration cfg  = Cfg.getConfig();
        DatabaseType      type = DatabaseType.valueOf(cfg.getString("storage.type", "LOCAL").toUpperCase());
        host = cfg.getString("storage.host", "localhost");
        port = cfg.getInt("storage.port", 3306);
        database = cfg.getString("storage.database", "fusion");
        user = cfg.getString("storage.user", "root");
        password = cfg.getString("storage.password", "password");

        Fusion.getInstance().getLogger().info("Initializing SQLManager with type: " + type);

        switch (type) {
            case LOCAL:
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
            String     url  = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
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
            String     url  = "jdbc:mysql://" + host + ":" + port + "/" + database;
            Connection conn = DriverManager.getConnection(url, user, password);
            Fusion.getInstance().getLogger().info("MySQL connection created for database: " + database);
            return conn;
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().severe("Error creating MySQL connection: " + e.getMessage());
        }
        return null;
    }

    private static Connection getMariaDBConnection(String host,
                                                   int port,
                                                   String database,
                                                   String user,
                                                   String password) {
        try {
            String     url  = "jdbc:mariadb://" + host + ":" + port + "/" + database;
            Connection conn = DriverManager.getConnection(url, user, password);
            Fusion.getInstance().getLogger().info("MariaDB connection created for database: " + database);
            return conn;
        } catch (SQLException e) {
            Fusion.getInstance().getLogger().severe("Error creating MariaDB connection: " + e.getMessage());
        }
        return null;
    }

    public static Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
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

    public static void swapToLocal() {
        // Delete all content of the local database
        try (Connection sqliteConnection = getSQLiteConnection();
             Statement statement = sqliteConnection.createStatement()) {

            statement.execute("DROP TABLE IF EXISTS fusion_players");
            statement.execute("DROP TABLE IF EXISTS fusion_professions");
            statement.execute("DROP TABLE IF EXISTS fusion_queues");
            statement.execute("CREATE TABLE IF NOT EXISTS fusion_players(UUID varchar(36), AutoCrafting boolean)");
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS fusion_professions(Id long, UUID varchar(36), Profession varchar(100), Experience numeric, Mastered boolean, Joined boolean)");
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS fusion_queues(Id long, UUID varchar(36), RecipePath varchar(100), Timestamp BIGINT, CraftingTime numeric, SavedSeconds numeric)");

        } catch (SQLException e) {
            Fusion.getInstance().getLogger().severe("Error while dropping tables: " + e.getMessage());
            return;
        }

        // Get all data from the current database
        try (Connection currentConnection = connection()) {
            // Retrieve data
            try (Statement statement = currentConnection.createStatement();
                 ResultSet resultPlayers = statement.executeQuery("SELECT * FROM fusion_players")) {

                try (Connection sqliteConnection = getSQLiteConnection();
                     PreparedStatement insertStatement = sqliteConnection.prepareStatement(
                             "INSERT INTO fusion_players (UUID, AutoCrafting) VALUES (?, ?)")) {
                    while (resultPlayers.next()) {
                        insertStatement.setString(1, resultPlayers.getString("UUID"));
                        insertStatement.setBoolean(2, resultPlayers.getBoolean("AutoCrafting"));
                        insertStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    Fusion.getInstance()
                            .getLogger()
                            .severe("Error while inserting data into locale database: " + e.getMessage());
                    return;
                }
            }

            try (Statement statement = currentConnection.createStatement();
                 ResultSet resultProfessions = statement.executeQuery("SELECT * FROM fusion_professions")) {

                try (Connection sqliteConnection = getSQLiteConnection();
                     PreparedStatement insertStatement = sqliteConnection.prepareStatement(
                             "INSERT INTO fusion_professions (Id, UUID, Profession, Experience, Mastered, Joined) VALUES (?, ?, ?, ?, ?, ?)")) {
                    insertProfession(resultProfessions, insertStatement);
                } catch (SQLException e) {
                    Fusion.getInstance()
                            .getLogger()
                            .severe("Error while inserting data into locale database: " + e.getMessage());
                    return;
                }
            }

            try (Statement statement = currentConnection.createStatement();
                 ResultSet resultQueues = statement.executeQuery("SELECT * FROM fusion_queues")) {

                try (Connection sqliteConnection = getSQLiteConnection();
                     PreparedStatement insertStatement = sqliteConnection.prepareStatement(
                             "INSERT INTO fusion_queues (Id, UUID, RecipePath, Timestamp, CraftingTime, SavedSeconds) VALUES (?, ?, ?, ?, ?, ?)")) {
                    insertQueue(resultQueues, insertStatement);
                } catch (SQLException e) {
                    Fusion.getInstance()
                            .getLogger()
                            .severe("Error while inserting data into locale database: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .severe("Error while retrieving data from current database: " + e.getMessage());
            return;
        }

        if (Cfg.setDatabaseType(DatabaseType.LOCAL)) {
            Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), SQLManager::init, 30L);
        }
        Fusion.getInstance().getLogger().info("Swapped to LOCALE database successfully.");
    }

    public static void swapToSql() {
        // Get the connection to MySQL
        try (Connection sqlConnection = getMySQLConnection(host, port, database, user, password);
             Statement sqlStatement = sqlConnection.createStatement()) {

            // Delete all content of the current database and recreate tables
            sqlStatement.execute("DROP TABLE IF EXISTS fusion_players, fusion_professions, fusion_queues");
            sqlStatement.execute("CREATE TABLE IF NOT EXISTS fusion_players(UUID varchar(36), AutoCrafting boolean)");
            sqlStatement.execute(
                    "CREATE TABLE IF NOT EXISTS fusion_professions(Id long, UUID varchar(36), Profession varchar(100), Experience numeric, Mastered boolean, Joined boolean)");
            sqlStatement.execute(
                    "CREATE TABLE IF NOT EXISTS fusion_queues(Id long, UUID varchar(36), RecipePath varchar(100), Timestamp BIGINT, CraftingTime numeric, SavedSeconds numeric)");

            // Get all data from the local database
            try (Connection sqliteConnection = getSQLiteConnection();
                 Statement localStatement = sqliteConnection.createStatement()) {

                // Retrieve data from local database
                ResultSet resultPlayers = localStatement.executeQuery("SELECT * FROM fusion_players");
                insertPlayers(sqlConnection, resultPlayers);

                ResultSet resultProfessions = localStatement.executeQuery("SELECT * FROM fusion_professions");
                insertProfessions(sqlConnection, resultProfessions);

                ResultSet resultQueues = localStatement.executeQuery("SELECT * FROM fusion_queues");
                insertQueues(sqlConnection, resultQueues);

            } catch (SQLException e) {
                Fusion.getInstance()
                        .getLogger()
                        .severe("Error while retrieving data from local database: " + e.getMessage());
                return;
            }

        } catch (SQLException e) {
            Fusion.getInstance()
                    .getLogger()
                    .severe("Error while managing SQL connection or executing statements: " + e.getMessage());
            return;
        }

        // Update configuration and reinitialize
        if (Cfg.setDatabaseType(DatabaseType.MYSQL)) {
            Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), SQLManager::init, 30L);
        }
        Fusion.getInstance().getLogger().info("Swapped to SQL database successfully.");
    }

    private static void insertPlayers(Connection connection, ResultSet resultSet) throws SQLException {
        String insertQuery = "INSERT INTO fusion_players (UUID, AutoCrafting) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            while (resultSet.next()) {
                preparedStatement.setString(1, resultSet.getString("UUID"));
                preparedStatement.setBoolean(2, resultSet.getBoolean("AutoCrafting"));
                preparedStatement.executeUpdate();
            }
        }
    }

    private static void insertProfessions(Connection connection, ResultSet resultSet) throws SQLException {
        String insertQuery =
                "INSERT INTO fusion_professions (Id, UUID, Profession, Experience, Mastered, Joined) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            insertProfession(resultSet, preparedStatement);
        }
    }

    private static void insertQueues(Connection connection, ResultSet resultSet) throws SQLException {
        String insertQuery =
                "INSERT INTO fusion_queues (Id, UUID, RecipePath, Timestamp, CraftingTime, SavedSeconds) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            insertQueue(resultSet, preparedStatement);
        }
    }

    private static void insertQueue(ResultSet resultQueues, PreparedStatement insertStatement) throws SQLException {
        while (resultQueues.next()) {
            insertStatement.setLong(1, resultQueues.getLong("Id"));
            insertStatement.setString(2, resultQueues.getString("UUID"));
            insertStatement.setString(3, resultQueues.getString("RecipePath"));
            insertStatement.setLong(4, resultQueues.getLong("Timestamp"));
            insertStatement.setDouble(5, resultQueues.getDouble("CraftingTime"));
            insertStatement.setDouble(6, resultQueues.getDouble("SavedSeconds"));
            insertStatement.executeUpdate();
        }
    }

    private static void insertProfession(ResultSet resultProfessions, PreparedStatement insertStatement) throws
            SQLException {
        while (resultProfessions.next()) {
            insertStatement.setLong(1, resultProfessions.getLong("Id"));
            insertStatement.setString(2, resultProfessions.getString("UUID"));
            insertStatement.setString(3, resultProfessions.getString("Profession"));
            insertStatement.setDouble(4, resultProfessions.getDouble("Experience"));
            insertStatement.setBoolean(5, resultProfessions.getBoolean("Mastered"));
            insertStatement.setBoolean(6, resultProfessions.getBoolean("Joined"));
            insertStatement.executeUpdate();
        }
    }
}
