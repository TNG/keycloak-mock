package com.tngtech.keycloakmock.impl.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.session.PersistentSession;
import com.tngtech.keycloakmock.impl.session.ResponseMode;
import com.tngtech.keycloakmock.impl.session.ResponseType;
import io.vertx.core.http.Cookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedirectHelperTest {

  private static final String REALM = "realm123";
  private static final String SESSION_ID = "session123";
  private static final String STATE = "state123";
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

  @Mock private TokenHelper tokenHelper;

  @Mock private PersistentSession session;
  @Mock private UrlConfiguration urlConfiguration;

  @InjectMocks private RedirectHelper uut;

  @Test
  void redirect_location_is_not_generated_on_missing_response_type() {
    String redirectLocation = uut.getRedirectLocation(session, urlConfiguration);

    verify(session, times(2)).getResponseType();
    verifyNoMoreInteractions(session);
    assertThat(redirectLocation).isNull();
  }

  static Stream<Arguments> typesAndModesAndExcectedUrl() {
    return Stream.of(
        Arguments.of(
            ResponseType.CODE,
            null,
            "https://localhost:1234/gohere?state=state123&session_state=session123&code=session123"),
        Arguments.of(
            ResponseType.CODE,
            ResponseMode.FRAGMENT,
            "https://localhost:1234/gohere#state=state123&session_state=session123&code=session123"),
        Arguments.of(
            ResponseType.CODE,
            ResponseMode.QUERY,
            "https://localhost:1234/gohere?state=state123&session_state=session123&code=session123"),
        Arguments.of(
            ResponseType.ID_TOKEN,
            null,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring"),
        Arguments.of(
            ResponseType.ID_TOKEN,
            ResponseMode.FRAGMENT,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring"),
        Arguments.of(
            ResponseType.ID_TOKEN,
            ResponseMode.QUERY,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring"),
        Arguments.of(
            ResponseType.ID_TOKEN_PLUS_TOKEN,
            null,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring&access_token=tokenstring&token_type=bearer"),
        Arguments.of(
            ResponseType.ID_TOKEN_PLUS_TOKEN,
            ResponseMode.FRAGMENT,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring&access_token=tokenstring&token_type=bearer"),
        Arguments.of(
            ResponseType.ID_TOKEN_PLUS_TOKEN,
            ResponseMode.QUERY,
            "https://localhost:1234/gohere#state=state123&session_state=session123&id_token=tokenstring&access_token=tokenstring&token_type=bearer"),
        Arguments.of(
            ResponseType.NONE,
            null,
            "https://localhost:1234/gohere?state=state123&session_state=session123"),
        Arguments.of(
            ResponseType.NONE,
            ResponseMode.FRAGMENT,
            "https://localhost:1234/gohere#state=state123&session_state=session123"),
        Arguments.of(
            ResponseType.NONE,
            ResponseMode.QUERY,
            "https://localhost:1234/gohere?state=state123&session_state=session123"));
  }

  @ParameterizedTest(name = "correct redirect for ''{0}'' and ''{1}''")
  @MethodSource("typesAndModesAndExcectedUrl")
  void redirect_location_is_generated_correctly(
      @Nonnull ResponseType type,
      @Nullable ResponseMode mode,
      @Nonnull String expectedRedirectUrl) {
    doReturn(SESSION_ID).when(session).getSessionId();
    doReturn(STATE).when(session).getState();
    doReturn(REDIRECT_URI).when(session).getRedirectUri();
    doReturn(type.toString()).when(session).getResponseType();
    if (mode != null) {
      doReturn(mode.toString()).when(session).getResponseMode();
    }
    doReturn(TOKEN).when(tokenHelper).getToken(session, urlConfiguration);

    String redirectLocation = uut.getRedirectLocation(session, urlConfiguration);

    assertThat(redirectLocation).isEqualTo(expectedRedirectUrl);
  }

  @Test
  void cookie_is_generated_correctly() {
    doReturn(SESSION_ID).when(session).getSessionId();
    doReturn(ISSUER).when(urlConfiguration).getIssuerPath();
    doReturn(REALM).when(urlConfiguration).getRealm();

    Cookie cookie = uut.getSessionCookie(session, urlConfiguration);

    assertThat(cookie.getName()).isEqualTo("KEYCLOAK_SESSION");
    assertThat(cookie.getValue()).startsWith(REALM + "/").endsWith("/" + SESSION_ID);
    assertThat(cookie.getPath()).isEqualTo(ISSUER_PATH);
  }
}
