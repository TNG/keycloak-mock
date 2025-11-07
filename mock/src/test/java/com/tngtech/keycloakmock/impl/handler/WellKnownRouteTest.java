package com.tngtech.keycloakmock.impl.handler;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.UrlConfigurationFactory;
import com.tngtech.keycloakmock.test.ConfigurationResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WellKnownRouteTest extends HandlerTestBase {
  private static final String ISSUER = "issuer";
  private static final String AUTHORIZATION_ENDPOINT = "authorizationEndpoint";
  private static final String END_SESSION_ENDPOINT = "endSessionEndpoint";
  private static final String JWKS_URI = "jwksUri";
  private static final String TOKEN_ENDPOINT = "tokenEndpoint";
  private static final String INTROSPECTION_ENDPOINT = "introspectionEndpoint";

  @Mock private UrlConfigurationFactory urlConfigurationFactory;
  @Mock private UrlConfiguration contextConfiguration;

  private WellKnownRoute wellKnownRoute;

  @BeforeEach
  void setup() {
    wellKnownRoute = new WellKnownRoute(urlConfigurationFactory);
  }

  @Test
  void well_known_configuration_is_complete() throws URISyntaxException {
    doReturn(contextConfiguration).when(urlConfigurationFactory).create(routingContext);
    doReturn(new URI(ISSUER)).when(contextConfiguration).getIssuer();
    doReturn(new URI(AUTHORIZATION_ENDPOINT)).when(contextConfiguration).getAuthorizationEndpoint();
    doReturn(new URI(END_SESSION_ENDPOINT)).when(contextConfiguration).getEndSessionEndpoint();
    doReturn(new URI(JWKS_URI)).when(contextConfiguration).getJwksUri();
    doReturn(new URI(TOKEN_ENDPOINT)).when(contextConfiguration).getTokenEndpoint();
    doReturn(new URI(INTROSPECTION_ENDPOINT))
        .when(contextConfiguration)
        .getTokenIntrospectionEndpoint();

    wellKnownRoute.handle(routingContext);

    verify(serverResponse).end(captor.capture());
    String response = captor.getValue();

    assertThatJson(response).isEqualTo(getExpectedResponse());
  }

  private ConfigurationResponse getExpectedResponse() {
    ConfigurationResponse response = new ConfigurationResponse();
    response.issuer = ISSUER;
    response.authorization_endpoint = AUTHORIZATION_ENDPOINT;
    response.token_endpoint = TOKEN_ENDPOINT;
    response.introspection_endpoint = INTROSPECTION_ENDPOINT;
    response.jwks_uri = JWKS_URI;
    response.end_session_endpoint = END_SESSION_ENDPOINT;
    response.response_types_supported =
        Arrays.asList("code", "code id_token", "id_token", "token id_token");
    response.subject_types_supported = Collections.singletonList("public");
    response.id_token_signing_alg_values_supported = Collections.singletonList("RS256");
    return response;
  }
}
