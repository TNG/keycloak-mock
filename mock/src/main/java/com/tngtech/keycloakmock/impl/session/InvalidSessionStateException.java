package com.tngtech.keycloakmock.impl.session;

public class InvalidSessionStateException extends RuntimeException {
  public InvalidSessionStateException(String message) {
    super(message);
  }
}
