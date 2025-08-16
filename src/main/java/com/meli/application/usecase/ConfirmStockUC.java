package com.meli.application.usecase;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Confirms previously reserved stock.
 */
@ApplicationScoped
public class ConfirmStockUC {
    private final StockRepository repository;
    private static final Logger LOG = Logger.getLogger(ConfirmStockUC.class);

    @Inject
    public ConfirmStockUC(StockRepository repository) {
        this.repository = repository;
    }

    public Uni<StockAggregate> confirm(SkuId skuId, long quantity) {
        LOG.infov("Confirming stock for {0} qty {1}", skuId, quantity);
        return repository.find(skuId)
                .flatMap(stock -> {
                    var event = stock.confirm(quantity);
                    return repository.save(stock, event);
                })
                .invoke(agg -> LOG.infov("Confirmed stock for {0}: onHand={1}, reserved={2}",
                        skuId, agg.onHand(), agg.reserved()))
                .onFailure().invoke(t -> LOG.errorf(t, "Error confirming stock for %s", skuId));
    }
}
