package com.example.ordermonolith.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Applies the order-level pricing rules. Each rule is its own method so it can be
 * read and unit-tested in isolation; there are no literals in the flow &ndash;
 * they all come from {@link PricingProperties}.
 *
 * <p>Deliberately free of Spring/JPA/web types: {@code new PricingCalculator(props)}
 * in a plain JUnit test is all it takes.
 */
@Component
public class PricingCalculator {

    private static final int MONEY_SCALE = 2;

    private final PricingProperties properties;

    public PricingCalculator(PricingProperties properties) {
        this.properties = properties;
    }

    public PriceBreakdown price(List<CartLine> lines, String couponCode) {
        BigDecimal subtotal = subtotal(lines);
        BigDecimal tax = tax(subtotal);
        BigDecimal shipping = shipping(subtotal);
        BigDecimal discount = discount(subtotal, couponCode);
        BigDecimal total = money(subtotal.add(tax).add(shipping).subtract(discount));
        return new PriceBreakdown(money(subtotal), tax, shipping, discount, total);
    }

    private BigDecimal subtotal(List<CartLine> lines) {
        return lines.stream()
                .map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal tax(BigDecimal subtotal) {
        return money(subtotal.multiply(properties.taxRate()));
    }

    private BigDecimal shipping(BigDecimal subtotal) {
        boolean qualifiesForFreeShipping = subtotal.compareTo(properties.freeShippingThreshold()) >= 0;
        return qualifiesForFreeShipping ? money(BigDecimal.ZERO) : money(properties.standardShippingFee());
    }

    private BigDecimal discount(BigDecimal subtotal, String couponCode) {
        if (couponCode == null) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal rate = properties.coupons().get(couponCode.trim());
        return rate == null ? money(BigDecimal.ZERO) : money(subtotal.multiply(rate));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
