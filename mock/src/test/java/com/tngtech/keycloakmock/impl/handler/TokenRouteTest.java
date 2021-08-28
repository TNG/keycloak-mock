package com.tngtech.keycloakmock.impl.handler;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tngtech.keycloakmock.impl.helper.TokenHelper;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
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
  public static final String UNKNOWN_SESSION = "unknown";

  @Mock private SessionRepository sessionRepository;
  @Mock private TokenHelper tokenHelper;

  @Mock private RoutingContext routingContext;
  @Mock private HttpServerRequest request;

  private TokenRoute uut;

  @Test
  void missing_grant_type_causes_error() {
    doReturn(request).when(routingContext).request();
    doReturn(null).when(request).getFormAttribute("grant_type");

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(sessionRepository, tokenHelper);
  }

  @Test
  void missing_authorization_code_causes_error_for_type_authorization_code() {
    doReturn(request).when(routingContext).request();
    doReturn(AUTH_CODE_GRANT_TYPE).when(request).getFormAttribute("grant_type");
    doReturn(null).when(request).getFormAttribute("code");

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(404);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void unknown_authorization_code_causes_error_for_type_authorization_code() {
    doReturn(request).when(routingContext).request();
    doReturn(AUTH_CODE_GRANT_TYPE).when(request).getFormAttribute("grant_type");
    doReturn(UNKNOWN_SESSION).when(request).getFormAttribute("code");
    doReturn(null).when(sessionRepository).getSession(UNKNOWN_SESSION);

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(404);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_token_causes_error_for_type_refresh_token() {
    doReturn(request).when(routingContext).request();
    doReturn(REFRESH_TOKEN_GRANT_TYPE).when(request).getFormAttribute("grant_type");
    doReturn(null).when(request).getFormAttribute("refresh_token");

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_client_id_causes_error_for_type_password() {
    doReturn(request).when(routingContext).request();
    doReturn(PASSWORD_GRANT_TYPE).when(request).getFormAttribute("grant_type");
    doReturn(null).when(request).getFormAttribute("client_id");

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_username_causes_error_for_type_password() {
    doReturn(request).when(routingContext).request();
    doReturn(PASSWORD_GRANT_TYPE).when(request).getFormAttribute("grant_type");
    doReturn("myclient").when(request).getFormAttribute("client_id");
    doReturn(null).when(request).getFormAttribute("username");

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_basic_authorization_token_causes_error_for_type_client_credentials() {
    doReturn(request).when(routingContext).request();
    doReturn(CLIENT_CREDENTIALS_GRANT_TYPE).when(request).getFormAttribute("grant_type");
    doReturn(null).when(routingContext).user();

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(401);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void missing_clientId_in_basic_authorization_token_causes_error_for_type_client_credentials() {
    doReturn(request).when(routingContext).request();
    doReturn(CLIENT_CREDENTIALS_GRANT_TYPE).when(request).getFormAttribute("grant_type");
    final User user = User.fromName("");
    doReturn(user).when(routingContext).user();

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }
}
