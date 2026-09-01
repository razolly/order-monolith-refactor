package com.example.ordermonolith.payment;

import com.example.ordermonolith.error.InvalidCheckoutException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the {@link PaymentStrategy} for a {@link PaymentMethod}.
 *
 * <p>Replaces the {@code switch (paymentMethod)} in the monolith. Spring injects
 * <em>every</em> {@code PaymentStrategy} bean as a list; this class indexes them
 * by {@link PaymentStrategy#method()} once at construction. A new provider joins
 * the map just by existing &ndash; this class never changes.
 */
@Component
public class PaymentStrategyRegistry {

    private final Map<PaymentMethod, PaymentStrategy> byMethod = new EnumMap<>(PaymentMethod.class);

    public PaymentStrategyRegistry(List<PaymentStrategy> strategies) {
        for (PaymentStrategy strategy : strategies) {
            PaymentStrategy previous = byMethod.put(strategy.method(), strategy);
            if (previous != null) {
                throw new IllegalStateException(
                        "Two PaymentStrategy beans claim " + strategy.method() + ": "
                                + previous.getClass().getSimpleName() + " and "
                                + strategy.getClass().getSimpleName());
            }
        }
    }

    public PaymentStrategy resolve(PaymentMethod method) {
        PaymentStrategy strategy = byMethod.get(method);
        if (strategy == null) {
            throw new InvalidCheckoutException("No payment strategy registered for " + method);
        }
        return strategy;
    }
}
