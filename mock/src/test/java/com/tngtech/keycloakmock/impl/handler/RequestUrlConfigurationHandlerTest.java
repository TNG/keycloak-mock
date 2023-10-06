package com.tngtech.keycloakmock.impl.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestUrlConfigurationHandlerTest {
  private static final String REQUEST_HOST = "requestHost";
  private static final String REQUEST_REALM = "requestRealm";

  @Mock private RoutingContext routingContext;
  @Mock private HttpServerRequest httpServerRequest;
  @Mock private UrlConfiguration baseConfiguration;
  @Mock private UrlConfiguration requestConfiguration;

  @Test
  void request_configuration_is_added_correctly() {
    doReturn(REQUEST_HOST).when(httpServerRequest).getHeader("Host");
    doReturn(httpServerRequest).when(routingContext).request();
    doReturn("/auth/realms/" + REQUEST_REALM).when(routingContext).normalizedPath();
    doReturn(requestConfiguration)
        .when(baseConfiguration)
        .forRequestContext(REQUEST_HOST, REQUEST_REALM);
    RequestUrlConfigurationHandler uut = new RequestUrlConfigurationHandler(baseConfiguration);

    uut.handle(routingContext);

    verify(routingContext).put(CTX_REQUEST_CONFIGURATION, requestConfiguration);
  }
}
