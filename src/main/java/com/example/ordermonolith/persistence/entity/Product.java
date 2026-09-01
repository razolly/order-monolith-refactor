package com.example.ordermonolith.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Row of the {@code products} table. Ids are assigned by the seed data, not the
 * database, so there is no {@code @GeneratedValue}.
 *
 * <p>Stock is never mutated through this entity; it is decremented with the
 * conditional bulk update on the repository so the "check then write" race is
 * closed. Kept as a read model here.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    protected Product() {
        // required by JPA
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }
}
