package com.jewelry.config;

import com.jewelry.exception.DatabaseInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class that loads {@code application.properties} from the classpath once
 * and caches the result. Thread-safe via class-loading semantics (holder pattern).
 *
 * <p>Spring Boot migration note: completely replaced by
 * {@code @Value} / {@code @ConfigurationProperties} – delete this class.
 */
public final class PropertiesLoader {

    private static final Logger log = LoggerFactory.getLogger(PropertiesLoader.class);
    private static final String PROPERTIES_FILE = "application.properties";

    /** Initialization-on-demand holder — lazy, thread-safe, zero synchronization overhead. */
    private static final class Holder {
        static final Properties INSTANCE = loadFromClasspath();
    }

    private PropertiesLoader() { /* utility */ }

    public static Properties load() {
        return Holder.INSTANCE;
    }

    private static Properties loadFromClasspath() {
        Properties properties = new Properties();
        try (InputStream is = PropertiesLoader.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (is == null) {
                throw new DatabaseInitializationException(
                        "'" + PROPERTIES_FILE + "' not found on classpath. "
                        + "Ensure it is in src/main/resources/.");
            }
            properties.load(is);
            log.info("Loaded configuration from '{}'", PROPERTIES_FILE);
        } catch (IOException e) {
            throw new DatabaseInitializationException(
                    "Failed to read '" + PROPERTIES_FILE + "'", e);
        }
        return properties;
    }
}
