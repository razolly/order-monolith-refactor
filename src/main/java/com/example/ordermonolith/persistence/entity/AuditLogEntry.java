package com.example.ordermonolith.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Row of {@code audit_log}. Written in the same transaction as the order so the
 * trail can never disagree with what was actually committed.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1024)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLogEntry() {
        // required by JPA
    }

    private AuditLogEntry(String message, Instant createdAt) {
        this.message = message;
        this.createdAt = createdAt;
    }

    public static AuditLogEntry now(String message) {
        return new AuditLogEntry(message, Instant.now());
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
