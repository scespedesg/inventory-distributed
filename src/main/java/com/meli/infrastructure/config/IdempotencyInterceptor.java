package com.meli.infrastructure.config;

import com.meli.infrastructure.repository.IdempotencyKeyEntity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Interceptor that ensures POST commands are processed once per Idempotency-Key header.
 */
@ApplicationScoped
public class IdempotencyInterceptor {

    @Inject
    EntityManager em;

    @ServerRequestFilter
    @Transactional
    public Uni<Void> filter(ContainerRequestContext ctx) {
        if (!"POST".equals(ctx.getMethod())) {
            return Uni.createFrom().voidItem();
        }
        String key = ctx.getHeaderString("Idempotency-Key");
        if (key == null) {
            return Uni.createFrom().voidItem();
        }
        return Uni.createFrom().item(() -> {
            if (em.find(IdempotencyKeyEntity.class, key) != null) {
                ctx.abortWith(Response.status(409).entity("Duplicate request").build());
            } else {
                em.persist(new IdempotencyKeyEntity(key));
            }
            return null;
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }
}
