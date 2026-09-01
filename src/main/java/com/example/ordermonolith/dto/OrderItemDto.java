package com.example.ordermonolith.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * API representation of an {@code order_items} row.
 */
@Data
@Builder
public class OrderItemDto {

    private Long id;
    private Long orderId;
    private Long productId;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
}
