package com.meli.application.service;

/**
 * Dedicated exception for errors produced inside IdempotencyServiceReactive.
 */
public class IdempotencyServiceException extends RuntimeException {
  public IdempotencyServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public IdempotencyServiceException(Throwable cause) {
    super(cause);
  }
}
