package edu.jmi.openatom.lab.controller;

import edu.jmi.openatom.lab.common.ApiResponse;
import edu.jmi.openatom.lab.domain.LabModels.LabCheckinLog;
import edu.jmi.openatom.lab.domain.LabModels.LabNotice;
import edu.jmi.openatom.lab.domain.LabModels.LabProblem;
import edu.jmi.openatom.lab.domain.LabModels.LabSubmission;
import edu.jmi.openatom.lab.domain.LabModels.LabUser;
import edu.jmi.openatom.lab.domain.LabModels.SubmissionRequest;
import edu.jmi.openatom.lab.service.LabStore;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lab")
public class StudentController {
  private final LabStore labStore;

  public StudentController(LabStore labStore) {
    this.labStore = labStore;
  }

  @GetMapping("/me")
  public ApiResponse<LabUser> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.currentUser(authorization));
  }

  @GetMapping("/problems/today")
  public ApiResponse<LabProblem> todayProblem() {
    return ApiResponse.ok(sanitize(labStore.todayProblem()));
  }

  @GetMapping("/problems")
  public ApiResponse<List<LabProblem>> problems() {
    return ApiResponse.ok(labStore.publicProblems().stream().map(this::sanitize).toList());
  }

  @PostMapping("/submissions")
  public ApiResponse<LabSubmission> submit(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody @Valid SubmissionRequest request,
      @org.springframework.web.bind.annotation.RequestParam Long problemId) {
    return ApiResponse.ok(labStore.submit(authorization, problemId, request));
  }

  @GetMapping("/submissions")
  public ApiResponse<List<LabSubmission>> submissions(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.submissions(authorization));
  }

  @GetMapping("/checkins/today")
  public ApiResponse<LabCheckinLog> todayCheckin(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.todayCheckin(authorization));
  }

  @PostMapping("/checkins/onsite")
  public ApiResponse<LabCheckinLog> onsiteCheckin(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.onsiteCheckin(authorization));
  }

  @GetMapping("/score/logs")
  public ApiResponse<List<LabCheckinLog>> scoreLogs(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.scoreLogs(authorization));
  }

  @GetMapping("/notices")
  public ApiResponse<List<LabNotice>> notices(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.notices(authorization));
  }

  private LabProblem sanitize(LabProblem problem) {
    return new LabProblem(
        problem.id(),
        problem.title(),
        problem.descriptionMd(),
        problem.difficulty(),
        problem.timeLimitMs(),
        problem.memoryLimitMb(),
        problem.status(),
        problem.publishDate(),
        problem.cases(),
        Map.of(),
        problem.createdAt());
  }
}
