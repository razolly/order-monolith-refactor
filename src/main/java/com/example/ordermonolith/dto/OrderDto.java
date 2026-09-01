package com.example.ordermonolith.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * API representation of an {@code orders} row, optionally with its line items.
 */
@Data
@Builder
public class OrderDto {

    private Long id;
    private String customerEmail;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal discount;
    private BigDecimal total;
    private String paymentMethod;
    private String paymentReference;
    private String status;
    private Instant createdAt;

    private List<OrderItemDto> items;
}
