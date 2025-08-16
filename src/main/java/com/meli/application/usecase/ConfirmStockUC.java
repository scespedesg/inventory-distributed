package com.meli.application.usecase;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Confirms previously reserved stock.
 */
@ApplicationScoped
public class ConfirmStockUC {
    private final StockRepository repository;

    @Inject
    public ConfirmStockUC(StockRepository repository) {
        this.repository = repository;
    }

    public Uni<StockAggregate> confirm(SkuId skuId, long quantity) {
        return repository.find(skuId)
                .flatMap(stock -> {
                    var event = stock.confirm(quantity);
                    return repository.save(stock, event);
                });
    }
}
