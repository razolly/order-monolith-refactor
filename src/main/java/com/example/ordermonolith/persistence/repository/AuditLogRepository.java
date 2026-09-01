package com.example.ordermonolith.persistence.repository;

import com.example.ordermonolith.persistence.entity.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {
}
