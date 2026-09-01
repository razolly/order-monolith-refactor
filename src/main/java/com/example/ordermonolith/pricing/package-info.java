/**
 * Order pricing: turning a set of priced cart lines into a
 * {@link com.example.ordermonolith.pricing.PriceBreakdown}.
 *
 * <p><b>Why this package exists.</b> Tax, shipping and coupon rules were inline
 * {@code BigDecimal} math in the controller with magic numbers ({@code 0.08},
 * {@code 100}, {@code 9.99}, {@code "SAVE5"}). Here each rule is a named method
 * and every number comes from {@link com.example.ordermonolith.pricing.PricingProperties}
 * ({@code pricing.*} in config). {@link com.example.ordermonolith.pricing.PricingCalculator}
 * has no Spring or JPA dependency, so it is unit-testable with a plain
 * constructor call &ndash; which the README asks for explicitly.
 */
package com.example.ordermonolith.pricing;
