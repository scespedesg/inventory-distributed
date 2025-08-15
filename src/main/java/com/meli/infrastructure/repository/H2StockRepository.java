package com.meli.infrastructure.repository;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * H2 implementation of the StockRepository using JPA and optimistic locking.
 */
@ApplicationScoped
public class H2StockRepository implements StockRepository {

    @Inject
    EntityManager em;

    @Override
    public Uni<StockAggregate> find(SkuId skuId) {
        return Uni.createFrom().item(() -> em.find(StockEntity.class, skuId.value()))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .onItem().ifNull().failWith(() -> new IllegalArgumentException("SKU not found"))
                .onItem().transform(StockEntity::toAggregate);
    }

    @Override
    @Transactional
    public Uni<StockAggregate> save(StockAggregate aggregate) {
        return Uni.createFrom().item(() -> {
            StockEntity merged = em.merge(StockEntity.fromAggregate(aggregate));
            // simplistic outbox entry
            em.persist(new OutboxEntity("StockUpdated", aggregate.skuId().value()));
            aggregate.incrementVersion();
            return merged.toAggregate();
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }
}
