package com.example.ordermonolith.service;

import com.example.ordermonolith.error.InsufficientStockException;
import com.example.ordermonolith.payment.PaymentResult;
import com.example.ordermonolith.persistence.entity.AuditLogEntry;
import com.example.ordermonolith.persistence.entity.Order;
import com.example.ordermonolith.persistence.repository.AuditLogRepository;
import com.example.ordermonolith.persistence.repository.OrderRepository;
import com.example.ordermonolith.persistence.repository.ProductRepository;
import com.example.ordermonolith.pricing.CartLine;
import com.example.ordermonolith.pricing.PriceBreakdown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The atomic write step of a checkout: insert the order and its items, decrement
 * stock, and record the audit entry &ndash; all in one transaction.
 *
 * <p><b>Why a separate bean.</b> {@code @Transactional} only takes effect when a
 * method is called <em>through</em> the Spring proxy. If this logic lived in a
 * private method of {@link DefaultOrderService} it would run with no transaction.
 * Keeping it here also draws the boundary the README asks for: the remote payment
 * call happens in {@code DefaultOrderService} <em>before</em> this method, so a
 * database transaction is never held open across an HTTP round-trip.
 *
 * <p><b>Trade-off.</b> Payment succeeds before this commits, so a failure here
 * (e.g. the stock check losing a race) leaves the customer charged with no order.
 * The transaction still guarantees the database is all-or-nothing; a production
 * system would enqueue a refund/compensation on this path. That is out of scope
 * but the seam (one method, one place to react) now exists.
 */
@Component
@RequiredArgsConstructor
class OrderWriter {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    Order persist(CheckoutCommand command, List<CartLine> lines, PriceBreakdown price, PaymentResult payment) {
        Order order = Order.builder()
                .customerEmail(command.customerEmail())
                .subtotal(price.subtotal())
                .tax(price.tax())
                .shipping(price.shipping())
                .discount(price.discount())
                .total(price.total())
                .paymentMethod(command.paymentMethod().name())
                .paymentReference(payment.reference())
                .build();

        for (CartLine line : lines) {
            order.addItem(line.productId(), line.unitPrice(), line.quantity(), line.lineTotal());
            reserveStock(line);
        }

        Order saved = orderRepository.save(order);

        auditLogRepository.save(AuditLogEntry.builder()
                .message("Order " + saved.getId() + " confirmed for " + saved.getCustomerEmail()
                        + " total=" + saved.getTotal()
                        + " via " + saved.getPaymentMethod()
                        + " ref=" + saved.getPaymentReference())
                .build());

        return saved;
    }

    private void reserveStock(CartLine line) {
        int updated = productRepository.decrementStock(line.productId(), line.quantity());
        if (updated == 0) {
            // Lost the race since the availability pre-check, or it was always short.
            // Throwing here rolls the whole transaction back.
            throw new InsufficientStockException(line.productId(), 0, line.quantity());
        }
    }
}
