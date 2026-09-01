package com.example.ordermonolith.error;

/**
 * A checkout line referenced a product id that does not exist in the catalog.
 * Maps to {@code 404 Not Found}.
 */
public class ProductNotFoundException extends CheckoutException {

    public ProductNotFoundException(long productId) {
        super("Product not found: " + productId);
    }
}
