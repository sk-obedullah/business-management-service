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
        OrderDTO dto = new OrderDTO();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerName(entity.getCustomerName());
        dto.setCustomerPhone(entity.getCustomerPhone());
        dto.setCustomerEmail(entity.getCustomerEmail());
        dto.setCustomerAddress(entity.getCustomerAddress());
        dto.setOrderDate(entity.getOrderDate());
        dto.setStatus(entity.getStatus());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setDiscount(entity.getDiscount());
        dto.setNotes(entity.getNotes());
        if (entity.getLines() != null) {
            dto.setLines(entity.getLines().stream()
                    .map(OrderMapper::toLineDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public static OrderLineDTO toLineDTO(OrderLine line) {
        OrderLineDTO dto = new OrderLineDTO();
        dto.setProductId(line.getProductId());
        dto.setProductName(line.getProductName());
        dto.setProductSku(line.getProductSku());
        dto.setQuantity(line.getQuantity());
        dto.setUnitPrice(line.getUnitPrice());
        dto.setCostPrice(line.getCostPrice());
        return dto;
    }

    public static OrderLine toLineEntity(OrderLineDTO dto) {
        return new OrderLine(
                dto.getProductId(), dto.getProductName(), dto.getProductSku(),
                dto.getQuantity(), dto.getUnitPrice(), dto.getCostPrice());
    }
}
