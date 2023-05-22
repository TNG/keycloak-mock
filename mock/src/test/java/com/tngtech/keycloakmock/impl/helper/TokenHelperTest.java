package com.tngtech.keycloakmock.impl.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;

import com.tngtech.keycloakmock.api.TokenConfig;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import com.tngtech.keycloakmock.impl.UrlConfiguration;
import com.tngtech.keycloakmock.impl.session.PersistentSession;
import com.tngtech.keycloakmock.impl.session.UserData;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenHelperTest {

  private static final String CLIENT_ID = "client123";
  private static final String SESSION_ID = "sessionId123";
  private static final String NONCE = "nonce123";
  private static final UserData USER = UserData.fromUsernameAndHostname("jane.user", "example.com");
  private static final String TOKEN = "token123";
  private static final List<String> ROLES = Arrays.asList("role1", "role2");
  private static final List<String> CONFIGURED_RESOURCES = Arrays.asList("resource1", "resource2");

  @Mock private TokenGenerator tokenGenerator;

  @Mock private PersistentSession session;
  @Mock private UrlConfiguration urlConfiguration;

  @Captor private ArgumentCaptor<TokenConfig> configCaptor;

  private TokenHelper uut;

  @BeforeEach
  void setup() {
    doReturn(CLIENT_ID).when(session).getClientId();
    doReturn(SESSION_ID).when(session).getSessionId();
    doReturn(NONCE).when(session).getNonce();
    doReturn(USER).when(session).getUserData();
    doReturn(ROLES).when(session).getRoles();
    doReturn(TOKEN).when(tokenGenerator).getToken(configCaptor.capture(), same(urlConfiguration));
  }

  @Test
  void token_is_correctly_generated() {
    uut = new TokenHelper(tokenGenerator, Collections.emptyList(), Collections.emptySet());

    uut.getToken(session, urlConfiguration);

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
    assertThat(tokenConfig.getGivenName()).isEqualTo(USER.getGivenName());
    assertThat(tokenConfig.getFamilyName()).isEqualTo(USER.getFamilyName());
    assertThat(tokenConfig.getName()).isEqualTo(USER.getName());
    assertThat(tokenConfig.getEmail()).isEqualTo(USER.getEmail());
    assertThat(tokenConfig.getIssuedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
    assertThat(tokenConfig.getPreferredUsername()).isEqualTo(USER.getPreferredUsername());
    assertThat(tokenConfig.getRealmAccess().getRoles()).containsExactlyInAnyOrderElementsOf(ROLES);
    assertThat(tokenConfig.getResourceAccess()).isEmpty();
    assertThat(tokenConfig.getSubject()).isEqualTo(USER.getPreferredUsername());
  }

  @Test
  void resource_roles_are_used_if_configured() {
    uut = new TokenHelper(tokenGenerator, CONFIGURED_RESOURCES, Collections.emptySet());

    uut.getToken(session, urlConfiguration);

    TokenConfig tokenConfig = configCaptor.getValue();
    assertThat(tokenConfig.getRealmAccess().getRoles()).isEmpty();
    assertThat(tokenConfig.getResourceAccess()).containsOnlyKeys("resource1", "resource2");
    assertThat(tokenConfig.getResourceAccess().get("resource1").getRoles())
        .containsExactlyInAnyOrderElementsOf(ROLES);
    assertThat(tokenConfig.getResourceAccess().get("resource2").getRoles())
        .containsExactlyInAnyOrderElementsOf(ROLES);
  }
}
