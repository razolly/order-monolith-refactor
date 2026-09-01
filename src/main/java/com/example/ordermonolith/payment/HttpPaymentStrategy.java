package com.example.ordermonolith.payment;

import com.example.ordermonolith.error.PaymentGatewayException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Shared machinery for providers that charge over HTTP (Stripe, PayPal).
 *
 * <p>Pulls the three things every HTTP provider was duplicating in the monolith
 * into one place: the (single, injected) {@link RestTemplate}, the
 * fake-approval short-circuit for local dev, and turning any transport failure
 * into a {@link PaymentGatewayException} instead of swallowing it. Subclasses
 * only describe <em>their</em> request: URL, headers, body, and how to read the
 * reference out of the response.
 */
abstract class HttpPaymentStrategy implements PaymentStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;
    private final PaymentProperties properties;

    protected HttpPaymentStrategy(RestTemplate restTemplate, PaymentProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    protected String baseUrl() {
        return properties.baseUrl();
    }

    protected PaymentProperties properties() {
        return properties;
    }

    /** Provider label for logs, the audit trail and the reference prefix. */
    protected abstract String provider();

    /** Absolute URL to POST the charge to. */
    protected abstract String chargeUrl();

    /** Provider-specific auth / content headers. */
    protected abstract HttpHeaders headers();

    /** Provider-specific request body. */
    protected abstract Map<String, Object> body(PaymentCommand command);

    @Override
    public PaymentResult charge(PaymentCommand command) {
        if (properties.fakeApprovals()) {
            String reference = provider().toLowerCase() + "_fake_" + UUID.randomUUID();
            log.info("[{}] fake-approvals enabled - not calling the gateway, reference={}", provider(), reference);
            return PaymentResult.builder().reference(reference).provider(provider()).build();
        }

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    chargeUrl(), HttpMethod.POST,
                    new HttpEntity<>(body(command), headers()), JsonNode.class);
            JsonNode json = response.getBody();
            if (json == null || !json.hasNonNull("id")) {
                throw new PaymentGatewayException(provider() + " returned no transaction id");
            }
            return PaymentResult.builder()
                    .reference(json.get("id").asText())
                    .provider(provider())
                    .build();
        } catch (RestClientException e) {
            throw new PaymentGatewayException(provider(), e);
        }
    }

    protected static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
