package edu.jmi.openatom.lab.oj.service;

import edu.jmi.openatom.lab.checkin.service.CheckInService;
import edu.jmi.openatom.lab.oj.entity.LabProblem;
import edu.jmi.openatom.lab.oj.entity.LabProblemTestCase;
import edu.jmi.openatom.lab.oj.entity.LabSubmission;
import edu.jmi.openatom.lab.oj.mapper.LabProblemMapper;
import edu.jmi.openatom.lab.oj.mapper.LabProblemTestCaseMapper;
import edu.jmi.openatom.lab.oj.mapper.LabSubmissionMapper;
import edu.jmi.openatom.lab.oj.sandbox.SandboxJudgeClient;
import edu.jmi.openatom.lab.oj.sandbox.SandboxJudgeModels.JudgeCase;
import edu.jmi.openatom.lab.oj.sandbox.SandboxJudgeModels.JudgeRequest;
import edu.jmi.openatom.lab.oj.sandbox.SandboxJudgeModels.JudgeResult;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OjJudgeService {
  private final LabSubmissionMapper submissionMapper;
  private final LabProblemMapper problemMapper;
  private final LabProblemTestCaseMapper testCaseMapper;
  private final SandboxJudgeClient sandboxJudgeClient;
  private final CheckInService checkInService;

  @Async
  public void submitForJudge(Long submissionId) {
    LabSubmission submission = submissionMapper.selectById(submissionId);
    if (submission == null) {
      return;
    }
    LabProblem problem = problemMapper.selectById(submission.getProblemId());
    if (problem == null) {
      mark(submission, "RE", 0, 0, "题目不存在", null);
      return;
    }
    var cases = testCaseMapper.selectByProblemId(problem.getId());
    JudgeRequest request =
        new JudgeRequest(
            submission.getLanguage(),
            submission.getCode(),
            problem.getTimeLimitMs(),
            problem.getMemoryLimitMb(),
            cases.stream().map(this::toJudgeCase).toList());
    try {
      JudgeResult result = sandboxJudgeClient.judge(request);
      mark(submission, result.status(), result.passed(), result.total(), result.message(), result);
      if ("AC".equals(result.status()) && problem.getProblemDate().equals(java.time.LocalDate.now())) {
        checkInService.autoCheckInFromAccepted(submission.getUserId(), problem.getTitle());
      }
    } catch (Exception ex) {
      mark(submission, "RE", 0, cases.size(), ex.getMessage(), null);
    }
  }

  private JudgeCase toJudgeCase(LabProblemTestCase testCase) {
    return new JudgeCase(testCase.getInputText(), testCase.getExpectedOutput());
  }

  private void mark(
      LabSubmission submission,
      String status,
      Integer passed,
      Integer total,
      String message,
      JudgeResult result) {
    submission.setJudgeStatus(status == null ? "RE" : status);
    submission.setScorePassed(passed == null ? 0 : passed);
    submission.setTotalCases(total == null ? 0 : total);
    submission.setRuntimeMs(result == null ? null : result.runtimeMs());
    submission.setMemoryKb(result == null ? null : result.memoryKb());
    submission.setErrorMessage(message);
    submission.setJudgedAt(LocalDateTime.now());
    submissionMapper.updateById(submission);
  }
}
