package com.meli.application.port.in;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

/**
 * Input port for inventory command operations.
 */
public interface InventoryCommandUseCase {

    Uni<Response> reserve(String key, ReserveRequest req);
    Uni<Response> confirm(String key, ConfirmRequest req);
    Uni<Response> release(String key, ReleaseRequest req);
    Uni<Response> adjust(String key, AdjustRequest req);

    record ReserveRequest(String skuId, long quantity) {}
    record ConfirmRequest(String skuId, long quantity) {}
    record ReleaseRequest(String skuId, long quantity) {}
    record AdjustRequest(String skuId, long delta) {}
    record StockResponse(String skuId, long onHand, long reserved) {}
}
