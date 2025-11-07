package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionalClientAuthHandlerTest {
  final OptionalClientAuthHandler uut = new OptionalClientAuthHandler();

  @Mock RoutingContext routingContext;

  @Mock HttpServerRequest request;

  @BeforeEach
  void setup() {
    when(routingContext.request()).thenReturn(request);
  }

  @Test
  void allow_missing_authentication() {
    when(request.getHeader(AUTHORIZATION)).thenReturn(null);
    when(request.getFormAttribute(anyString())).thenReturn(null);

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("client_id")).isNull();
    assertThat(result.result().<String>get("client_secret")).isNull();
  }

  @Test
  void ignore_basic_non_base64_data() {
    when(request.getHeader(AUTHORIZATION)).thenReturn("Basic but not base64");

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("client_id")).isNull();
    assertThat(result.result().<String>get("client_secret")).isNull();
  }

  @Test
  void forward_basic_username() {
    String auth = Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8));
    when(request.getHeader(AUTHORIZATION)).thenReturn("Basic " + auth);

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("client_id")).isEqualTo("test");
    assertThat(result.result().<String>get("client_secret")).isNull();
  }

  @Test
  void forward_basic_username_and_password() {
    String auth =
        Base64.getEncoder().encodeToString("test:secret".getBytes(StandardCharsets.UTF_8));
    when(request.getHeader(AUTHORIZATION)).thenReturn("Basic " + auth);

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("client_id")).isEqualTo("test");
    assertThat(result.result().<String>get("client_secret")).isEqualTo("secret");
  }

  @Test
  void forward_form_client_id() {
    when(request.getHeader(AUTHORIZATION)).thenReturn(null);
    when(request.getFormAttribute("client_id")).thenReturn("test");

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("client_id")).isEqualTo("test");
    assertThat(result.result().<String>get("client_secret")).isNull();
  }

  @Test
  void forward_form_client_id_and_client_secret() {
    when(request.getHeader(AUTHORIZATION)).thenReturn(null);
    when(request.getFormAttribute("client_id")).thenReturn("test");
    when(request.getFormAttribute("client_secret")).thenReturn("secret");

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("client_id")).isEqualTo("test");
    assertThat(result.result().<String>get("client_secret")).isEqualTo("secret");
  }
}
