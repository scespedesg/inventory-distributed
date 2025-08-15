package com.meli.infrastructure.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import io.smallrye.faulttolerance.api.CircuitBreaker;
import io.smallrye.faulttolerance.api.Retry;
import io.quarkus.scheduler.Scheduled;
import java.util.List;

/**
 * Periodically publishes outbox events applying retry and circuit breaker.
 */
@ApplicationScoped
public class OutboxPublisher {

    @Inject
    EntityManager em;

    @Scheduled(every = "10s")
    @Retry(maxRetries = 3)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.75)
    @Transactional
    void publish() {
        List<OutboxEntity> events = em.createQuery("from OutboxEntity where status='NEW'", OutboxEntity.class)
                .getResultList();
        for (OutboxEntity e : events) {
            // placeholder for real publication
            e.markSent();
        }
    }
}
