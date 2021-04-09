package com.tngtech.keycloakmock.impl.session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Possible response types for an authorization request.
 *
 * @see <a href="https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html">response
 *     types</a>
 */
public enum ResponseType {
  /** The response must contain an ID token (implicit flow). */
  ID_TOKEN(ResponseMode.FRAGMENT, false),
  /** The response must contain an ID token and an access token (implicit flow). */
  ID_TOKEN_PLUS_TOKEN(ResponseMode.FRAGMENT, false),
  /** The response must contain an authorization code (authorization code flow). */
  CODE(ResponseMode.QUERY, true),
  /** The response must not contain any secrets, but should contain the server's state. */
  NONE(ResponseMode.QUERY, true);

  @Nonnull private final ResponseMode defaultMode;
  private final boolean differentModeAllowed;

  ResponseType(@Nonnull final ResponseMode defaultMode, boolean differentModeAllowed) {
    this.defaultMode = defaultMode;
    this.differentModeAllowed = differentModeAllowed;
  }

  @Nonnull
  public ResponseMode getValidResponseMode(@Nullable final String responseMode) {
    if (!differentModeAllowed) {
      return defaultMode;
    }
    ResponseMode requestedMode = ResponseMode.fromValue(responseMode);
    if (requestedMode != null) {
      return requestedMode;
    }
    return defaultMode;
  }

  @Nullable
  public static ResponseType fromValueOrNull(@Nullable final String value) {
    if (value == null) {
      return null;
    }
    switch (value) {
      case "id_token":
        return ID_TOKEN;
      case "token id_token":
      case "id_token token":
        return ID_TOKEN_PLUS_TOKEN;
      case "code":
        return CODE;
      case "none":
        return NONE;
      default:
        // invalid combinations (e.g. hybrid case) are ignored
        return null;
    }
  }

  @Override
  public String toString() {
    switch (this) {
      case ID_TOKEN:
        return "id_token";
      case ID_TOKEN_PLUS_TOKEN:
        return "id_token token";
      case CODE:
        return "code";
      case NONE:
        return "none";
      default:
        throw new IllegalStateException();
    }
  }
}
