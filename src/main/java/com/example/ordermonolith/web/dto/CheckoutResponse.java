package com.example.ordermonolith.web.dto;

import com.example.ordermonolith.persistence.entity.Order;

import java.math.BigDecimal;
import java.util.List;

/**
 * The checkout success body. Replaces the hand-built {@code LinkedHashMap}.
 * Built from an {@link Order} entity by {@link #from} so the mapping lives in
 * one place and the entity itself is never serialized.
 */
public record CheckoutResponse(
        Long orderId,
        String status,
        String customerEmail,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal shipping,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        String paymentReference
) {

    /** A priced line in the response. */
    public record Line(
            long productId,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {
    }

    public static CheckoutResponse from(Order order) {
        List<Line> lines = order.getItems().stream()
                .map(item -> new Line(item.getProductId(), item.getUnitPrice(),
                        item.getQuantity(), item.getLineTotal()))
                .toList();
        return new CheckoutResponse(
                order.getId(),
                order.getStatus().name(),
                order.getCustomerEmail(),
                lines,
                order.getSubtotal(),
                order.getTax(),
                order.getShipping(),
                order.getDiscount(),
                order.getTotal(),
                order.getPaymentMethod(),
                order.getPaymentReference());
    }
}
