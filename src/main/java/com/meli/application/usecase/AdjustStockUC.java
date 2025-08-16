package com.meli.application.usecase;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adjusts on hand stock by a delta value.
 */
@ApplicationScoped
public class AdjustStockUC {
    private final StockRepository repository;

    @Inject
    public AdjustStockUC(StockRepository repository) {
        this.repository = repository;
    }

    public Uni<StockAggregate> adjust(SkuId skuId, long delta) {
        return repository.find(skuId)
                .flatMap(stock -> {
                    var event = stock.adjust(delta);
                    return repository.save(stock, event);
                });
    }
}
