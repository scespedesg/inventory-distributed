package com.meli.application.service;

import com.meli.application.usecase.*;
import com.meli.domain.model.SkuId;
import com.meli.domain.model.StockAggregate;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

/**
 * Application service coordinating inventory commands with idempotency and transactions.
 * Encapsulates use case execution and response mapping keeping REST resource lean.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class InventoryCommandService {

    private final IdempotencyServiceReactive idem;
    private final ReserveStockUC reserveUC;
    private final ConfirmStockUC confirmUC;
    private final ReleaseStockUC releaseUC;
    private final AdjustStockUC  adjustUC;

    @WithTransaction
    public Uni<Response> reserve(String key, ReserveRequest req) {
        String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/reserve", req);
        return Panache.getSession().flatMap(session ->
            idem.execute(key, hash,
                () -> reserveUC.reserve(new SkuId(req.skuId()), req.quantity())
                               .map(this::toCreatedResponse),
                session));
    }

    @WithTransaction
    public Uni<Response> confirm(String key, ConfirmRequest req) {
        String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/confirm", req);
        return Panache.getSession().flatMap(session ->
            idem.execute(key, hash,
                () -> confirmUC.confirm(new SkuId(req.skuId()), req.quantity())
                                .replaceWith(Response.noContent().build()),
                session));
    }

    @WithTransaction
    public Uni<Response> release(String key, ReleaseRequest req) {
        String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/release", req);
        return Panache.getSession().flatMap(session ->
            idem.execute(key, hash,
                () -> releaseUC.release(new SkuId(req.skuId()), req.quantity())
                                .replaceWith(Response.noContent().build()),
                session));
    }

    @WithTransaction
    public Uni<Response> adjust(String key, AdjustRequest req) {
        String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/adjust", req);
        return Panache.getSession().flatMap(session ->
            idem.execute(key, hash,
                () -> adjustUC.adjust(new SkuId(req.skuId()), req.delta())
                                .map(agg -> Response.ok(toResponse(agg)).build()),
                session));
    }

    private Response toCreatedResponse(StockAggregate agg) {
        return Response.status(Response.Status.CREATED).entity(toResponse(agg)).build();
    }

    private StockResponse toResponse(StockAggregate agg) {
        return new StockResponse(agg.skuId().value(), agg.onHand(), agg.reserved());
    }

    // DTOs
    public record ReserveRequest(String skuId, long quantity) {}
    public record ConfirmRequest(String skuId, long quantity) {}
    public record ReleaseRequest(String skuId, long quantity) {}
    public record AdjustRequest(String skuId, long delta) {}
    public record StockResponse(String skuId, long onHand, long reserved) {}
}

