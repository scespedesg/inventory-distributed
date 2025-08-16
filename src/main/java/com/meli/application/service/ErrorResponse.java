package com.meli.application.service;

import java.util.Map;

/**
 * DTO for returning structured error details to API clients.
 */
public record ErrorResponse(String error, Map<String, Object> details) {}
