package edu.jmi.openatom.server.openatomsystem.bootstrap;

import edu.jmi.openatom.server.openatomsystem.config.SeafileOauthClientProperties;
import edu.jmi.openatom.server.openatomsystem.entity.OauthClient;
import edu.jmi.openatom.server.openatomsystem.mapper.OauthClientMapper;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class SeafileOauthClientInitializer implements ApplicationRunner {
  static final String SCOPES = "openid profile email";
  static final String GRANT_TYPES = "authorization_code refresh_token";

  private final OauthClientMapper oauthClientMapper;
  private final PasswordService passwordService;
  private final SeafileOauthClientProperties properties;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) {
    String secret = trim(properties.getClientSecret());
    if (secret == null) {
      log.info("Seafile OAuth client bootstrap is disabled because no client secret is configured");
      return;
    }
    if (secret.length() < 32) {
      throw new IllegalStateException("app.seafile-oauth.client-secret must contain at least 32 characters");
    }

    String clientId = required(properties.getClientId(), "app.seafile-oauth.client-id");
    OauthClient client = oauthClientMapper.selectByClientId(clientId);
    if (client == null) {
      oauthClientMapper.insert(
          OauthClient.builder()
              .clientId(clientId)
              .clientSecret(passwordService.encode(secret))
              .clientName(required(properties.getClientName(), "app.seafile-oauth.client-name"))
              .redirectUris(required(properties.getRedirectUri(), "app.seafile-oauth.redirect-uri"))
              .scopes(SCOPES)
              .grantTypes(GRANT_TYPES)
              .enabled(true)
              .build());
      log.info("Initialized confidential OAuth client: {}", clientId);
      return;
    }

    boolean changed = false;
    if (!passwordService.matches(secret, client.getClientSecret())) {
      client.setClientSecret(passwordService.encode(secret));
      changed = true;
    }
    changed |= setIfDifferent(client.getClientName(), properties.getClientName(), client::setClientName);
    changed |= setIfDifferent(client.getRedirectUris(), properties.getRedirectUri(), client::setRedirectUris);
    changed |= setIfDifferent(client.getScopes(), SCOPES, client::setScopes);
    changed |= setIfDifferent(client.getGrantTypes(), GRANT_TYPES, client::setGrantTypes);
    if (!Boolean.TRUE.equals(client.getEnabled())) {
      client.setEnabled(true);
      changed = true;
    }
    if (changed) {
      oauthClientMapper.updateById(client);
      log.info("Synchronized confidential OAuth client: {}", clientId);
    }
  }

  private boolean setIfDifferent(String current, String configured, java.util.function.Consumer<String> setter) {
    String value = trim(configured);
    if (value == null || value.equals(current)) return false;
    setter.accept(value);
    return true;
  }

  private String required(String value, String propertyName) {
    String normalized = trim(value);
    if (normalized == null) throw new IllegalStateException(propertyName + " must not be blank");
    return normalized;
  }

  private String trim(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }
}
