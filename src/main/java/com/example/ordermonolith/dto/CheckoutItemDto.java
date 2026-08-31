package com.example.ordermonolith.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutItemDto {

    @NotNull(message = "each item needs productId")
    private Long productId;

    @NotNull(message = "each item needs quantity")
    @Positive(message = "quantity must be > 0")
    private Integer quantity;
}
