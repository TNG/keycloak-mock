package com.tngtech.keycloakmock.impl.handler;

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

  private static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
  private static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
  private static final String PASSWORD_GRANT_TYPE = "password";
  private static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
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
    when(request.getFormAttribute("grant_type")).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(sessionRepository, tokenHelper);
  }

  @Test
  void missing_authorization_code_causes_error_for_type_authorization_code() {
    when(request.getFormAttribute("grant_type")).thenReturn(AUTH_CODE_GRANT_TYPE);
    when(request.getFormAttribute("code")).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(404);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void unknown_authorization_code_causes_error_for_type_authorization_code() {
    when(request.getFormAttribute("grant_type")).thenReturn(AUTH_CODE_GRANT_TYPE);
    when(request.getFormAttribute("code")).thenReturn(UNKNOWN_SESSION);
    when(sessionRepository.getSession(UNKNOWN_SESSION)).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(404);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_token_causes_error_for_type_refresh_token() {
    when(request.getFormAttribute("grant_type")).thenReturn(REFRESH_TOKEN_GRANT_TYPE);
    when(request.getFormAttribute("refresh_token")).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_authentication_causes_error_for_type_password() {
    when(request.getFormAttribute("grant_type")).thenReturn(PASSWORD_GRANT_TYPE);
    when(routingContext.user()).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_client_id_causes_error_for_type_password() {
    when(request.getFormAttribute("grant_type")).thenReturn(PASSWORD_GRANT_TYPE);
    when(routingContext.user()).thenReturn(user);
    when(user.get("client_id")).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_username_causes_error_for_type_password() {
    when(request.getFormAttribute("grant_type")).thenReturn(PASSWORD_GRANT_TYPE);
    when(routingContext.user()).thenReturn(user);
    when(user.get("client_id")).thenReturn("myclient");
    when(request.getFormAttribute("username")).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_authentication_causes_error_for_type_client_credentials() {
    when(request.getFormAttribute("grant_type")).thenReturn(CLIENT_CREDENTIALS_GRANT_TYPE);
    when(routingContext.user()).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(401);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_clientId_causes_error_for_type_client_credentials() {
    when(request.getFormAttribute("grant_type")).thenReturn(CLIENT_CREDENTIALS_GRANT_TYPE);
    when(routingContext.user()).thenReturn(user);
    when(user.get("client_id")).thenReturn(null);

    uut = new TokenRoute(sessionRepository, tokenHelper, urlConfigurationFactory);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }
}
