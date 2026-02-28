package com.jewelry.util;

import com.jewelry.dto.OrderDTO;
import com.jewelry.dto.OrderLineDTO;
import com.jewelry.entity.Order;
import com.jewelry.entity.OrderLine;

import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper between Order/OrderLine entities and DTOs.
 */
@Mapper
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    OrderDTO toDTO(Order entity);

    @Mapping(target = "stockAvailable", ignore = true)
    OrderLineDTO toLineDTO(OrderLine line);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    OrderLine toLineEntity(OrderLineDTO dto);
}
