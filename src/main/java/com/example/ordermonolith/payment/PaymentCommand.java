package com.example.ordermonolith.payment;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Immutable instruction to charge a customer. This is the payment package's own
 * input type &ndash; the service builds it from validated data, so strategies
 * never see a web DTO or a raw request map.
 *
 * <p>Built with the generated builder rather than the positional constructor:
 * {@code customerEmail} and {@code cardNumber} are both {@code String}, so a
 * named builder call removes the chance of transposing them.
 *
 * @param method        which provider should handle the charge
 * @param amount        the gross amount to capture, in major currency units
 * @param currency      ISO currency code, lower-case (e.g. {@code "usd"})
 * @param customerEmail who is being charged (used as the provider-side reference)
 * @param cardNumber    PAN, only populated for {@link PaymentMethod#CREDIT_CARD};
 *                      never logged
 */
@Builder
public record PaymentCommand(
        PaymentMethod method,
        BigDecimal amount,
        String currency,
        String customerEmail,
        String cardNumber
) {
}
