package com.jewelry.controller;

import com.jewelry.dto.ProductDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ProductViewController {

    private static final Logger log = LoggerFactory.getLogger(ProductViewController.class);

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

        String imagePathStr = dto.getImagePath();
        if (imagePathStr != null && !imagePathStr.isBlank()) {
            Path imagePath = Paths.get(System.getProperty("user.dir"), imagePathStr);
            if (Files.exists(imagePath)) {
                javafx.scene.image.Image image = new javafx.scene.image.Image(imagePath.toUri().toString(), 280, 280, true, true);
                imgProduct.setImage(image);
                imgProduct.setVisible(true);
                imgProduct.setManaged(true);
                imagePlaceholderPane.setVisible(false);
                imagePlaceholderPane.setManaged(false);
            } else {
                log.warn("Stored image not found at {}", imagePath);
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
        lblName.setText(dto.getName() != null ? dto.getName() : "Unknown Product");
        lblSku.setText(dto.getSku() != null ? dto.getSku() : "N/A");
        lblCategory.setText(dto.getCategory() != null ? dto.getCategory() : "N/A");
        lblMetal.setText(dto.getMetal() != null ? dto.getMetal() : "N/A");
        lblPurity.setText(dto.getPurity() != null && !dto.getPurity().isBlank() ? dto.getPurity() : "N/A");

        lblCostPrice.setText(dto.getCostPrice() != null ? "₹ " + dto.getCostPrice().toString() : "₹ 0.00");
        lblSellingPrice.setText(dto.getSellingPrice() != null ? "₹ " + dto.getSellingPrice().toString() : "₹ 0.00");

        if (dto.getSellingPrice() != null && dto.getCostPrice() != null && dto.getCostPrice().doubleValue() > 0) {
            double margin = ((dto.getSellingPrice().doubleValue() - dto.getCostPrice().doubleValue())
                    / dto.getCostPrice().doubleValue()) * 100;
            lblMargin.setText(String.format("%.1f%%", margin));
        } else {
            lblMargin.setText("N/A");
        }

        lblQuantity.setText(String.valueOf(dto.getQuantityOnHand()) + " Units");
        lblWeight.setText(dto.getWeightGrams() != null ? dto.getWeightGrams().toString() + "g" : "N/A");
        lblDescription.setText(dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription()
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
