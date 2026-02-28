package com.jewelry.util;

import com.jewelry.dto.OrderDTO;
import com.jewelry.dto.OrderLineDTO;
import com.jewelry.entity.Order;
import com.jewelry.entity.OrderLine;

import java.util.stream.Collectors;

/**
 * Stateless mapper between Order/OrderLine entities and DTOs.
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderDTO toDTO(Order entity) {
        if (entity == null)
            return null;
            
        java.util.List<OrderLineDTO> lines = new java.util.ArrayList<>();
        if (entity.getLines() != null) {
            lines = entity.getLines().stream()
                    .map(OrderMapper::toLineDTO)
                    .collect(Collectors.toList());
        }
        
        return new OrderDTO(
                entity.getId(),
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getCustomerPhone(),
                entity.getCustomerEmail(),
                entity.getCustomerAddress(),
                entity.getOrderDate(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getDiscount(),
                entity.getNotes(),
                lines
        );
    }

    public static OrderLineDTO toLineDTO(OrderLine line) {
        return new OrderLineDTO(
                line.getProductId(),
                line.getProductName(),
                line.getProductSku(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getCostPrice(),
                0 // stockAvailable is transient
        );
    }

    public static OrderLine toLineEntity(OrderLineDTO dto) {
        return new OrderLine(
                dto.productId(), dto.productName(), dto.productSku(),
                dto.quantity(), dto.unitPrice(), dto.costPrice());
    }
}
