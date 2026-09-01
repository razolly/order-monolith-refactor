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

    private final PaymentCommand command = new PaymentCommand(
            PaymentMethod.STRIPE, new BigDecimal("42.00"), "usd", "buyer@example.com", null);

    private StripePaymentStrategy strategy(boolean fakeApprovals) {
        return new StripePaymentStrategy(restTemplate,
                new PaymentProperties("https://gw.test/charge", fakeApprovals, "sk_test", "pp_test"));
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
