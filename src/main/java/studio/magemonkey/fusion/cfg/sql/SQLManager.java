package studio.magemonkey.fusion.cfg.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.sql.tables.FusionProfessionsSQL;
import studio.magemonkey.fusion.cfg.sql.tables.FusionQueuesSQL;

import javax.sql.DataSource;
import java.sql.Connection;

public class SQLManager {

    @Getter
    private static DataSource dataSource;
    private static DatabaseType type;
    private static String host, database, user, password;
    private static int port;

    private static FusionProfessionsSQL fusionProfessionsSQL;
    private static FusionQueuesSQL fusionQueuesSQL;

    public static void init() {
        FileConfiguration cfg = Fusion.getInstance().getConfig();
        SQLManager.type = DatabaseType.valueOf(cfg.getString("storage.type", "LOCALE").toUpperCase());
        SQLManager.host = cfg.getString("storage.host", "localhost");
        SQLManager.port = cfg.getInt("storage.port", 3306);
        SQLManager.database = cfg.getString("storage.database", "fusion");
        SQLManager.user = cfg.getString("storage.user", "root");
        SQLManager.password = cfg.getString("storage.password", "password");

        switch (type) {
            case LOCALE:
                dataSource = getSQLiteDataSource(database);
                break;
            case MYSQL:
                dataSource = getMySQLDataSource(host, port, database, user, password);
                break;
            case MARIADB:
                dataSource = getMariaDBDataSource(host, port, database, user, password);
                break;
        }
    }
    private static DataSource getSQLiteDataSource(String dbPath) {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    private static DataSource getMySQLDataSource(String host, int port, String database, String user, String password) {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(user);
            config.setPassword(password);
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    private static DataSource getMariaDBDataSource(String host, int port, String database, String user, String password) {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
            config.setUsername(user);
            config.setPassword(password);
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    public static Connection connection() {
        if (dataSource != null) {
            try {
                return dataSource.getConnection();
            } catch (Exception e) {
                Fusion.getInstance().getLogger().severe("Error while connecting to the database: " + e.getMessage());
                return null;
            }
        } else {
            init();
            return dataSource != null ? connection() : null;
        }
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
