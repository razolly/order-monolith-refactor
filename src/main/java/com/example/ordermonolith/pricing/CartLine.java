package com.example.ordermonolith.pricing;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * A single order line after the product has been looked up and priced, but
 * before order-level tax/shipping/discount. Input to {@link PricingCalculator}
 * and carried through to persistence and the response.
 *
 * @param productId   catalog id
 * @param productName snapshot of the name at purchase time
 * @param unitPrice   price per unit at purchase time
 * @param quantity    units ordered (already validated &gt; 0)
 */
@Builder
public record CartLine(
        long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity
) {

    /** Extended price for this line: {@code unitPrice * quantity}. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
