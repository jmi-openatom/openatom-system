package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAutoScheduleInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewSession;
import edu.jmi.openatom.server.openatomsystem.service.InterviewSessionService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewerOptionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewScheduleVO;
import jakarta.validation.Valid;
import java.util.List;
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

  @GetMapping("/interview-sessions/{sessionId}")
  @SaCheckPermission("interview:detail")
  public Result<ResponseInterviewScheduleVO> detail(@PathVariable Integer sessionId) {
    return service.detail(sessionId);
  }

  @PostMapping("/interview-sessions/{sessionId}/publish")
  @SaCheckPermission("interview:update")
  public Result<String> publish(@PathVariable Integer sessionId) {
    return service.publish(sessionId);
  }

  @GetMapping("/interview-sessions/interviewer-options")
  @SaCheckPermission("interview:create")
  public Result<List<ResponseInterviewerOptionVO>> interviewerOptions(
      @RequestParam(required = false) String keyword) {
    return service.interviewerOptions(keyword);
  }
}
