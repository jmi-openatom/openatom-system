package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAutoScheduleInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAddInterviewCandidatesDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestRescheduleInterviewSessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateInterviewRoomDTO;
import edu.jmi.openatom.server.openatomsystem.service.InterviewScheduleManagementService;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewSession;
import edu.jmi.openatom.server.openatomsystem.service.InterviewSessionService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewerOptionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewScheduleVO;
import jakarta.validation.Valid;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InterviewSessionController {
  private final InterviewSessionService service;
  private final InterviewScheduleManagementService scheduleManagement;

  @PostMapping("/interview-sessions/auto-schedule")
  @SaCheckPermission("interview:create")
  public Result<ResponseInterviewScheduleVO> autoSchedule(
      @Valid @RequestBody RequestAutoScheduleInterviewDTO request) {
    return service.autoSchedule(request);
  }

  @GetMapping("/interview-sessions")
  @SaCheckPermission("interview:list")
  public Result<List<InterviewSession>> list(
      @RequestParam(required = false) Integer campaignId) {
    return service.list(campaignId);
  }

  @PostMapping("/interview-sessions/{sessionId}/candidates")
  @SaCheckPermission("interview:create")
  public Result<ResponseInterviewScheduleVO> addCandidates(@PathVariable Integer sessionId,
      @Valid @RequestBody RequestAddInterviewCandidatesDTO request) {
    return service.addCandidates(sessionId, request);
  }

  @GetMapping("/interview-sessions/{sessionId}")
  @SaCheckPermission("interview:detail")
  public Result<ResponseInterviewScheduleVO> detail(@PathVariable Integer sessionId) {
    return service.detail(sessionId);
  }

  @PostMapping("/interview-sessions/{sessionId}/rooms")
  @SaCheckPermission("interview:update")
  public Result<Integer> addRoom(@PathVariable Integer sessionId,
      @Valid @RequestBody RequestUpdateInterviewRoomDTO request) {
    return scheduleManagement.addRoom(sessionId, request);
  }

  @PostMapping("/interview-sessions/{sessionId}/reschedule")
  @SaCheckPermission("interview:update")
  public Result<ResponseInterviewScheduleVO> reschedule(@PathVariable Integer sessionId,
      @Valid @RequestBody RequestRescheduleInterviewSessionDTO request) {
    return scheduleManagement.reschedule(sessionId, request);
  }

  @PostMapping("/interview-sessions/{sessionId}/publish")
  @SaCheckPermission("interview:update")
  public Result<String> publish(@PathVariable Integer sessionId) {
    return service.publish(sessionId);
  }

  @GetMapping("/interview-sessions/{sessionId}/schedule.csv")
  @SaCheckPermission("interview:list")
  public ResponseEntity<byte[]> exportSchedule(@PathVariable Integer sessionId) {
    Result<byte[]> result = service.exportSchedule(sessionId);
    if (result.getCode() != Result.SUCCESS_CODE) {
      throw new ResponseStatusException(HttpStatusCode.valueOf(result.getCode()), result.getMessage());
    }
    String name = URLEncoder.encode("面试时间安排-" + sessionId + ".csv", StandardCharsets.UTF_8)
        .replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(result.getData());
  }

  @GetMapping("/interview-sessions/interviewer-options")
  @SaCheckPermission(value = {"interview:create", "interview:update"}, mode = SaMode.OR)
  public Result<List<ResponseInterviewerOptionVO>> interviewerOptions(
      @RequestParam(required = false) String keyword) {
    return service.interviewerOptions(keyword);
  }
}
