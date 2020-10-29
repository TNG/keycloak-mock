package com.tngtech.keycloakmock.impl.handler;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import io.vertx.core.http.HttpServerRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WellKnownRouteTest extends HandlerTestBase {
  private static final String REQUEST_HOST = "requestHost";
  private static final String REQUEST_REALM = "requestRealm";
  private static final String ISSUER = "issuer";
  private static final String AUTHORIZATION_ENDPOINT = "authorizationEndpoint";
  private static final String END_SESSION_ENDPOINT = "endSessionEndpoint";
  private static final String JWKS_URI = "jwksUri";
  private static final String TOKEN_ENDPOINT = "tokenEndpoint";

  @Mock private HttpServerRequest request;

  @Mock private UrlConfiguration urlConfiguration;

  @InjectMocks private WellKnownRoute wellKnownRoute;

  @BeforeEach
  void setup() {
    doReturn(REQUEST_HOST).when(request).getHeader("Host");
    doReturn(request).when(routingContext).request();
    doReturn(REQUEST_REALM).when(routingContext).pathParam("realm");
  }

  @Test
  void well_known_configuration_is_complete() {
    doReturn(ISSUER).when(urlConfiguration).getIssuer(REQUEST_HOST, REQUEST_REALM);
    doReturn(AUTHORIZATION_ENDPOINT).when(urlConfiguration).getAuthorizationEndpoint(REQUEST_HOST);
    doReturn(END_SESSION_ENDPOINT).when(urlConfiguration).getEndSessionEndpoint(REQUEST_HOST);
    doReturn(JWKS_URI).when(urlConfiguration).getJwksUri(REQUEST_HOST, REQUEST_REALM);
    doReturn(TOKEN_ENDPOINT).when(urlConfiguration).getTokenEndpoint(REQUEST_HOST, REQUEST_REALM);

    wellKnownRoute.handle(routingContext);

    verify(serverResponse).end(captor.capture());
    String response = captor.getValue();

    assertThatJson(response).isEqualTo(new ConfigurationResponse());
  }

  private static class ConfigurationResponse {
    public String issuer = ISSUER;
    public String authorization_endpoint = AUTHORIZATION_ENDPOINT;
    public String token_endpoint = TOKEN_ENDPOINT;
    public String jwks_uri = JWKS_URI;
    public List<String> response_types_supported =
        Arrays.asList("code", "code id_token", "id_token", "token id_token");
    public List<String> subject_types_supported = Collections.singletonList("public");
    public List<String> id_token_signing_alg_values_supported = Collections.singletonList("RS256");
    public String end_session_endpoint = END_SESSION_ENDPOINT;
  }
}
