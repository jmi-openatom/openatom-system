package edu.jmi.openatom.mail.domain;

public record MailboxAccount(
    long id,
    String oauthSub,
    long userId,
    String displayName,
    String primaryAddress,
    String localPart,
    String mailDomain,
    String stalwartAccountId,
    long quotaBytes,
    String status,
    String provisionStatus,
    String lastEventId,
    String lastError) {}
