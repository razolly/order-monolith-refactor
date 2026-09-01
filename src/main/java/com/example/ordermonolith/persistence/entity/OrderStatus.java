package com.example.ordermonolith.persistence.entity;

/**
 * Lifecycle state of an {@link Order}. Only {@code CONFIRMED} is reachable today
 * (the monolith hardcoded the string {@code "CONFIRMED"}); the enum leaves room
 * for {@code CANCELLED} / {@code REFUNDED} without another string literal hunt.
 */
public enum OrderStatus {
    CONFIRMED
}
