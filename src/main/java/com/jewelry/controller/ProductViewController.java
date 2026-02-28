package com.jewelry.controller;

import com.jewelry.dto.ProductDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import com.jewelry.service.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ProductViewController {

    private static final Logger log = LoggerFactory.getLogger(ProductViewController.class);

    @Autowired
    private ImageStorageService imageStorageService;

    @FXML
    private ImageView imgProduct;
    @FXML
    private VBox imagePlaceholderPane;

    @FXML
    private Label lblName;
    @FXML
    private Label lblSku;
    @FXML
    private Label lblCategory;
    @FXML
    private Label lblMetal;
    @FXML
    private Label lblPurity;

    @FXML
    private Label lblCostPrice;
    @FXML
    private Label lblSellingPrice;
    @FXML
    private Label lblMargin;

    @FXML
    private Label lblQuantity;
    @FXML
    private Label lblWeight;
    @FXML
    private Label lblDescription;

    private ProductDTO currentDTO;
    private Consumer<ProductDTO> onEditAction;

    private Runnable onBackAction;

    public void initData(ProductDTO dto) {
        this.currentDTO = dto;
        populateFields(dto);

        String imagePathStr = dto.imagePath();
        if (imagePathStr != null && !imagePathStr.isBlank()) {
            javafx.scene.image.Image image = imageStorageService.loadProductImage(imagePathStr, 280, 280);
            if (image != null) {
                imgProduct.setImage(image);
                imgProduct.setVisible(true);
                imgProduct.setManaged(true);
                imagePlaceholderPane.setVisible(false);
                imagePlaceholderPane.setManaged(false);
            } else {
                showPlaceholder();
            }
        } else {
            showPlaceholder();
        }
    }

    private void showPlaceholder() {
        imgProduct.setVisible(false);
        imgProduct.setManaged(false);
        imagePlaceholderPane.setVisible(true);
        imagePlaceholderPane.setManaged(true);
    }

    public void setOnEditAction(Consumer<ProductDTO> onEditAction) {
        this.onEditAction = onEditAction;
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    private void populateFields(ProductDTO dto) {
        lblName.setText(dto.name() != null ? dto.name() : "Unknown Product");
        lblSku.setText(dto.sku() != null ? dto.sku() : "N/A");
        lblCategory.setText(dto.category() != null ? dto.category() : "N/A");
        lblMetal.setText(dto.metal() != null ? dto.metal() : "N/A");
        lblPurity.setText(dto.purity() != null && !dto.purity().isBlank() ? dto.purity() : "N/A");

        lblCostPrice.setText(dto.costPrice() != null ? "₹ " + dto.costPrice().toString() : "₹ 0.00");
        lblSellingPrice.setText(dto.sellingPrice() != null ? "₹ " + dto.sellingPrice().toString() : "₹ 0.00");

        if (dto.sellingPrice() != null && dto.costPrice() != null && dto.costPrice().doubleValue() > 0) {
            double margin = ((dto.sellingPrice().doubleValue() - dto.costPrice().doubleValue())
                    / dto.costPrice().doubleValue()) * 100;
            lblMargin.setText(String.format("%.1f%%", margin));
        } else {
            lblMargin.setText("N/A");
        }

        lblQuantity.setText(String.valueOf(dto.quantityOnHand()) + " Units");
        lblWeight.setText(dto.weightGrams() != null ? dto.weightGrams().toString() + "g" : "N/A");
        lblDescription.setText(dto.description() != null && !dto.description().isBlank() ? dto.description()
                : "No description provided.");
    }

    @FXML
    private void onEdit() {
        if (onEditAction != null && currentDTO != null) {
            onEditAction.accept(currentDTO);
        }
    }

    @FXML
    private void onBack() {
        if (onBackAction != null) {
            onBackAction.run();
        } else {
            MainLayoutController.navigateTo("/fxml/product/ProductList.fxml", (ProductListController controller) -> {
                // Can pass state back if necessary
            });
        }
    }
}
