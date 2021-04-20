package com.tngtech.keycloakmock.impl.handler;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tngtech.keycloakmock.impl.helper.TokenHelper;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenRouteTest {

  private static final String AUTH_CODE_GRANT_TYPE = "authorization_code";
  private static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
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
  void missing_authorization_code_causes_error() {
    doReturn(request).when(routingContext).request();
    doReturn(AUTH_CODE_GRANT_TYPE).when(request).getFormAttribute("grant_type");
    doReturn(null).when(request).getFormAttribute("code");

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(404);
    verifyNoMoreInteractions(tokenHelper);
  }

  @Test
  void unknown_authorization_code_causes_error() {
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
  void missing_token_causes_error() {
    doReturn(request).when(routingContext).request();
    doReturn(REFRESH_TOKEN_GRANT_TYPE).when(request).getFormAttribute("grant_type");
    doReturn(null).when(request).getFormAttribute("refresh_token");

    uut = new TokenRoute(sessionRepository, tokenHelper);

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoMoreInteractions(tokenHelper);
  }
}
