package com.example.ordermonolith.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * API representation of an {@code audit_log} row.
 */
@Data
@Builder
public class AuditLogDto {

    private Long id;
    private String message;
    private Instant createdAt;
}
