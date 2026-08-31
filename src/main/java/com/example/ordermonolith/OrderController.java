package com.example.ordermonolith;

import com.example.ordermonolith.dto.CheckoutDto;
import com.example.ordermonolith.dto.CheckoutItemDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 *  THE "MESSY MONOLITH"  -  DO NOT USE THIS AS A REFERENCE FOR GOOD CODE
 * ============================================================================
 *
 * This one class is deliberately doing the job of a controller, a request
 * validator, three repositories, a pricing engine, a payment gateway client,
 * an audit service and a transaction script - all at once.
 *
 * It compiles. It runs. The happy path even works. Your job (see README.md) is
 * to carve it into something a senior engineer would be happy to own.
 *
 * Known smells intentionally baked in:
 *   1. Mixed responsibilities   - JDBC + HTTP + math + logging in the handler
 *   2. Unstructured payloads     - Map<String,Object> / JsonNode instead of DTOs
 *   3. Nested control flow       - pyramid of if/else validation, catch-all 500s
 *   4. Hardcoded payment routing - switch on "STRIPE"/"PAYPAL"/"CREDIT_CARD"
 *   5. Missing tx boundaries     - 3 writes, no @Transactional, partial failure = corruption
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    // Injected in the constructor... but also re-created inline further down. Nice.
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String PAYMENT_API = "https://api.paymentservice.com/v1/charge";

    public OrderController(JdbcTemplate jdbcTemplate, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutDto body) {
        log.info("Incoming checkout request: {}", body);

        try {
            // ---------------------------------------------------------------
            // STEP 1: request-shape validation now happens declaratively via
            // @Valid on the CheckoutDto - a bad payload never reaches this method
            // and comes back as a 400 MethodArgumentNotValidException.
            // ---------------------------------------------------------------
            String customerEmail = body.getCustomerEmail();
            String paymentMethod = body.getPaymentMethod();
            List<CheckoutItemDto> items = body.getItems();

            // ---------------------------------------------------------------
            // STEP 2: load products one-by-one, check stock, do pricing math
            // ---------------------------------------------------------------
            BigDecimal subtotal = BigDecimal.ZERO;
            List<Map<String, Object>> pricedLines = new ArrayList<>();

            for (CheckoutItemDto item : items) {
                Long productId = item.getProductId();
                int quantity = item.getQuantity();

                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, name, price, stock FROM products WHERE id = ?", productId);

                if (rows.isEmpty()) {
                    return ResponseEntity.status(404).body("Error: product not found: " + productId);
                }

                Map<String, Object> product = rows.get(0);
                int stock = ((Number) product.get("STOCK")).intValue();
                BigDecimal price = (BigDecimal) product.get("PRICE");

                if (stock < quantity) {
                    return ResponseEntity.status(422).body("Error: not enough stock for product " + productId
                            + " (have " + stock + ", want " + quantity + ")");
                }

                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
                subtotal = subtotal.add(lineTotal);

                Map<String, Object> line = new LinkedHashMap<>();
                line.put("productId", productId);
                line.put("name", product.get("NAME"));
                line.put("unitPrice", price);
                line.put("quantity", quantity);
                line.put("lineTotal", lineTotal);
                pricedLines.add(line);
            }

            // Business rules, inline, with magic numbers:
            // - 8% tax
            // - free shipping over 100, otherwise flat 9.99
            // - 5% discount if the customer used a coupon that equals "SAVE5"
            BigDecimal tax = subtotal.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal shipping = subtotal.compareTo(new BigDecimal("100")) >= 0
                    ? BigDecimal.ZERO
                    : new BigDecimal("9.99");
            BigDecimal discount = BigDecimal.ZERO;
            if ("SAVE5".equals(body.getCoupon())) {
                discount = subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal total = subtotal.add(tax).add(shipping).subtract(discount).setScale(2, RoundingMode.HALF_UP);

            log.info("Pricing for {}: subtotal={}, tax={}, shipping={}, discount={}, total={}",
                    customerEmail, subtotal, tax, shipping, discount, total);

            // ---------------------------------------------------------------
            // STEP 3: charge the customer - giant switch, inline HTTP
            // ---------------------------------------------------------------
            String paymentReference;

            switch (paymentMethod) {
                case "STRIPE": {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("provider", "stripe");
                    payload.put("amount", total.multiply(new BigDecimal("100")).intValue()); // cents
                    payload.put("currency", "usd");
                    payload.put("customer", customerEmail);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("Authorization", "Bearer sk_live_HARDCODED_STRIPE_KEY");

                    try {
                        ResponseEntity<JsonNode> resp = restTemplate.exchange(
                                PAYMENT_API, org.springframework.http.HttpMethod.POST,
                                new HttpEntity<>(payload, headers), JsonNode.class);
                        JsonNode json = resp.getBody();
                        if (json != null && json.has("id")) {
                            paymentReference = json.get("id").asText();
                        } else {
                            paymentReference = "stripe_" + System.currentTimeMillis();
                        }
                    } catch (Exception ex) {
                        // swallow and fake it so the demo keeps working
                        log.warn("Stripe call failed, using fake reference: {}", ex.getMessage());
                        paymentReference = "stripe_fake_" + System.currentTimeMillis();
                    }
                    break;
                }
                case "PAYPAL": {
                    // A different, also-inline integration shape
                    RestTemplate paypalClient = new RestTemplate(); // brand new one, why not
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("intent", "CAPTURE");
                    payload.put("amount", total.toPlainString());
                    payload.put("payer_email", customerEmail);
                    try {
                        JsonNode json = paypalClient.postForObject(PAYMENT_API + "?provider=paypal", payload, JsonNode.class);
                        paymentReference = (json != null && json.has("id"))
                                ? json.get("id").asText()
                                : "pp_" + System.currentTimeMillis();
                    } catch (Exception ex) {
                        log.warn("PayPal call failed, using fake reference: {}", ex.getMessage());
                        paymentReference = "pp_fake_" + System.currentTimeMillis();
                    }
                    break;
                }
                case "CREDIT_CARD": {
                    // "Processed" entirely in-process. No validation of the card at all.
                    String cardNumber = body.getCardNumber() == null ? "" : body.getCardNumber();
                    if (cardNumber.length() < 12) {
                        return ResponseEntity.status(422).body("Error: cardNumber looks invalid");
                    }
                    log.info("Charging card ending {} for {}", cardNumber.substring(cardNumber.length() - 4), total);
                    paymentReference = "cc_" + Math.abs((customerEmail + total).hashCode());
                    break;
                }
                default:
                    // unreachable because of the validation pyramid above, but the compiler doesn't know
                    return ResponseEntity.status(400).body("Error: unsupported paymentMethod " + paymentMethod);
            }

            // ---------------------------------------------------------------
            // STEP 4: three writes, no transaction. If write #2 or #3 throws,
            // we have already taken the customer's money AND written a half order.
            // ---------------------------------------------------------------
            jdbcTemplate.update(
                    "INSERT INTO orders (customer_email, subtotal, tax, shipping, discount, total, payment_method, payment_reference, status, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    customerEmail, subtotal, tax, shipping, discount, total,
                    paymentMethod, paymentReference, "CONFIRMED", Timestamp.from(Instant.now()));

            Long orderId = jdbcTemplate.queryForObject(
                    "SELECT id FROM orders WHERE payment_reference = ? ORDER BY id DESC LIMIT 1",
                    Long.class, paymentReference);

            for (Map<String, Object> line : pricedLines) {
                jdbcTemplate.update(
                        "INSERT INTO order_items (order_id, product_id, unit_price, quantity, line_total) VALUES (?, ?, ?, ?, ?)",
                        orderId, line.get("productId"), line.get("unitPrice"), line.get("quantity"), line.get("lineTotal"));

                // decrement stock - separate statement, still no tx
                jdbcTemplate.update(
                        "UPDATE products SET stock = stock - ? WHERE id = ?",
                        line.get("quantity"), line.get("productId"));
            }

            jdbcTemplate.update(
                    "INSERT INTO audit_log (message, created_at) VALUES (?, ?)",
                    "Order " + orderId + " confirmed for " + customerEmail + " total=" + total
                            + " via " + paymentMethod + " ref=" + paymentReference,
                    Timestamp.from(Instant.now()));

            // ---------------------------------------------------------------
            // STEP 5: hand-rolled response map
            // ---------------------------------------------------------------
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("orderId", orderId);
            response.put("status", "CONFIRMED");
            response.put("customerEmail", customerEmail);
            response.put("lines", pricedLines);
            response.put("subtotal", subtotal);
            response.put("tax", tax);
            response.put("shipping", shipping);
            response.put("discount", discount);
            response.put("total", total);
            response.put("paymentMethod", paymentMethod);
            response.put("paymentReference", paymentReference);

            log.info("Checkout complete for order {}", orderId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            // The catch-all. Every failure - bad input, DB down, NPE - becomes a 500 string.
            log.error("Checkout blew up", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
