package com.burstmeman.midi2keys.infrastructure.persistence.database;

import com.burstmeman.midi2keys.infrastructure.config.ApplicationConfig;
import com.burstmeman.midi2keys.infrastructure.persistence.database.migrations.MigrationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages SQLite database connections and lifecycle.
 * Implements singleton pattern for application-wide database access.
 */
public final class DatabaseManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String JDBC_URL_PREFIX = "jdbc:sqlite:";
    
    private static DatabaseManager instance;
    private static final ReentrantLock lock = new ReentrantLock();
    
    private Connection connection;
    private final Path databasePath;
    private boolean initialized = false;
    
    private DatabaseManager() {
        this.databasePath = ApplicationConfig.getDatabasePath();
    }
    
    /**
     * Gets the singleton instance of DatabaseManager.
     * 
     * @return The DatabaseManager instance
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            lock.lock();
            try {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }
    
    /**
     * Initializes the database - creates file and runs migrations.
     */
    public void initialize() {
        if (initialized) {
            logger.debug("Database already initialized");
            return;
        }
        
        lock.lock();
        try {
            if (initialized) {
                return;
            }
            
            logger.info("Initializing database at: {}", databasePath);
            
            // Ensure parent directory exists
            ensureDirectoryExists();
            
            // Create connection
            connection = createConnection();
            
            // Configure SQLite for performance
            configureSqlite();
            
            // Run migrations
            MigrationManager migrationManager = new MigrationManager(connection);
            migrationManager.runMigrations();
            
            initialized = true;
            logger.info("Database initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new DatabaseException("Failed to initialize database", e);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Gets a connection to the database.
     * 
     * @return Database connection
     * @throws DatabaseException if not initialized or connection fails
     */
    public Connection getConnection() {
        if (!initialized) {
            throw new DatabaseException("Database not initialized. Call initialize() first.");
        }
        
        try {
            if (connection == null || connection.isClosed()) {
                connection = createConnection();
                configureSqlite();
            }
            return connection;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get database connection", e);
        }
    }
    
    /**
     * Closes the database connection.
     */
    public void close() {
        lock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        } finally {
            connection = null;
            initialized = false;
            lock.unlock();
        }
    }
    
    /**
     * Checks if the database is initialized.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Gets the database file path.
     * 
     * @return Path to the database file
     */
    public Path getDatabasePath() {
        return databasePath;
    }
    
    /**
     * Executes a query within a transaction.
     * 
     * @param operation The operation to execute
     * @param <T> Return type
     * @return The result of the operation
     */
    public <T> T executeInTransaction(TransactionOperation<T> operation) {
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            T result = operation.execute(conn);
            conn.commit();
            return result;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackException) {
                logger.error("Failed to rollback transaction", rollbackException);
            }
            throw new DatabaseException("Transaction failed", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Failed to reset auto-commit", e);
            }
        }
    }
    
    /**
     * Functional interface for transaction operations.
     * 
     * @param <T> Return type
     */
    @FunctionalInterface
    public interface TransactionOperation<T> {
        T execute(Connection connection) throws Exception;
    }
    
    private void ensureDirectoryExists() throws Exception {
        Path parentDir = databasePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            logger.debug("Created database directory: {}", parentDir);
        }
    }
    
    private Connection createConnection() throws SQLException {
        String url = JDBC_URL_PREFIX + databasePath.toAbsolutePath();
        logger.debug("Creating database connection to: {}", url);
        return DriverManager.getConnection(url);
    }
    
    private void configureSqlite() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Enable foreign keys
            stmt.execute("PRAGMA foreign_keys = ON");
            
            // Use WAL mode for better concurrent access
            stmt.execute("PRAGMA journal_mode = WAL");
            
            // Synchronous mode for balance of safety and performance
            stmt.execute("PRAGMA synchronous = NORMAL");
            
            // Cache size (negative = KB, so -2000 = 2MB)
            stmt.execute("PRAGMA cache_size = -2000");
            
            // Temp store in memory
            stmt.execute("PRAGMA temp_store = MEMORY");
            
            logger.debug("SQLite PRAGMA settings configured");
        }
    }
    
    /**
     * Resets the singleton instance (primarily for testing).
     */
    static void resetInstance() {
        lock.lock();
        try {
            if (instance != null) {
                instance.close();
                instance = null;
            }
        } finally {
            lock.unlock();
        }
    }
}

