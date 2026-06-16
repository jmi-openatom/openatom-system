package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAiActivityMessageDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestConfirmAiRequirementDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateAiActivitySessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestGenerateActivityDocumentsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviseAiActivityPlanDTO;
import edu.jmi.openatom.server.openatomsystem.service.AiActivityAutomationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 活动自动化控制器。 */
@RestController
@RequiredArgsConstructor
public class AiActivityAutomationController {
  private final AiActivityAutomationService aiActivityAutomationService;

  @PostMapping("/ai/activity/sessions")
  @SaCheckPermission("activity:create")
  public Result<Map<String, Object>> createSession(
      @Valid @RequestBody RequestCreateAiActivitySessionDTO request) {
    return aiActivityAutomationService.createSession(request);
  }

  @PostMapping(value = "/ai/activity/sessions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @SaCheckPermission("activity:create")
  public SseEmitter createSessionStream(
      @Valid @RequestBody RequestCreateAiActivitySessionDTO request) {
    return aiActivityAutomationService.createSessionStream(request);
  }

  @GetMapping("/ai/activity/sessions")
  @SaCheckPermission("activity:list")
  public Result<List<Map<String, Object>>> sessions() {
    return aiActivityAutomationService.sessions();
  }

  @GetMapping("/ai/activity/sessions/{sessionId}")
  @SaCheckPermission("activity:list")
  public Result<Map<String, Object>> detail(@PathVariable Long sessionId) {
    return aiActivityAutomationService.detail(sessionId);
  }

  @PostMapping("/ai/activity/sessions/{sessionId}/messages")
  @SaCheckPermission("activity:create")
  public Result<Map<String, Object>> sendMessage(
      @PathVariable Long sessionId, @Valid @RequestBody RequestAiActivityMessageDTO request) {
    return aiActivityAutomationService.sendMessage(sessionId, request);
  }

  @PostMapping(value = "/ai/activity/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @SaCheckPermission("activity:create")
  public SseEmitter sendMessageStream(
      @PathVariable Long sessionId, @Valid @RequestBody RequestAiActivityMessageDTO request) {
    return aiActivityAutomationService.sendMessageStream(sessionId, request);
  }

  @PostMapping("/ai/activity/sessions/{sessionId}/confirm-requirement")
  @SaCheckPermission("activity:create")
  public Result<Map<String, Object>> confirmRequirement(
      @PathVariable Long sessionId, @RequestBody(required = false) RequestConfirmAiRequirementDTO request) {
    return aiActivityAutomationService.confirmRequirement(
        sessionId, request == null ? new RequestConfirmAiRequirementDTO() : request);
  }

  @PostMapping("/ai/activity/sessions/{sessionId}/generate-plan")
  @SaCheckPermission("activity:create")
  public Result<Map<String, Object>> generatePlan(@PathVariable Long sessionId) {
    return aiActivityAutomationService.generatePlan(sessionId);
  }

  @PostMapping(value = "/ai/activity/sessions/{sessionId}/generate-plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @SaCheckPermission("activity:create")
  public SseEmitter generatePlanStream(@PathVariable Long sessionId) {
    return aiActivityAutomationService.generatePlanStream(sessionId);
  }

  @PostMapping("/ai/activity/sessions/{sessionId}/revise-plan")
  @SaCheckPermission("activity:create")
  public Result<Map<String, Object>> revisePlan(
      @PathVariable Long sessionId, @Valid @RequestBody RequestReviseAiActivityPlanDTO request) {
    return aiActivityAutomationService.revisePlan(sessionId, request);
  }

  @PostMapping(value = "/ai/activity/sessions/{sessionId}/revise-plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @SaCheckPermission("activity:create")
  public SseEmitter revisePlanStream(
      @PathVariable Long sessionId, @Valid @RequestBody RequestReviseAiActivityPlanDTO request) {
    return aiActivityAutomationService.revisePlanStream(sessionId, request);
  }

  @PostMapping("/ai/activity/sessions/{sessionId}/confirm-plan")
  @SaCheckPermission("activity:create")
  public Result<Map<String, Object>> confirmPlan(@PathVariable Long sessionId) {
    return aiActivityAutomationService.confirmPlan(sessionId);
  }

  @PostMapping("/ai/activity/sessions/{sessionId}/create-activity")
  @SaCheckPermission("activity:create")
  public Result<Integer> createActivityDraft(@PathVariable Long sessionId) {
    return aiActivityAutomationService.createActivityDraft(sessionId);
  }

  @PostMapping("/ai/activity/sessions/{sessionId}/documents/generate")
  @SaCheckPermission("document:create")
  public Result<List<Map<String, Object>>> generateDocuments(
      @PathVariable Long sessionId,
      @RequestBody(required = false) RequestGenerateActivityDocumentsDTO request) {
    return aiActivityAutomationService.generateDocuments(
        sessionId, request == null ? new RequestGenerateActivityDocumentsDTO() : request);
  }
}
