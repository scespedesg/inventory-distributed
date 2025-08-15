package com.meli.infrastructure.repository;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

@ApplicationScoped
public class ReactiveStockRepository implements StockRepository {

  @Override
  public Uni<StockAggregate> find(SkuId skuId) {
    return Panache.getSession()
        .flatMap(session -> session.find(StockEntity.class, skuId.value())
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("SKU not found"))
            .map(StockEntity::toAggregate));
  }

  @Override
  public Uni<StockAggregate> save(StockAggregate aggregate) {
    return Panache.getSession()
        .flatMap(session -> {
          StockEntity entity = StockEntity.fromAggregate(aggregate);
          return session.merge(entity)
              .chain(merged -> {
                OutboxEntity outbox = new OutboxEntity("StockUpdated", aggregate.skuId().value());
                return session.persist(outbox).replaceWith(merged.toAggregate());
              })
              .invoke(aggregate::incrementVersion);
        });
  }
}
