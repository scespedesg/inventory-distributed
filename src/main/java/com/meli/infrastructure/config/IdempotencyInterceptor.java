package com.meli.infrastructure.config;

import com.meli.infrastructure.repository.IdempotencyKeyEntity;
import io.quarkus.vertx.http.runtime.filters.RouteFilter;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Interceptor that ensures POST commands are processed once per Idempotency-Key header.
 */
@ApplicationScoped
public class IdempotencyInterceptor {

    @Inject
    EntityManager em;

    @RouteFilter(100)
    @Blocking
    @Transactional
    void filter(RoutingContext rc) {
        if (rc.request().method() != HttpMethod.POST) {
            return;
        }
        String key = rc.request().getHeader("Idempotency-Key");
        if (key == null) {
            return;
        }
        if (em.find(IdempotencyKeyEntity.class, key) != null) {
            rc.response().setStatusCode(409).end("Duplicate request");
        } else {
            em.persist(new IdempotencyKeyEntity(key));
        }
    }
}
