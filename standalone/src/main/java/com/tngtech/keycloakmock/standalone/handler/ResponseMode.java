package com.tngtech.keycloakmock.standalone.handler;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * The response mode through which to return the result of an authorization call.
 *
 * @see <a href="https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html">response
 *     types</a>
 */
public enum ResponseMode {
  FRAGMENT("#"),
  QUERY("?");

  private final String sign;

  ResponseMode(String sign) {
    this.sign = sign;
  }

  String getSign() {
    return sign;
  }

  @Nullable
  static ResponseMode fromValue(@Nullable final String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    return ResponseMode.valueOf(value.toUpperCase(Locale.ROOT));
  }
}
