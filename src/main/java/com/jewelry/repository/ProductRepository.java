package com.jewelry.repository;

import com.jewelry.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * Product-specific repository contract.
 *
 * <p>
 * Extends {@link BaseRepository} and declares product-domain queries
 * that go beyond standard CRUD.
 */
public interface ProductRepository extends BaseRepository<Product, Long> {

    /** Checks existence by SKU to enforce uniqueness before insert. */
    boolean existsBySku(String sku);

    /** Returns products whose quantity-on-hand is below the reorder level. */
    List<Product> findLowStock(int threshold);

    /** Returns products matching the given category (case-insensitive). */
    List<Product> findByCategory(String category);

    /** Full-text search across name and description. */
    List<Product> search(String keyword);

    /** Returns the product with the supplied SKU if it exists. */
    Optional<Product> findBySku(String sku);
}
