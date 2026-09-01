package com.example.ordermonolith.payment;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * PayPal integration. Sends the amount as a decimal string and authenticates
 * with its own key. Shares the same {@link RestTemplate} as every other client
 * (the monolith's PayPal branch {@code new}-ed its own).
 */
@Component
class PaypalPaymentStrategy extends HttpPaymentStrategy {

    PaypalPaymentStrategy(RestTemplate restTemplate, PaymentProperties properties) {
        super(restTemplate, properties);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    protected String provider() {
        return "paypal";
    }

    @Override
    protected String chargeUrl() {
        return baseUrl() + "?provider=paypal";
    }

    @Override
    protected HttpHeaders headers() {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(properties().paypalApiKey());
        return headers;
    }

    @Override
    protected Map<String, Object> body(PaymentCommand command) {
        return Map.of(
                "intent", "CAPTURE",
                "amount", command.amount().toPlainString(),
                "payer_email", command.customerEmail());
    }
}
