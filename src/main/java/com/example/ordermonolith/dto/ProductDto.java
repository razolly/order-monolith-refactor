package com.example.ordermonolith.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * API representation of a {@code products} row.
 */
@Data
@Builder
public class ProductDto {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
}
