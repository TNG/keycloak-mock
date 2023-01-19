package com.tngtech.keycloakmock.api;

import javax.annotation.Nonnull;

/** Runtime exception which is thrown when starting or stopping the mock fails */
public class MockServerException extends RuntimeException {
  MockServerException(@Nonnull final String message, @Nonnull final Throwable cause) {
    super(message, cause);
  }
}
