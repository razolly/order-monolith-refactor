package com.example.ordermonolith.payment;

/**
 * Outcome of a successful charge. A failed charge is signalled by a
 * {@link com.example.ordermonolith.error.PaymentGatewayException} or
 * {@link com.example.ordermonolith.error.InvalidCheckoutException}, not by a
 * flag on this type &ndash; so callers cannot accidentally treat a failure as a
 * success (which is exactly what the old "fake a reference" code did).
 *
 * @param reference provider-side transaction id, persisted on the order
 * @param provider  human-readable provider name, for the audit trail
 */
public record PaymentResult(String reference, String provider) {
}
