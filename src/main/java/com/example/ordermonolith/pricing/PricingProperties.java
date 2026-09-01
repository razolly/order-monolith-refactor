package com.example.ordermonolith.pricing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The pricing rules, externalised from code into {@code pricing.*}.
 *
 * @param taxRate               fraction of subtotal added as tax (e.g. 0.08)
 * @param freeShippingThreshold subtotal at/above which shipping is free
 * @param standardShippingFee   flat shipping fee below the threshold
 * @param coupons               coupon code &rarr; fraction of subtotal discounted
 *                              (e.g. {@code SAVE5 -> 0.05}); unknown codes are ignored
 */
@ConfigurationProperties(prefix = "pricing")
public record PricingProperties(
        BigDecimal taxRate,
        BigDecimal freeShippingThreshold,
        BigDecimal standardShippingFee,
        Map<String, BigDecimal> coupons
) {

    public PricingProperties {
        coupons = coupons == null ? Map.of() : coupons;
    }
}
