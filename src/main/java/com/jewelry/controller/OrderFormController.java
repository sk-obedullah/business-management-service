package com.jewelry.controller;

import com.jewelry.dto.OrderLineDTO;
import com.jewelry.entity.Order;
import com.jewelry.entity.OrderLine;
import com.jewelry.entity.OrderStatus;
import com.jewelry.entity.Customer;
import com.jewelry.entity.Product;
import com.jewelry.exception.AppException;
import com.jewelry.service.CustomerService;
import com.jewelry.service.OrderService;
import com.jewelry.service.ProductService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import com.jewelry.util.SnackbarUtil;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import com.jewelry.controller.ProductViewController;

// ... (existing code, appending import to the top section and adding the method call)

/**
 * Controller for the New Order modal dialog (OrderForm.fxml).
 *
 * <p>
 * Flow:
 * <ol>
 * <li>User picks a customer from the ComboBox</li>
 * <li>User picks a product + quantity and clicks "Add Item"
 * — item is added to the in-memory line-item table</li>
 * <li>Order total updates live as items are added/removed</li>
 * <li>User clicks "Place Order" — {@link OrderService#createOrder} is called
 * (atomic JDBC transaction: inserts order + lines + decrements stock)</li>
 * </ol>
 *
 * <p>
 * <strong>Architecture rule:</strong> No SQL here.
 * All business logic (stock check, price snapshot, transaction) lives in
 * {@link com.jewelry.service.impl.OrderServiceImpl}.
 */
