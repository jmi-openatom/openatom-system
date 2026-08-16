package edu.jmi.openatom.mail.oauth;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record MailSession(
    String sub,
    long userId,
    String displayName,
    String address,
    String mailboxStatus,
    String mailAccountId,
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    String csrfToken,
    List<String> roles)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MailSession withTokens(String access, String refresh, Instant expiresAt) {
    return new MailSession(
        sub, userId, displayName, address, mailboxStatus, mailAccountId,
        access, refresh, expiresAt, csrfToken, roles);
  }

  public MailSession withMailAccountId(String accountId) {
    return new MailSession(
        sub, userId, displayName, address, mailboxStatus, accountId,
        accessToken, refreshToken, accessTokenExpiresAt, csrfToken, roles);
  }

  public MailSession withRoles(List<String> newRoles) {
    return new MailSession(
        sub, userId, displayName, address, mailboxStatus, mailAccountId,
        accessToken, refreshToken, accessTokenExpiresAt, csrfToken, newRoles);
  }

  /** True when the user holds a site admin role (mirrors the main site). */
  public boolean isAdmin() {
    return roles != null
        && roles.stream()
            .anyMatch(
                role -> "super_admin".equals(role) || "club_admin".equals(role));
  }
}