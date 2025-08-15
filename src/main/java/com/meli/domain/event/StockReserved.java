package com.meli.domain.event;

import com.meli.domain.model.SkuId;

/**
 * Event emitted when stock is reserved.
 */
public record StockReserved(SkuId skuId, long quantity) {}
