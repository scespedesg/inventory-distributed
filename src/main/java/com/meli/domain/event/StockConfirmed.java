package com.meli.domain.event;

import com.meli.domain.model.SkuId;

/**
 * Event emitted when reserved stock is confirmed.
 */
public record StockConfirmed(SkuId skuId, long quantity) {}
