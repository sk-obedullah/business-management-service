package com.jewelry.service;

import com.jewelry.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * Business logic contract for Product management.
 *
 * <p>
 * Encapsulates validation, SKU uniqueness enforcement, and domain invariants.
 * Controllers depend on this interface, never on the repository directly.
 */
public interface ProductService {

    Product createProduct(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    void updateProduct(Product product);

    void deleteProduct(Long id);

    List<Product> search(String keyword);

    List<Product> findByCategory(String category);

    List<Product> findLowStock(int threshold);

    Optional<Product> findBySku(String sku);
}
