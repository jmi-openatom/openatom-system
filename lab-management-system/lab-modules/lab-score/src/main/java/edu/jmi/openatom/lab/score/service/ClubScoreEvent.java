package edu.jmi.openatom.lab.score.service;

import java.time.LocalDate;

public record ClubScoreEvent(
    String eventType,
    Long checkinLogId,
    Long labUserId,
    Long clubUserId,
    LocalDate checkinDate,
    Integer scoreDelta,
    String source,
    String idempotencyKey) {}
