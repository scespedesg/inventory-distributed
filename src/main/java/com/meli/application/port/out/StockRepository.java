package com.meli.application.port.out;

import com.meli.domain.model.*;
import io.smallrye.mutiny.Uni;

/**
 * Port to persist and retrieve stock aggregates.
 */
public interface StockRepository {
    Uni<StockAggregate> find(SkuId skuId);

    /**
     * Persists the aggregate and records the emitted domain event in the outbox.
     */
    Uni<StockAggregate> save(StockAggregate aggregate, Object event);
}
