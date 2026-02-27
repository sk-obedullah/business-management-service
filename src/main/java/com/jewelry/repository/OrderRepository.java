package com.jewelry.repository;

import com.jewelry.entity.Order;
import com.jewelry.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Order}.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :id")
    void updateStatus(@Param("id") Long orderId, @Param("status") OrderStatus newStatus);

    @Modifying
    @Query("UPDATE Order o SET o.totalAmount = :total, o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :id")
    void updateTotal(@Param("id") Long orderId, @Param("total") BigDecimal totalAmount);
}
