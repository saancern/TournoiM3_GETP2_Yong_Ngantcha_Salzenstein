package my.insa.yong.utils.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modern connection pool implementation using HikariCP for better performance
 * and multi-user support.
 * 
 * @author saancern
 */
public class ConnectionPool {
    
    private static final Logger LOGGER = Logger.getLogger(ConnectionPool.class.getName());
    private static HikariDataSource dataSource;
    private static volatile boolean initialized = false;
    
    // Configuration par défaut (peut être surchargée)
    private static final String DEFAULT_HOST = "92.222.25.165";
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DATABASE = "m3_syong01";
    private static final String DEFAULT_USERNAME = "m3_syong01";
    private static final String DEFAULT_PASSWORD = "46f1dd1c";
    
    // Empêcher l'instanciation
    private ConnectionPool() {}
    
    /**
     * Initialise le pool de connexions avec les paramètres par défaut
     */
    public static synchronized void initialize() {
        initialize(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_DATABASE, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }
    
    /**
     * Initialise le pool de connexions avec des paramètres personnalisés
     */
    public static synchronized void initialize(String host, int port, String database, String username, String password) {
        if (initialized) {
            LOGGER.warning("ConnectionPool already initialized");
            return;
        }
        
        try {
            // Vérifier la disponibilité du driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            HikariConfig config = new HikariConfig();
            
            // Configuration de base
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s", host, port, database));
            config.setUsername(username);
            config.setPassword(password);
            
            // Configuration du pool
            config.setMaximumPoolSize(20); // Augmenté pour supporter plus d'utilisateurs
            config.setMinimumIdle(5);
            config.setIdleTimeout(300000); // 5 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setConnectionTimeout(30000); // 30 secondes
            config.setLeakDetectionThreshold(60000); // 1 minute
            
            // Optimisations MySQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useUnicode", "true");
            config.addDataSourceProperty("characterEncoding", "utf8");
            config.addDataSourceProperty("serverTimezone", "UTC");
            
            // Isolation des transactions
            config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");
            
            // Validation des connexions
            config.setConnectionTestQuery("SELECT 1");
            
            // Pool name pour debugging
            config.setPoolName("TournoiPool");
            
            dataSource = new HikariDataSource(config);
            initialized = true;
            
            LOGGER.info("ConnectionPool initialized successfully with " + config.getMaximumPoolSize() + " max connections");
            
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "MySQL driver not found", ex);
            throw new RuntimeException("MySQL driver not found", ex);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize connection pool", ex);
            throw new RuntimeException("Failed to initialize connection pool", ex);
        }
    }
    
    /**
     * Obtient une connexion du pool
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initialize();
        }
        
        try {
            Connection connection = dataSource.getConnection();
            LOGGER.fine("Connection obtained from pool. Active connections: " + dataSource.getHikariPoolMXBean().getActiveConnections());
            return connection;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to get connection from pool", ex);
            throw ex;
        }
    }
    
    /**
     * Ferme le pool de connexions proprement
     */
    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            LOGGER.info("Shutting down connection pool...");
            dataSource.close();
            initialized = false;
            LOGGER.info("Connection pool shut down successfully");
        }
    }
    
    /**
     * Obtient des statistiques du pool pour monitoring
     */
    public static String getPoolStats() {
        if (!initialized || dataSource == null) {
            return "Pool not initialized";
        }
        
        var mxBean = dataSource.getHikariPoolMXBean();
        return String.format(
            "Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
            mxBean.getActiveConnections(),
            mxBean.getIdleConnections(),
            mxBean.getTotalConnections(),
            mxBean.getThreadsAwaitingConnection()
        );
    }
    
    /**
     * Vérifie si le pool est initialisé et fonctionnel
     */
    public static boolean isHealthy() {
        if (!initialized || dataSource == null || dataSource.isClosed()) {
            return false;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // Timeout de 5 secondes
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Health check failed", ex);
            return false;
        }
    }
    
    /**
     * Génère le SQL pour les clés auto-générées selon le SGBD
     */
    public static String sqlForGeneratedKeys(Connection con, String nomColonne) throws SQLException {
        String sgbdName = con.getMetaData().getDatabaseProductName();
        if (sgbdName.equals("MySQL")) {
            return nomColonne + "  INT AUTO_INCREMENT PRIMARY KEY";
        } else {
            return nomColonne + " INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY";
        }
    }
}
