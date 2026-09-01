package com.example.ordermonolith.error;

/**
 * Common supertype for every failure that is part of the checkout <em>domain</em>
 * (as opposed to an infrastructure failure like a dropped DB connection).
 *
 * <p>Having a shared base lets the exception handler catch the family in one
 * place if it wants to, while still allowing per-type mapping. It is deliberately
 * unchecked: these are not conditions a caller can meaningfully recover from
 * mid-flow &ndash; they should bubble up to the web edge and become a response.
 */
public abstract class CheckoutException extends RuntimeException {

    protected CheckoutException(String message) {
        super(message);
    }

    protected CheckoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
