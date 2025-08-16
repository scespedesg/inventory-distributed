package com.meli.infrastructure.adapter.out.persistence;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import io.smallrye.mutiny.Uni;
import java.time.Instant;

@ApplicationScoped
public class IdempotencyCleanupScheduler {

  @Inject
  Mutiny.SessionFactory sessionFactory;

  @Scheduled(every = "1h")
  public Uni<Void> purgeExpired() {
    return sessionFactory.withTransaction((session, tx) ->
        session.createQuery("delete from IdempotencyKeyEntity where expiresAt < :now")
            .setParameter("now", Instant.now())
            .executeUpdate()
            .replaceWithVoid()
    );
  }
}
