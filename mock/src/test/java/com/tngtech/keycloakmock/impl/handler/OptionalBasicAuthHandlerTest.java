package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
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
class OptionalBasicAuthHandlerTest {
  final OptionalBasicAuthHandler uut = new OptionalBasicAuthHandler();

  @Mock RoutingContext routingContext;

  @Mock HttpServerRequest request;

  @BeforeEach
  void setup() {
    when(routingContext.request()).thenReturn(request);
  }

  @Test
  void allows_missing_authentication() {
    when(request.getHeader(AUTHORIZATION)).thenReturn(null);

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("username")).isNull();
    assertThat(result.result().<String>get("password")).isNull();
  }

  @Test
  void ignore_non_base64_data() {
    when(request.getHeader(AUTHORIZATION)).thenReturn("Basic but not base64");

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("username")).isNull();
    assertThat(result.result().<String>get("password")).isNull();
  }

  @Test
  void forward_username() {
    String auth = Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.UTF_8));
    when(request.getHeader(AUTHORIZATION)).thenReturn("Basic " + auth);

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("username")).isEqualTo("test");
    assertThat(result.result().<String>get("password")).isNull();
  }

  @Test
  void forward_username_and_password() {
    String auth =
        Base64.getEncoder().encodeToString("test:secret".getBytes(StandardCharsets.UTF_8));
    when(request.getHeader(AUTHORIZATION)).thenReturn("Basic " + auth);

    Future<User> result = uut.handle(routingContext);

    assertThat(result.isComplete()).isTrue();
    assertThat(result.result()).isNotNull();
    assertThat(result.result().<String>get("username")).isEqualTo("test");
    assertThat(result.result().<String>get("password")).isEqualTo("secret");
  }
}
