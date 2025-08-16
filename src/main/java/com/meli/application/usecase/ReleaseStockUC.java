package com.meli.application.usecase;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Releases reserved stock back to available pool.
 */
@ApplicationScoped
public class ReleaseStockUC {
    private final StockRepository repository;

    @Inject
    public ReleaseStockUC(StockRepository repository) {
        this.repository = repository;
    }

    public Uni<StockAggregate> release(SkuId skuId, long quantity) {
        return repository.find(skuId)
                .flatMap(stock -> {
                    var event = stock.release(quantity);
                    return repository.save(stock, event);
                });
    }
}
