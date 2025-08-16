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
                .flatMap(stock -> {
                    var event = stock.reserve(quantity);
                    return repository.save(stock, event);
                });
    }
}
