package com.meli.application.service;

import com.meli.application.port.in.InventoryCommandUseCase;
import com.meli.application.port.in.InventoryCommandUseCase.AdjustRequest;
import com.meli.application.port.in.InventoryCommandUseCase.ConfirmRequest;
import com.meli.application.port.in.InventoryCommandUseCase.ReleaseRequest;
import com.meli.application.port.in.InventoryCommandUseCase.ReserveRequest;
import com.meli.application.port.in.InventoryCommandUseCase.StockResponse;
import com.meli.application.usecase.*;
import com.meli.domain.model.SkuId;
import com.meli.domain.model.StockAggregate;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Application service coordinating inventory commands with idempotency and transactions.
 * Encapsulates use case execution and response mapping keeping REST resource lean.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class InventoryCommandService implements InventoryCommandUseCase {

    private final IdempotencyServiceReactive idem;
    private final ReserveStockUC reserveUC;
    private final ConfirmStockUC confirmUC;
    private final ReleaseStockUC releaseUC;
    private final AdjustStockUC  adjustUC;
    private static final Logger LOG = Logger.getLogger(InventoryCommandService.class);

    @Override
    @WithTransaction
    public Uni<Response> reserve(String key, ReserveRequest req) {
        LOG.infov("Reserve request key={0} sku={1} qty={2}", key, req.skuId(), req.quantity());
        String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/reserve", req);
        return Panache.getSession().flatMap(session ->
            idem.execute(key, hash,
                () -> reserveUC.reserve(new SkuId(req.skuId()), req.quantity())
                               .map(this::toCreatedResponse)
                               .onFailure(IllegalArgumentException.class)
                               .recoverWithItem(t -> Response.status(422).entity(
                                       new ErrorResponse("Invalid request", Map.of(
                                               "skuId", req.skuId(),
                                               "quantity", req.quantity(),
                                               "reason", t.getMessage()))).build())
                               .onFailure(IllegalStateException.class)
                               .recoverWithItem(t -> Response.status(422).entity(
                                       new ErrorResponse("Reserve failed", Map.of(
                                               "skuId", req.skuId(),
                                               "quantity", req.quantity(),
                                               "reason", t.getMessage()))).build())
                               .onFailure(OptimisticLockException.class)
                               .recoverWithItem(t -> Response.status(409).entity(
                                       new ErrorResponse("Conflict", Map.of(
                                               "skuId", req.skuId(),
                                               "quantity", req.quantity(),
                                               "reason", "Concurrent update, retry"))).build()),
                session))
            .invoke(resp -> LOG.infov("Reserve response key={0} status={1}", key, resp.getStatus()))
            .onFailure().invoke(t -> LOG.errorf(t, "Reserve failed key=%s", key));
    }

    @Override
    @WithTransaction
    public Uni<Response> confirm(String key, ConfirmRequest req) {
        LOG.infov("Confirm request key={0} sku={1} qty={2}", key, req.skuId(), req.quantity());
        String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/confirm", req);
        return Panache.getSession().flatMap(session ->
            idem.execute(key, hash,
                () -> confirmUC.confirm(new SkuId(req.skuId()), req.quantity())
                                .replaceWith(Response.noContent().build())
                                .onFailure(IllegalArgumentException.class)
                                .recoverWithItem(t -> Response.status(422).entity(
                                        new ErrorResponse("Invalid request", Map.of(
                                                "skuId", req.skuId(),
                                                "quantity", req.quantity(),
                                                "reason", t.getMessage()))).build())
                                .onFailure(IllegalStateException.class)
                                .recoverWithItem(t -> Response.status(422).entity(
                                        new ErrorResponse("Confirm failed", Map.of(
                                                "skuId", req.skuId(),
                                                "quantity", req.quantity(),
                                                "reason", t.getMessage()))).build())
                                .onFailure(OptimisticLockException.class)
                                .recoverWithItem(t -> Response.status(409).entity(
                                        new ErrorResponse("Conflict", Map.of(
                                                "skuId", req.skuId(),
                                                "quantity", req.quantity(),
                                                "reason", "Concurrent update, retry"))).build()),
                session))
            .invoke(resp -> LOG.infov("Confirm response key={0} status={1}", key, resp.getStatus()))
            .onFailure().invoke(t -> LOG.errorf(t, "Confirm failed key=%s", key));
    }

    @Override
    @WithTransaction
    public Uni<Response> release(String key, ReleaseRequest req) {
        LOG.infov("Release request key={0} sku={1} qty={2}", key, req.skuId(), req.quantity());
        String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/release", req);
        return Panache.getSession().flatMap(session ->
            idem.execute(key, hash,
                () -> releaseUC.release(new SkuId(req.skuId()), req.quantity())
                                .replaceWith(Response.noContent().build())
                                .onFailure(IllegalArgumentException.class)
                                .recoverWithItem(t -> Response.status(422).entity(
                                        new ErrorResponse("Invalid request", Map.of(
                                                "skuId", req.skuId(),
                                                "quantity", req.quantity(),
                                                "reason", t.getMessage()))).build())
                                .onFailure(IllegalStateException.class)
                                .recoverWithItem(t -> Response.status(422).entity(
                                        new ErrorResponse("Release failed", Map.of(
                                                "skuId", req.skuId(),
                                                "quantity", req.quantity(),
                                                "reason", t.getMessage()))).build())
                                .onFailure(OptimisticLockException.class)
                                .recoverWithItem(t -> Response.status(409).entity(
                                        new ErrorResponse("Conflict", Map.of(
                                                "skuId", req.skuId(),
                                                "quantity", req.quantity(),
                                                "reason", "Concurrent update, retry"))).build()),
                session))
            .invoke(resp -> LOG.infov("Release response key={0} status={1}", key, resp.getStatus()))
            .onFailure().invoke(t -> LOG.errorf(t, "Release failed key=%s", key));
    }

    @Override
    @WithTransaction
    public Uni<Response> adjust(String key, AdjustRequest req) {
        LOG.infov("Adjust request key={0} sku={1} delta={2}", key, req.skuId(), req.delta());
        String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/adjust", req);
        return Panache.getSession().flatMap(session ->
            idem.execute(key, hash,
                () -> adjustUC.adjust(new SkuId(req.skuId()), req.delta())
                                .map(agg -> Response.ok(toResponse(agg)).build())
                                .onFailure(IllegalStateException.class)
                                .recoverWithItem(t -> Response.status(422).entity(
                                        new ErrorResponse("Adjust failed", Map.of(
                                                "skuId", req.skuId(),
                                                "delta", req.delta(),
                                                "reason", t.getMessage()))).build())
                                .onFailure(OptimisticLockException.class)
                                .recoverWithItem(t -> Response.status(409).entity(
                                        new ErrorResponse("Conflict", Map.of(
                                                "skuId", req.skuId(),
                                                "delta", req.delta(),
                                                "reason", "Concurrent update, retry"))).build()),
                session))
            .invoke(resp -> LOG.infov("Adjust response key={0} status={1}", key, resp.getStatus()))
            .onFailure().invoke(t -> LOG.errorf(t, "Adjust failed key=%s", key));
    }

    private Response toCreatedResponse(StockAggregate agg) {
        return Response.status(Response.Status.CREATED).entity(toResponse(agg)).build();
    }

    private StockResponse toResponse(StockAggregate agg) {
        return new StockResponse(agg.skuId().value(), agg.onHand(), agg.reserved());
    }

    // DTOs moved to input port
}

