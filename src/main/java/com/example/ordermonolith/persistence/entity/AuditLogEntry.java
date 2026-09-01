package com.example.ordermonolith.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Row of {@code audit_log}. Written in the same transaction as the order so the
 * trail can never disagree with what was actually committed.
 *
 * <p>Created with {@code AuditLogEntry.builder().message(...).build()};
 * {@code createdAt} is stamped at build time.
 */
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1024)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Builder
    private AuditLogEntry(String message) {
        this.message = message;
        this.createdAt = Instant.now();
    }
}
