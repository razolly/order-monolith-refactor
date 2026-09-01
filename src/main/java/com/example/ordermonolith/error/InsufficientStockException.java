package com.example.ordermonolith.error;

/**
 * A product exists but does not have enough stock to satisfy the requested
 * quantity. This is a business-rule rejection, not a client mistake, so it maps
 * to {@code 422 Unprocessable Entity}.
 */
public class InsufficientStockException extends CheckoutException {

    public InsufficientStockException(long productId, int available, int requested) {
        super("Not enough stock for product " + productId
                + " (have " + available + ", want " + requested + ")");
    }
}
