package com.tngtech.keycloakmock.impl.dagger;

import com.tngtech.keycloakmock.api.LoginRoleMapping;
import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.handler.AuthenticationRoute;
import com.tngtech.keycloakmock.impl.handler.CommonHandler;
import com.tngtech.keycloakmock.impl.handler.DocumentationRoute;
import com.tngtech.keycloakmock.impl.handler.FailureHandler;
import com.tngtech.keycloakmock.impl.handler.IFrameRoute;
import com.tngtech.keycloakmock.impl.handler.JwksRoute;
import com.tngtech.keycloakmock.impl.handler.LoginRoute;
import com.tngtech.keycloakmock.impl.handler.LogoutRoute;
import com.tngtech.keycloakmock.impl.handler.OptionalBasicAuthHandler;
import com.tngtech.keycloakmock.impl.handler.OutOfBandLoginRoute;
import com.tngtech.keycloakmock.impl.handler.ResourceFileHandler;
import com.tngtech.keycloakmock.impl.handler.TokenRoute;
import com.tngtech.keycloakmock.impl.handler.WellKnownRoute;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Module
public class ServerModule {
  private static final Logger LOG = LoggerFactory.getLogger(ServerModule.class);

  @Provides
  @Singleton
  TemplateEngine provideTemplateEngine(@Nonnull Vertx vertx) {
    return FreeMarkerTemplateEngine.create(vertx);
  }

  @Provides
  @Singleton
  @Named("webCryptoShimJs")
  ResourceFileHandler provideWebCryptoShimJsHandler() {
    return new ResourceFileHandler(
        "/theme/keycloak/common/resources/vendor/web-crypto-shim/web-crypto-shim.js");
  }

  @Provides
  @Singleton
  @Named("cookie1")
  ResourceFileHandler provideCookie1Handler() {
    return new ResourceFileHandler("/org/keycloak/protocol/oidc/endpoints/3p-cookies-step1.html");
  }

  @Provides
  @Singleton
  @Named("cookie2")
  ResourceFileHandler provideCookie2Handler() {
    return new ResourceFileHandler("/org/keycloak/protocol/oidc/endpoints/3p-cookies-step2.html");
  }

  @Provides
  @Singleton
  @Named("keycloakJs")
  ResourceFileHandler provideKeycloakJsHandler() {
    return new ResourceFileHandler("/package/dist/keycloak.js");
  }

  @Provides
  @Singleton
  @Named("stylesheet")
  ResourceFileHandler provideStylesheetHandler() {
    return new ResourceFileHandler("/style.css");
  }

