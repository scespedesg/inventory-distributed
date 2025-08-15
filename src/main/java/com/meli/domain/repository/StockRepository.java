package com.meli.domain.repository;

import com.meli.domain.model.*;
import io.smallrye.mutiny.Uni;

/**
 * Port to persist and retrieve stock aggregates.
 */
public interface StockRepository {
    Uni<StockAggregate> find(SkuId skuId);
    Uni<StockAggregate> save(StockAggregate aggregate);
}
