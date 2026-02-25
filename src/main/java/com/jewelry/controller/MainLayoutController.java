package com.jewelry.controller;

import com.jewelry.config.AppContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the root {@code MainLayout.fxml}.
 *
 * <p>
 * Owns the center {@link StackPane} content area into which child FXML
 * views are dynamically swapped via {@link #loadView(String)}.
 * This is the application's primary navigation backbone.
 *
 * <p>
 * All menu action handlers are {@code @FXML} methods that simply delegate to
 * {@link #loadView(String)} — zero business logic lives here.
 */
public class MainLayoutController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MainLayoutController.class);

    // Static reference to the active layout controller for global navigation
    private static MainLayoutController instance;

    @FXML
    private StackPane contentArea;

    @FXML
    private javafx.scene.control.Button btnNavDashboard;
    @FXML
    private javafx.scene.control.Button btnNavProducts;
    @FXML
    private javafx.scene.control.Button btnNavCustomers;
    @FXML
    private javafx.scene.control.Button btnNavOrders;
    @FXML
    private javafx.scene.control.Button btnNavReports;

    private java.util.List<javafx.scene.control.Button> navButtons;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("MainLayoutController initialized.");
        instance = this;
        navButtons = java.util.Arrays.asList(
                btnNavDashboard, btnNavProducts, btnNavCustomers, btnNavOrders, btnNavReports);
    }

    // ── Menu Actions ─────────────────────────────────────────────────────────

    @FXML
    private void onDashboard() {
        setActiveButton(btnNavDashboard);
        loadView("/fxml/Dashboard.fxml");
    }

    @FXML
    private void onProducts() {
        setActiveButton(btnNavProducts);
        loadView("/fxml/product/ProductList.fxml");
    }

    @FXML
    private void onCustomers() {
        setActiveButton(btnNavCustomers);
        loadView("/fxml/customer/CustomerList.fxml");
    }

    @FXML
    private void onOrders() {
        setActiveButton(btnNavOrders);
        loadView("/fxml/order/OrderList.fxml");
    }

    @FXML
    private void onReports() {
        setActiveButton(btnNavReports);
        loadView("/fxml/Reports.fxml");
    }

    private void setActiveButton(javafx.scene.control.Button activeBtn) {
        if (navButtons == null)
            return;
        for (javafx.scene.control.Button btn : navButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-btn-active");
            }
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("nav-btn-active")) {
            activeBtn.getStyleClass().add("nav-btn-active");
        }
    }

    private javafx.scene.control.Label buildPlaceholderLabel(String text) {
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(text);
        lbl.getStyleClass().add("welcome-label");
        return lbl;
    }

    @FXML
    private void onExit() {
        javafx.application.Platform.exit();
    }

    // ── Navigation Helper ────────────────────────────────────────────────────

    /**
     * Loads an FXML resource from {@code /fxml/<name>} into the center content area
     * and optionally applies an initialization function to the controller before
     * showing it.
     *
     * @param fxmlPath classpath-relative path, e.g.
     *                 {@code "/fxml/product/ProductList.fxml"}
     * @param initFunc Optional consumer to initialize the controller (e.g. passing
     *                 an ID to edit)
     */
    public void loadView(String fxmlPath, Consumer<Object> initFunc) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource(fxmlPath),
                            "FXML not found: " + fxmlPath));

            loader.setControllerFactory(clazz -> {
                try {
                    try {
                        return clazz.getConstructor(AppContext.class)
                                .newInstance(AppContext.getInstance());
                    } catch (NoSuchMethodException ex) {
                        return clazz.getDeclaredConstructor().newInstance();
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Could not instantiate controller: " + clazz, ex);
                }
            });

            Parent view = loader.load();

            if (initFunc != null) {
                Object controller = loader.getController();
                initFunc.accept(controller);
            }

            contentArea.getChildren().setAll(view);
            log.info("Loaded view: {}", fxmlPath);
        } catch (IOException e) {
            log.error("Failed to load view: {}", fxmlPath, e);
        }
    }

    public void loadView(String fxmlPath) {
        loadView(fxmlPath, null);
    }

    /**
     * Global navigation helper.
     */
    public static void navigateTo(String fxmlPath) {
        if (instance != null) {
            instance.loadView(fxmlPath);
        } else {
            log.error("Cannot navigate, MainLayoutController instance is null");
        }
    }

    /**
     * Global navigation helper with parameter passing.
     */
    @SuppressWarnings("unchecked")
    public static <T> void navigateTo(String fxmlPath, Consumer<T> initFunc) {
        if (instance != null) {
            instance.loadView(fxmlPath, obj -> initFunc.accept((T) obj));
        } else {
            log.error("Cannot navigate, MainLayoutController instance is null");
        }
    }
}
