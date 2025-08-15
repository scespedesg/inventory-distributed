package com.meli.infrastructure.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class OutboxPublisher {

  @Inject
  Mutiny.SessionFactory sessionFactory;

  @Scheduled(every = "10s")
  @Retry(maxRetries = 3)
  @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.75)
  public Uni<Void> publish() {
    return sessionFactory.withTransaction((session, tx) ->
        session.createQuery("from OutboxEntity where status='NEW'", OutboxEntity.class)
            .getResultList()
            .invoke(list -> {
              for (OutboxEntity e : list) {
                // placeholder for real publication
                e.markSent();
              }
            })
            .replaceWithVoid()
    );
  }
}
