package com.example.ordermonolith.payment;

import java.math.BigDecimal;

/**
 * Immutable instruction to charge a customer. This is the payment package's own
 * input type &ndash; the service builds it from validated data, so strategies
 * never see a web DTO or a raw request map.
 *
 * @param method        which provider should handle the charge
 * @param amount        the gross amount to capture, in major currency units
 * @param currency      ISO currency code, lower-case (e.g. {@code "usd"})
 * @param customerEmail who is being charged (used as the provider-side reference)
 * @param cardNumber    PAN, only populated for {@link PaymentMethod#CREDIT_CARD};
 *                      never logged
 */
public record PaymentCommand(
        PaymentMethod method,
        BigDecimal amount,
        String currency,
        String customerEmail,
        String cardNumber
) {

    public static PaymentCommand of(PaymentMethod method, BigDecimal amount, String customerEmail, String cardNumber) {
        return new PaymentCommand(method, amount, "usd", customerEmail, cardNumber);
    }
}
