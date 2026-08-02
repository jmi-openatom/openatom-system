package edu.jmi.openatom.server.openatomsystem.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.jmi.openatom.server.openatomsystem.config.SeafileOauthClientProperties;
import edu.jmi.openatom.server.openatomsystem.entity.OauthClient;
import edu.jmi.openatom.server.openatomsystem.mapper.OauthClientMapper;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeafileOauthClientInitializerTest {
  private static final String SECRET = "a-strong-seafile-oauth-secret-with-32-chars";

  @Mock private OauthClientMapper mapper;
  @Mock private PasswordService passwordService;
  private SeafileOauthClientProperties properties;
  private SeafileOauthClientInitializer initializer;

  @BeforeEach
  void setUp() {
    properties = new SeafileOauthClientProperties();
    initializer = new SeafileOauthClientInitializer(mapper, passwordService, properties);
  }

  @Test
  void skipsBootstrapWhenSecretIsAbsent() {
    initializer.run(null);

    verify(mapper, never()).selectByClientId(any());
  }

  @Test
  void insertsAConfidentialClientWhenMigrationRowIsMissing() {
    properties.setClientSecret(SECRET);
    when(mapper.selectByClientId("openatom-seafile")).thenReturn(null);
    when(passwordService.encode(SECRET)).thenReturn("bcrypt-hash");

    initializer.run(null);

    ArgumentCaptor<OauthClient> captor = ArgumentCaptor.forClass(OauthClient.class);
    verify(mapper).insert(captor.capture());
    OauthClient client = captor.getValue();
    org.assertj.core.api.Assertions.assertThat(client.getClientSecret()).isEqualTo("bcrypt-hash");
    org.assertj.core.api.Assertions.assertThat(client.getRedirectUris())
        .isEqualTo("https://cloud.jmi-openatom.cn/oauth/callback/");
  }

  @Test
  void rotatesTheHashAndRepairsClientMetadata() {
    properties.setClientSecret(SECRET);
    OauthClient client =
        OauthClient.builder()
            .id(9)
            .clientId("openatom-seafile")
            .clientSecret("old-hash")
            .clientName("Old")
            .redirectUris("https://wrong.example/callback")
            .scopes("openid")
            .grantTypes("authorization_code")
            .enabled(false)
            .build();
    when(mapper.selectByClientId("openatom-seafile")).thenReturn(client);
    when(passwordService.matches(SECRET, "old-hash")).thenReturn(false);
    when(passwordService.encode(SECRET)).thenReturn("new-hash");

    initializer.run(null);

    verify(mapper).updateById(client);
    org.assertj.core.api.Assertions.assertThat(client.getClientSecret()).isEqualTo("new-hash");
    org.assertj.core.api.Assertions.assertThat(client.getEnabled()).isTrue();
    org.assertj.core.api.Assertions.assertThat(client.getScopes())
        .isEqualTo(SeafileOauthClientInitializer.SCOPES);
  }

  @Test
  void rejectsWeakConfiguredSecrets() {
    properties.setClientSecret("too-short");

    assertThatThrownBy(() -> initializer.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("between 32 and 72 UTF-8 bytes");
  }

  @Test
  void rejectsSecretsLongerThanBcryptLimit() {
    properties.setClientSecret("x".repeat(73));

    assertThatThrownBy(() -> initializer.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("between 32 and 72 UTF-8 bytes");
  }
}
