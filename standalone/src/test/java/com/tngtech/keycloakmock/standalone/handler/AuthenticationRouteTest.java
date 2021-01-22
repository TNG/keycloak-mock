package com.tngtech.keycloakmock.standalone.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.standalone.helper.RedirectHelper;
import com.tngtech.keycloakmock.standalone.session.Session;
import com.tngtech.keycloakmock.standalone.session.SessionRepository;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationRouteTest {

  private static final String USER = "user123";
  private static final String ROLES = "role1,role2,role3";
  private static final String NONCE = "nonce123";
  private static final String SESSION_ID = "session123";
  private static final String CLIENT_ID = "client123";
  private static final String REDIRECT_URI = "redirectUri";

  @Mock private SessionRepository sessionRepository;
  @Mock private RedirectHelper redirectHelper;

  @Mock private RoutingContext routingContext;
  @Mock private HttpServerRequest request;
  @Mock private HttpServerResponse response;
  @Mock private UrlConfiguration urlConfiguration;
  @Mock private Session session;
  @Mock private Cookie cookie;

  @Captor private ArgumentCaptor<List<String>> rolesCaptor;

  private AuthenticationRoute uut;

  @BeforeEach
  void setup() {
    doReturn(SESSION_ID).when(routingContext).pathParam("sessionId");
  }

  @Test
  void missing_session_causes_error() {
    uut = new AuthenticationRoute(sessionRepository, redirectHelper);

    uut.handle(routingContext);

    verify(sessionRepository).getSession(SESSION_ID);
    verify(routingContext).fail(404);
    verifyNoMoreInteractions(urlConfiguration, sessionRepository, redirectHelper);
  }

  @Test
  void missing_username_causes_error() {
    doReturn(session).when(sessionRepository).getSession(SESSION_ID);
    doReturn(request).when(routingContext).request();

    uut = new AuthenticationRoute(sessionRepository, redirectHelper);

    uut.handle(routingContext);

    verify(sessionRepository).getSession(SESSION_ID);
    verify(routingContext).fail(400);
    verifyNoMoreInteractions(urlConfiguration, sessionRepository, redirectHelper);
  }

  @Test
  void correct_token_is_created() {
    setupValidRequest();
    uut = new AuthenticationRoute(sessionRepository, redirectHelper);

    uut.handle(routingContext);

    verify(sessionRepository).getSession(SESSION_ID);
    verify(session).setUsername(USER);
    verify(session).setRoles(rolesCaptor.capture());
    assertThat(rolesCaptor.getValue()).containsExactlyInAnyOrder("role1", "role2", "role3");
    verify(response).putHeader("location", REDIRECT_URI);
    verify(response).addCookie(cookie);
    verify(response).setStatusCode(302);
    verify(response).end();
    verifyNoMoreInteractions(response);
  }

  private void setupValidRequest() {
    doReturn(USER).when(request).getFormAttribute("username");
    doReturn(ROLES).when(request).getFormAttribute("password");
    doReturn(request).when(routingContext).request();
    doReturn(session).when(sessionRepository).getSession(SESSION_ID);
    doReturn(urlConfiguration).when(routingContext).get(CTX_REQUEST_CONFIGURATION);
    doReturn(response).when(routingContext).response();
    doReturn(response).when(response).addCookie(any(Cookie.class));
    doReturn(response).when(response).putHeader(eq("location"), anyString());
    doReturn(response).when(response).setStatusCode(anyInt());
    doReturn(cookie).when(redirectHelper).getSessionCookie(session, urlConfiguration);
    doReturn(REDIRECT_URI).when(redirectHelper).getRedirectLocation(session, urlConfiguration);
  }
}
