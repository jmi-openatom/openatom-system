package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateLeaderboardVisibilityRequest(@NotNull Boolean visible) {}
