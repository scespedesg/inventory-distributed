package com.meli.domain.model;

import com.meli.domain.event.*;
import java.util.Objects;

/**
 * Aggregate root managing stock quantities with strong consistency.
 */
public class StockAggregate {
    private final SkuId skuId;
    private long onHand;
    private long reserved;
    private long version;

    public StockAggregate(SkuId skuId, long onHand, long reserved, long version) {
        this.skuId = Objects.requireNonNull(skuId);
        this.onHand = onHand;
        this.reserved = reserved;
        this.version = version;
    }

    public SkuId skuId() { return skuId; }
    public long onHand() { return onHand; }
    public long reserved() { return reserved; }
    public long version() { return version; }
    public long available() { return onHand - reserved; }

    public StockReserved reserve(long quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        if (available() < quantity) throw new IllegalStateException("insufficient stock");
        reserved += quantity;
        return new StockReserved(skuId, quantity);
    }

    public StockConfirmed confirm(long quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        if (reserved < quantity) throw new IllegalStateException("not enough reserved");
        reserved -= quantity;
        onHand -= quantity;
        return new StockConfirmed(skuId, quantity);
    }

    public StockReleased release(long quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        if (reserved < quantity) throw new IllegalStateException("not enough reserved");
        reserved -= quantity;
        return new StockReleased(skuId, quantity);
    }

    public StockAdjusted adjust(long delta) {
        long newOnHand = onHand + delta;
        if (newOnHand < reserved) {
            throw new IllegalStateException("onHand cannot be less than reserved");
        }
        onHand = newOnHand;
        return new StockAdjusted(skuId, delta);
    }

    public void incrementVersion() { this.version++; }
}
