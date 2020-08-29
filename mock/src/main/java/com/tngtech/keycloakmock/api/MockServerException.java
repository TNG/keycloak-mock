package com.tngtech.keycloakmock.api;

import javax.annotation.Nonnull;

public class MockServerException extends RuntimeException {
  public MockServerException(@Nonnull final String message, @Nonnull final Throwable cause) {
    super(message, cause);
  }
}
