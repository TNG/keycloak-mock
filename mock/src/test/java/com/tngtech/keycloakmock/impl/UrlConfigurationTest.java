package com.tngtech.keycloakmock.impl;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.keycloakmock.api.ServerConfig;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class UrlConfigurationTest {
  private static final String DEFAULT_HOSTNAME = "defaultHost";
  private static final String DEFAULT_REALM = "defaultRealm";
  private static final String REQUEST_HOST = "requestHost";
  private static final String REQUEST_REALM = "requestRealm";

  private UrlConfiguration urlConfiguration;

  private static Stream<Arguments> server_config_and_expected_base_url() {
    return Stream.of(
        Arguments.of(aServerConfig().build(), "http://localhost:8000"),
        Arguments.of(
            aServerConfig().withDefaultHostname(DEFAULT_HOSTNAME).build(),
            "http://defaultHost:8000"),
        Arguments.of(aServerConfig().withPort(80).build(), "http://localhost"),
        Arguments.of(aServerConfig().withPort(443).build(), "http://localhost:443"),
        Arguments.of(aServerConfig().withTls(true).withPort(80).build(), "https://localhost:80"),
        Arguments.of(aServerConfig().withTls(true).withPort(443).build(), "https://localhost"));
  }

  private static Stream<Arguments> server_config_and_expected_issuer_url() {
    return Stream.of(
        Arguments.of(aServerConfig().build(), "http://localhost:8000/auth/realms/master"),
        Arguments.of(
            aServerConfig().withDefaultHostname(DEFAULT_HOSTNAME).withDefaultRealm(DEFAULT_REALM)
                .build(),
            "http://defaultHost:8000/auth/realms/defaultRealm"),
        Arguments.of(aServerConfig().withPort(80).build(), "http://localhost/auth/realms/master"),
        Arguments.of(
            aServerConfig().withTls(true).withPort(443).build(),
            "https://localhost/auth/realms/master"));
  }

  private static Stream<Arguments> request_host_and_realm_and_expected() {
    return Stream.of(
        Arguments.of(null, null, "http://localhost:8000/auth/realms/master"),
        Arguments.of(REQUEST_HOST, null, "http://requestHost/auth/realms/master"),
        Arguments.of(null, REQUEST_REALM, "http://localhost:8000/auth/realms/requestRealm"),
        Arguments.of(REQUEST_HOST, REQUEST_REALM, "http://requestHost/auth/realms/requestRealm"));
  }

  @ParameterizedTest
  @MethodSource("server_config_and_expected_base_url")
  void base_url_is_generated_correctly(ServerConfig serverConfig, String expected) {
    urlConfiguration = new UrlConfiguration(serverConfig);

    assertThat(urlConfiguration.getBaseUrl()).hasToString(expected);
  }

  @ParameterizedTest
  @MethodSource("server_config_and_expected_issuer_url")
  void issuer_url_is_generated_correctly(ServerConfig serverConfig, String expected) {
    urlConfiguration = new UrlConfiguration(serverConfig);

    assertThat(urlConfiguration.getIssuer()).hasToString(expected);
  }

  @ParameterizedTest
  @MethodSource("request_host_and_realm_and_expected")
  void context_parameters_are_used_correctly(
      String requestHost, String requestRealm, String expected) {
    urlConfiguration =
        new UrlConfiguration(aServerConfig().build()).forRequestContext(requestHost, requestRealm);

    assertThat(urlConfiguration.getIssuer()).hasToString(expected);
  }

  @Test
  void urls_are_correct() {
    urlConfiguration = new UrlConfiguration(aServerConfig().build());

    assertThat(urlConfiguration.getIssuerPath())
        .hasToString("http://localhost:8000/auth/realms/master/");
    assertThat(urlConfiguration.getOpenIdPath("1234"))
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/1234");
    assertThat(urlConfiguration.getAuthorizationEndpoint())
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/auth");
    assertThat(urlConfiguration.getEndSessionEndpoint())
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/logout");
    assertThat(urlConfiguration.getJwksUri())
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/certs");
    assertThat(urlConfiguration.getTokenEndpoint())
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/token");
  }

  @Test
  void port_is_correct() {
    urlConfiguration = new UrlConfiguration(aServerConfig().withPort(1234).build());

    assertThat(urlConfiguration.getPort()).isEqualTo(1234);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void protocol_is_correct(boolean tls) {
    urlConfiguration = new UrlConfiguration(aServerConfig().withTls(tls).build());

    assertThat(urlConfiguration.getProtocol()).isEqualTo(tls ? Protocol.HTTPS : Protocol.HTTP);
  }
}
