package edu.jmi.openatom.mail.oauth;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

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
    String csrfToken)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MailSession withTokens(String access, String refresh, Instant expiresAt) {
    return new MailSession(
        sub, userId, displayName, address, mailboxStatus, mailAccountId,
        access, refresh, expiresAt, csrfToken);
  }

  public MailSession withMailAccountId(String accountId) {
    return new MailSession(
        sub, userId, displayName, address, mailboxStatus, accountId,
        accessToken, refreshToken, accessTokenExpiresAt, csrfToken);
  }
}
