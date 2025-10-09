package my.insa.yong.utils.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Définition d'une "connecion pool" à l'aide de la librairie HikariCP.
 * <pre>
 * repris de https://www.baeldung.com/hikaricp.
 * Voir https://github.com/brettwooldridge/hikaricp/wiki/MYSQL-Configuration pour
 * une explication de certains paramètres
 * </pre>
 *
 * @author saancern
 */
public class ConnectionPool {

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    // un bloc static directement dans une classe est exécuté au chargement
    // de la classe
    // pour une BdD en mémoire en utilisant le sgbd H2
    static {
        config.setJdbcUrl("jdbc:h2:mem:pourCoursVaadin");
        // peut être pas indispensable, mais dans le doute...
        config.setUsername("inutilePourH2Mem");
        config.setPassword("inutilePourH2Mem");
        config.setMaximumPoolSize(10);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");
        ds = new HikariDataSource(config);
    }
    // pour une BdD en utilisant le sgbd mysql pour module M3
//    static {
//        try {
//            Class.forName("com.mysql.cj.jdbc.Driver");
//        } catch (ClassNotFoundException ex) {
//            throw new Error("driver mysql not found", ex);
//        }
//        config.setJdbcUrl("jdbc:mysql://92.222.25.165:3306/m3_fdebertranddeb01");
//        config.setUsername("m3_fdebertranddeb01");
//        config.setPassword("je le donne pas");
//        config.setMaximumPoolSize(10);
//        config.addDataSourceProperty("cachePrepStmts", "true");
//        config.addDataSourceProperty("useServerPrepStmts", "true");
//        config.addDataSourceProperty("prepStmtCacheSize", "250");
//        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
//        config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");
//        ds = new HikariDataSource(config);
//    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
