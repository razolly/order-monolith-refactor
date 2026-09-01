package com.example.ordermonolith.payment;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Stripe integration. Amounts are sent in cents. Registered automatically by
 * being a {@code @Component}; nothing routes to it by name.
 */
@Component
class StripePaymentStrategy extends HttpPaymentStrategy {

    StripePaymentStrategy(RestTemplate restTemplate, PaymentProperties properties) {
        super(restTemplate, properties);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.STRIPE;
    }

    @Override
    protected String provider() {
        return "stripe";
    }

    @Override
    protected String chargeUrl() {
        return baseUrl();
    }

    @Override
    protected HttpHeaders headers() {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(properties().stripeApiKey());
        return headers;
    }

    @Override
    protected Map<String, Object> body(PaymentCommand command) {
        return Map.of(
                "provider", "stripe",
                "amount", command.amount().multiply(BigDecimal.valueOf(100)).intValueExact(),
                "currency", command.currency(),
                "customer", command.customerEmail());
    }
}
