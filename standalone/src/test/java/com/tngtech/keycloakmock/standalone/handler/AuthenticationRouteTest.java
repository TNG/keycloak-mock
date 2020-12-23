package com.tngtech.keycloakmock.standalone.handler;

import static com.tngtech.keycloakmock.impl.handler.RequestUrlConfigurationHandler.CTX_REQUEST_CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.standalone.token.TokenRepository;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationRouteTest {

  private static final String USER = "user123";
  private static final String ROLES = "role1,role2,role3";
  private static final String REALM = "realm123";
  private static final String STATE = "state123";
  private static final String NONCE = "nonce123";
  private static final String SESSION_ID = "session123";
  private static final String CLIENT_ID = "client123";
  private static final String REDIRECT_URI = "https://localhost:1234/gohere";
  private static final String TOKEN = "tokenstring";
  private static final String ISSUER_PATH = "/the/issuer/path";
  private static final URI ISSUER;

  static {
    try {
      ISSUER = new URI(ISSUER_PATH);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static final List<String> CONFIGURED_RESOURCES = Arrays.asList("resource1", "resource2");

  @Mock
  private TokenGenerator tokenGenerator;
  @Mock
  private TokenRepository tokenRepository;

  @Mock
  private RoutingContext routingContext;

  @Mock
  private UrlConfiguration urlConfiguration;
  @Mock
  private HttpServerResponse serverResponse;

  @Captor
  private ArgumentCaptor<TokenConfig> configCaptor;
  @Captor
  private ArgumentCaptor<Cookie> cookieCaptor;
  @Captor
  private ArgumentCaptor<String> locationCaptor;
  @Captor
  private ArgumentCaptor<Integer> statusCaptor;

  private final HeadersMultiMap queryParams = HeadersMultiMap.headers();

  private AuthenticationRoute uut;

  @BeforeEach
  void setup() {
    doReturn(queryParams).when(routingContext).queryParams();
  }

  @Test
  void missing_response_type_causes_error() {
    uut = new AuthenticationRoute(tokenGenerator, tokenRepository, Collections.emptyList());

    uut.handle(routingContext);

    verify(routingContext).fail(400);
    verifyNoInteractions(urlConfiguration, tokenGenerator, tokenRepository);
  }

  static Stream<Arguments> typesAndModesAndExcectedUrl() {
    return Stream.of(
        Arguments.of(ResponseType.CODE, null,
            "https://localhost:1234/gohere?state=state123&session_state=session123&code=session123"),
        Arguments.of(ResponseType.CODE, ResponseMode.FRAGMENT,
            "https://localhost:1234/gohere#state=state123&session_state=session123&code=session123"),
        Arguments.of(ResponseType.CODE, ResponseMode.QUERY,
            "https://localhost:1234/gohere?state=state123&session_state=session123&code=session123"),
        Arguments.of(ResponseType.ID_TOKEN, null,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring"),
        Arguments.of(ResponseType.ID_TOKEN, ResponseMode.FRAGMENT,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring"),
        Arguments.of(ResponseType.ID_TOKEN, ResponseMode.QUERY,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring"),
        Arguments.of(ResponseType.ID_TOKEN_PLUS_TOKEN, null,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring&access_token=tokenstring&token_type=bearer"),
        Arguments.of(ResponseType.ID_TOKEN_PLUS_TOKEN, ResponseMode.FRAGMENT,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring&access_token=tokenstring&token_type=bearer"),
        Arguments.of(ResponseType.ID_TOKEN_PLUS_TOKEN, ResponseMode.QUERY,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring&access_token=tokenstring&token_type=bearer"),
        Arguments.of(ResponseType.NONE, null,
            "https://localhost:1234/gohere?state=state123&session_state=session123"),
        Arguments.of(ResponseType.NONE, ResponseMode.FRAGMENT,
            "https://localhost:1234/gohere#state=state123&session_state=session123"),
        Arguments.of(ResponseType.NONE, ResponseMode.QUERY,
            "https://localhost:1234/gohere?state=state123&session_state=session123")
    );
  }

  @ParameterizedTest(name = "correct redirect for ''{0}'' and ''{1}''")
  @MethodSource("typesAndModesAndExcectedUrl")
  void correct_redirect_is_created(@Nonnull ResponseType type, @Nullable ResponseMode mode,
      @Nonnull String expectedRedirectUrl) {
    setupValidRequest(type, mode);
    uut = new AuthenticationRoute(tokenGenerator, tokenRepository, Collections.emptyList());

    uut.handle(routingContext);

    if (type == ResponseType.CODE) {
      verify(tokenRepository).putToken(anyString(), eq(TOKEN));
    } else {
      verifyNoInteractions(tokenRepository);
    }
    verify(serverResponse).end();
    TokenConfig tokenConfig = configCaptor.getValue();
    assertThat(tokenConfig.getAuthorizedParty()).isEqualTo(CLIENT_ID);
    assertThat(tokenConfig.getAudience()).containsExactly(CLIENT_ID);
    assertThat(tokenConfig.getAuthenticationContextClassReference()).isEqualTo("1");
    assertThat(tokenConfig.getAuthenticationTime())
        .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
    assertThat(tokenConfig.getClaims())
        .containsEntry("nonce", NONCE)
        .containsEntry("session_state", SESSION_ID);
    assertThat(tokenConfig.getExpiration()).isAfter(Instant.now().plus(9, ChronoUnit.HOURS));
    assertThat(tokenConfig.getFamilyName()).isEqualTo(USER);
    assertThat(tokenConfig.getIssuedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
    assertThat(tokenConfig.getPreferredUsername()).isEqualTo(USER);
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getResourceAccess()).isEmpty();
    assertThat(tokenConfig.getSubject()).isEqualTo(USER);
    Cookie cookie = cookieCaptor.getValue();
    assertThat(cookie.getName()).isEqualTo("KEYCLOAK_SESSION");
    assertThat(cookie.getValue()).startsWith(REALM + "/")
        .endsWith("/" + SESSION_ID);
    assertThat(cookie.getPath()).isEqualTo(ISSUER_PATH);
    assertThat(locationCaptor.getValue()).isEqualTo(expectedRedirectUrl);
    assertThat(statusCaptor.getValue()).isEqualTo(302);
  }

  @Test
  void resource_roles_are_used_if_configured() {
    setupValidRequest(ResponseType.CODE, null);
    uut = new AuthenticationRoute(tokenGenerator, tokenRepository, CONFIGURED_RESOURCES);

    uut.handle(routingContext);

    TokenConfig tokenConfig = configCaptor.getValue();
    assertThat(tokenConfig.getRealmAccess().getRoles())
        .isEmpty();
    assertThat(tokenConfig.getResourceAccess())
        .containsOnlyKeys("resource1", "resource2");
    assertThat(tokenConfig.getResourceAccess().get("resource1").getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
    assertThat(tokenConfig.getResourceAccess().get("resource2").getRoles())
        .containsExactlyInAnyOrder("role1", "role2", "role3");
  }

  private void setupValidRequest(@Nonnull ResponseType type, @Nullable ResponseMode mode) {
    queryParams.add("user", USER)
        .add("roles", ROLES)
        .add("realm", REALM)
        .add("state", STATE)
        .add("nonce", NONCE)
        .add("session_id", SESSION_ID)
        .add("client_id", CLIENT_ID)
        .add("redirect_uri", REDIRECT_URI)
        .add("response_type", type.toString());
    if (mode != null) {
      queryParams.add("response_mode", mode.toString());
    }
    doReturn(urlConfiguration).when(routingContext).get(CTX_REQUEST_CONFIGURATION);
    doReturn(ISSUER).when(urlConfiguration).getIssuerPath();
    doReturn(TOKEN).when(tokenGenerator).getToken(configCaptor.capture(), same(urlConfiguration));
    doReturn(serverResponse).when(routingContext).response();
    doReturn(serverResponse).when(serverResponse).addCookie(cookieCaptor.capture());
    doReturn(serverResponse).when(serverResponse)
        .putHeader(eq("location"), locationCaptor.capture());
    doReturn(serverResponse).when(serverResponse).setStatusCode(statusCaptor.capture());
  }
}
