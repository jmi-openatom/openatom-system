package edu.jmi.openatom.quest.model;

public record OauthUserInfo(
    String subject,
    String displayName,
    String avatarUrl,
    String email
) {
}
