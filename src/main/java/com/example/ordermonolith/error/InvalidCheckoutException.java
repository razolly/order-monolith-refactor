package com.example.ordermonolith.error;

/**
 * The request was well-formed enough to bind and pass bean validation, but a
 * deeper business rule rejected it (e.g. a card number that fails the payment
 * provider's own check). Maps to {@code 422 Unprocessable Entity}.
 *
 * <p>Contrast with a bean-validation failure, which never reaches the service and
 * is turned into {@code 400 Bad Request} by the framework.
 */
public class InvalidCheckoutException extends CheckoutException {

    public InvalidCheckoutException(String message) {
        super(message);
    }
}
