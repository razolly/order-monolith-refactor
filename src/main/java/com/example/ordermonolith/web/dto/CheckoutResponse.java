package com.example.ordermonolith.web.dto;

import com.example.ordermonolith.persistence.entity.Order;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * The checkout success body. Replaces the hand-built {@code LinkedHashMap}.
 * Built from an {@link Order} entity by {@link #from} so the mapping lives in
 * one place and the entity itself is never serialized.
 */
@Builder
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
    @Builder
    public record Line(
            long productId,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {
    }

    public static CheckoutResponse from(Order order) {
        List<Line> lines = order.getItems().stream()
                .map(item -> Line.builder()
                        .productId(item.getProductId())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .customerEmail(order.getCustomerEmail())
                .lines(lines)
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .shipping(order.getShipping())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentReference(order.getPaymentReference())
                .build();
    }
}
