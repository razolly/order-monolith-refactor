package com.example.ordermonolith.service;

import com.example.ordermonolith.payment.PaymentMethod;
import lombok.Builder;

import java.util.List;

/**
 * The service-layer input for a checkout &ndash; a plain, already-validated value
 * object. The controller maps the web {@code CheckoutRequest} onto this so no
 * {@code jakarta.*} / Jackson / MVC type crosses the layer boundary.
 */
@Builder
public record CheckoutCommand(
        String customerEmail,
        List<Item> items,
        PaymentMethod paymentMethod,
        String coupon,
        String cardNumber
) {

    @Builder
    public record Item(long productId, int quantity) {
    }
}
