package com.meli.infrastructure.config;

import com.meli.infrastructure.repository.IdempotencyKeyEntity;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Interceptor that ensures POST commands are processed once per Idempotency-Key header.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class IdempotencyInterceptor implements ContainerRequestFilter {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    @Blocking
    public void filter(ContainerRequestContext ctx) {
        if (!"POST".equals(ctx.getMethod())) {
            return;
        }
        String key = ctx.getHeaderString("Idempotency-Key");
        if (key == null) {
            return;
        }
        if (em.find(IdempotencyKeyEntity.class, key) != null) {
            ctx.abortWith(Response.status(Response.Status.CONFLICT).entity("Duplicate request").build());
        } else {
            em.persist(new IdempotencyKeyEntity(key));
        }
    }
}
