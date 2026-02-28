package com.jewelry.controller;

import com.jewelry.dto.ProductDTO;
import com.jewelry.entity.Product;
import com.jewelry.exception.AppException;
import com.jewelry.service.ProductService;
import com.jewelry.util.ProductMapper;

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
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Product management screen (ProductList.fxml).
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Load all products from {@link ProductService} in a background
 * {@link Task}</li>
 * <li>Populate a {@link TableView} backed by an {@link ObservableList}</li>
 * <li>Live search via {@link FilteredList} — zero DB calls on each
 * keystroke</li>
 * <li>Open Add / Edit modal dialogs</li>
 * <li>Delete with confirmation</li>
 * <li>Low-stock badge on the status label</li>
 * </ul>
 *
 * <p>
 * <strong>Architecture rule:</strong> No SQL, no business logic here.
 * We only orchestrate service calls and JavaFX bindings.
 */
@Component
public class ProductListController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ProductListController.class);
    private static final int LOW_STOCK_THRESHOLD = 5;

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
    private Label lblLowStock;
    @FXML
    private ProgressIndicator loadingIndicator;

    // ── Table & Columns ──────────────────────────────────────────────────────
    @FXML
    private TableView<ProductDTO> productTable;
    @FXML
    private TableColumn<ProductDTO, String> colName;
    @FXML
    private TableColumn<ProductDTO, String> colSku;
    @FXML
    private TableColumn<ProductDTO, String> colCategory;
    @FXML
    private TableColumn<ProductDTO, String> colMetal;
    @FXML
    private TableColumn<ProductDTO, String> colPurity;
    @FXML
    private TableColumn<ProductDTO, BigDecimal> colCost;
    @FXML
    private TableColumn<ProductDTO, BigDecimal> colSell;
    @FXML
    private TableColumn<ProductDTO, BigDecimal> colMargin;
    @FXML
    private TableColumn<ProductDTO, Integer> colQty;

    // ── State ────────────────────────────────────────────────────────────────
    @Autowired
    private ProductService productService;
    private final ObservableList<ProductDTO> masterList = FXCollections.observableArrayList();
    private FilteredList<ProductDTO> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureColumns();
        configureSearch();
        configureSelectionBindings();
        loadProductsAsync();
    }

    // ── Column Setup ─────────────────────────────────────────────────────────

    private void configureColumns() {
        colName.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().name()));
        colSku.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().sku()));
        colCategory.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().category()));
        colMetal.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().metal()));
        colMargin.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getProfitMarginPercent()));

        // Price columns: format as currency
        colCost.setCellFactory(tc -> currencyCell());
        colSell.setCellFactory(tc -> currencyCell());

        // Margin column: format as "%"
        colMargin.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value + " %");
            }
        });

        // Qty column: colour code low stock
        colQty.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(value));
                    setStyle(value <= LOW_STOCK_THRESHOLD
                            ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                            : "-fx-text-fill: #2ecc71;");
                }
            }
        });

        // Allow all columns to be resized/sorted (non-deprecated API from JavaFX 20+)
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        productTable.setRowFactory(tv -> {
            TableRow<ProductDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    productTable.getSelectionModel().select(row.getItem());
                    onView();
                }
            });
            return row;
        });
    }

    private TableCell<ProductDTO, BigDecimal> currencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : "₹ " + value.toPlainString());
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
                return (dto.name() != null && dto.name().toLowerCase().contains(lower))
                        || (dto.sku() != null && dto.sku().toLowerCase().contains(lower))
                        || (dto.category() != null && dto.category().toLowerCase().contains(lower))
                        || (dto.metal() != null && dto.metal().toLowerCase().contains(lower));
            });
            updateStatus();
        });

        SortedList<ProductDTO> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sortedList);
    }

    // ── Selection-Binding for Edit/Delete buttons ────────────────────────────

    private void configureSelectionBindings() {
        // Enable Edit & Delete only when a row is selected
        btnView.disableProperty().bind(
                productTable.getSelectionModel().selectedItemProperty().isNull());
        btnEdit.disableProperty().bind(
                productTable.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(
                productTable.getSelectionModel().selectedItemProperty().isNull());
    }

    // ── Async Data Loading ───────────────────────────────────────────────────

    private void loadProductsAsync() {
        loadingIndicator.setVisible(true);
        lblStatus.setText("Loading products...");

        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() {
                return productService.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            List<Product> entities = task.getValue();
            masterList.setAll(entities.stream().map(ProductMapper::toDTO).toList());
            updateStatus();
            loadingIndicator.setVisible(false);
            log.info("Loaded {} products", masterList.size());
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            lblStatus.setText("Error loading products");
            log.error("Failed to load products", task.getException());
            showError("Load Error", "Failed to load products: " + task.getException().getMessage());
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
        ProductDTO selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            MainLayoutController.navigateTo("/fxml/product/ProductView.fxml", (ProductViewController controller) -> {
                controller.initData(selected);
                controller.setOnEditAction(dto -> openFormDialog(dto));
            });
        }
    }

    @FXML
    private void onEdit() {
        ProductDTO selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null)
            openFormDialog(selected);
    }

    @FXML
    private void onDelete() {
        ProductDTO selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete product: " + selected.name() + "?");
        confirm.setContentText("SKU: " + selected.sku() + "\nThis action cannot be undone.");
        applyDialogStyle(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                productService.deleteProduct(selected.id());
                masterList.removeIf(dto -> dto.id().equals(selected.id()));
                updateStatus();
                log.info("Deleted product id={}", selected.id());
                com.jewelry.util.SnackbarUtil.showSuccess(productTable, "Product deleted successfully!");
            } catch (AppException ex) {
                showError("Delete Failed", ex.getMessage());
            }
        }
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        loadProductsAsync();
    }

    // ── Dialog ───────────────────────────────────────────────────────────────

    /**
     * Opens the Add/Edit form.
     *
     * @param dto {@code null} for Add mode; non-null for Edit mode
     */
    private void openFormDialog(ProductDTO dto) {
        MainLayoutController.navigateTo("/fxml/product/ProductForm.fxml", (ProductFormController controller) -> {
            controller.initForEdit(dto);
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateStatus() {
        int total = masterList.size();
        int filtered = filteredList.size();
        int lowStock = (int) masterList.stream()
                .filter(p -> p.quantityOnHand() <= LOW_STOCK_THRESHOLD)
                .count();

        lblStatus.setText(String.format("Showing %d of %d products", filtered, total));

        if (lowStock > 0) {
            lblLowStock.setText("⚠ " + lowStock + " low stock");
            lblLowStock.setVisible(true);
        } else {
            lblLowStock.setVisible(false);
        }
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
