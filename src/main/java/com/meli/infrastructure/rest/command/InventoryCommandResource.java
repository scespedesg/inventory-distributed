package com.meli.infrastructure.rest.command;

import com.meli.application.service.IdempotencyServiceReactive;
import com.meli.application.usecase.*;
import com.meli.domain.model.SkuId;
import com.meli.domain.model.StockAggregate;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

@Path("/v1/inventory")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InventoryCommandResource {

  @Inject IdempotencyServiceReactive idem;
  @Inject ReserveStockUC reserveUC;
  @Inject ConfirmStockUC confirmUC;
  @Inject ReleaseStockUC releaseUC;
  @Inject AdjustStockUC  adjustUC;

  @POST @Path("/reserve")
  @WithTransaction
  public Uni<Response> reserve(@HeaderParam("Idempotency-Key") String key, ReserveRequest req) {
    ensureKey(key);
    String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/reserve", req);

    return Panache.getSession().flatMap(session ->
      idem.execute(key, hash,
        () -> reserveUC.reserve(new SkuId(req.skuId()), req.quantity())
                       .map(this::toCreatedResponse), // 201 + body
        session));
  }

  @POST @Path("/confirm")
  @WithTransaction
  public Uni<Response> confirm(@HeaderParam("Idempotency-Key") String key, ConfirmRequest req) {
    ensureKey(key);
    String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/confirm", req);

    return Panache.getSession().flatMap(session ->
      idem.execute(key, hash,
        () -> confirmUC.confirm(new SkuId(req.skuId()), req.quantity())
                       .replaceWith(Response.noContent().build()), // 204
        session));
  }

  @POST @Path("/release")
  @WithTransaction
  public Uni<Response> release(@HeaderParam("Idempotency-Key") String key, ReleaseRequest req) {
    ensureKey(key);
    String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/release", req);

    return Panache.getSession().flatMap(session ->
      idem.execute(key, hash,
        () -> releaseUC.release(new SkuId(req.skuId()), req.quantity())
                       .replaceWith(Response.noContent().build()),
        session));
  }

  @POST @Path("/adjust")
  @WithTransaction
  public Uni<Response> adjust(@HeaderParam("Idempotency-Key") String key, AdjustRequest req) {
    ensureKey(key);
    String hash = IdempotencyServiceReactive.hash("POST", "/v1/inventory/adjust", req);

    return Panache.getSession().flatMap(session ->
      idem.execute(key, hash,
        () -> adjustUC.adjust(new SkuId(req.skuId()), req.delta())
                      .map(agg -> Response.ok(toResponse(agg)).build()),
        session));
  }

  private static void ensureKey(String key) {
    if (key == null || key.isBlank())
      throw new WebApplicationException("Idempotency-Key required", 400);
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
