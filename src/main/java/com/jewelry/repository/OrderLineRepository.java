package com.jewelry.repository;

import com.jewelry.entity.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link OrderLine}.
 */
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    @Query("SELECT ol FROM OrderLine ol WHERE ol.order.id = :orderId")
    List<OrderLine> findByOrderId(@Param("orderId") Long orderId);

    @Modifying
    @Query("DELETE FROM OrderLine ol WHERE ol.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);
}
