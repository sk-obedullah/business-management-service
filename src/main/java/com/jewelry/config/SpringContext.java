package com.jewelry.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Static holder that gives the JavaFX layer access to Spring-managed beans.
 *
 * <p>Initialized once in {@code MainApp.init()} before any FXML is loaded.
 * JavaFX controllers are retrieved via {@link #getBean(Class)}, which is wired
 * into {@code FXMLLoader.setControllerFactory()} so that every controller is
 * a Spring-managed bean and can use {@code @Autowired}.
 */
public class SpringContext {

    private static AnnotationConfigApplicationContext context;

    /** Boot the Spring application context. Call once from {@code MainApp.init()}. */
    public static void init() {
        context = new AnnotationConfigApplicationContext(PersistenceConfig.class);
    }

    /**
     * Retrieve a Spring-managed bean by type.
     * Used as the JavaFX {@code controllerFactory}.
     */
    public static <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }

    /** Cleanly shut down the Spring context on application exit. */
    public static void close() {
        if (context != null && context.isActive()) {
            context.close();
        }
    }
}
