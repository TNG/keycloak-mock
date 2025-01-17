package com.tngtech.keycloakmock.impl.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;

import com.tngtech.keycloakmock.api.LoginRoleMapping;
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
import org.assertj.core.util.Lists;
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
    uut = new TokenHelper(tokenGenerator, Collections.emptyList(), LoginRoleMapping.TO_REALM);

    uut.getToken(session, urlConfiguration);

    TokenConfig tokenConfig = configCaptor.getValue();
    assertThat(tokenConfig.getAuthorizedParty()).isEqualTo(CLIENT_ID);
    assertThat(tokenConfig.getAudience()).containsExactly(CLIENT_ID);
    assertThat(tokenConfig.getAuthenticationContextClassReference()).isEqualTo("1");
    assertThat(tokenConfig.getAuthenticationTime())
        .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
    assertThat(tokenConfig.getSessionId()).isEqualTo(SESSION_ID);
    assertThat(tokenConfig.getClaims()).containsEntry("nonce", NONCE);
    assertThat(tokenConfig.getExpiration()).isNull();
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
  void default_audiences_are_added() {
    uut =
        new TokenHelper(
            tokenGenerator, Lists.list("audience1", "audience2"), LoginRoleMapping.TO_REALM);

    uut.getToken(session, urlConfiguration);

    TokenConfig tokenConfig = configCaptor.getValue();
    assertThat(tokenConfig.getAudience())
        .containsExactlyInAnyOrder(CLIENT_ID, "audience1", "audience2");
    assertThat(tokenConfig.getResourceAccess()).isEmpty();
  }

  @Test
  void resource_roles_are_added() {
    uut =
        new TokenHelper(
            tokenGenerator, Lists.list("audience1", "audience2"), LoginRoleMapping.TO_RESOURCE);

    uut.getToken(session, urlConfiguration);

    TokenConfig tokenConfig = configCaptor.getValue();
    assertThat(tokenConfig.getAudience())
        .containsExactlyInAnyOrder(CLIENT_ID, "audience1", "audience2");
    assertThat(tokenConfig.getRealmAccess().getRoles()).isEmpty();
    assertThat(tokenConfig.getResourceAccess())
        .containsOnlyKeys(CLIENT_ID, "audience1", "audience2");
    assertThat(tokenConfig.getResourceAccess().get(CLIENT_ID).getRoles())
        .containsExactlyInAnyOrderElementsOf(ROLES);
    assertThat(tokenConfig.getResourceAccess().get("audience1").getRoles())
        .containsExactlyInAnyOrderElementsOf(ROLES);
    assertThat(tokenConfig.getResourceAccess().get("audience2").getRoles())
        .containsExactlyInAnyOrderElementsOf(ROLES);
  }

  @Test
  void resource_and_realm_roles_are_added() {
    uut =
        new TokenHelper(
            tokenGenerator, Lists.list("audience1", "audience2"), LoginRoleMapping.TO_BOTH);

    uut.getToken(session, urlConfiguration);

    TokenConfig tokenConfig = configCaptor.getValue();
    assertThat(tokenConfig.getAudience())
        .containsExactlyInAnyOrder(CLIENT_ID, "audience1", "audience2");
    assertThat(tokenConfig.getResourceAccess())
        .containsOnlyKeys(CLIENT_ID, "audience1", "audience2");
    assertThat(tokenConfig.getResourceAccess().get(CLIENT_ID).getRoles())
        .containsExactlyInAnyOrderElementsOf(ROLES);
    assertThat(tokenConfig.getResourceAccess().get("audience1").getRoles())
        .containsExactlyInAnyOrderElementsOf(ROLES);
    assertThat(tokenConfig.getResourceAccess().get("audience2").getRoles())
        .containsExactlyInAnyOrderElementsOf(ROLES);
  }
}
