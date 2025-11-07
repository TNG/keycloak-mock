package com.tngtech.keycloakmock.impl.handler;

import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tngtech.keycloakmock.impl.TokenGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenIntrospectionRouteTest {
  private static final String TOKEN = "token123";

  @Mock TokenGenerator tokenGenerator;

  @Mock RoutingContext routingContext;
  @Mock HttpServerRequest request;
  @Mock HttpServerResponse response;

  @Captor ArgumentCaptor<String> bodyCaptor;

  TokenIntrospectionRoute uut;

  @BeforeEach
  void setUp() {
    uut = new TokenIntrospectionRoute(tokenGenerator);

    when(routingContext.request()).thenReturn(request);
    when(request.getFormAttribute("token")).thenReturn(TOKEN);
    when(routingContext.response()).thenReturn(response);
    when(response.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(response);
    User user = User.create(JsonObject.of("client_id", "client123"));
    when(routingContext.user()).thenReturn(user);
  }

  @Test
  void happy_case() {
    Claims claims = Jwts.claims().audience().add("client123").and().add("foo", "bar").build();
    when(tokenGenerator.parseToken(TOKEN)).thenReturn(claims);

    uut.handle(routingContext);

    verify(response).end(bodyCaptor.capture());

    assertThatJson(bodyCaptor.getValue())
        .isObject()
        .containsOnly(
            entry("active", true), entry("foo", "bar"), entry("aud", singletonList("client123")));
  }

  @Test
  void exception_returns_active_false() {
    when(tokenGenerator.parseToken(TOKEN)).thenThrow(new RuntimeException("test"));

    uut.handle(routingContext);

    verify(response).end(bodyCaptor.capture());

    assertThatJson(bodyCaptor.getValue()).isObject().containsOnly(entry("active", false));
  }
}