  @Provides
  @Singleton
  Buffer keystoreBuffer(@Nonnull KeyStore keyStore) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      keyStore.store(outputStream, new char[0]);
      return Buffer.buffer(outputStream.toByteArray());
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new IllegalStateException("Unable to prepare keystore for TLS", e);
    }
  }

  @Provides
  @Singleton
  HttpServerOptions provideHttpServerOptions(
      @Nonnull UrlConfiguration defaultConfiguration, @Nonnull Lazy<Buffer> keyStoreBuffer) {
    HttpServerOptions options = new HttpServerOptions().setPort(defaultConfiguration.getPort());
    if (defaultConfiguration.getProtocol().isTls()) {
      options
          .setSsl(true)
          .setKeyCertOptions(new JksOptions().setValue(keyStoreBuffer.get()).setPassword(""));
    }
    return options;
  }

  @Provides
  @Singleton
  @SuppressWarnings("java:S107")
  Router provideRouter(
      @Nonnull UrlConfiguration defaultConfiguration,
      @Nonnull Vertx vertx,
      @Nonnull CommonHandler commonHandler,
      @Nonnull FailureHandler failureHandler,
      @Nonnull JwksRoute jwksRoute,
      @Nonnull WellKnownRoute wellKnownRoute,
      @Nonnull LoginRoute loginRoute,
      @Nonnull AuthenticationRoute authenticationRoute,
      @Nonnull OptionalBasicAuthHandler basicAuthHandler,
      @Nonnull TokenRoute tokenRoute,
      @Nonnull IFrameRoute iframeRoute,
      @Nonnull @Named("webCryptoShimJs") ResourceFileHandler webCryptoShimJsHandler,
      @Nonnull @Named("cookie1") ResourceFileHandler thirdPartyCookies1Route,
      @Nonnull @Named("cookie2") ResourceFileHandler thirdPartyCookies2Route,
      @Nonnull LogoutRoute logoutRoute,
      @Nonnull OutOfBandLoginRoute outOfBandLoginRoute,
      @Nonnull @Named("keycloakJs") ResourceFileHandler keycloakJsRoute,
      @Nonnull @Named("stylesheet") ResourceFileHandler stylesheetRoute,
      @Nonnull DocumentationRoute documentationRoute) {
    UrlConfiguration routing = defaultConfiguration.forRequestContext(null, ":realm");
    Router router = Router.router(vertx);
    router
        .route()
        .handler(commonHandler)
        .failureHandler(failureHandler)
        .failureHandler(ErrorHandler.create(vertx));
    router.get(routing.getJwksUri().getPath()).setName("key signing data").handler(jwksRoute);
    router
        .get(routing.getIssuerPath().resolve(".well-known/*").getPath())
        .setName("configuration discovery data")
        .handler(wellKnownRoute);
    router
        .get(routing.getAuthorizationEndpoint().getPath())
        .setName("login page")
        .handler(loginRoute);
    router
        .post(routing.getAuthenticationCallbackEndpoint(":sessionId").getPath())
        .setName("custom authentication endpoint used by login page")
        .handler(BodyHandler.create())
        .handler(authenticationRoute);
    router
        .post(routing.getTokenEndpoint().getPath())
        .setName("token endpoint")
        .handler(BodyHandler.create())
        .handler(basicAuthHandler)
        .handler(tokenRoute);
    router
        .get(routing.getOpenIdPath("login-status-iframe.html*").getPath())
        .setName("Keycloak login iframe")
        .handler(iframeRoute);
    router
        .get(IFrameRoute.getWebCryptoShimPath(routing).getPath())
        .setName("provided web-crypto-shim.js")
        .handler(webCryptoShimJsHandler);
    router
        .get(routing.getOpenIdPath("3p-cookies/step1.html").getPath())
        .setName("keycloak third party cookies - step 1")
        .handler(thirdPartyCookies1Route);
    router
        .get(routing.getOpenIdPath("3p-cookies/step2.html").getPath())
        .setName("Keycloak third party cookies - step 2")
        .handler(thirdPartyCookies2Route);
    router
        .route(routing.getEndSessionEndpoint().getPath())
        .setName("logout endpoint")
        .method(HttpMethod.GET)
        .method(HttpMethod.POST)
        .handler(logoutRoute);
    router
        .get(routing.getOutOfBandLoginLoginEndpoint().getPath())
        .setName("out-of-band login endpoint")
        .handler(outOfBandLoginRoute);
    router
        .get(routing.getJsPath().resolve("keycloak.js").getPath())
        .setName("provided keycloak.js")
        .handler(keycloakJsRoute);
    router.get("/style.css").handler(stylesheetRoute);
    router
        .get("/docs")
        .setName("documentation endpoint")
        .produces("text/html")
        .handler(documentationRoute);
    return router;
  }

  @Provides
  @Singleton
  HttpServer provideServer(
      @Nonnull Vertx vertx, @Nonnull HttpServerOptions options, @Nonnull Router router) {
    return vertx
        .createHttpServer(options)
        .requestHandler(router)
        .exceptionHandler(t -> LOG.error("Exception while processing request", t));
  }

  @Provides
  @Singleton
  LoginRoleMapping provideLoginRoleMapping(@Nonnull ServerConfig serverConfig) {
    return serverConfig.getLoginRoleMapping();
  }

  @Provides
  @Singleton
  @Named("audiences")
  Collection<String> provideDefaultAudiences(@Nonnull ServerConfig serverConfig) {
    return serverConfig.getDefaultAudiences();
  }
}
