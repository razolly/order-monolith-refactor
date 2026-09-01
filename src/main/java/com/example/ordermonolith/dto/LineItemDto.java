package com.example.ordermonolith.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * A priced order line as returned to the client - the order_items columns
 * without the surrogate/foreign keys.
 */
@Data
@Builder
public class LineItemDto {

    private Long productId;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
}
