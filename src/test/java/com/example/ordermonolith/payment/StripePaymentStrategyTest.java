package com.example.ordermonolith.payment;

import com.example.ordermonolith.error.PaymentGatewayException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** One payment strategy exercised with a mocked HTTP client (Mockito). */
class StripePaymentStrategyTest {

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final PaymentCommand command = PaymentCommand.builder()
            .method(PaymentMethod.STRIPE)
            .amount(new BigDecimal("42.00"))
            .currency("usd")
            .customerEmail("buyer@example.com")
            .build();

    private StripePaymentStrategy strategy(boolean fakeApprovals) {
        return new StripePaymentStrategy(restTemplate, PaymentProperties.builder()
                .baseUrl("https://gw.test/charge")
                .fakeApprovals(fakeApprovals)
                .stripeApiKey("sk_test")
                .paypalApiKey("pp_test")
                .build());
    }

    @Test
    void fakeApprovalsSkipTheNetworkAndReturnASyntheticReference() {
        PaymentResult result = strategy(true).charge(command);

        assertThat(result.provider()).isEqualTo("stripe");
        assertThat(result.reference()).startsWith("stripe_fake_");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void realCallReturnsTheProviderTransactionId() throws Exception {
        JsonNode body = mapper.readTree("{\"id\":\"ch_live_123\"}");
        when(restTemplate.exchange(eq("https://gw.test/charge"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(body));

        PaymentResult result = strategy(false).charge(command);

        assertThat(result.reference()).isEqualTo("ch_live_123");
    }

    @Test
    void transportFailureBecomesPaymentGatewayException() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThatThrownBy(() -> strategy(false).charge(command))
                .isInstanceOf(PaymentGatewayException.class);
    }
}
