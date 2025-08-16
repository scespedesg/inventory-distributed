package com.meli.infrastructure.adapter.in.logging;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * Simple request/response logging filter for audit purposes.
 */
@Provider
public class AuditLogFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(AuditLogFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOG.infov("Incoming {0} {1}", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        LOG.infov("Outgoing {0} {1} -> {2}",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus());
    }
}
