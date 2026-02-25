package com.jewelry.controller;

import com.jewelry.config.AppContext;
import com.jewelry.dto.OrderDTO;
import com.jewelry.entity.Order;
import com.jewelry.entity.OrderStatus;
import com.jewelry.exception.AppException;
import com.jewelry.service.OrderService;
import com.jewelry.util.OrderMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Order list screen (OrderList.fxml).
 *
 * <p>
 * Displays all orders in a TableView with live filtering by customer name,
 * status, and order total. Provides New Order, View Details, and Cancel
 * actions.
 */
public class OrderListController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(OrderListController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    // ── FXML Bindings ────────────────────────────────────────────────────────
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<OrderStatus> cboUpdateStatus;
    @FXML
    private Button btnNewOrder;
    @FXML
    private Button btnView;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnRefresh;
    @FXML
    private Label lblStatus;
    @FXML
    private ProgressIndicator loadingIndicator;

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML
    private TableView<OrderDTO> orderTable;
    @FXML
    private TableColumn<OrderDTO, Long> colId;
    @FXML
    private TableColumn<OrderDTO, String> colCustomer;
    @FXML
    private TableColumn<OrderDTO, String> colPhone;
    @FXML
    private TableColumn<OrderDTO, String> colDate;
    @FXML
    private TableColumn<OrderDTO, OrderStatus> colStatus;
    @FXML
    private TableColumn<OrderDTO, Integer> colItems;
    @FXML
    private TableColumn<OrderDTO, BigDecimal> colTotal;
    @FXML
    private TableColumn<OrderDTO, BigDecimal> colNet;
    @FXML
    private TableColumn<OrderDTO, String> colNotes;

    // ── State ────────────────────────────────────────────────────────────────
    private final OrderService orderService;
    private final ObservableList<OrderDTO> masterList = FXCollections.observableArrayList();
    private FilteredList<OrderDTO> filteredList;

    public OrderListController() {
        this.orderService = AppContext.getInstance().getOrderService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureColumns();
        configureFilters();
        configureSelectionBindings();
        loadOrdersAsync();
    }

    // ── Column Setup ─────────────────────────────────────────────────────────

    private void configureColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("customerPhone"));
        colItems.setCellValueFactory(new PropertyValueFactory<>("itemCount"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colNet.setCellValueFactory(new PropertyValueFactory<>("netAmount"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Date column: formatted from LocalDateTime
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getOrderDate() != null
                        ? data.getValue().getOrderDate().format(DATE_FMT)
                        : ""));

        // Status column: displayed as bold colored text
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(OrderStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(status.getDisplayName());
                    String color = switch (status) {
                        case PENDING -> "#f39c12"; // Orange
                        case PROCESSING -> "#3498db"; // Blue
                        case SHIPPED -> "#8e44ad"; // Purple
                        case IN_TRANSIT -> "#16a085"; // Teal
                        case DELIVERED -> "#27ae60"; // Green
                        case COMPLETED -> "#2ecc71"; // Light Green
                        case CANCELLED -> "#e74c3c"; // Red
                    };
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                    setGraphic(null);
                }
            }
        });

        // Currency columns
        colTotal.setCellFactory(tc -> currencyCell());
        colNet.setCellFactory(tc -> currencyCell());

        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private TableCell<OrderDTO, BigDecimal> currencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : "₹ " + value.toPlainString());
            }
        };
    }

    // ── Filters ──────────────────────────────────────────────────────────────

    private void configureFilters() {
        statusFilter.getItems().addAll("All", "Pending", "Processing", "Completed", "Cancelled");
        statusFilter.setValue("All");

        filteredList = new FilteredList<>(masterList, p -> true);

        Runnable applyFilter = () -> {
            String search = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
            String status = statusFilter.getValue();
            filteredList.setPredicate(dto -> {
                boolean matchesSearch = search.isBlank()
                        || (dto.getCustomerName() != null && dto.getCustomerName().toLowerCase().contains(search))
                        || (dto.getCustomerPhone() != null && dto.getCustomerPhone().contains(search))
                        || (dto.getId() != null && dto.getId().toString().contains(search));
                boolean matchesStatus = "All".equals(status)
                        || (dto.getStatus() != null && dto.getStatus().getDisplayName().equals(status));
                return matchesSearch && matchesStatus;
            });
            updateStatus();
        };

        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        SortedList<OrderDTO> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sortedList);
    }

    // ── Selection Bindings ───────────────────────────────────────────────────

    private void configureSelectionBindings() {
        btnView.disableProperty().bind(
                orderTable.getSelectionModel().selectedItemProperty().isNull());
        btnCancel.disableProperty().bind(
                orderTable.getSelectionModel().selectedItemProperty().isNull());

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateStatusDropdown(newVal);
        });

        cboUpdateStatus.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(OrderStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });

        cboUpdateStatus.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(OrderStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(cboUpdateStatus.getPromptText());
                    setStyle("-fx-text-fill: #e2b04a;"); // Use gold accent for prompt to be visible
                } else {
                    setText(item.getDisplayName());
                    setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void updateStatusDropdown(OrderDTO selected) {
        cboUpdateStatus.setOnAction(null); // Temporarily remove listener
        cboUpdateStatus.getItems().clear();

        if (selected == null) {
            cboUpdateStatus.setDisable(true);
            cboUpdateStatus.setPromptText("📦 Select order to update");
        } else {
            OrderStatus current = selected.getStatus();
            if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
                cboUpdateStatus.setDisable(true);
                cboUpdateStatus.setPromptText("Order is " + current.getDisplayName());
            } else {
                cboUpdateStatus.setDisable(false);
                cboUpdateStatus.setPromptText("Update to...");
                if (current == OrderStatus.PENDING) {
                    cboUpdateStatus.getItems().addAll(OrderStatus.PROCESSING, OrderStatus.CANCELLED);
                } else if (current == OrderStatus.PROCESSING) {
                    cboUpdateStatus.getItems().addAll(OrderStatus.SHIPPED, OrderStatus.COMPLETED,
                            OrderStatus.CANCELLED);
                } else if (current == OrderStatus.SHIPPED) {
                    cboUpdateStatus.getItems().addAll(OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED,
                            OrderStatus.COMPLETED, OrderStatus.CANCELLED);
                } else if (current == OrderStatus.IN_TRANSIT) {
                    cboUpdateStatus.getItems().addAll(OrderStatus.DELIVERED, OrderStatus.COMPLETED,
                            OrderStatus.CANCELLED);
                } else if (current == OrderStatus.DELIVERED) {
                    cboUpdateStatus.getItems().addAll(OrderStatus.COMPLETED);
                }
            }
        }

        // Restore listener
        cboUpdateStatus.setOnAction(e -> {
            OrderStatus newStatus = cboUpdateStatus.getValue();
            OrderDTO sel = orderTable.getSelectionModel().getSelectedItem();
            if (newStatus != null && sel != null && newStatus != sel.getStatus()) {
                handleStatusUpdate(sel, newStatus);
            }
        });
    }

    private void handleStatusUpdate(OrderDTO selected, OrderStatus newStatus) {
        try {
            orderService.updateStatus(selected.getId(), newStatus);
            selected.setStatus(newStatus); // Update DTO locally
            orderTable.refresh(); // Refresh table view to reflect new status

            // Defer dropdown update until current JavaFX event processing finishes
            // Prevents IndexOutOfBoundsException when clearing ComboBox items
            Platform.runLater(() -> updateStatusDropdown(selected));

            log.info("Updated order id={} to {}", selected.getId(), newStatus);
        } catch (AppException ex) {
            showError("Update Failed", ex.getMessage());
            Platform.runLater(() -> updateStatusDropdown(selected)); // reset to valid item state
        }
    }

    // ── Async Load ───────────────────────────────────────────────────────────

    private void loadOrdersAsync() {
        loadingIndicator.setVisible(true);
        lblStatus.setText("Loading orders...");

        Task<List<Order>> task = new Task<>() {
            @Override
            protected List<Order> call() {
                return orderService.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            masterList.setAll(task.getValue().stream().map(OrderMapper::toDTO).toList());
            updateStatus();
            loadingIndicator.setVisible(false);
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            lblStatus.setText("Error loading orders");
            log.error("Load orders failed", task.getException());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── FXML Actions ─────────────────────────────────────────────────────────

    @FXML
    private void onNewOrder() {
        openOrderForm(null);
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        statusFilter.setValue("All");
        loadOrdersAsync();
    }

    @FXML
    private void onView() {
        OrderDTO selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        openOrderForm(selected);
    }

    @FXML
    private void onCancel() {
        OrderDTO selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        if (selected.getStatus() == OrderStatus.CANCELLED || selected.getStatus() == OrderStatus.COMPLETED) {
            showError("Cannot Cancel", "Only pending or processing orders can be cancelled.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Order");
        confirm.setHeaderText("Cancel Order #" + selected.getId() + "?");
        confirm.setContentText(
                "Customer: " + selected.getCustomerName()
                        + "\nTotal: ₹" + selected.getTotalAmount()
                        + "\n\nThis will restore stock for all items in the order.");
        applyDialogStyle(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                orderService.updateStatus(selected.getId(), OrderStatus.CANCELLED);
                loadOrdersAsync();
                log.info("Cancelled order id={}", selected.getId());
            } catch (AppException ex) {
                showError("Cancel Failed", ex.getMessage());
            }
        }
    }

    // ── Dialog ───────────────────────────────────────────────────────────────

    private void openOrderForm(OrderDTO orderToView) {
        MainLayoutController.navigateTo("/fxml/order/OrderForm.fxml", (OrderFormController formController) -> {
            if (orderToView != null) {
                Order fullOrder = orderService.loadWithLines(orderToView.getId());
                formController.setOrderForView(fullOrder);
            } else {
                formController.setOrderForView(null);
            }
        });
    }

    public void setSearchQuery(String query) {
        if (searchField != null && query != null) {
            searchField.setText(query);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateStatus() {
        lblStatus.setText(String.format(
                "Showing %d of %d orders", filteredList.size(), masterList.size()));
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    private void applyDialogStyle(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/theme.css")).toExternalForm());
        dialog.getDialogPane().getStyleClass().add("root-pane");
    }
}
