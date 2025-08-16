package com.meli.application.usecase;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Releases reserved stock back to available pool.
 */
@ApplicationScoped
public class ReleaseStockUC {
    private final StockRepository repository;
    private static final Logger LOG = Logger.getLogger(ReleaseStockUC.class);

    @Inject
    public ReleaseStockUC(StockRepository repository) {
        this.repository = repository;
    }

    public Uni<StockAggregate> release(SkuId skuId, long quantity) {
        LOG.infov("Releasing stock for {0} qty {1}", skuId, quantity);
        return repository.find(skuId)
                .flatMap(stock -> {
                    var event = stock.release(quantity);
                    return repository.save(stock, event);
                })
                .invoke(agg -> LOG.infov("Released stock for {0}: onHand={1}, reserved={2}",
                        skuId, agg.onHand(), agg.reserved()))
                .onFailure().invoke(t -> LOG.errorf(t, "Error releasing stock for %s", skuId));
    }
}
