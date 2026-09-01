package com.example.ordermonolith.payment;

/**
 * One payment provider integration.
 *
 * <p>The Open/Closed seam of the system: the checkout flow depends only on this
 * interface, so a new provider is added by writing a new implementation and
 * registering it as a Spring bean &ndash; no existing class changes.
 */
public interface PaymentStrategy {

    /** The method this strategy handles. Used by the registry to route a charge. */
    PaymentMethod method();

    /**
     * Attempt to capture {@code command.amount()}.
     *
     * @throws com.example.ordermonolith.error.InvalidCheckoutException if the
     *         payment instrument itself is rejected (e.g. malformed card) &ndash; a
     *         {@code 422}
     * @throws com.example.ordermonolith.error.PaymentGatewayException if the
     *         provider is unreachable or errors &ndash; a {@code 502}
     */
    PaymentResult charge(PaymentCommand command);
}
