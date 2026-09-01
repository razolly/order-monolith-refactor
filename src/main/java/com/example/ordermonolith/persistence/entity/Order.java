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
 */
@Entity
@Table(name = "orders")
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

    protected Order() {
        // required by JPA
    }

    private Order(String customerEmail, BigDecimal subtotal, BigDecimal tax, BigDecimal shipping,
                  BigDecimal discount, BigDecimal total, String paymentMethod, String paymentReference,
                  OrderStatus status, Instant createdAt) {
        this.customerEmail = customerEmail;
        this.subtotal = subtotal;
        this.tax = tax;
        this.shipping = shipping;
        this.discount = discount;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Creates a CONFIRMED order with {@code createdAt} set to now. */
    public static Order confirmed(String customerEmail, BigDecimal subtotal, BigDecimal tax, BigDecimal shipping,
                                  BigDecimal discount, BigDecimal total, String paymentMethod, String paymentReference) {
        return new Order(customerEmail, subtotal, tax, shipping, discount, total,
                paymentMethod, paymentReference, OrderStatus.CONFIRMED, Instant.now());
    }

    public void addItem(long productId, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
        items.add(new OrderItem(this, productId, unitPrice, quantity, lineTotal));
    }

    public Long getId() {
        return id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getShipping() {
        return shipping;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
