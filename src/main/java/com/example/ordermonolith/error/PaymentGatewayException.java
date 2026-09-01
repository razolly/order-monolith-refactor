package com.example.ordermonolith.error;

/**
 * The downstream payment provider failed or was unreachable. The original code
 * swallowed this and fabricated a reference so the order still "succeeded" &ndash;
 * hiding a real outage. We now surface it as {@code 502 Bad Gateway} so the
 * caller knows the charge did not happen and no order was created.
 */
public class PaymentGatewayException extends CheckoutException {

    public PaymentGatewayException(String provider, Throwable cause) {
        super("Payment provider '" + provider + "' failed or is unavailable", cause);
    }

    public PaymentGatewayException(String message) {
        super(message);
    }
}
