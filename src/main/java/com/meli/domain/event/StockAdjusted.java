package com.meli.domain.event;

import com.meli.domain.model.SkuId;

/**
 * Event emitted when on hand stock is adjusted.
 */
public record StockAdjusted(SkuId skuId, long delta) {}
