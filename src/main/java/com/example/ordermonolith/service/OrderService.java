package com.example.ordermonolith.service;

import com.example.ordermonolith.dto.CheckoutItemDto;
import com.example.ordermonolith.dto.LineItemDto;
import com.example.ordermonolith.entity.Product;
import com.example.ordermonolith.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {

    private ProductRepository productRepository;
    private ObjectMapper objectMapper;

    public List<LineItemDto> getRequestedItems(List<CheckoutItemDto> items) {
        for (CheckoutItemDto item : items) {
            Long productId = item.getProductId();
            int quantity = item.getQuantity();

            Optional<Product> productById = productRepository.findProductById(item.getProductId());

            if (productById.isEmpty()) {
                return ResponseEntity.status(404).body("Error: product not found: " + productId);
            }

            Map<String, Object> product = rows.get(0);
            int stock = productById.get().getStock();
            BigDecimal price = productById.get().getPrice();

            if (stock < quantity) {
                return ResponseEntity.status(422).body("Error: not enough stock for product " + productId
                        + " (have " + stock + ", want " + quantity + ")");
            }

            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(lineTotal);

            Map<String, Object> line = new LinkedHashMap<>();
            LineItemDto.builder().build();
            line.put("productId", productId);
            line.put("name", productById.get().getName());
            line.put("unitPrice", price);
            line.put("quantity", quantity);
            line.put("lineTotal", lineTotal);
            pricedLines.add(line);

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
        }


        return null;
    }
}
