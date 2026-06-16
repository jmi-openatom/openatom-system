package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAiActivityMessageDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestConfirmAiRequirementDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateAiActivitySessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestGenerateActivityDocumentsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviseAiActivityPlanDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveAiActivityPlanDTO;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiActivityAutomationService {
  Result<Map<String, Object>> createSession(RequestCreateAiActivitySessionDTO request);

  Result<List<Map<String, Object>>> sessions();

  Result<Map<String, Object>> detail(Long sessionId);

  Result<Map<String, Object>> sendMessage(Long sessionId, RequestAiActivityMessageDTO request);

  SseEmitter createSessionStream(RequestCreateAiActivitySessionDTO request);

  SseEmitter sendMessageStream(Long sessionId, RequestAiActivityMessageDTO request);

  Result<Map<String, Object>> confirmRequirement(Long sessionId, RequestConfirmAiRequirementDTO request);

  Result<Map<String, Object>> generatePlan(Long sessionId);

  SseEmitter generatePlanStream(Long sessionId);

  Result<Map<String, Object>> revisePlan(Long sessionId, RequestReviseAiActivityPlanDTO request);

  SseEmitter revisePlanStream(Long sessionId, RequestReviseAiActivityPlanDTO request);

  Result<Map<String, Object>> savePlanDraft(Long sessionId, RequestSaveAiActivityPlanDTO request);

  Result<Map<String, Object>> confirmPlan(Long sessionId);

  Result<Integer> createActivityDraft(Long sessionId);

  Result<List<Map<String, Object>>> generateDocuments(
      Long sessionId, RequestGenerateActivityDocumentsDTO request);
}
