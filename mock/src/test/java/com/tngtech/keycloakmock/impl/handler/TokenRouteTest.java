package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.handler.OptionalClientAuthHandler.CTX_CLIENT_ID;
import static com.tngtech.keycloakmock.impl.handler.TokenRoute.GRANT_AUTHORIZATION_CODE;
import static com.tngtech.keycloakmock.impl.handler.TokenRoute.GRANT_CLIENT_CREDENTIALS;
import static com.tngtech.keycloakmock.impl.handler.TokenRoute.GRANT_PASSWORD;
import static com.tngtech.keycloakmock.impl.handler.TokenRoute.GRANT_REFRESH_TOKEN;
import static com.tngtech.keycloakmock.impl.handler.TokenRoute.TOKEN_PARAM_CODE;
import static com.tngtech.keycloakmock.impl.handler.TokenRoute.TOKEN_PARAM_GRANT_TYPE;
import static com.tngtech.keycloakmock.impl.handler.TokenRoute.TOKEN_PARAM_REFRESH_TOKEN;
import static com.tngtech.keycloakmock.impl.handler.TokenRoute.TOKEN_PARAM_USERNAME;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.tngtech.keycloakmock.impl.UrlConfigurationFactory;
import com.tngtech.keycloakmock.impl.helper.TokenHelper;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenRouteTest {

  private static final String UNKNOWN_SESSION = "unknown";

  @Mock SessionRepository sessionRepository;
  @Mock TokenHelper tokenHelper;
  @Mock UrlConfigurationFactory urlConfigurationFactory;

  @Mock RoutingContext routingContext;
  @Mock HttpServerRequest request;
  @Mock User user;

  TokenRoute uut;

  @BeforeEach
  void setup() {
    when(routingContext.request()).thenReturn(request);
  }

  @Test
  void missing_grant_type_causes_error() {
    when(request.getFormAttribute(TOKEN_PARAM_GRANT_TYPE)).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(sessionRepository, tokenHelper);
  }

  @Test
  void missing_authorization_code_causes_error_for_type_authorization_code() {
    when(request.getFormAttribute(TOKEN_PARAM_GRANT_TYPE)).thenReturn(GRANT_AUTHORIZATION_CODE);
    when(request.getFormAttribute(TOKEN_PARAM_CODE)).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(404);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void unknown_authorization_code_causes_error_for_type_authorization_code() {
    when(request.getFormAttribute(TOKEN_PARAM_GRANT_TYPE)).thenReturn(GRANT_AUTHORIZATION_CODE);
    when(request.getFormAttribute(TOKEN_PARAM_CODE)).thenReturn(UNKNOWN_SESSION);
    when(sessionRepository.getSession(UNKNOWN_SESSION)).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(404);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_token_causes_error_for_type_refresh_token() {
    when(request.getFormAttribute(TOKEN_PARAM_GRANT_TYPE)).thenReturn(GRANT_REFRESH_TOKEN);
    when(request.getFormAttribute(TOKEN_PARAM_REFRESH_TOKEN)).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_authentication_causes_error_for_type_password() {
    when(request.getFormAttribute(TOKEN_PARAM_GRANT_TYPE)).thenReturn(GRANT_PASSWORD);
    when(routingContext.user()).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_client_id_causes_error_for_type_password() {
    when(request.getFormAttribute(TOKEN_PARAM_GRANT_TYPE)).thenReturn(GRANT_PASSWORD);
    when(routingContext.user()).thenReturn(user);
    when(user.get(CTX_CLIENT_ID)).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_username_causes_error_for_type_password() {
    when(request.getFormAttribute(TOKEN_PARAM_GRANT_TYPE)).thenReturn(GRANT_PASSWORD);
    when(routingContext.user()).thenReturn(user);
    when(user.get(CTX_CLIENT_ID)).thenReturn("myclient");
    when(request.getFormAttribute(TOKEN_PARAM_USERNAME)).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_authentication_causes_error_for_type_client_credentials() {
    when(request.getFormAttribute(TOKEN_PARAM_GRANT_TYPE)).thenReturn(GRANT_CLIENT_CREDENTIALS);
    when(routingContext.user()).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(401);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_clientId_causes_error_for_type_client_credentials() {
    when(request.getFormAttribute(TOKEN_PARAM_GRANT_TYPE)).thenReturn(GRANT_CLIENT_CREDENTIALS);
    when(routingContext.user()).thenReturn(user);
    when(user.get(CTX_CLIENT_ID)).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }
}
