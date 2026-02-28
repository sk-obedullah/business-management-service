package com.jewelry.controller;

import com.jewelry.util.SnackbarUtil;
import com.jewelry.dto.ProductDTO;
import com.jewelry.entity.Product;
import com.jewelry.exception.AppException;
import com.jewelry.service.ProductService;
import com.jewelry.util.ProductMapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;
import com.jewelry.service.ImageStorageService;

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
@Component
public class ProductFormController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ProductFormController.class);

    @Autowired
    private ProductService productService;
    
    @Autowired
    private ImageStorageService imageStorageService;

    // ── FXML Bindings ────────────────────────────────────────────────────────
    @FXML
    private Label lblTitle;
    @FXML
    private ImageView imgPreview;
    @FXML
    private VBox imagePlaceholder;
    @FXML
    private Button btnRemoveImage;
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
    private ProductDTO currentDTO; // null = Add mode
    private boolean saved = false;
    private String selectedImagePath;
    private Path selectedImageFile;

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
    private void onUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        java.io.File file = fileChooser.showOpenDialog(btnSave.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file.toPath();
            updateImagePreview(file.toURI().toString());
            selectedImagePath = null; // Reset path until saved
        }
    }

    @FXML
    private void onRemoveImage() {
        selectedImageFile = null;
        selectedImagePath = null;
        imgPreview.setImage(null);
        imgPreview.setVisible(false);
        imgPreview.setManaged(false);
        imagePlaceholder.setVisible(true);
        imagePlaceholder.setManaged(true);
        btnRemoveImage.setVisible(false);
        btnRemoveImage.setManaged(false);
    }

    @FXML
    private void onSave() {
        lblError.setVisible(false);

        try {
            ProductDTO dto = collectFormData();

            if (currentDTO == null) {
                // Add mode
                Product created = productService.createProduct(ProductMapper.toEntity(dto));
                log.info("Product created via form: id={}", created.getId());
                SnackbarUtil.showSuccess(btnSave, "Product added successfully!");
            } else {
                // Edit mode
                productService.updateProduct(ProductMapper.toEntity(dto));
                log.info("Product updated via form: id={}", dto.id());
                SnackbarUtil.showSuccess(btnSave, "Product updated successfully!");
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
        txtName.setText(dto.name() != null ? dto.name() : "");
        txtSku.setText(dto.sku() != null ? dto.sku() : "");
        cboCategory.setValue(dto.category());
        cboMetal.setValue(dto.metal());
        txtPurity.setText(dto.purity() != null ? dto.purity() : "");
        txtWeight.setText(dto.weightGrams() != null ? dto.weightGrams().toPlainString() : "");
        txtCostPrice.setText(dto.costPrice() != null ? dto.costPrice().toPlainString() : "");
        txtSellingPrice.setText(dto.sellingPrice() != null ? dto.sellingPrice().toPlainString() : "");
        txtQuantity.setText(String.valueOf(dto.quantityOnHand()));
        txtDescription.setText(dto.description() != null ? dto.description() : "");
        
        selectedImagePath = dto.imagePath();
        if (selectedImagePath != null && !selectedImagePath.isBlank()) {
            Image loadedImage = imageStorageService.loadProductImage(selectedImagePath, 140, 140);
            if (loadedImage != null) {
                imgPreview.setImage(loadedImage);
                imgPreview.setVisible(true);
                imgPreview.setManaged(true);
                imagePlaceholder.setVisible(false);
                imagePlaceholder.setManaged(false);
                btnRemoveImage.setVisible(true);
                btnRemoveImage.setManaged(true);
            } else {
                onRemoveImage(); // reset UI
            }
        }
    }

    private void updateImagePreview(String uri) {
        Image image = new Image(uri, 140, 140, true, true);
        imgPreview.setImage(image);
        imgPreview.setVisible(true);
        imgPreview.setManaged(true);
        imagePlaceholder.setVisible(false);
        imagePlaceholder.setManaged(false);
        btnRemoveImage.setVisible(true);
        btnRemoveImage.setManaged(true);
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

        // Values parsing handles string to number parsing only
        BigDecimal costPrice = costStr.isBlank() ? null : parseBigDecimal(costStr, "Cost price");
        BigDecimal sellingPrice = sellStr.isBlank() ? null : parseBigDecimal(sellStr, "Selling price");
        BigDecimal weight = weightStr.isBlank() ? null : parseBigDecimal(weightStr, "Weight");
        int qty = qtyStr.isBlank() ? 0 : parseInt(qtyStr, "Quantity");

        String finalImagePath = selectedImagePath;

        // Handle Image Save
        if (selectedImageFile != null) {
            finalImagePath = imageStorageService.saveProductImage(selectedImageFile);
        }
        
        return new ProductDTO(
                currentDTO == null ? null : currentDTO.id(),
                name,
                sku,
                category,
                metal,
                purity.isBlank() ? null : purity,
                weight,
                costPrice,
                sellingPrice,
                qty,
                txtDescription.getText().trim(),
                finalImagePath
        );
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
