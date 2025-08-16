package com.meli.application.usecase;

import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Adjusts on hand stock by a delta value.
 */
@ApplicationScoped
public class AdjustStockUC {
    private final StockRepository repository;
    private static final Logger LOG = Logger.getLogger(AdjustStockUC.class);

    @Inject
    public AdjustStockUC(StockRepository repository) {
        this.repository = repository;
    }

    public Uni<StockAggregate> adjust(SkuId skuId, long delta) {
        LOG.infov("Adjusting stock for {0} by {1}", skuId, delta);
        return repository.find(skuId)
                .flatMap(stock -> {
                    var event = stock.adjust(delta);
                    return repository.save(stock, event);
                })
                .invoke(agg -> LOG.infov("Adjusted stock for {0}: onHand={1}, reserved={2}",
                        skuId, agg.onHand(), agg.reserved()))
                .onFailure().invoke(t -> LOG.errorf(t, "Error adjusting stock for %s", skuId));
    }
}
