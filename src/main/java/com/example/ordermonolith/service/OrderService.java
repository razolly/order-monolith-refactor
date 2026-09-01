package com.example.ordermonolith.service;

import com.example.ordermonolith.persistence.entity.Order;

/**
 * Orchestrates a checkout end to end: price the cart, charge the customer,
 * persist the order atomically.
 *
 * <p>An interface so the web layer and tests depend on the contract, not the
 * wiring.
 */
public interface OrderService {

    /**
     * @return the persisted, CONFIRMED order
     * @throws com.example.ordermonolith.error.ProductNotFoundException   unknown product
     * @throws com.example.ordermonolith.error.InsufficientStockException not enough stock
     * @throws com.example.ordermonolith.error.InvalidCheckoutException   business rule rejected the request
     * @throws com.example.ordermonolith.error.PaymentGatewayException    payment provider failed
     */
    Order checkout(CheckoutCommand command);
}
