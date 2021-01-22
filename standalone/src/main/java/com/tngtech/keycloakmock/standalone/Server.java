package com.tngtech.keycloakmock.standalone;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;

import com.tngtech.keycloakmock.api.KeycloakMock;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.standalone.handler.AuthenticationRoute;
import com.tngtech.keycloakmock.standalone.handler.CommonHandler;
import com.tngtech.keycloakmock.standalone.handler.FailureHandler;
import com.tngtech.keycloakmock.standalone.handler.IFrameRoute;
import com.tngtech.keycloakmock.standalone.handler.LoginRoute;
import com.tngtech.keycloakmock.standalone.handler.LogoutRoute;
import com.tngtech.keycloakmock.standalone.handler.ResourceFileHandler;
import com.tngtech.keycloakmock.standalone.handler.TokenRoute;
import com.tngtech.keycloakmock.standalone.helper.RedirectHelper;
import com.tngtech.keycloakmock.standalone.helper.RenderHelper;
import com.tngtech.keycloakmock.standalone.helper.TokenHelper;
import com.tngtech.keycloakmock.standalone.session.SessionRepository;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import java.util.List;
import javax.annotation.Nonnull;

class Server extends KeycloakMock {

  @Nonnull private final CommonHandler commonHandler = new CommonHandler();
  @Nonnull private final FailureHandler failureHandler = new FailureHandler();
  @Nonnull private final LoginRoute loginRoute;
  @Nonnull private final AuthenticationRoute authenticationRoute;
  @Nonnull private final TokenRoute tokenRoute;
  @Nonnull private final LogoutRoute logoutRoute;
  @Nonnull private final IFrameRoute iframeRoute = new IFrameRoute();

  @Nonnull
  private final ResourceFileHandler thirdPartyCookies1Route =
      new ResourceFileHandler("/3p-cookies-step1.html");

  @Nonnull
  private final ResourceFileHandler thirdPartyCookies2Route =
      new ResourceFileHandler("/3p-cookies-step2.html");

  @Nonnull
  private final ResourceFileHandler keycloakJsRoute = new ResourceFileHandler("/keycloak.js");

  Server(final int port, final boolean tls, @Nonnull final List<String> resourcesToMapRolesTo) {
    super(aServerConfig().withPort(port).withTls(tls).build());
    TemplateEngine engine = FreeMarkerTemplateEngine.create(vertx);
    TokenHelper tokenHelper = new TokenHelper(tokenGenerator, resourcesToMapRolesTo);
    RedirectHelper redirectHelper = new RedirectHelper(tokenHelper);
    RenderHelper renderHelper = new RenderHelper(engine);
    SessionRepository sessionRepository = new SessionRepository();
    loginRoute = new LoginRoute(sessionRepository, redirectHelper, renderHelper);
    authenticationRoute = new AuthenticationRoute(sessionRepository, redirectHelper);
    tokenRoute = new TokenRoute(sessionRepository, tokenHelper, renderHelper);
    logoutRoute = new LogoutRoute(sessionRepository, redirectHelper);
    start();
  }

  @Override
  @Nonnull
  protected Router configureRouter() {
    Router router = super.configureRouter();
    UrlConfiguration routing = urlConfiguration.forRequestContext(null, ":realm");
    router
        .route()
        .handler(commonHandler)
        .failureHandler(failureHandler)
        .failureHandler(ErrorHandler.create(vertx));
    router.get(routing.getAuthorizationEndpoint().getPath()).handler(loginRoute);
    router
        .post(routing.getAuthenticationCallbackEndpoint(":sessionId").getPath())
        .handler(BodyHandler.create())
        .handler(authenticationRoute);
    router
        .post(routing.getTokenEndpoint().getPath())
        .handler(BodyHandler.create())
        .handler(tokenRoute);
    router.get(routing.getOpenIdPath("login-status-iframe.html*").getPath()).handler(iframeRoute);
    router
        .get(routing.getOpenIdPath("3p-cookies/step1.html").getPath())
        .handler(thirdPartyCookies1Route);
    router
        .get(routing.getOpenIdPath("3p-cookies/step2.html").getPath())
        .handler(thirdPartyCookies2Route);
    router.get(routing.getEndSessionEndpoint().getPath()).handler(logoutRoute);
    router.route("/auth/js/keycloak.js").handler(keycloakJsRoute);
    return router;
  }
}
