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
  private static final String REQUEST_HOST_NO_CONTEXT_PATH = "requestHostNoContextPath";
  private static final String REQUEST_HOST_CUSTOM_CONTEXT_PATH = "requestHostCustomContextPath";
  private static final String REQUEST_REALM = "requestRealm";

  private UrlConfiguration uut;

  private static Stream<Arguments> server_config_and_expected_base_url_and_issuer() {
    return Stream.of(
        Arguments.of(aServerConfig().build(), "http://localhost:8000", "/auth/realms/master"),
        Arguments.of(
            aServerConfig()
                .withDefaultHostname(DEFAULT_HOSTNAME)
                .withDefaultRealm(DEFAULT_REALM)
                .build(),
            "http://defaultHost:8000",
            "/auth/realms/defaultRealm"),
        Arguments.of(
            aServerConfig().withPort(80).build(), "http://localhost", "/auth/realms/master"),
        Arguments.of(
            aServerConfig().withPort(443).build(), "http://localhost:443", "/auth/realms/master"),
        Arguments.of(
            aServerConfig().withTls(true).withPort(80).build(),
            "https://localhost:80",
            "/auth/realms/master"),
        Arguments.of(
            aServerConfig().withTls(true).withPort(443).build(),
            "https://localhost",
            "/auth/realms/master"),
        Arguments.of(
            aServerConfig().withNoContextPath().build(), "http://localhost:8000", "/realms/master"),
        Arguments.of(
            aServerConfig().withContextPath("auth").build(),
            "http://localhost:8000",
            "/auth/realms/master"),
        Arguments.of(
            aServerConfig().withContextPath("/auth").build(),
            "http://localhost:8000",
            "/auth/realms/master"),
        Arguments.of(
            aServerConfig().withContextPath("/context-path").build(),
            "http://localhost:8000",
            "/context-path/realms/master"),
        Arguments.of(
            aServerConfig().withContextPath("complex/context/path").build(),
            "http://localhost:8000",
            "/complex/context/path/realms/master"));
  }

  private static Stream<Arguments> request_host_and_realm_and_expected_issuer() {
    return Stream.of(
        Arguments.of(null, null, "http://localhost:8000/auth/realms/master"),
        Arguments.of(REQUEST_HOST, null, "http://requestHost/auth/realms/master"),
        Arguments.of(null, REQUEST_REALM, "http://localhost:8000/auth/realms/requestRealm"),
        Arguments.of(REQUEST_HOST, REQUEST_REALM, "http://requestHost/auth/realms/requestRealm"));
  }

  @ParameterizedTest
  @MethodSource("server_config_and_expected_base_url_and_issuer")
  void base_url_is_generated_correctly(
      ServerConfig serverConfig, String expectedBaseUrl, String expectedIssuerPath) {
    uut = new UrlConfiguration(serverConfig, null, null);

    assertThat(uut.getBaseUrl()).hasToString(expectedBaseUrl);
    assertThat(uut.getIssuer()).hasToString(expectedBaseUrl + expectedIssuerPath);
  }

  @ParameterizedTest
  @MethodSource("request_host_and_realm_and_expected_issuer")
  void context_parameters_are_used_correctly(
      String requestHost, String requestRealm, String expectedIssuer) {
    uut = new UrlConfiguration(aServerConfig().build(), requestHost, requestRealm);

    assertThat(uut.getIssuer()).hasToString(expectedIssuer);
  }

  @Test
  void context_parameters_are_used_correctly_for_server_config_with_no_context_path() {
    uut =
        new UrlConfiguration(
            aServerConfig().withNoContextPath().build(),
            REQUEST_HOST_NO_CONTEXT_PATH,
            REQUEST_REALM);

    assertThat(uut.getIssuer()).hasToString("http://requestHostNoContextPath/realms/requestRealm");
  }

  @Test
  void context_parameters_are_used_correctly_for_server_config_with_custom_context_path() {
    uut =
        new UrlConfiguration(
            aServerConfig().withContextPath("custom/context/path").build(),
            REQUEST_HOST_CUSTOM_CONTEXT_PATH,
            REQUEST_REALM);

    assertThat(uut.getIssuer())
        .hasToString("http://requestHostCustomContextPath/custom/context/path/realms/requestRealm");
  }

  @Test
  void urls_are_correct() {
    uut = new UrlConfiguration(aServerConfig().build(), null, null);

    assertThat(uut.getIssuerPath()).hasToString("http://localhost:8000/auth/realms/master/");
    assertThat(uut.getOpenIdPath("1234"))
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/1234");
    assertThat(uut.getAuthorizationEndpoint())
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/auth");
    assertThat(uut.getEndSessionEndpoint())
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/logout");
    assertThat(uut.getJwksUri())
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/certs");
    assertThat(uut.getTokenEndpoint())
        .hasToString("http://localhost:8000/auth/realms/master/protocol/openid-connect/token");
  }

  @Test
  void urls_are_correct_with_no_context_path() {
    uut = new UrlConfiguration(aServerConfig().withNoContextPath().build(), null, null);

    assertThat(uut.getIssuerPath()).hasToString("http://localhost:8000/realms/master/");
    assertThat(uut.getOpenIdPath("1234"))
        .hasToString("http://localhost:8000/realms/master/protocol/openid-connect/1234");
    assertThat(uut.getAuthorizationEndpoint())
        .hasToString("http://localhost:8000/realms/master/protocol/openid-connect/auth");
    assertThat(uut.getEndSessionEndpoint())
        .hasToString("http://localhost:8000/realms/master/protocol/openid-connect/logout");
    assertThat(uut.getJwksUri())
        .hasToString("http://localhost:8000/realms/master/protocol/openid-connect/certs");
    assertThat(uut.getTokenEndpoint())
        .hasToString("http://localhost:8000/realms/master/protocol/openid-connect/token");
  }

  @Test
  void urls_are_correct_with_custom_context_path() {
    uut =
        new UrlConfiguration(
            aServerConfig().withContextPath("/custom/context/path").build(), null, null);

    assertThat(uut.getIssuerPath())
        .hasToString("http://localhost:8000/custom/context/path/realms/master/");
    assertThat(uut.getOpenIdPath("1234"))
        .hasToString(
            "http://localhost:8000/custom/context/path/realms/master/protocol/openid-connect/1234");
    assertThat(uut.getAuthorizationEndpoint())
        .hasToString(
            "http://localhost:8000/custom/context/path/realms/master/protocol/openid-connect/auth");
    assertThat(uut.getEndSessionEndpoint())
        .hasToString(
            "http://localhost:8000/custom/context/path/realms/master/protocol/openid-connect/logout");
    assertThat(uut.getJwksUri())
        .hasToString(
            "http://localhost:8000/custom/context/path/realms/master/protocol/openid-connect/certs");
    assertThat(uut.getTokenEndpoint())
        .hasToString(
            "http://localhost:8000/custom/context/path/realms/master/protocol/openid-connect/token");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void protocol_is_correct(boolean tls) {
    uut = new UrlConfiguration(aServerConfig().withTls(tls).build(), null, null);

    assertThat(uut.getProtocol()).isEqualTo(tls ? Protocol.HTTPS : Protocol.HTTP);
  }
}
