package com.jewelry.service.impl;

import com.jewelry.entity.Product;
import com.jewelry.exception.DuplicateEntityException;
import com.jewelry.exception.EntityNotFoundException;
import com.jewelry.repository.ProductRepository;
import com.jewelry.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Production implementation of {@link ProductService}.
 *
 * <p>
 * Business rules enforced here:
 * <ul>
 * <li>SKU uniqueness — checked before insert</li>
 * <li>Existence check before update/delete</li>
 * <li>Input validation (non-null required fields)</li>
 * </ul>
 *
 * <p>
 * <strong>Spring Boot migration note:</strong> annotate with {@code @Service},
 * inject {@link ProductRepository} via constructor, remove manual validation
 * in favour of {@code @Valid} + Bean Validation.
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Product createProduct(Product product) {
        validateNotNull(product, "Product");
        validateNotBlank(product.getName(), "Product name");
        validateNotBlank(product.getSku(), "Product SKU");

        if (productRepository.existsBySku(product.getSku())) {
            throw new DuplicateEntityException(
                    "A product with SKU '" + product.getSku() + "' already exists.");
        }

        Product saved = productRepository.save(product);
        log.info("Created product id={} sku={}", saved.getId(), saved.getSku());
        return saved;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public void updateProduct(Product product) {
        validateNotNull(product, "Product");
        validateNotNull(product.getId(), "Product ID");

        // Ensure entity exists
        productRepository.findById(product.getId())
                .orElseThrow(() -> new EntityNotFoundException("Product", product.getId()));

        // Ensure SKU uniqueness when SKU changes
        productRepository.findBySku(product.getSku()).ifPresent(existing -> {
            if (!existing.getId().equals(product.getId())) {
                throw new DuplicateEntityException(
                        "SKU '" + product.getSku() + "' is already used by another product.");
            }
        });

        productRepository.save(product);
        log.info("Updated product id={}", product.getId());
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
        productRepository.deleteById(id);
        log.info("Deleted product id={}", id);
    }

    @Override
    public List<Product> search(String keyword) {
        if (keyword == null || keyword.isBlank())
            return findAll();
        return productRepository.search(keyword.trim());
    }

    @Override
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Override
    public List<Product> findLowStock(int threshold) {
        return productRepository.findByQuantityOnHandLessThanEqual(threshold);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    // ── Validation helpers ───────────────────────────────────────────────────

    private void validateNotNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
