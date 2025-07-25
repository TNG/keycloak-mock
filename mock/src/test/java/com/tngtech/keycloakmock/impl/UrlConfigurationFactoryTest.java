package com.tngtech.keycloakmock.impl;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tngtech.keycloakmock.api.ServerConfig;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UrlConfigurationFactoryTest {
  private static final String DEFAULT_HOSTNAME = "defaultHost";
  private static final String DEFAULT_REALM = "defaultRealm";
  private static final String REQUEST_HOST = "requestHost";
  private static final String REQUEST_REALM = "requestRealm";
  private static final ServerConfig SERVER_CONFIG =
      aServerConfig().withDefaultHostname(DEFAULT_HOSTNAME).withDefaultRealm(DEFAULT_REALM).build();

  private final UrlConfigurationFactory uut = new UrlConfigurationFactory(SERVER_CONFIG);

  private static Stream<Arguments> request_host_and_realm_and_expected_host_and_realm() {
    return Stream.of(
        Arguments.of(null, null, DEFAULT_HOSTNAME + ":8000", DEFAULT_REALM),
        Arguments.of(REQUEST_HOST, null, REQUEST_HOST, DEFAULT_REALM),
        Arguments.of(null, REQUEST_REALM, DEFAULT_HOSTNAME + ":8000", REQUEST_REALM),
        Arguments.of(REQUEST_HOST, REQUEST_REALM, REQUEST_HOST, REQUEST_REALM));
  }

  @ParameterizedTest
  @MethodSource("request_host_and_realm_and_expected_host_and_realm")
  void context_parameters_are_used_correctly(
      String requestHost, String requestRealm, String expectedHost, String expectedRealm) {

    UrlConfiguration result = uut.create(requestHost, requestRealm);

    assertThat(result.getHostname()).hasToString(expectedHost);
    assertThat(result.getRealm()).hasToString(expectedRealm);
  }

  @ParameterizedTest
  @MethodSource("request_host_and_realm_and_expected_host_and_realm")
  void context_parameters_are_extracted_correctly(
      String requestHost, String requestRealm, String expectedHost, String expectedRealm) {
    RoutingContext routingContext = mock();
    HttpServerRequest httpServerRequest = mock();
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(httpServerRequest.getHeader("Host")).thenReturn(requestHost);
    when(routingContext.pathParam("realm")).thenReturn(requestRealm);

    UrlConfiguration result = uut.create(routingContext);

    assertThat(result.getHostname()).hasToString(expectedHost);
    assertThat(result.getRealm()).hasToString(expectedRealm);
  }
}
