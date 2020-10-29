package com.tngtech.keycloakmock.impl;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.keycloakmock.api.ServerConfig;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UrlConfigurationTest {
  private static final String DEFAULT_HOSTNAME = "defaultHost";
  private static final String DEFAULT_REALM = "defaultRealm";
  private static final String REQUEST_HOST = "requestHost";
  private static final String REQUEST_REALM = "requestRealm";

  private UrlConfiguration urlConfiguration;

  private static Stream<Arguments> server_config_and_request_host_and_expected_base_url() {
    return Stream.of(
        Arguments.of(aServerConfig().build(), null, "http://localhost:8000"),
        Arguments.of(
            aServerConfig().withHostname(DEFAULT_HOSTNAME).build(),
            null,
            "http://defaultHost:8000"),
        Arguments.of(aServerConfig().withPort(80).build(), null, "http://localhost"),
        Arguments.of(aServerConfig().withPort(443).build(), null, "http://localhost:443"),
        Arguments.of(
            aServerConfig().withTls(true).withPort(80).build(), null, "https://localhost:80"),
        Arguments.of(
            aServerConfig().withTls(true).withPort(443).build(), null, "https://localhost"),
        Arguments.of(
            aServerConfig().withHostname(DEFAULT_HOSTNAME).build(),
            REQUEST_HOST,
            "http://requestHost"),
        Arguments.of(
            aServerConfig().withHostname(DEFAULT_HOSTNAME).withTls(true).build(),
            REQUEST_HOST,
            "https://requestHost"));
  }

  private static Stream<Arguments>
      server_config_and_request_host_and_request_realm_and_expected_issuer_url() {
    return Stream.of(
        Arguments.of(
            aServerConfig().build(), null, null, "http://localhost:8000/auth/realms/master"),
        Arguments.of(
            aServerConfig().withHostname(DEFAULT_HOSTNAME).withRealm(DEFAULT_REALM).build(),
            null,
            null,
            "http://defaultHost:8000/auth/realms/defaultRealm"),
        Arguments.of(
            aServerConfig().withPort(80).build(),
            null,
            null,
            "http://localhost/auth/realms/master"),
        Arguments.of(
            aServerConfig().withTls(true).withPort(443).build(),
            null,
            null,
            "https://localhost/auth/realms/master"),
        Arguments.of(
            aServerConfig().withHostname(DEFAULT_HOSTNAME).withRealm(DEFAULT_REALM).build(),
            REQUEST_HOST,
            null,
            "http://requestHost/auth/realms/defaultRealm"),
        Arguments.of(
            aServerConfig().withHostname(DEFAULT_HOSTNAME).withRealm(DEFAULT_REALM).build(),
            null,
            REQUEST_REALM,
            "http://defaultHost:8000/auth/realms/requestRealm"),
        Arguments.of(
            aServerConfig().withHostname(DEFAULT_HOSTNAME).withRealm(DEFAULT_REALM).build(),
            REQUEST_HOST,
            REQUEST_REALM,
            "http://requestHost/auth/realms/requestRealm"),
        Arguments.of(
            aServerConfig()
                .withHostname(DEFAULT_HOSTNAME)
                .withRealm(DEFAULT_REALM)
                .withTls(true)
                .build(),
            REQUEST_HOST,
            REQUEST_REALM,
            "https://requestHost/auth/realms/requestRealm"));
  }

  @ParameterizedTest
  @MethodSource("server_config_and_request_host_and_expected_base_url")
  void base_url_is_generated_correctly(
      ServerConfig serverConfig, String requestHost, String expected) {
    urlConfiguration = new UrlConfiguration(serverConfig);

    assertThat(urlConfiguration.getBaseUrl(requestHost)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("server_config_and_request_host_and_request_realm_and_expected_issuer_url")
  void issuer_url_is_generated_correctly(
      ServerConfig serverConfig, String requestHost, String requestRealm, String expected) {
    urlConfiguration = new UrlConfiguration(serverConfig);

    assertThat(urlConfiguration.getIssuer(requestHost, requestRealm)).isEqualTo(expected);
  }
}
