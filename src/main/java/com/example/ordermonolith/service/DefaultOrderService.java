package com.example.ordermonolith.service;

import com.example.ordermonolith.payment.PaymentCommand;
import com.example.ordermonolith.payment.PaymentResult;
import com.example.ordermonolith.payment.PaymentStrategyRegistry;
import com.example.ordermonolith.persistence.entity.Order;
import com.example.ordermonolith.pricing.CartLine;
import com.example.ordermonolith.pricing.PriceBreakdown;
import com.example.ordermonolith.pricing.PricingCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default {@link OrderService}. Reads as the four steps of a checkout and
 * delegates each to a focused collaborator:
 *
 * <ol>
 *   <li>{@link ProductCatalog} &ndash; look up + price the lines (read-only tx)</li>
 *   <li>{@link PricingCalculator} &ndash; apply tax / shipping / coupon</li>
 *   <li>{@link PaymentStrategyRegistry} &ndash; charge, <em>outside</em> any tx</li>
 *   <li>{@link OrderWriter} &ndash; persist order + items + stock + audit atomically</li>
 * </ol>
 *
 * This method has no branching on payment method, no SQL and no HTTP.
 */
@Service
class DefaultOrderService implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(DefaultOrderService.class);

    private final ProductCatalog productCatalog;
    private final PricingCalculator pricingCalculator;
    private final PaymentStrategyRegistry paymentStrategies;
    private final OrderWriter orderWriter;

    DefaultOrderService(ProductCatalog productCatalog,
                        PricingCalculator pricingCalculator,
                        PaymentStrategyRegistry paymentStrategies,
                        OrderWriter orderWriter) {
        this.productCatalog = productCatalog;
        this.pricingCalculator = pricingCalculator;
        this.paymentStrategies = paymentStrategies;
        this.orderWriter = orderWriter;
    }

    @Override
    public Order checkout(CheckoutCommand command) {
        List<CartLine> lines = productCatalog.toCartLines(command.items());
        PriceBreakdown price = pricingCalculator.price(lines, command.coupon());

        PaymentResult payment = paymentStrategies.resolve(command.paymentMethod())
                .charge(new PaymentCommand(
                        command.paymentMethod(), price.total(), "usd",
                        command.customerEmail(), command.cardNumber()));

        Order order = orderWriter.persist(command, lines, price, payment);
        log.info("Checkout complete: order {} for {} total {}",
                order.getId(), order.getCustomerEmail(), order.getTotal());
        return order;
    }
}
