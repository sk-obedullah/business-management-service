package com.jewelry.controller;

import com.jewelry.config.AppContext;
import com.jewelry.dto.ProductDTO;
import com.jewelry.entity.Product;
import com.jewelry.exception.AppException;
import com.jewelry.service.ProductService;
import com.jewelry.util.ProductMapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Product Add/Edit modal dialog (ProductForm.fxml).
 *
 * <p>
 * Operates in two modes determined by {@link #initForEdit(ProductDTO)}:
 * <ul>
 * <li><strong>Add mode</strong> (dto == null) — creates a new Product</li>
 * <li><strong>Edit mode</strong> (dto != null) — populates fields, then
 * updates</li>
 * </ul>
 *
 * <p>
 * All validation is delegated to {@link ProductService}; the controller
 * only catches {@link AppException} and surfaces it as an Alert.
 */
public class ProductFormController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ProductFormController.class);

    // ── FXML Bindings ────────────────────────────────────────────────────────
    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtName;
    @FXML
    private TextField txtSku;
    @FXML
    private ComboBox<String> cboCategory;
    @FXML
    private ComboBox<String> cboMetal;
    @FXML
    private TextField txtPurity;
    @FXML
    private TextField txtWeight;
    @FXML
    private TextField txtCostPrice;
    @FXML
    private TextField txtSellingPrice;
    @FXML
    private TextField txtQuantity;
    @FXML
    private TextArea txtDescription;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;
    @FXML
    private Label lblError;

    // ── State ────────────────────────────────────────────────────────────────
    private final ProductService productService;
    private ProductDTO currentDTO; // null = Add mode
    private boolean saved = false;

    public ProductFormController() {
        this.productService = AppContext.getInstance().getProductService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cboCategory.getItems().addAll(
                "Ring", "Necklace", "Bracelet", "Earring",
                "Pendant", "Anklet", "Bangle", "Chain", "Other");
        cboMetal.getItems().addAll(
                "Gold", "Silver", "Platinum", "Rose Gold", "White Gold",
                "Palladium", "Titanium", "Other");

        lblError.setVisible(false);

        // Live decimal validation for price/weight fields
        addDecimalFilter(txtCostPrice);
        addDecimalFilter(txtSellingPrice);
        addDecimalFilter(txtWeight);
        addIntegerFilter(txtQuantity);
    }

    /**
     * Called by {@link ProductListController} before the dialog is shown.
     *
     * @param dto {@code null} → Add mode; existing DTO → Edit mode
     */
    public void initForEdit(ProductDTO dto) {
        this.currentDTO = dto;

        if (dto == null) {
            lblTitle.setText("Add New Product");
            btnSave.setText("Save Product");
        } else {
            lblTitle.setText("Edit Product");
            btnSave.setText("Update Product");
            populateFields(dto);
        }
    }

    // ── FXML Actions ─────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        lblError.setVisible(false);

        try {
            ProductDTO dto = collectFormData();

            if (currentDTO == null) {
                // Add mode
                Product created = productService.createProduct(ProductMapper.toEntity(dto));
                log.info("Product created via form: id={}", created.getId());
            } else {
                // Edit mode
                dto.setId(currentDTO.getId());
                productService.updateProduct(ProductMapper.toEntity(dto));
                log.info("Product updated via form: id={}", dto.getId());
            }

            saved = true;
            closeDialog();

        } catch (AppException | IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void populateFields(ProductDTO dto) {
        txtName.setText(dto.getName() != null ? dto.getName() : "");
        txtSku.setText(dto.getSku() != null ? dto.getSku() : "");
        cboCategory.setValue(dto.getCategory());
        cboMetal.setValue(dto.getMetal());
        txtPurity.setText(dto.getPurity() != null ? dto.getPurity() : "");
        txtWeight.setText(dto.getWeightGrams() != null ? dto.getWeightGrams().toPlainString() : "");
        txtCostPrice.setText(dto.getCostPrice() != null ? dto.getCostPrice().toPlainString() : "");
        txtSellingPrice.setText(dto.getSellingPrice() != null ? dto.getSellingPrice().toPlainString() : "");
        txtQuantity.setText(String.valueOf(dto.getQuantityOnHand()));
        txtDescription.setText(dto.getDescription() != null ? dto.getDescription() : "");
    }

    private String safeTrim(String str) {
        return str == null ? "" : str.trim();
    }

    private ProductDTO collectFormData() {
        String name = safeTrim(txtName.getText());
        String sku = safeTrim(txtSku.getText()).toUpperCase();
        String category = cboCategory.getValue();
        String metal = cboMetal.getValue();
        String purity = safeTrim(txtPurity.getText());
        String costStr = safeTrim(txtCostPrice.getText());
        String sellStr = safeTrim(txtSellingPrice.getText());
        String weightStr = safeTrim(txtWeight.getText());
        String qtyStr = safeTrim(txtQuantity.getText());

        // UI-level validation (service validates business rules)
        if (name.isBlank())
            throw new IllegalArgumentException("Product name is required.");
        if (sku.isBlank())
            throw new IllegalArgumentException("SKU is required.");
        if (category == null)
            throw new IllegalArgumentException("Category is required.");
        if (metal == null)
            throw new IllegalArgumentException("Metal is required.");
        if (costStr.isBlank())
            throw new IllegalArgumentException("Cost price is required.");
        if (sellStr.isBlank())
            throw new IllegalArgumentException("Selling price is required.");

        BigDecimal costPrice = parseBigDecimal(costStr, "Cost price");
        BigDecimal sellingPrice = parseBigDecimal(sellStr, "Selling price");
        BigDecimal weight = weightStr.isBlank() ? null : parseBigDecimal(weightStr, "Weight");
        int qty = qtyStr.isBlank() ? 0 : parseInt(qtyStr, "Quantity");

        if (costPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Cost price cannot be negative.");
        if (sellingPrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Selling price must be positive.");

        ProductDTO dto = new ProductDTO();
        dto.setName(name);
        dto.setSku(sku);
        dto.setCategory(category);
        dto.setMetal(metal);
        dto.setPurity(purity.isBlank() ? null : purity);
        dto.setWeightGrams(weight);
        dto.setCostPrice(costPrice);
        dto.setSellingPrice(sellingPrice);
        dto.setQuantityOnHand(qty);
        dto.setDescription(txtDescription.getText().trim());
        return dto;
    }

    private BigDecimal parseBigDecimal(String text, String fieldName) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    private int parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    private void addDecimalFilter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*\\.?\\d*")) {
                field.setText(oldVal);
            }
        });
    }

    private void addIntegerFilter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                field.setText(oldVal);
            }
        });
    }

    private void showError(String message) {
        lblError.setText("⚠ " + message);
        lblError.setVisible(true);
    }

    private void closeDialog() {
        MainLayoutController.navigateTo("/fxml/product/ProductList.fxml");
    }

    /** Called by the parent controller to check if a save occurred. */
    public boolean isSaved() {
        return saved;
    }
}
