package com.meli.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Simple outbox entity storing events for later publication.
 */
@Entity
@Table(name = "outbox")
public class OutboxEntity {
    @Id
    @GeneratedValue
    private Long id;
    @Column(name = "event_type")
    private String eventType;
    @Lob
    private String payload;
    private String status = "NEW";
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public OutboxEntity() {}
    public OutboxEntity(String eventType, String payload) {
        this.eventType = eventType;
        this.payload = payload;
    }

    public void markSent() { this.status = "SENT"; }
    public String status() { return status; }
}
