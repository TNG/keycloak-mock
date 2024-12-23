package com.tngtech.keycloakmock.impl.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.helper.RedirectHelper;
import com.tngtech.keycloakmock.impl.session.PersistentSession;
import com.tngtech.keycloakmock.impl.session.SessionRepository;
import com.tngtech.keycloakmock.impl.session.SessionRequest;
import com.tngtech.keycloakmock.impl.session.UserData;
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

  private static final String USERNAME = "jane.user";
  private static final String HOSTNAME = "example.com";
  private static final UserData USER = UserData.fromUsernameAndHostname(USERNAME, HOSTNAME);
  private static final String ROLES = "role1,role2,role3";
  private static final String SESSION_ID = "session123";
  private static final String REDIRECT_URI = "redirectUri";

  @Mock private SessionRepository sessionRepository;
  @Mock private RedirectHelper redirectHelper;

  @Mock private RoutingContext routingContext;
  @Mock private HttpServerRequest request;
  @Mock private HttpServerResponse response;
  @Mock private UrlConfiguration baseConfiguration;
  @Mock private UrlConfiguration contextConfiguration;
  @Mock private SessionRequest sessionRequest;
  @Mock private PersistentSession session;
  @Mock private Cookie cookie;

  @Captor private ArgumentCaptor<List<String>> rolesCaptor;

  private AuthenticationRoute uut;

  @BeforeEach
  void setup() {
    doReturn(SESSION_ID).when(routingContext).pathParam("sessionId");
  }

  @Test
  void missing_session_causes_error() {
    uut = new AuthenticationRoute(sessionRepository, redirectHelper, baseConfiguration);

    uut.handle(routingContext);

    verify(sessionRepository).getRequest(SESSION_ID);
    verify(routingContext).fail(404);
    verifyNoMoreInteractions(
        baseConfiguration, contextConfiguration, sessionRepository, redirectHelper);
  }

  @Test
  void missing_username_causes_error() {
    doReturn(sessionRequest).when(sessionRepository).getRequest(SESSION_ID);
    doReturn(request).when(routingContext).request();

    uut = new AuthenticationRoute(sessionRepository, redirectHelper, baseConfiguration);

    uut.handle(routingContext);

    verify(sessionRepository).getRequest(SESSION_ID);
    verify(routingContext).fail(400);
    verifyNoMoreInteractions(
        baseConfiguration, contextConfiguration, sessionRepository, redirectHelper);
  }

  @Test
  void correct_token_is_created() {
    setupValidRequest();
    uut = new AuthenticationRoute(sessionRepository, redirectHelper, baseConfiguration);

    uut.handle(routingContext);

    verify(sessionRepository).getRequest(SESSION_ID);
    verify(sessionRequest).toSession(eq(USER), rolesCaptor.capture());
    assertThat(rolesCaptor.getValue()).containsExactlyInAnyOrder("role1", "role2", "role3");
    verify(response).putHeader("location", REDIRECT_URI);
    verify(response).addCookie(cookie);
    verify(response).setStatusCode(302);
    verify(response).end();
    verifyNoMoreInteractions(response);
  }

  private void setupValidRequest() {
    doReturn(USERNAME).when(request).getFormAttribute("username");
    doReturn(ROLES).when(request).getFormAttribute("password");
    doReturn(request).when(routingContext).request();
    doReturn(sessionRequest).when(sessionRepository).getRequest(SESSION_ID);
    doReturn(session).when(sessionRequest).toSession(eq(USER), anyList());
    doReturn(contextConfiguration).when(baseConfiguration).forRequestContext(routingContext);
    doReturn(HOSTNAME).when(contextConfiguration).getHostname();
    doReturn(response).when(routingContext).response();
    doReturn(response).when(response).addCookie(any(Cookie.class));
    doReturn(response).when(response).putHeader(eq("location"), anyString());
    doReturn(response).when(response).setStatusCode(anyInt());
    doReturn(cookie).when(redirectHelper).getSessionCookie(session, contextConfiguration);
    doReturn(REDIRECT_URI).when(redirectHelper).getRedirectLocation(session, contextConfiguration);
  }
}
