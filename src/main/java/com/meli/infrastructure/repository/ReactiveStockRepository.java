package com.meli.infrastructure.repository;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;

@ApplicationScoped
public class ReactiveStockRepository implements StockRepository {

  @Inject
  Mutiny.Session session;

  @Override
  public Uni<StockAggregate> find(SkuId skuId) {
    return session.find(StockEntity.class, skuId.value())
        .onItem().ifNull().failWith(() -> new IllegalArgumentException("SKU not found"))
        .map(StockEntity::toAggregate);
  }

  @Override
  public Uni<StockAggregate> save(StockAggregate aggregate) {
    StockEntity entity = StockEntity.fromAggregate(aggregate);
    return session.merge(entity)
        .chain(merged -> {
          OutboxEntity outbox = new OutboxEntity("StockUpdated", aggregate.skuId().value());
          return session.persist(outbox).replaceWith(merged.toAggregate());
        })
        .invoke(aggregate::incrementVersion);
  }
}
