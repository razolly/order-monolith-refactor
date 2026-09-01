package com.example.ordermonolith.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * One requested line of a checkout. Nested inside {@link CheckoutRequest}, which
 * carries {@code @Valid} so these constraints run too.
 */
public record CheckoutItem(

        @NotNull(message = "productId is required")
        @Min(value = 1, message = "productId must be positive")
        Long productId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {
}
