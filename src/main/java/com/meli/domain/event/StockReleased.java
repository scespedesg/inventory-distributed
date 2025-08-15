package com.meli.domain.event;

import com.meli.domain.model.SkuId;

/**
 * Event emitted when reserved stock is released.
 */
public record StockReleased(SkuId skuId, long quantity) {}
