package com.meli.infrastructure.adapter.in.rest;

import com.meli.application.port.in.InventoryCommandUseCase;
import com.meli.application.port.in.InventoryCommandUseCase.AdjustRequest;
import com.meli.application.port.in.InventoryCommandUseCase.ConfirmRequest;
import com.meli.application.port.in.InventoryCommandUseCase.ReleaseRequest;
import com.meli.application.port.in.InventoryCommandUseCase.ReserveRequest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Path("/v1/inventory")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class InventoryCommandResource {

  private final InventoryCommandUseCase service;

  @POST
  @Path("/reserve")
  public Uni<Response> reserve(@HeaderParam("Idempotency-Key") String key, ReserveRequest req) {
    ensureKey(key);
    return service.reserve(key, req);
  }

  @POST
  @Path("/confirm")
  public Uni<Response> confirm(@HeaderParam("Idempotency-Key") String key, ConfirmRequest req) {
    ensureKey(key);
    return service.confirm(key, req);
  }

  @POST
  @Path("/release")
  public Uni<Response> release(@HeaderParam("Idempotency-Key") String key, ReleaseRequest req) {
    ensureKey(key);
    return service.release(key, req);
  }

  @POST
  @Path("/adjust")
  public Uni<Response> adjust(@HeaderParam("Idempotency-Key") String key, AdjustRequest req) {
    ensureKey(key);
    return service.adjust(key, req);
  }

  private static void ensureKey(String key) {
    if (key == null || key.isBlank()) {
      throw new WebApplicationException("Idempotency-Key required", 400);
    }
  }
}
