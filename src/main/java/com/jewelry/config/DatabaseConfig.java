package com.jewelry.config;

import com.jewelry.exception.DatabaseInitializationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton factory responsible for initializing and providing the HikariCP
 * connection pool. This class is the single source of truth for {@link DataSource}
 * in the application.
 *
 * <p>Design: fail-fast – if the pool cannot be initialized (bad credentials,
 * MySQL unreachable), a {@link DatabaseInitializationException} is thrown
 * immediately at startup so the user sees an actionable error rather than a
 * NullPointerException deep inside the repository layer.
 *
 * <p>Spring Boot migration note: replace with a {@code @Bean DataSource} inside
 * a {@code @Configuration} class. HikariCP is Spring Boot's default pool, so
 * the property names map 1-to-1.
 */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() { /* utility – no instantiation */ }

    /**
     * Returns the singleton {@link DataSource}, initializing the HikariCP pool
     * on first call.
     *
     * @return configured, validated {@link HikariDataSource}
     * @throws DatabaseInitializationException if pool cannot be established
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    dataSource = initialize();
                }
            }
        }
        return dataSource;
    }

    private static HikariDataSource initialize() {
        log.info("Initializing HikariCP connection pool...");
        Properties props = PropertiesLoader.load();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.username"));
        config.setPassword(props.getProperty("db.password"));
        config.setMaximumPoolSize(
                Integer.parseInt(props.getProperty("db.pool.maximumPoolSize", "10")));
        config.setMinimumIdle(
                Integer.parseInt(props.getProperty("db.pool.minimumIdle", "2")));
        config.setConnectionTimeout(
                Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
        config.setPoolName("JewelryPool");

        // MySQL-specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        HikariDataSource ds;
        try {
            ds = new HikariDataSource(config);
        } catch (Exception e) {
            throw new DatabaseInitializationException(
                    "Failed to create HikariCP data source. Check application.properties.", e);
        }

        // Validate connectivity immediately – fail fast
        try (Connection conn = ds.getConnection()) {
            log.info("HikariCP pool initialized. Connected to: {}", conn.getMetaData().getURL());
        } catch (SQLException e) {
            ds.close();
            throw new DatabaseInitializationException(
                    "HikariCP pool created but test connection failed. Is MySQL running?", e);
        }

        return ds;
    }

    /** Gracefully shuts down the pool. Call from application shutdown hook. */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Shutting down HikariCP pool...");
            dataSource.close();
        }
    }
}
