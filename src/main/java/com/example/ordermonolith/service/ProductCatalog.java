package com.example.ordermonolith.service;

import com.example.ordermonolith.error.InsufficientStockException;
import com.example.ordermonolith.error.ProductNotFoundException;
import com.example.ordermonolith.persistence.entity.Product;
import com.example.ordermonolith.persistence.repository.ProductRepository;
import com.example.ordermonolith.pricing.CartLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read side of the catalog: turns the requested {@code (productId, quantity)}
 * pairs into fully priced {@link CartLine}s, or fails fast with the right
 * exception.
 *
 * <p>Annotated {@code @Transactional(readOnly = true)} &ndash; a deliberate
 * contrast with the read-write {@link OrderWriter}. The availability check here
 * is only a fast-fail for the common case; the authoritative oversell guard is
 * the conditional {@code decrementStock} in the write transaction.
 */
@Component
@RequiredArgsConstructor
class ProductCatalog {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    List<CartLine> toCartLines(List<CheckoutCommand.Item> items) {
        return items.stream().map(this::toCartLine).toList();
    }

    private CartLine toCartLine(CheckoutCommand.Item item) {
        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new ProductNotFoundException(item.productId()));

        if (product.getStock() < item.quantity()) {
            throw new InsufficientStockException(product.getId(), product.getStock(), item.quantity());
        }

        return CartLine.builder()
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(item.quantity())
                .build();
    }
}
