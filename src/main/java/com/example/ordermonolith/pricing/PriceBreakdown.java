package com.example.ordermonolith.pricing;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Immutable result of pricing an order. Every field is already rounded to 2
 * decimal places. {@code total = subtotal + tax + shipping - discount}.
 */
@Builder
public record PriceBreakdown(
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal shipping,
        BigDecimal discount,
        BigDecimal total
) {
}
