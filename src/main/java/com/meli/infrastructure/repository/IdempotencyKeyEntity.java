package com.meli.infrastructure.repository;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyEntity extends PanacheEntityBase {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  public String id; // Header: Idempotency-Key

  @Column(name = "request_hash", nullable = false, updatable = false)
  public String requestHash;

  @Column(name = "status", nullable = false)
  public String status; // PENDING | COMPLETED

  public Integer httpStatus;

  @Lob
  public String responseJson;

  @Column(nullable = false, updatable = false)
  public Instant createdAt;

  @Column(nullable = false)
  public Instant expiresAt;
}
