package com.example.ordermonolith;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Characterisation tests for the CURRENT (messy) behaviour.
 *
 * These pass against the monolith as shipped. When you refactor, keep the
 * observable contract green - or consciously decide to change it and update
 * the assertions (e.g. the 500-string responses should become structured
 * problem-detail bodies with proper status codes).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void checkout_withCreditCard_confirmsOrderAndDecrementsStock() throws Exception {
        Integer stockBefore = jdbcTemplate.queryForObject(
                "SELECT stock FROM products WHERE id = 2", Integer.class);

        Map<String, Object> request = Map.of(
                "customerEmail", "buyer@example.com",
                "paymentMethod", "CREDIT_CARD",
                "cardNumber", "4111111111111111",
                "items", List.of(Map.of("productId", 2, "quantity", 3)));

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.total").value(127.98)); // 3 * 39.50 = 118.50, +8% tax 9.48, free shipping over 100

        Integer stockAfter = jdbcTemplate.queryForObject(
                "SELECT stock FROM products WHERE id = 2", Integer.class);
        assertThat(stockAfter).isEqualTo(stockBefore - 3);

        Long audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE message LIKE '%buyer@example.com%'", Long.class);
        assertThat(audits).isEqualTo(1L);
    }

    @Test
    void checkout_withMissingEmail_returns400() throws Exception {
        Map<String, Object> request = Map.of(
                "paymentMethod", "CREDIT_CARD",
                "items", List.of(Map.of("productId", 1, "quantity", 1)));

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_withUnknownProduct_returns404() throws Exception {
        Map<String, Object> request = Map.of(
                "customerEmail", "buyer@example.com",
                "paymentMethod", "CREDIT_CARD",
                "cardNumber", "4111111111111111",
                "items", List.of(Map.of("productId", 9999, "quantity", 1)));

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkout_withInsufficientStock_returns422() throws Exception {
        Map<String, Object> request = Map.of(
                "customerEmail", "buyer@example.com",
                "paymentMethod", "CREDIT_CARD",
                "cardNumber", "4111111111111111",
                "items", List.of(Map.of("productId", 5, "quantity", 999)));

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
