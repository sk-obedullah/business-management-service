package com.jewelry.controller;

import com.jewelry.dto.CustomerDTO;
import com.jewelry.entity.Customer;
import com.jewelry.exception.AppException;
import com.jewelry.service.CustomerService;
import com.jewelry.util.CustomerMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Customer management screen (CustomerList.fxml).
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Asynchronous customer loading via {@link Task} — UI never blocks</li>
 * <li>Live search across name, email, and phone via {@link FilteredList}</li>
 * <li>Open Add / Edit modal dialog ({@link CustomerFormController})</li>
 * <li>Delete with confirmation alert</li>
 * <li>Customer count badge in the header</li>
 * </ul>
 *
 * <p>
 * <strong>Architecture rule:</strong> No SQL or business logic here.
 * All operations delegate to {@link CustomerService}.
 */
@Component
public class CustomerListController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CustomerListController.class);

    @Autowired
    private CustomerService customerService;

    // ── FXML Bindings ────────────────────────────────────────────────────────
    @FXML
    private TextField searchField;
    @FXML
    private Button btnView;
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnRefresh;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblTotal;
    @FXML
    private ProgressIndicator loadingIndicator;

    // ── Table & Columns ──────────────────────────────────────────────────────
    @FXML
    private TableView<CustomerDTO> customerTable;
    @FXML
    private TableColumn<CustomerDTO, String> colFullName;
    @FXML
    private TableColumn<CustomerDTO, String> colEmail;
    @FXML
    private TableColumn<CustomerDTO, String> colPhone;
    @FXML
    private TableColumn<CustomerDTO, String> colAddress;
    @FXML
    private TableColumn<CustomerDTO, String> colNotes;
    @FXML
    private TableColumn<CustomerDTO, Void> colOrders;

    // ── State ────────────────────────────────────────────────────────────────
    private final ObservableList<CustomerDTO> masterList = FXCollections.observableArrayList();
    private FilteredList<CustomerDTO> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureColumns();
        configureSearch();
        configureSelectionBindings();
        loadCustomersAsync();
    }

    // ── Column Setup ─────────────────────────────────────────────────────────

    private void configureColumns() {
        colFullName.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getFullName()));
        colEmail.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().email()));
        colPhone.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().phone()));
        colAddress.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().address()));
        colNotes.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().notes()));

        // Wrap long text in address / notes columns
        colAddress.setCellFactory(tc -> wrapCell());
        colNotes.setCellFactory(tc -> wrapCell());

        // Action column for viewing orders
        colOrders.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("View Orders");
            {
                btn.getStyleClass().add("btn-secondary");
                btn.setStyle("-fx-padding: 2 8; -fx-font-size: 11px;");
                btn.setOnAction(e -> {
                    CustomerDTO customer = getTableView().getItems().get(getIndex());
                    openOrdersForCustomer(customer);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        customerTable.setRowFactory(tv -> {
            TableRow<CustomerDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    customerTable.getSelectionModel().select(row.getItem());
                    onView();
                }
            });
            return row;
        });
    }

    private TableCell<CustomerDTO, String> wrapCell() {
        return new TableCell<>() {
            {
                setWrapText(true);
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
            }
        };
    }

    // ── Search / Filter ──────────────────────────────────────────────────────

    private void configureSearch() {
        filteredList = new FilteredList<>(masterList, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(dto -> {
                if (newVal == null || newVal.isBlank())
                    return true;
                String lower = newVal.toLowerCase();
                return (dto.getFullName() != null && dto.getFullName().toLowerCase().contains(lower))
                        || (dto.email() != null && dto.email().toLowerCase().contains(lower))
                        || (dto.phone() != null && dto.phone().contains(newVal));
            });
            updateStatus();
        });

        SortedList<CustomerDTO> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(customerTable.comparatorProperty());
        customerTable.setItems(sortedList);
    }

    // ── Selection Bindings ───────────────────────────────────────────────────

    private void configureSelectionBindings() {
        btnView.disableProperty().bind(
                customerTable.getSelectionModel().selectedItemProperty().isNull());
        btnEdit.disableProperty().bind(
                customerTable.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(
                customerTable.getSelectionModel().selectedItemProperty().isNull());
    }

    // ── Async Data Loading ───────────────────────────────────────────────────

    private void loadCustomersAsync() {
        loadingIndicator.setVisible(true);
        lblStatus.setText("Loading customers...");

        Task<List<Customer>> task = new Task<>() {
            @Override
            protected List<Customer> call() {
                return customerService.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            List<Customer> entities = task.getValue();
            masterList.setAll(entities.stream().map(CustomerMapper::toDTO).toList());
            updateStatus();
            loadingIndicator.setVisible(false);
            log.info("Loaded {} customers", masterList.size());
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            lblStatus.setText("Error loading customers");
            log.error("Failed to load customers", task.getException());
            showError("Load Error", "Failed to load customers: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ── FXML Actions ─────────────────────────────────────────────────────────

    @FXML
    private void onAdd() {
        openFormDialog(null);
    }

    @FXML
    private void onView() {
        CustomerDTO selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            MainLayoutController.navigateTo("/fxml/customer/CustomerView.fxml", (CustomerViewController controller) -> {
                controller.initData(selected);
                controller.setOnEditAction(dto -> openFormDialog(dto));
            });
        }
    }

    @FXML
    private void onEdit() {
        CustomerDTO selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected != null)
            openFormDialog(selected);
    }

    @FXML
    private void onDelete() {
        CustomerDTO selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete customer: " + selected.getFullName() + "?");
        confirm.setContentText(
                "Email: " + selected.email()
                        + "\n\nAll orders for this customer will be blocked from deletion. "
                        + "Ensure no active orders exist before deleting.");
        applyDialogStyle(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                customerService.deleteCustomer(selected.id());
                masterList.removeIf(dto -> dto.id().equals(selected.id()));
                updateStatus();
                log.info("Deleted customer id={}", selected.id());
                com.jewelry.util.SnackbarUtil.showSuccess(customerTable, "Customer deleted successfully!");
            } catch (AppException ex) {
                showError("Delete Failed", ex.getMessage());
            }
        }
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        loadCustomersAsync();
    }

    // ── Dialog ───────────────────────────────────────────────────────────────

    private void openFormDialog(CustomerDTO dto) {
        MainLayoutController.navigateTo("/fxml/customer/CustomerForm.fxml", (CustomerFormController controller) -> {
            controller.initForEdit(dto);
        });
    }

    private void openOrdersForCustomer(CustomerDTO customer) {
        MainLayoutController.navigateTo("/fxml/order/OrderList.fxml", (OrderListController controller) -> {
            controller.setSearchQuery(customer.phone());
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateStatus() {
        int total = masterList.size();
        int filtered = filteredList.size();
        lblStatus.setText(String.format("Showing %d of %d customers", filtered, total));
        lblTotal.setText(total + " total");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogStyle(alert);
        alert.showAndWait();
    }

    private void applyDialogStyle(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/theme.css")).toExternalForm());
        alert.getDialogPane().getStyleClass().add("root-pane");
    }
}
