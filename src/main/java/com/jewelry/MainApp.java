package com.jewelry;

import com.jewelry.config.SpringContext;
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
 * <p>Spring is booted in {@link #init()} (before any JavaFX controller
 * is instantiated). The {@link FXMLLoader#setControllerFactory} is wired
 * to {@link SpringContext#getBean} so every FXML controller is a Spring
 * managed bean and can use {@code @Autowired}.
 */
public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private static final double MIN_WIDTH  = 1100;
    private static final double MIN_HEIGHT = 700;

    @Override
    public void init() {
        log.info("Booting Spring application context...");
        SpringContext.init();
        log.info("Spring context ready.");
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Jewelry Management System...");

        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(
                            getClass().getResource("/fxml/MainLayout.fxml"),
                            "MainLayout.fxml not found on classpath"));

            // Wire all FXML controllers through the Spring IoC container
            // — enables @Autowired inside every controller
            loader.setControllerFactory(SpringContext::getBean);

            Parent root = loader.load();

            Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/css/theme.css"),
                            "theme.css not found").toExternalForm());

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
        SpringContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
