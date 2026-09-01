package com.example.ordermonolith.payment;

/**
 * The payment methods the checkout accepts.
 *
 * <p>An enum (rather than a free-form string) gives compile-time safety in the
 * flow and lets Jackson reject an unknown method at bind time &ndash; the
 * original "is it one of these three strings?" check disappears.
 */
public enum PaymentMethod {
    STRIPE,
    PAYPAL,
    CREDIT_CARD
}
