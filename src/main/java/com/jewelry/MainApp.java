package com.jewelry;

import com.jewelry.config.AppContext;
import com.jewelry.config.DatabaseConfig;
import com.jewelry.exception.DatabaseInitializationException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX application entry point.
 *
 * <p>
 * Responsibilities:
 * <ol>
 * <li>Bootstrap the HikariCP connection pool (fail-fast)</li>
 * <li>Assemble the {@link AppContext} (manual DI wiring)</li>
 * <li>Load the root FXML and display the primary stage</li>
 * <li>Register a shutdown hook to cleanly release pool connections</li>
 * </ol>
 *
 * <p>
 * <strong>Spring Boot migration:</strong> replace with a Spring Boot
 * {@code @SpringBootApplication} class and move FX startup into a
 * {@code ApplicationRunner} that also runs {@code Application.launch()}.
 */
public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    /** Minimum window dimensions — prevent unusable layouts. */
    private static final double MIN_WIDTH = 1100;
    private static final double MIN_HEIGHT = 700;

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Jewelry Management System...");

        // 1. Initialise database pool (throws DatabaseInitializationException on
        // failure)
        try {
            AppContext.getInstance(); // triggers DatabaseConfig + all wiring
        } catch (DatabaseInitializationException e) {
            showFatalError(primaryStage, e);
            return;
        }

        // 2. Load root layout
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(
                            getClass().getResource("/fxml/MainLayout.fxml"),
                            "MainLayout.fxml not found on classpath"));

            Parent root = loader.load();

            // 3. Build scene and apply global CSS
            Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/css/theme.css"),
                            "theme.css not found").toExternalForm());

            // 4. Configure stage
            primaryStage.setTitle("Jewelry Management System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            primaryStage.show();

            log.info("Application started successfully.");

        } catch (IOException e) {
            log.error("Failed to load MainLayout.fxml", e);
            throw new RuntimeException("Fatal: could not load main layout", e);
        }
    }

    @Override
    public void stop() {
        log.info("Application shutting down...");
        DatabaseConfig.shutdown();
    }

    private void showFatalError(Stage stage, Exception e) {
        log.error("Fatal startup error", e);
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Startup Failed");
        alert.setHeaderText("Could not connect to the database.");
        alert.setContentText(e.getMessage()
                + "\n\nPlease check application.properties and ensure MySQL is running.");
        alert.showAndWait();
        stage.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
