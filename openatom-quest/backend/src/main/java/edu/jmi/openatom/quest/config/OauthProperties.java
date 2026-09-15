package edu.jmi.openatom.quest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quest.oauth")
public record OauthProperties(
    String issuer,
    String clientId,
    String clientSecret,
    String redirectUri,
    String scopes,
    String bootstrapAdminSubjects
) {
    public boolean isBootstrapAdminSubject(String subject) {
        if (subject == null || subject.isBlank() || bootstrapAdminSubjects == null || bootstrapAdminSubjects.isBlank()) {
            return false;
        }
        return java.util.Arrays.stream(bootstrapAdminSubjects.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .anyMatch(subject::equals);
    }
}
