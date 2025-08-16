package com.meli.application.usecase;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Reserves available stock for a SKU.
 */
@ApplicationScoped
public class ReserveStockUC {
    private final StockRepository repository;
    private static final Logger LOG = Logger.getLogger(ReserveStockUC.class);

    @Inject
    public ReserveStockUC(StockRepository repository) {
        this.repository = repository;
    }

    public Uni<StockAggregate> reserve(SkuId skuId, long quantity) {
        LOG.infov("Reserving stock for {0} qty {1}", skuId, quantity);
        return repository.find(skuId)
                .flatMap(stock -> {
                    var event = stock.reserve(quantity);
                    return repository.save(stock, event);
                })
                .invoke(agg -> LOG.infov("Reserved stock for {0}: onHand={1}, reserved={2}",
                        skuId, agg.onHand(), agg.reserved()))
                .onFailure().invoke(t -> LOG.errorf(t, "Error reserving stock for %s", skuId));
    }
}
