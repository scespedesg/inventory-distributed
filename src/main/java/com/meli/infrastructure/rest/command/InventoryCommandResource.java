package com.meli.infrastructure.rest.command;

import com.meli.application.usecase.*;
import com.meli.domain.model.SkuId;
import com.meli.domain.model.StockAggregate;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST adapter exposing command API for stock operations.
 */
@Path("/v1/inventory")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InventoryCommandResource {

    @Inject
    ReserveStockUC reserveUC;
    @Inject
    ConfirmStockUC confirmUC;
    @Inject
    ReleaseStockUC releaseUC;
    @Inject
    AdjustStockUC adjustUC;

    @POST
    @Path("/reserve")
    public Uni<Response> reserve(ReserveRequest request) {
        return reserveUC.reserve(new SkuId(request.skuId()), request.quantity())
                .map(this::toResponse)
                .map(Response::ok)
                .map(Response.ResponseBuilder::build);
    }

    @POST
    @Path("/confirm")
    public Uni<Response> confirm(ConfirmRequest request) {
        return confirmUC.confirm(new SkuId(request.skuId()), request.quantity())
                .map(this::toResponse)
                .map(Response::ok)
                .map(Response.ResponseBuilder::build);
    }

    @POST
    @Path("/release")
    public Uni<Response> release(ReleaseRequest request) {
        return releaseUC.release(new SkuId(request.skuId()), request.quantity())
                .map(this::toResponse)
                .map(Response::ok)
                .map(Response.ResponseBuilder::build);
    }

    @POST
    @Path("/adjust")
    public Uni<Response> adjust(AdjustRequest request) {
        return adjustUC.adjust(new SkuId(request.skuId()), request.delta())
                .map(this::toResponse)
                .map(Response::ok)
                .map(Response.ResponseBuilder::build);
    }

    private StockResponse toResponse(StockAggregate agg) {
        return new StockResponse(agg.skuId().value(), agg.onHand(), agg.reserved());
    }

    public record ReserveRequest(String skuId, long quantity) {}
    public record ConfirmRequest(String skuId, long quantity) {}
    public record ReleaseRequest(String skuId, long quantity) {}
    public record AdjustRequest(String skuId, long delta) {}
    public record StockResponse(String skuId, long onHand, long reserved) {}
}
