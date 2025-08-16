package com.meli.infrastructure.adapter.in.logging;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Logs unhandled exceptions before letting JAX-RS generate a response.
 */
@Provider
public class AuditExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(AuditExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException wae) {
            LOG.errorf(wae, "HTTP %s error", wae.getResponse().getStatus());
            return wae.getResponse();
        }
        LOG.error("Unhandled error", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Internal Server Error")
                .build();
    }
}
