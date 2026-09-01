package com.example.ordermonolith;

import com.example.ordermonolith.persistence.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Proves the checkout write path is atomic: if anything after "payment" throws,
 * the transaction rolls back and leaves no order and no stock change.
 *
 * <p>We simulate the failure by making the audit-log write blow up - the last
 * step in {@link com.example.ordermonolith.service.OrderWriter}. In the monolith
 * this happened after two un-transacted writes, so it corrupted the order.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutAtomicityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    void failureAfterPaymentLeavesNoOrderAndNoStockChange() throws Exception {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("audit sink down"));

        Integer stockBefore = jdbcTemplate.queryForObject(
                "SELECT stock FROM products WHERE id = 1", Integer.class);
        Long ordersBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);

        Map<String, Object> request = Map.of(
                "customerEmail", "buyer@example.com",
                "paymentMethod", "CREDIT_CARD",
                "cardNumber", "4111111111111111",
                "items", List.of(Map.of("productId", 1, "quantity", 2)));

        // The audit failure is an infrastructure error, not a domain one: it
        // propagates out of the dispatcher. We only care that the DB rolled back.
        try {
            mockMvc.perform(post("/api/v1/orders/checkout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        } catch (Exception expected) {
            // swallow - asserted via DB state below
        }

        Integer stockAfter = jdbcTemplate.queryForObject(
                "SELECT stock FROM products WHERE id = 1", Integer.class);
        Long ordersAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);

        assertThat(stockAfter).isEqualTo(stockBefore);
        assertThat(ordersAfter).isEqualTo(ordersBefore);
    }
}
