package edu.jmi.openatom.quest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quest.oauth")
public record OauthProperties(
    String issuer,
    String clientId,
    String clientSecret,
    String redirectUri,
    String scopes
) {
}
