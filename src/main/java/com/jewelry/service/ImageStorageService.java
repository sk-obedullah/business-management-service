package com.jewelry.service;

import javafx.scene.image.Image;
import java.nio.file.Path;

/**
 * Service for managing application image assets (e.g. product images).
 */
public interface ImageStorageService {

    /**
     * Saves a product image to storage from the given source file.
     *
     * @param sourceFile the path to the original uploaded file
     * @return a relative path or URI to be stored in the database
     * @throws RuntimeException if saving fails
     */
    String saveProductImage(Path sourceFile);

    /**
     * Loads a product image from storage.
     *
     * @param imagePath the relative path or URI stored in the database
     * @param width     the requested image width
     * @param height    the requested image height
     * @return the loaded Image, or null if not found
     */
    Image loadProductImage(String imagePath, double width, double height);

    /**
     * Deletes a product image from storage.
     *
     * @param imagePath the relative path or URI stored in the database
     */
    void deleteProductImage(String imagePath);
}
