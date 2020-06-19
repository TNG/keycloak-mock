package com.tngtech.keycloakmock.api;

public class MockServerException extends RuntimeException {
  public MockServerException(String message, Throwable cause) {
    super(message, cause);
  }
}
