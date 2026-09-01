package com.example.ordermonolith.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate root for the {@code orders} table and its {@code order_items}.
 *
 * <p>Persisting one {@code Order} cascades its items in a single unit of work,
 * which is how the write path becomes atomic &ndash; the monolith did three
 * unrelated {@code JdbcTemplate} calls and re-queried the generated id.
 *
 * <p>Instances are created with {@code Order.builder()...build()}. The builder
 * only exposes the business fields; {@code id} is DB-assigned, and a new order
 * is always {@code CONFIRMED} with {@code createdAt} set to now.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shipping;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "payment_method", nullable = false, length = 32)
    private String paymentMethod;

    @Column(name = "payment_reference", nullable = false, length = 128)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Builder
    private Order(String customerEmail, BigDecimal subtotal, BigDecimal tax, BigDecimal shipping,
                  BigDecimal discount, BigDecimal total, String paymentMethod, String paymentReference) {
        this.customerEmail = customerEmail;
        this.subtotal = subtotal;
        this.tax = tax;
        this.shipping = shipping;
        this.discount = discount;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.status = OrderStatus.CONFIRMED;
        this.createdAt = Instant.now();
    }

    public void addItem(long productId, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
        items.add(OrderItem.builder()
                .order(this)
                .productId(productId)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .lineTotal(lineTotal)
                .build());
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
