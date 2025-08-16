package com.meli.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReactiveStockRepository implements StockRepository {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public Uni<StockAggregate> find(SkuId skuId) {
    return Panache.getSession()
        .flatMap(session -> session.find(StockEntity.class, skuId.value())
            .onItem().ifNull().continueWith(StockEntity.newEmpty(skuId.value()))
            .map(StockEntity::toAggregate));
  }

  @Override
  public Uni<StockAggregate> save(StockAggregate aggregate, Object event) {
    return Panache.getSession()
        .flatMap(session -> {
          StockEntity entity = StockEntity.fromAggregate(aggregate);
          return session.merge(entity)
              .chain(merged -> {
                String payload;
                try {
                  payload = MAPPER.writeValueAsString(event);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
                OutboxEntity outbox = new OutboxEntity(event.getClass().getSimpleName(), payload);
                return session.persist(outbox).replaceWith(merged.toAggregate());
              })
              .invoke(aggregate::incrementVersion);
        });
  }
}
