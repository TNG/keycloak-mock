package com.tngtech.keycloakmock.examplebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfiguration {
  private final String clientId;

  @ConstructorBinding
  public KeycloakConfiguration(String clientId) {
    this.clientId = clientId;
  }

  public String getClientId() {
    return clientId;
  }
}
