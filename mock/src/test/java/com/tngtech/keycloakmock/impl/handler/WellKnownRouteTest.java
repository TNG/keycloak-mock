package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.test.ConfigurationResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WellKnownRouteTest extends HandlerTestBase {
  private static final String ISSUER = "issuer";
  private static final String AUTHORIZATION_ENDPOINT = "authorizationEndpoint";
  private static final String END_SESSION_ENDPOINT = "endSessionEndpoint";
  private static final String JWKS_URI = "jwksUri";
  private static final String TOKEN_ENDPOINT = "tokenEndpoint";

  @Mock private UrlConfiguration urlConfiguration;

  @InjectMocks private WellKnownRoute wellKnownRoute;

  @Test
  void well_known_configuration_is_complete() throws URISyntaxException {
    doReturn(urlConfiguration).when(routingContext).get(CTX_REQUEST_CONFIGURATION);
    doReturn(new URI(ISSUER)).when(urlConfiguration).getIssuer();
    doReturn(new URI(AUTHORIZATION_ENDPOINT)).when(urlConfiguration).getAuthorizationEndpoint();
    doReturn(new URI(END_SESSION_ENDPOINT)).when(urlConfiguration).getEndSessionEndpoint();
    doReturn(new URI(JWKS_URI)).when(urlConfiguration).getJwksUri();
    doReturn(new URI(TOKEN_ENDPOINT)).when(urlConfiguration).getTokenEndpoint();

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
    response.jwks_uri = JWKS_URI;
    response.end_session_endpoint = END_SESSION_ENDPOINT;
    response.response_types_supported =
        Arrays.asList("code", "code id_token", "id_token", "token id_token");
    response.subject_types_supported = Collections.singletonList("public");
    response.id_token_signing_alg_values_supported = Collections.singletonList("RS256");
    return response;
  }
}
