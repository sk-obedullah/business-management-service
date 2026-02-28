package com.jewelry.service.impl;

import com.jewelry.exception.AppException;
import com.jewelry.service.ImageStorageService;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Implementation of {@link ImageStorageService} using the local file system
 * (e.g. user home directory).
 */
@Service
public class LocalImageStorageServiceImpl implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorageServiceImpl.class);

    private final Path baseStorageDir;

    public LocalImageStorageServiceImpl() {
        this.baseStorageDir = Paths.get(System.getProperty("user.home"), ".jewelry_management");
    }

    @Override
    public String saveProductImage(Path sourceFile) {
        if (sourceFile == null || !Files.exists(sourceFile)) {
            throw new AppException("Invalid source file for image upload.");
        }

        try {
            Path targetDir = baseStorageDir.resolve(Paths.get("data", "images", "products"));
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String extension = "";
            String fileName = sourceFile.getFileName().toString();
            int i = fileName.lastIndexOf('.');
            if (i >= 0) {
                extension = fileName.substring(i);
            }

            String newFileName = UUID.randomUUID().toString() + extension;
            Path targetPath = targetDir.resolve(newFileName);

            Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved product image to {}", targetPath);

            // Return relative path
            return Paths.get("data", "images", "products", newFileName).toString();

        } catch (IOException e) {
            log.error("Failed to save product image", e);
            throw new AppException("Failed to save product image: " + e.getMessage());
        }
    }

    @Override
    public Image loadProductImage(String imagePath, double width, double height) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        Path fullPath = baseStorageDir.resolve(imagePath);
        if (Files.exists(fullPath)) {
            return new Image(fullPath.toUri().toString(), width, height, true, true);
        } else {
            log.warn("Stored image not found at {}", fullPath);
            return null;
        }
    }

    @Override
    public void deleteProductImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        Path fullPath = baseStorageDir.resolve(imagePath);
        try {
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                log.info("Deleted product image at {}", fullPath);
            }
        } catch (IOException e) {
            log.error("Failed to delete product image at {}", fullPath, e);
        }
    }
}
