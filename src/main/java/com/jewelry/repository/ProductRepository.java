package com.jewelry.repository;

import com.jewelry.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Product}.
 * All standard CRUD (save, findById, findAll, delete) is inherited from JpaRepository.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByCategory(String category);

    /** Products whose stock is at or below the threshold. */
    List<Product> findByQuantityOnHandLessThanEqual(int threshold);

    /** Full-text keyword search across name, sku, category, and description. */
    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(p.name)        LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(p.sku)         LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(p.category)    LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :kw, '%'))
            """)
    List<Product> search(@Param("kw") String keyword);
}
