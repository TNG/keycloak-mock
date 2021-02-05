package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TokenRoute implements Handler<RoutingContext> {
  private TokenConfig accessTokenConfig;
  private TokenConfig idTokenConfig;
  private TokenConfig refreshTokenConfig;
  private int statusCode = 200;
  private int expiresIn = 3600;
  private String errorBody;
  private final TokenGenerator tokenGenerator = new TokenGenerator();

  /**
   * Change token endpoint to return error response.
   *
   * @param statusCode Http status to be returned: Example 404.
   * @param errorBody body to be returned.
   * @return TokenRoute
   */
  public TokenRoute withErrorResponse(@Nonnull int statusCode, @Nonnull String errorBody) {
    this.statusCode = statusCode;
    this.errorBody = errorBody;
    this.accessTokenConfig = null;
    this.idTokenConfig = null;
    this.refreshTokenConfig = null;
    return this;
  }

  /**
   * Changes token endpoint to return a successful response (http status = 200) and body with
   * TokenResponse.
   *
   * @param accessTokenConfig TokenConfig to use when generate access token.
   * @param idTokenConfig TokenConfig to use when generate idToken. If null id_token will be a empty
   *     string
   * @param refreshTokenConfig TokenConfig to use when generate refresh token. If null refresh_token
   *     will be a empty string
   * @param expiresIn Token expires, in seconds. If null will be used as default 3600
   * @see TokenConfig.Builder
   * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#TokenResponse">Successful
   *     Token Response</a>
   * @return TokenRoute
   */
  public TokenRoute withOkResponse(
      @Nonnull TokenConfig accessTokenConfig,
      @Nullable TokenConfig idTokenConfig,
      @Nullable TokenConfig refreshTokenConfig,
      @Nullable Integer expiresIn) {
    this.statusCode = 200;
    this.errorBody = null;
    this.accessTokenConfig = accessTokenConfig;
    this.idTokenConfig = idTokenConfig;
    this.refreshTokenConfig = refreshTokenConfig;
    this.expiresIn = expiresIn != null ? expiresIn.intValue() : 3600;
    return this;
  }

  @Override
  public void handle(@Nonnull final RoutingContext routingContext) {

    String response = errorBody;
    if (errorBody == null) {
      JsonObject responseBody = new JsonObject();
      UrlConfiguration requestConfiguration = routingContext.get(CTX_REQUEST_CONFIGURATION);
      responseBody.put(
          "access_token",
          accessTokenConfig != null
              ? tokenGenerator.getToken(accessTokenConfig, requestConfiguration)
              : "");
      responseBody.put(
          "refresh_token",
          refreshTokenConfig != null
              ? tokenGenerator.getToken(refreshTokenConfig, requestConfiguration)
              : "");
      responseBody.put(
          "id_token",
          idTokenConfig != null
              ? tokenGenerator.getToken(idTokenConfig, requestConfiguration)
              : "");
      responseBody.put("token_type", "Bearer");
      responseBody.put("expires_in", expiresIn);
      response = responseBody.encode();
    }
    routingContext
        .response()
        .setStatusCode(statusCode)
        .putHeader("content-type", "application/json")
        .end(response);
  }


}
