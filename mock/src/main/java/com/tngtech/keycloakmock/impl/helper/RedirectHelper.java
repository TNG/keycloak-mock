package com.tngtech.keycloakmock.impl.helper;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.session.PersistentSession;
import com.tngtech.keycloakmock.impl.session.ResponseMode;
import com.tngtech.keycloakmock.impl.session.ResponseType;
import io.vertx.core.http.Cookie;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedirectHelper {

  public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";

  private static final Logger LOG = LoggerFactory.getLogger(RedirectHelper.class);

  private static final String STATE = "state";
  private static final String SESSION_STATE = "session_state";
  private static final String CODE = "code";
  private static final String ID_TOKEN = "id_token";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String TOKEN_TYPE = "token_type";

  private static final String OOB_REDIRECT = "urn:ietf:wg:oauth:2.0:oob";

  private static final String DUMMY_USER_ID = "dummy-user-id";

  @Nonnull private final TokenHelper tokenHelper;

  public RedirectHelper(@Nonnull TokenHelper tokenHelper) {
    this.tokenHelper = tokenHelper;
  }

  @Nullable
  public String getRedirectLocation(
      @Nonnull final PersistentSession session,
      @Nonnull final UrlConfiguration requestConfiguration) {
    ResponseType responseType = ResponseType.fromValueOrNull(session.getResponseType());
    if (responseType == null) {
      LOG.warn("Invalid response type '{}' requested!", session.getResponseType());
      return null;
    }

    ResponseMode responseMode = responseType.getValidResponseMode(session.getResponseMode());
    String originalRedirectUri = session.getRedirectUri();
    StringBuilder redirectUri;
    if (OOB_REDIRECT.equals(originalRedirectUri)) {
      redirectUri =
          new StringBuilder(requestConfiguration.getOutOfBandLoginLoginEndpoint().toASCIIString());
    } else {
      redirectUri = new StringBuilder(originalRedirectUri);
    }
    redirectUri.append(getResponseParameter(responseMode, SESSION_STATE, session.getSessionId()));
    redirectUri.append(getResponseParameter(null, STATE, session.getState()));
    String token = tokenHelper.getToken(session, requestConfiguration);
    if (token == null) {
      LOG.warn("No token available for session {}", session.getSessionId());
      return null;
    }
    switch (responseType) {
      case CODE:
        // for simplicity, use session ID as authorization code
        redirectUri.append(getResponseParameter(null, CODE, session.getSessionId()));
        break;
      case ID_TOKEN:
        redirectUri.append(getResponseParameter(null, ID_TOKEN, token));
        break;
      case ID_TOKEN_PLUS_TOKEN:
        redirectUri.append(getResponseParameter(null, ID_TOKEN, token));
        redirectUri.append(getResponseParameter(null, ACCESS_TOKEN, token));
        redirectUri.append(getResponseParameter(null, TOKEN_TYPE, "bearer"));
        break;
      case NONE:
      default:
        break;
    }
    return redirectUri.toString();
  }

  @Nonnull
  public Cookie getSessionCookie(PersistentSession session, UrlConfiguration requestConfiguration) {
    return Cookie.cookie(
            KEYCLOAK_SESSION_COOKIE,
            String.join(
                "/", requestConfiguration.getRealm(), DUMMY_USER_ID, session.getSessionId()))
        .setPath(requestConfiguration.getIssuerPath().getPath())
        .setMaxAge(36000)
        .setSecure(false);
  }

  @Nonnull
  public Cookie invalidateSessionCookie(UrlConfiguration requestConfiguration) {
    return Cookie.cookie(
            KEYCLOAK_SESSION_COOKIE,
            String.join("/", requestConfiguration.getRealm(), DUMMY_USER_ID))
        .setPath(requestConfiguration.getIssuerPath().getPath())
        .setMaxAge(0)
        .setSecure(false);
  }

  private String getResponseParameter(
      @Nullable final ResponseMode responseMode,
      @Nonnull final String name,
      @Nullable final String value) {
    if (value == null) {
      return "";
    }
    return (responseMode != null ? responseMode.getSign() : "&") + name + "=" + value;
  }
}
