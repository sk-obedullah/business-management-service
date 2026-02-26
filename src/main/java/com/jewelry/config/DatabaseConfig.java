package com.jewelry.config;

import com.jewelry.exception.DatabaseInitializationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * Singleton factory for the HikariCP connection pool backed by an
 * embedded H2 database.
 *
 * <p>On first call the pool is initialized and, if the schema does not yet
 * exist (i.e. a brand-new install), {@code jewelry_db.sql} and
 * {@code seed_data.sql} are executed automatically so the user never has to
 * touch a database CLI.</p>
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
        log.info("Initializing HikariCP connection pool (Embedded H2)...");
        Properties props = PropertiesLoader.load();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.username"));
        config.setPassword(props.getProperty("db.password", ""));
        config.setMaximumPoolSize(
                Integer.parseInt(props.getProperty("db.pool.maximumPoolSize", "10")));
        config.setMinimumIdle(
                Integer.parseInt(props.getProperty("db.pool.minimumIdle", "2")));
        config.setConnectionTimeout(
                Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
        config.setPoolName("JewelryPool");

        // H2 works well with these standard cache settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        HikariDataSource ds;
        try {
            ds = new HikariDataSource(config);
        } catch (Exception e) {
            throw new DatabaseInitializationException(
                    "Failed to create HikariCP data source. Check application.properties.", e);
        }

        // Validate connectivity and auto-initialize schema on first run
        try (Connection conn = ds.getConnection()) {
            log.info("Connected to: {}", conn.getMetaData().getURL());
            autoInitializeIfNeeded(conn);
        } catch (SQLException e) {
            ds.close();
            throw new DatabaseInitializationException(
                    "Test connection failed after pool creation.", e);
        }

        return ds;
    }

    /**
     * Checks whether the schema already exists and, if not, runs the DDL and
     * seed-data scripts. This means first-time users get a working database
     * automatically — no manual SQL setup needed.
     */
    private static void autoInitializeIfNeeded(Connection conn) {
        try {
            // Check for existence of the 'product' table as a proxy for "schema exists"
            boolean schemaExists =
                    conn.getMetaData().getTables(null, null, "PRODUCT", null).next() ||
                    conn.getMetaData().getTables(null, null, "product", null).next();

            if (schemaExists) {
                log.info("Database schema already exists — skipping initialization.");
                return;
            }

            log.info("No schema found. Running first-time database initialization...");

            // 1. Create tables
            try (Reader schemaReader = new InputStreamReader(
                    Objects.requireNonNull(
                            DatabaseConfig.class.getResourceAsStream("/ddl/jewelry_db.sql"),
                            "jewelry_db.sql not found on classpath"),
                    StandardCharsets.UTF_8)) {
                RunScript.execute(conn, schemaReader);
                log.info("Schema created successfully.");
            }

            // 2. Insert seed / demo data
            var seedStream = DatabaseConfig.class.getResourceAsStream("/ddl/seed_data.sql");
            if (seedStream != null) {
                try (Reader seedReader = new InputStreamReader(seedStream, StandardCharsets.UTF_8)) {
                    RunScript.execute(conn, seedReader);
                    log.info("Seed data inserted successfully.");
                }
            } else {
                log.warn("seed_data.sql not found on classpath — skipping seed data.");
            }

        } catch (Exception ex) {
            throw new DatabaseInitializationException(
                    "Auto-initialization of the embedded database failed.", ex);
        }
    }

    /** Gracefully shuts down the pool. Called from the application shutdown hook. */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Shutting down HikariCP pool...");
            dataSource.close();
        }
    }
}
