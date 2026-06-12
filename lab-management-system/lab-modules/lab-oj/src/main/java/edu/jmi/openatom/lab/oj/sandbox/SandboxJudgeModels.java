package edu.jmi.openatom.lab.oj.sandbox;

import java.util.List;

public final class SandboxJudgeModels {
  private SandboxJudgeModels() {}

  public record JudgeCase(String input, String expectedOutput) {}

  public record JudgeRequest(
      String language, String code, Integer timeLimitMs, Integer memoryLimitMb, List<JudgeCase> cases) {}

  public record JudgeResult(
      String status, Integer passed, Integer total, Integer runtimeMs, Integer memoryKb, String message) {}
}
