package com.meli.application.usecase;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Reserves available stock for a SKU.
 */
@ApplicationScoped
public class ReserveStockUC {
    private final StockRepository repository;

    @Inject
    public ReserveStockUC(StockRepository repository) {
        this.repository = repository;
    }

    public Uni<StockAggregate> reserve(SkuId skuId, long quantity) {
        return repository.find(skuId)
                .onItem().transform(stock -> {
                    stock.reserve(quantity);
                    return stock;
                })
                .flatMap(repository::save);
    }
}
