package com.example.ordermonolith.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test - no Spring context. Locks down each pricing rule and the
 * final composition, using explicit properties instead of the old magic numbers.
 */
class PricingCalculatorTest {

    private final PricingProperties properties = PricingProperties.builder()
            .taxRate(new BigDecimal("0.08"))
            .freeShippingThreshold(new BigDecimal("100.00"))
            .standardShippingFee(new BigDecimal("9.99"))
            .coupons(Map.of("SAVE5", new BigDecimal("0.05")))
            .build();

    private final PricingCalculator calculator = new PricingCalculator(properties);

    private static CartLine line(String unitPrice, int qty) {
        return CartLine.builder()
                .productId(1L)
                .productName("widget")
                .unitPrice(new BigDecimal(unitPrice))
                .quantity(qty)
                .build();
    }

    @Test
    void appliesTaxAndFlatShippingBelowThreshold() {
        PriceBreakdown result = calculator.price(List.of(line("20.00", 1)), null);

        assertThat(result.subtotal()).isEqualByComparingTo("20.00");
        assertThat(result.tax()).isEqualByComparingTo("1.60");
        assertThat(result.shipping()).isEqualByComparingTo("9.99");
        assertThat(result.discount()).isEqualByComparingTo("0.00");
        assertThat(result.total()).isEqualByComparingTo("31.59");
    }

    @Test
    void shippingIsFreeAtOrAboveThreshold() {
        PriceBreakdown result = calculator.price(List.of(line("50.00", 2)), null);

        assertThat(result.subtotal()).isEqualByComparingTo("100.00");
        assertThat(result.shipping()).isEqualByComparingTo("0.00");
        assertThat(result.total()).isEqualByComparingTo("108.00");
    }

    @Test
    void knownCouponDiscountsSubtotal() {
        PriceBreakdown result = calculator.price(List.of(line("100.00", 1)), "SAVE5");

        assertThat(result.discount()).isEqualByComparingTo("5.00");
        assertThat(result.total()).isEqualByComparingTo("103.00"); // 100 + 8 tax + 0 shipping - 5
    }

    @Test
    void unknownCouponIsIgnored() {
        PriceBreakdown withBadCoupon = calculator.price(List.of(line("100.00", 1)), "NOPE");
        PriceBreakdown withNoCoupon = calculator.price(List.of(line("100.00", 1)), null);

        assertThat(withBadCoupon.discount()).isEqualByComparingTo("0.00");
        assertThat(withBadCoupon.total()).isEqualByComparingTo(withNoCoupon.total());
    }

    @Test
    void sumsMultipleLines() {
        PriceBreakdown result = calculator.price(
                List.of(line("10.00", 2), line("5.00", 4)), null);

        assertThat(result.subtotal()).isEqualByComparingTo("40.00");
        assertThat(result.tax()).isEqualByComparingTo("3.20");
        assertThat(result.total()).isEqualByComparingTo("53.19");
    }
}