@Component
public class OrderFormController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(OrderFormController.class);

    @Autowired
    private CustomerService customerService;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblSubtitle;

    // ── FXML – Customer / Notes
    // ──────────────────────────────────────────────
    @FXML
    private Button btnNewCustomer;
    @FXML
    private VBox customerSelectionPane;
    @FXML
    private TextField txtCustomerSearch;
    @FXML
    private ComboBox<Customer> cboCustomer;
    @FXML
    private VBox customerInfoCard;
    @FXML
    private Label lblCustomerName;
    @FXML
    private Label lblCustomerPhone;
    @FXML
    private Label lblCustomerEmail;
    @FXML
    private Label lblCustomerAddress;
    @FXML
    private TextArea txtNotes;
    @FXML
    private TextField txtDiscount;
    @FXML
    private Label lblOrderTotal;
    @FXML
    private Label lblError;

    // ── FXML – Item Picker
    // ───────────────────────────────────────────────────
    @FXML
    private VBox addProductSection;
    @FXML
    private ComboBox<Product> cboProduct;
    @FXML
    private TextField txtQuantity;
    @FXML
    private Label lblStock;
    @FXML
    private Label lblUnitPrice;
    @FXML
    private Button btnAddItem;

    // ── FXML – Line-item Table
    // ───────────────────────────────────────────────
    @FXML
    private TableView<OrderLineDTO> lineTable;
    @FXML
    private TableColumn<OrderLineDTO, String> colProduct;
    @FXML
    private TableColumn<OrderLineDTO, String> colSku;
    @FXML
    private TableColumn<OrderLineDTO, Integer> colQty;
    @FXML
    private TableColumn<OrderLineDTO, BigDecimal> colUnitPrice;
    @FXML
    private TableColumn<OrderLineDTO, BigDecimal> colLineTotal;
    @FXML
    private TableColumn<OrderLineDTO, Void> colRemove;

    // ── FXML – Footer
    // ────────────────────────────────────────────────────────
    @FXML
    private Button btnPlaceOrder;
    @FXML
    private Button btnCancel;

    // ── State
    // ────────────────────────────────────────────────────────────────

    private final ObservableList<OrderLineDTO> lineItems = FXCollections.observableArrayList();
    private List<Customer> allCustomers = new java.util.ArrayList<>();
    private boolean saved = false;
    private Order currentOrderForView;

    public void setOrderForView(Order order) {
        this.saved = false;
        this.currentOrderForView = order;

        if (order == null) {
            lblTitle.setText("📦 New Order");
            lblSubtitle.setText("Select a customer, add products, then place the order.");

            customerSelectionPane.setVisible(true);
            customerSelectionPane.setManaged(true);
            
            customerInfoCard.setVisible(false);
            customerInfoCard.setManaged(false);

            txtNotes.setEditable(true);
            txtDiscount.setEditable(true);
            txtDiscount.setDisable(false);

            addProductSection.setVisible(true);
            addProductSection.setManaged(true);

            btnPlaceOrder.setVisible(true);
            btnPlaceOrder.setManaged(true);

            btnCancel.setText("Cancel");
            colRemove.setVisible(true);
            return;
        }

        lblTitle.setText("📦 View Order #" + order.getId());
        lblSubtitle.setText("Read-only details for this order.");

        // Switch to Customer Info Card
        customerSelectionPane.setVisible(false);
        customerSelectionPane.setManaged(false);

        customerInfoCard.setVisible(true);
        customerInfoCard.setManaged(true);
        lblCustomerName.setText(order.getCustomerName() != null ? order.getCustomerName() : "Unknown Customer");
        lblCustomerPhone.setText(order.getCustomerPhone() != null ? "📞 " + order.getCustomerPhone() : "No Phone");

        if (order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank()) {
            lblCustomerEmail.setText("✉️ " + order.getCustomerEmail());
            lblCustomerEmail.setManaged(true);
            lblCustomerEmail.setVisible(true);
        } else {
            lblCustomerEmail.setManaged(false);
            lblCustomerEmail.setVisible(false);
        }

        if (order.getCustomerAddress() != null && !order.getCustomerAddress().isBlank()) {
            lblCustomerAddress.setText("🏠 " + order.getCustomerAddress());
            lblCustomerAddress.setManaged(true);
            lblCustomerAddress.setVisible(true);
        } else {
            lblCustomerAddress.setManaged(false);
            lblCustomerAddress.setVisible(false);
        }

        // Disable editing
        txtNotes.setEditable(false);
        txtDiscount.setEditable(false);
        txtDiscount.setDisable(true);

        // Hide and collapse the Add Product section entirely
        addProductSection.setVisible(false);
        addProductSection.setManaged(false);

        // Hide and collapse Place Order button
        btnPlaceOrder.setVisible(false);
        btnPlaceOrder.setManaged(false);

        btnCancel.setText("Close");
        colRemove.setVisible(false);

        // Populate customer
        customerService.findAll().stream()
                .filter(c -> c.getId().equals(order.getCustomerId()))
                .findFirst()
                .ifPresent(c -> cboCustomer.setValue(c));

        // Populate fields
        txtNotes.setText(order.getNotes() != null ? order.getNotes() : "");
        txtDiscount.setText(order.getDiscount() != null ? order.getDiscount().toPlainString() : "");

        // Populate lines
        lineItems.clear();
        for (OrderLine l : order.getLines()) {
            String pName = null;
            String pSku = null;
            int stock = 0;
            
            java.util.Optional<Product> optProduct = productService.findById(l.getProductId());
            if (optProduct.isPresent()) {
                Product p = optProduct.get();
                pName = p.getName();
                pSku = p.getSku();
                stock = p.getQuantityOnHand();
            }

            OrderLineDTO dto = new OrderLineDTO(
                    l.getProductId(),
                    pName,
                    pSku,
                    l.getQuantity(),
                    l.getUnitPrice(),
                    l.getCostPrice(),
                    stock
            );

            lineItems.add(dto);
        }
        refreshTotal();
    }

    public void initNewOrder() {
        setOrderForView(null);
        lineItems.clear();
        cboCustomer.setValue(null);
        txtNotes.clear();
        txtDiscount.clear();
        txtCustomerSearch.clear();
        cboProduct.setValue(null);
        txtQuantity.clear();
        lblStock.setText("");
        lblUnitPrice.setText("");
        refreshTotal();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblError.setVisible(false);

        configureLineTable();
        loadCustomers();
        loadProducts();
        configureProductSelectionListener();
        configureCustomerSearchListener();
        configureDecimalFilter(txtDiscount);
        configureIntegerFilter(txtQuantity);

        lineItems.addListener((javafx.collections.ListChangeListener<OrderLineDTO>) c -> refreshTotal());
    }

    // ── Setup
    // ─────────────────────────────────────────────────────────────────

    private void configureLineTable() {
        colProduct.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().productName()));
        colSku.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().productSku()));
        colQty.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().quantity()));
        colUnitPrice.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().unitPrice()));
        colLineTotal.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getLineTotal()));

        colUnitPrice.setCellFactory(tc -> currencyCell());
        colLineTotal.setCellFactory(tc -> currencyCell());

        // Remove button column
        colRemove.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("✕");
            {
                btn.getStyleClass().add("btn-danger");
                btn.setStyle("-fx-padding: 2 8; -fx-font-size: 11px;");
                btn.setOnAction(e -> {
                    OrderLineDTO item = getTableView().getItems().get(getIndex());
                    lineItems.remove(item);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        lineTable.setItems(lineItems);
        lineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        lineTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<OrderLineDTO> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    OrderLineDTO item = row.getItem();
                    if (item != null && item.productId() != null) {
                        productService.findById(item.productId()).ifPresent(product -> {
                            com.jewelry.dto.ProductDTO productDTO = com.jewelry.util.ProductMapper.toDTO(product);
                            MainLayoutController.navigateTo("/fxml/product/ProductView.fxml",
                                    (ProductViewController controller) -> {
                                        controller.initData(productDTO);
                                        controller.setOnBackAction(() -> {
                                            MainLayoutController.navigateTo("/fxml/order/OrderForm.fxml",
                                                    (OrderFormController formCtrl) -> {
                                                        formCtrl.setOrderForView(currentOrderForView);
                                                    });
                                        });
                                    });
                        });
                    }
                }
            });
            return row;
        });
    }

    private void loadCustomers() {
        allCustomers = customerService.findAll();
        cboCustomer.setItems(FXCollections.observableArrayList(allCustomers));
        cboCustomer.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Customer c) {
                return c == null ? "" : c.getFirstName() + " " + c.getLastName() + " (" + c.getPhone() + ")";
            }

            @Override
            public Customer fromString(String s) {
                return null;
            }
        });
    }

    private void loadProducts() {
        List<Product> products = productService.findAll();
        cboProduct.setItems(FXCollections.observableArrayList(products));
        cboProduct.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Product p) {
                return p == null ? "" : p.getName() + " [" + p.getSku() + "]";
            }

            @Override
            public Product fromString(String s) {
                return null;
            }
        });
    }

    private void configureProductSelectionListener() {
        cboProduct.valueProperty().addListener((obs, old, product) -> {
            if (product == null) {
                lblStock.setText("");
                lblUnitPrice.setText("");
                return;
            }
            lblStock.setText("In stock: " + product.getQuantityOnHand());
            lblUnitPrice.setText("₹ " + product.getSellingPrice().toPlainString());
            lblStock.setStyle(product.getQuantityOnHand() <= 5
                    ? "-fx-text-fill: #e74c3c;"
                    : "-fx-text-fill: #2ecc71;");
        });
    }

    private void configureCustomerSearchListener() {
        txtCustomerSearch.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || newV.isBlank()) {
                cboCustomer.setItems(FXCollections.observableArrayList(allCustomers));
            } else {
                String term = newV.toLowerCase();
                List<Customer> filtered = allCustomers.stream()
                        .filter(c -> c.getFirstName().toLowerCase().contains(term)
                                || c.getLastName().toLowerCase().contains(term)
                                || (c.getPhone() != null && c.getPhone().contains(term)))
                        .toList();
                cboCustomer.setItems(FXCollections.observableArrayList(filtered));
                if (!filtered.isEmpty()) {
                    cboCustomer.show();
                }
            }
        });
    }

    // ── FXML Actions ────────────────────────────────────────────────────────

    @FXML
    private void onNewCustomer() {
        try {
            FXMLLoader loader = new FXMLLoader(java.util.Objects.requireNonNull(
                    getClass().getResource("/fxml/customer/CustomerForm.fxml")));
            // Use Spring to create controllers — enables @Autowired in the child controller
            loader.setControllerFactory(com.jewelry.config.SpringContext::getBean);
            
            Parent root = loader.load();
            CustomerFormController controller = loader.getController();

            // Open in Add Mode
            controller.initForEdit(null);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Add New Customer");
            dialog.setMinWidth(520);
            dialog.setMinHeight(600);

            // Override close to just close the popup
            controller.setCloseAction(() -> dialog.close());

            Scene scene = new Scene(root);
            scene.getStylesheets().add(java.util.Objects.requireNonNull(
                    getClass().getResource("/css/theme.css")).toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();

            if (controller.isSaved()) {
                loadCustomers(); // refresh the list from DB

                // Select the most recently added customer
                allCustomers.stream()
                        .max(java.util.Comparator.comparing(Customer::getId))
                        .ifPresent(c -> cboCustomer.setValue(c));
            }
        } catch (IOException e) {
            log.error("Could not open customer form", e);
            showError("Could not open Customer form: " + e.getMessage());
        }
    }

    @FXML
    private void onAddItem() {
        lblError.setVisible(false);
        Product product = cboProduct.getValue();
        if (product == null) {
            showError("Please select a product.");
            return;
        }

        String qtyStr = txtQuantity.getText().trim();
        if (qtyStr.isBlank()) {
            showError("Please enter a quantity.");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            showError("Quantity must be a whole number.");
            return;
        }
        if (qty <= 0) {
            showError("Quantity must be at least 1.");
            return;
        }

        // Check if same product already in list
        boolean duplicate = lineItems.stream().anyMatch(l -> l.productId().equals(product.getId()));
        if (duplicate) {
            showError("'" + product.getName() + "' is already in the order. Remove it first to change quantity.");
            return;
        }

        if (qty > product.getQuantityOnHand()) {
            showError("Only " + product.getQuantityOnHand() + " units available for '" + product.getName() + "'.");
            return;
        }

        OrderLineDTO line = new OrderLineDTO(
                product.getId(),
                product.getName(),
                product.getSku(),
                qty,
                product.getSellingPrice(),
                product.getCostPrice(),
                product.getQuantityOnHand()
        );
        lineItems.add(line);

        // Reset picker
        cboProduct.setValue(null);
        txtQuantity.clear();
        lblStock.setText("");
        lblUnitPrice.setText("");
    }

    @FXML
    private void onPlaceOrder() {
        lblError.setVisible(false);

        try {
            Order order = new Order();
            order.setCustomerId(cboCustomer.getValue().getId());
            order.setStatus(OrderStatus.PENDING);
            order.setNotes(txtNotes.getText().trim());

            String discStr = txtDiscount.getText().trim();
            order.setDiscount(discStr.isBlank() ? BigDecimal.ZERO : new BigDecimal(discStr));

            // Convert DTOs → entities (prices will be re-snapshotted by service)
            List<OrderLine> lines = lineItems.stream().map(dto -> {
                OrderLine l = new OrderLine();
                l.setProductId(dto.productId());
                l.setQuantity(dto.quantity());
                return l;
            }).toList();
            order.setLines(lines);

            Order created = orderService.createOrder(order);
            log.info("Order placed id={}", created.getId());
            saved = true;
            closeDialog();

        } catch (AppException | IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void onCancelDialog() {
        closeDialog();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void refreshTotal() {
        BigDecimal total = lineItems.stream()
                .map(OrderLineDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        try {
            String d = txtDiscount.getText().trim();
            if (!d.isBlank())
                discount = new BigDecimal(d);
        } catch (NumberFormatException ignored) {
        }

        BigDecimal net = total.subtract(discount);
        lblOrderTotal.setText("Subtotal: ₹" + total.toPlainString()
                + "   |   Net Payable: ₹" + net.toPlainString());
        btnPlaceOrder.setDisable(lineItems.isEmpty());
    }

    private TableCell<OrderLineDTO, BigDecimal> currencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : "₹ " + v.toPlainString());
            }
        };
    }

    private void configureDecimalFilter(TextField field) {
        field.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.matches("\\d*\\.?\\d*"))
                field.setText(o);
        });
    }

    private void configureIntegerFilter(TextField field) {
        field.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.matches("\\d*"))
                field.setText(o);
        });
    }

    private void showError(String msg) {
        lblError.setText("⚠️ " + msg);
        lblError.setVisible(true);
    }

    private void closeDialog() {
        MainLayoutController.navigateTo("/fxml/order/OrderList.fxml");
    }

    public boolean isSaved() {
        return saved;
    }
}
