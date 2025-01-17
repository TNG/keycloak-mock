package com.tngtech.keycloakmock.impl.helper;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.session.PersistentSession;
import com.tngtech.keycloakmock.impl.session.ResponseMode;
import com.tngtech.keycloakmock.impl.session.ResponseType;
import io.vertx.core.http.Cookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
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

  @Inject
  RedirectHelper(@Nonnull TokenHelper tokenHelper) {
    this.tokenHelper = tokenHelper;
  }

  @Nullable
  public String getRedirectLocation(
      @Nonnull PersistentSession session, @Nonnull UrlConfiguration requestConfiguration) {
    ResponseType responseType = ResponseType.fromValueOrNull(session.getResponseType());
    if (responseType == null) {
      LOG.warn("Invalid response type '{}' requested!", session.getResponseType());
      return null;
    }

    String originalRedirectUri = session.getRedirectUri();
    URI redirectUriBase;
    if (OOB_REDIRECT.equals(originalRedirectUri)) {
      redirectUriBase = requestConfiguration.getOutOfBandLoginLoginEndpoint();
    } else {
      try {
        redirectUriBase = new URI(originalRedirectUri);
      } catch (URISyntaxException e) {
        LOG.warn("Invalid redirect URI '{}'!", originalRedirectUri, e);
        return null;
      }
    }

    ResponseMode responseMode = responseType.getValidResponseMode(session.getResponseMode());
    try {
      switch (responseMode) {
        case FRAGMENT:
          String fragment =
              appendResponseParameters(
                  session, requestConfiguration, responseType, redirectUriBase.getFragment());
          return new URI(
                  redirectUriBase.getScheme(),
                  redirectUriBase.getUserInfo(),
                  redirectUriBase.getHost(),
                  redirectUriBase.getPort(),
                  redirectUriBase.getPath(),
                  redirectUriBase.getQuery(),
                  fragment)
              .toASCIIString();
        case QUERY:
          String query =
              appendResponseParameters(
                  session, requestConfiguration, responseType, redirectUriBase.getQuery());
          return new URI(
                  redirectUriBase.getScheme(),
                  redirectUriBase.getUserInfo(),
                  redirectUriBase.getHost(),
                  redirectUriBase.getPort(),
                  redirectUriBase.getPath(),
                  query,
                  redirectUriBase.getFragment())
              .toASCIIString();
        default:
          LOG.warn("Invalid response mode '{}' encountered!", responseMode);
          return null;
      }
    } catch (URISyntaxException e) {
      LOG.warn("Error while generating final redirect URI from '{}'!", redirectUriBase, e);
      return null;
    }
  }

  private String appendResponseParameters(
      @Nonnull PersistentSession session,
      @Nonnull UrlConfiguration requestConfiguration,
      @Nonnull ResponseType responseType,
      @Nullable String existingParameters) {
    List<String> parameters = new ArrayList<>();
    parameters.add(getResponseParameter(SESSION_STATE, session.getSessionId()));
    parameters.add(getResponseParameter(STATE, session.getState()));
    String token = tokenHelper.getToken(session, requestConfiguration);
    if (token == null) {
      LOG.warn("No token available for session {}", session.getSessionId());
      return null;
    }
    switch (responseType) {
      case CODE:
        // for simplicity, use session ID as authorization code
        parameters.add(getResponseParameter(CODE, session.getSessionId()));
        break;
      case ID_TOKEN:
        parameters.add(getResponseParameter(ID_TOKEN, token));
        break;
      case ID_TOKEN_PLUS_TOKEN:
        parameters.add(getResponseParameter(ID_TOKEN, token));
        parameters.add(getResponseParameter(ACCESS_TOKEN, token));
        parameters.add(getResponseParameter(TOKEN_TYPE, "bearer"));
        break;
      case NONE:
      default:
        break;
    }
    String parameterString =
        parameters.stream().filter(s -> !s.isEmpty()).collect(Collectors.joining("&"));
    if (parameterString.isEmpty()) {
      return existingParameters;
    }
    if (existingParameters == null || existingParameters.isEmpty()) {
      return parameterString;
    }
    return existingParameters + "&" + parameterString;
  }

  @Nonnull
  public Cookie getSessionCookie(
      @Nonnull PersistentSession session, @Nonnull UrlConfiguration requestConfiguration) {
    return Cookie.cookie(
            KEYCLOAK_SESSION_COOKIE,
            String.join(
                "/", requestConfiguration.getRealm(), DUMMY_USER_ID, session.getSessionId()))
        .setPath(requestConfiguration.getIssuerPath().getPath())
        .setMaxAge(36000)
        .setSecure(false);
  }

  @Nonnull
  public Cookie invalidateSessionCookie(@Nonnull UrlConfiguration requestConfiguration) {
    return Cookie.cookie(
            KEYCLOAK_SESSION_COOKIE,
            String.join("/", requestConfiguration.getRealm(), DUMMY_USER_ID))
        .setPath(requestConfiguration.getIssuerPath().getPath())
        .setMaxAge(0)
        .setSecure(false);
  }

  private String getResponseParameter(@Nonnull String name, @Nullable String value) {
    if (value == null) {
      return "";
    }
    return name + "=" + value;
  }
}
