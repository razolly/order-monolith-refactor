package com.example.ordermonolith.payment;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised payment configuration &ndash; replaces the hardcoded URL and the
 * {@code "Bearer sk_live_HARDCODED_STRIPE_KEY"} literal in the monolith.
 *
 * <p>Bind from {@code payment.*} in {@code application.yml}; secrets (API keys)
 * come from the environment, never from a committed file.
 *
 * @param baseUrl       base URL of the payment service
 * @param fakeApprovals when {@code true}, the HTTP providers skip the network
 *                      call and return a synthetic reference. This makes the
 *                      "explicit and configurable" fake behaviour the README
 *                      asks for: {@code true} for local/dev against the
 *                      non-existent endpoint, {@code false} in real environments
 *                      where a gateway outage must surface as a {@code 502}.
 * @param stripeApiKey  Stripe secret key (env-supplied)
 * @param paypalApiKey  PayPal secret key (env-supplied)
 */
@Builder
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        String baseUrl,
        boolean fakeApprovals,
        String stripeApiKey,
        String paypalApiKey
) {
}
