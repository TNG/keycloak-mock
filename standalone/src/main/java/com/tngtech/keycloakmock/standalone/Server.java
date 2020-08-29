package com.tngtech.keycloakmock.standalone;

import com.tngtech.keycloakmock.api.KeycloakMock;
import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.standalone.handler.AuthenticationRoute;
import com.tngtech.keycloakmock.standalone.handler.CommonHandler;
import com.tngtech.keycloakmock.standalone.handler.FailureHandler;
import com.tngtech.keycloakmock.standalone.handler.IframeRoute;
import com.tngtech.keycloakmock.standalone.handler.LoginRoute;
import com.tngtech.keycloakmock.standalone.handler.LogoutRoute;
import com.tngtech.keycloakmock.standalone.handler.ThirdPartyCookiesRoute;
import com.tngtech.keycloakmock.standalone.handler.TokenRoute;
import com.tngtech.keycloakmock.standalone.render.RenderHelper;
import com.tngtech.keycloakmock.standalone.token.TokenFactory;
import com.tngtech.keycloakmock.standalone.token.TokenRepository;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import javax.annotation.Nonnull;

class Server extends KeycloakMock implements TokenFactory {
  private final TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx);
  private final RenderHelper renderHelper = new RenderHelper(engine);
  private final CommonHandler commonHandler = new CommonHandler();
  private final FailureHandler failureHandler = new FailureHandler();
  private final LoginRoute loginRoute = new LoginRoute(renderHelper);
  private final TokenRepository tokenRepository = new TokenRepository();
  private final AuthenticationRoute authenticationRoute =
      new AuthenticationRoute(this, tokenRepository);
  private final TokenRoute tokenRoute = new TokenRoute(tokenRepository, renderHelper);
  private final IframeRoute iframeRoute = new IframeRoute(renderHelper);
  private final ThirdPartyCookiesRoute thirdPartyCookiesRoute =
      new ThirdPartyCookiesRoute(renderHelper);
  private final LogoutRoute logoutRoute = new LogoutRoute();

  Server(final int port, final boolean tls) {
    super(port, "master", tls);
    start();
  }

  @Override
  protected Router configureRouter() {
    Router router = super.configureRouter();
    router
        .route()
        .handler(commonHandler)
        .failureHandler(failureHandler)
        .failureHandler(ErrorHandler.create());
    router.get("/auth/realms/:realm/protocol/openid-connect/auth").handler(loginRoute);
    router.get("/authenticate").handler(authenticationRoute);
    router
        .post("/auth/realms/:realm/protocol/openid-connect/token")
        .handler(BodyHandler.create())
        .handler(tokenRoute);
    router
        .get("/auth/realms/:realm/protocol/openid-connect/login-status-iframe.html*")
        .handler(iframeRoute);
    router
        .get("/auth/realms/:realm/protocol/openid-connect/3p-cookies/*")
        .handler(thirdPartyCookiesRoute);
    router.get("/auth/realms/:realm/protocol/openid-connect/logout").handler(logoutRoute);
    return router;
  }

  @Nonnull
  @Override
  public String getToken(TokenConfig config, String hostname, String realm) {
    return getAccessTokenForHostnameAndRealm(config, hostname, realm);
  }
}
