package edu.jmi.openatom.quest.model;

import java.util.List;

public record CurrentMember(
    Long id,
    String nickname,
    String avatarUrl,
    String status,
    String currentLevel,
    Integer totalPoints,
    boolean profileCompleted,
    boolean onboardingCompleted,
    List<String> roles,
    List<String> permissions
) {
}
