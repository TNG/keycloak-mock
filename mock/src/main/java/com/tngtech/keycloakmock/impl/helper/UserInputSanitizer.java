package com.tngtech.keycloakmock.impl.helper;

import java.util.Objects;
import javax.annotation.Nullable;

public class UserInputSanitizer {

  @Nullable private final String input;
  @Nullable private String sanitized;

  public UserInputSanitizer(@Nullable String input) {
    this.input = input;
  }

  @Nullable
  public String getSanitizedInput() {
    if (input != null && sanitized == null) {
      sanitized = input.replaceAll("[\n\r\t|]", "_");
    }
    return sanitized;
  }

  @Override
  public String toString() {
    return Objects.toString(getSanitizedInput());
  }
}
