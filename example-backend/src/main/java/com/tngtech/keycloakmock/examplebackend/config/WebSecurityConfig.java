package com.tngtech.keycloakmock.examplebackend.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {
  private final KeycloakConfiguration keycloakConfiguration;

  public WebSecurityConfig(KeycloakConfiguration keycloakConfiguration) {
    this.keycloakConfiguration = keycloakConfiguration;
  }

  @Bean
  public SecurityFilterChain configure(final HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorizeRequests ->
                authorizeRequests
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .requestMatchers("/**")
                    .permitAll())
        .oauth2ResourceServer(
            server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(this::convert)));
    return http.build();
  }

  // as Keycloak no longer supports their Spring Boot Starter module, we need to extract roles
  // ourselves here ...
  private AbstractAuthenticationToken convert(Jwt jwt) {
    Set<GrantedAuthority> authorities =
        Stream.concat(
                jwt
                    .<Map<String, Collection<String>>>getClaim("realm_access")
                    .getOrDefault("roles", Collections.emptyList())
                    .stream(),
                jwt
                    .<Map<String, Map<String, Collection<String>>>>getClaim("resource_access")
                    .getOrDefault(keycloakConfiguration.getClientId(), Collections.emptyMap())
                    .getOrDefault("roles", Collections.emptyList())
                    .stream())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());
    return new JwtAuthenticationToken(jwt, authorities);
  }
}
