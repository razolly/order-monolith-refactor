package com.example.ordermonolith.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Row of the {@code products} table. Ids are assigned by the seed data, not the
 * database, so there is no {@code @GeneratedValue}.
 *
 * <p>Read-only from the application's point of view: instances only ever come
 * from Hibernate. Stock is never mutated through this entity &ndash; it is
 * decremented with the conditional bulk update on the repository so the
 * "check then write" race is closed.
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
public class Product {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;
}
