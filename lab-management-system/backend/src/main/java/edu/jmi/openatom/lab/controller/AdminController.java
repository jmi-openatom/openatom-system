package edu.jmi.openatom.lab.controller;

import edu.jmi.openatom.lab.common.ApiResponse;
import edu.jmi.openatom.lab.domain.LabModels.Dashboard;
import edu.jmi.openatom.lab.domain.LabModels.LabCheckinLog;
import edu.jmi.openatom.lab.domain.LabModels.LabNotice;
import edu.jmi.openatom.lab.domain.LabModels.LabProblem;
import edu.jmi.openatom.lab.domain.LabModels.LabSettings;
import edu.jmi.openatom.lab.domain.LabModels.LabSubmission;
import edu.jmi.openatom.lab.domain.LabModels.LabUser;
import edu.jmi.openatom.lab.domain.LabModels.LabUserRequest;
import edu.jmi.openatom.lab.domain.LabModels.NoticeRequest;
import edu.jmi.openatom.lab.domain.LabModels.ProblemRequest;
import edu.jmi.openatom.lab.domain.LabModels.SettingsRequest;
import edu.jmi.openatom.lab.service.LabStore;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final LabStore labStore;

  public AdminController(LabStore labStore) {
    this.labStore = labStore;
  }

  @GetMapping("/dashboard")
  public ApiResponse<Dashboard> dashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.dashboard(authorization));
  }

  @GetMapping("/users")
  public ApiResponse<List<LabUser>> users(@RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.adminUsers(authorization));
  }

  @PostMapping("/users")
  public ApiResponse<LabUser> saveUser(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody LabUserRequest request) {
    return ApiResponse.ok(labStore.saveUser(authorization, request));
  }

  @GetMapping("/problems")
  public ApiResponse<List<LabProblem>> problems(@RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.adminProblems(authorization));
  }

  @PostMapping("/problems")
  public ApiResponse<LabProblem> saveProblem(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody ProblemRequest request) {
    return ApiResponse.ok(labStore.saveProblem(authorization, request));
  }

  @PostMapping("/problems/generate")
  public ApiResponse<LabProblem> generateProblem(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.generateProblem(authorization));
  }

  @GetMapping("/submissions")
  public ApiResponse<List<LabSubmission>> submissions(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.adminSubmissions(authorization));
  }

  @GetMapping("/checkins")
  public ApiResponse<List<LabCheckinLog>> checkins(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.adminCheckins(authorization));
  }

  @PostMapping("/checkins/close-day")
  public ApiResponse<Map<String, Integer>> closeDay(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(Map.of("created", labStore.closeDay(authorization)));
  }

  @GetMapping("/settings")
  public ApiResponse<LabSettings> settings(@RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.settings(authorization));
  }

  @PatchMapping("/settings")
  public ApiResponse<LabSettings> updateSettings(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody SettingsRequest request) {
    return ApiResponse.ok(labStore.updateSettings(authorization, request));
  }

  @GetMapping("/notices")
  public ApiResponse<List<LabNotice>> notices(@RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(labStore.adminNotices(authorization));
  }

  @PostMapping("/notices")
  public ApiResponse<LabNotice> createNotice(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody NoticeRequest request) {
    return ApiResponse.ok(labStore.createNotice(authorization, request));
  }
}
