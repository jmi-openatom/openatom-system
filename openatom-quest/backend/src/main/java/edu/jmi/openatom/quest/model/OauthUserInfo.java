package edu.jmi.openatom.quest.model;

public record OauthUserInfo(
    String subject,
    String displayName,
    String avatarUrl,
    String email,
    String school,
    String college,
    String major,
    String grade,
    Integer labRole
) {
}
