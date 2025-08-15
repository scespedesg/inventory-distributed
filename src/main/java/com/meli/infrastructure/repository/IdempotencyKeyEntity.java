package com.meli.infrastructure.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores processed idempotency keys to avoid duplicate command execution.
 */
@Entity
@Table(name = "idempotency")
public class IdempotencyKeyEntity {
    @Id
    private String id;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public IdempotencyKeyEntity() {}
    public IdempotencyKeyEntity(String id) { this.id = id; }
}
