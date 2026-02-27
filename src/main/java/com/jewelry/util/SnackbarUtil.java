package com.jewelry.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Utility class to show timed "Snackbar" or "Toast" notifications.
 */
public class SnackbarUtil {

    private static final int DISPLAY_DURATION_MILLIS = 3000;
    private static final int ANIMATION_DURATION_MILLIS = 350;

    /**
     * Shows a success snackbar on the window containing the given node.
     *
     * @param ownerNode Any node currently in the scene (or previously) to hints the
     *                  window.
     * @param message   The message to display.
     */
    public static void showSuccess(Node ownerNode, String message) {
        show(ownerNode, message, "snackbar-success");
    }

    /**
     * Shows an error snackbar on the window containing the given node.
     *
     * @param ownerNode Any node currently in the scene to find the owner window.
     * @param message   The message to display.
     */
    public static void showError(Node ownerNode, String message) {
        show(ownerNode, message, "snackbar-error");
    }

    private static void show(Node ownerNode, String message, String styleClass) {
        Platform.runLater(() -> {
            Window targetWindow = null;
            if (ownerNode != null && ownerNode.getScene() != null && ownerNode.getScene().getWindow() != null) {
                targetWindow = ownerNode.getScene().getWindow();
            }

            // If the original window is closed (e.g. modal closed) or node detached, find
            // active window
            if (targetWindow == null || !targetWindow.isShowing()) {
                targetWindow = Window.getWindows().stream()
                        .filter(Window::isFocused)
                        .findFirst()
                        .orElse(null);
            }

            // Fallback: just get the first visible window
            if (targetWindow == null || !targetWindow.isShowing()) {
                targetWindow = Window.getWindows().stream()
                        .filter(Window::isShowing)
                        .findFirst()
                        .orElse(null);
            }

            if (targetWindow == null) {
                System.err.println("Cannot show snackbar: No active window found. Message: " + message);
                return;
            }

            Window finalWindow = targetWindow;
            Scene scene = finalWindow.getScene();

            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            Label label = new Label(message);
            label.getStyleClass().addAll("snackbar", styleClass);

            StackPane root = new StackPane(label);
            if (scene != null) {
                root.getStylesheets().addAll(scene.getStylesheets());
            }
            try {
                java.net.URL cssUrl = SnackbarUtil.class.getResource("/css/theme.css");
                if (cssUrl != null && (scene == null || scene.getStylesheets().isEmpty())) {
                    root.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception ignored) {
            }

            root.setStyle("-fx-background-color: transparent;");
            root.setOpacity(0); // start invisible for fade-in

            popup.getContent().add(root);

            // Bind position to bottom-center of the window
            popup.setOnShown(e -> {
                double popupWidth = root.getWidth();
                double popupHeight = root.getHeight();
                double windowX = finalWindow.getX();
                double windowY = finalWindow.getY();
                double windowWidth = finalWindow.getWidth();
                double windowHeight = finalWindow.getHeight();

                // Position bottom-center, slightly above the bottom edge
                popup.setX(windowX + (windowWidth / 2) - (popupWidth / 2));
                popup.setY(windowY + windowHeight - popupHeight - 40);

                // Fade In
                FadeTransition fadeIn = new FadeTransition(Duration.millis(ANIMATION_DURATION_MILLIS), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            // Start a timer to auto-hide
            PauseTransition delay = new PauseTransition(Duration.millis(DISPLAY_DURATION_MILLIS));
            delay.setOnFinished(e -> {
                // Fade Out
                FadeTransition fadeOut = new FadeTransition(Duration.millis(ANIMATION_DURATION_MILLIS), root);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> popup.hide());
                fadeOut.play();
            });

            popup.show(finalWindow);
            delay.play();
        });
    }
}
