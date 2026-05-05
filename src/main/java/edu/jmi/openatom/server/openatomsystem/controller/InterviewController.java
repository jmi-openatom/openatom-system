package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestInterviewFeedbackDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.service.InterviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InterviewController {
  private final InterviewService interviewService;

  @GetMapping("/interviews")
  @SaCheckPermission("interview:list")
  public ApiResponse<List<Interview>> list(
      @RequestParam(required = false) Integer campaignId,
      @RequestParam(required = false) Integer applicationId,
      @RequestParam(required = false) Integer interviewerId,
      @RequestParam(required = false) String status) {
    return interviewService.list(campaignId, applicationId, interviewerId, status);
  }

  @PostMapping("/interviews")
  @SaCheckPermission("interview:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateInterviewDTO request) {
    return interviewService.create(request);
  }

  @GetMapping("/interviews/{interviewId}")
  @SaCheckPermission("interview:detail")
  public ApiResponse<Interview> detail(@PathVariable Integer interviewId) {
    return interviewService.detail(interviewId);
  }

  @PatchMapping("/interviews/{interviewId}")
  @SaCheckPermission("interview:update")
  public ApiResponse<String> update(
      @PathVariable Integer interviewId, @Valid @RequestBody RequestUpdateInterviewDTO request) {
    return interviewService.update(interviewId, request);
  }

  @PostMapping("/interviews/{interviewId}/confirm")
  @SaCheckPermission("interview:confirm")
  public ApiResponse<String> confirm(@PathVariable Integer interviewId) {
    return interviewService.confirm(interviewId);
  }

  @PostMapping("/interviews/{interviewId}/feedback")
  @SaCheckPermission("interview:feedback")
  public ApiResponse<String> feedback(
      @PathVariable Integer interviewId, @Valid @RequestBody RequestInterviewFeedbackDTO request) {
    return interviewService.feedback(interviewId, request);
  }

  @PostMapping("/interviews/{interviewId}/complete")
  @SaCheckPermission("interview:complete")
  public ApiResponse<String> complete(@PathVariable Integer interviewId) {
    return interviewService.complete(interviewId);
  }
}
