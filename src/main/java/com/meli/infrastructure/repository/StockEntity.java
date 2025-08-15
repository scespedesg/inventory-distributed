package com.meli.infrastructure.repository;

import com.meli.domain.model.*;
import jakarta.persistence.*;

/**
 * JPA entity backing the StockAggregate with optimistic locking.
 */
@Entity
@Table(name = "stock")
public class StockEntity {
    @Id
    @Column(name = "sku_id")
    private String skuId;
    @Column(name = "on_hand")
    private long onHand;
    @Column(name = "reserved")
    private long reserved;
    @Version
    private long version;

    public static StockEntity fromAggregate(StockAggregate agg) {
        StockEntity e = new StockEntity();
        e.skuId = agg.skuId().value();
        e.onHand = agg.onHand();
        e.reserved = agg.reserved();
        e.version = agg.version();
        return e;
    }

    public StockAggregate toAggregate() {
        return new StockAggregate(new SkuId(skuId), onHand, reserved, version);
    }
}
