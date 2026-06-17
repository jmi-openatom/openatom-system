package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAiActivityMessageDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestConfirmAiRequirementDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateAiActivitySessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestGenerateActivityDocumentsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviseAiActivityPlanDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveAiActivityPlanDTO;
import edu.jmi.openatom.server.openatomsystem.entity.AiActivityMessage;
import edu.jmi.openatom.server.openatomsystem.entity.AiActivityPlan;
import edu.jmi.openatom.server.openatomsystem.entity.AiActivitySession;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.entity.DocumentTemplate;
import edu.jmi.openatom.server.openatomsystem.entity.GeneratedDocument;
import edu.jmi.openatom.server.openatomsystem.mapper.AiActivityMessageMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.AiActivityPlanMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.AiActivitySessionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubActivityMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.DocumentTemplateMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.GeneratedDocumentMapper;
import edu.jmi.openatom.server.openatomsystem.service.AiActivityAutomationService;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class AiActivityAutomationServiceImpl implements AiActivityAutomationService {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Pattern PARTICIPANT_PATTERN = Pattern.compile("(?:预计|约|大约|左右)?\\s*(\\d{1,5})\\s*[人名]");
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";
  private static final List<String> DOCUMENT_TYPES =
      List.of("activity_proposal", "activity_application_form", "volunteer_application_form");

  private final ClubMapper clubMapper;
  private final ClubActivityMapper clubActivityMapper;
  private final AiActivitySessionMapper sessionMapper;
  private final AiActivityMessageMapper messageMapper;
  private final AiActivityPlanMapper planMapper;
  private final DocumentTemplateMapper templateMapper;
  private final GeneratedDocumentMapper generatedDocumentMapper;
  private final DeepSeekClientService deepSeekClientService;
  private final DocumentTemplateServiceImpl documentTemplateService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> createSession(RequestCreateAiActivitySessionDTO request) {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    Club club = defaultClub();
    String title =
        request.getTitle() == null || request.getTitle().isBlank()
            ? titleFrom(request.getInitialMessage())
            : request.getTitle().trim();
    AiActivitySession session =
        AiActivitySession.builder()
            .clubId(club == null ? null : club.getId())
            .userId(StpUtil.getLoginIdAsInt())
            .title(title)
            .status("drafting")
            .build();
    sessionMapper.insert(session);
    return sendMessage(
        session.getId(),
        RequestAiActivityMessageDTO.builder()
            .message(request.getInitialMessage())
            .mode("requirement_clarification")
            .build());
  }

  @Override
  public Result<List<Map<String, Object>>> sessions() {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    return Result.success(sessionMapper.selectByUser(StpUtil.getLoginIdAsInt()).stream().map(this::sessionMap).toList());
  }

  @Override
  public Result<Map<String, Object>> detail(Long sessionId) {
    AiActivitySession session = sessionForCurrentUser(sessionId);
    if (session == null) return Result.error(404, "会话不存在");
    return Result.success(detailMap(session));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> sendMessage(Long sessionId, RequestAiActivityMessageDTO request) {
    AiActivitySession session = sessionForCurrentUser(sessionId);
    if (session == null) return Result.error(404, "会话不存在");
    String message = request.getMessage().trim();
    messageMapper.insert(
        AiActivityMessage.builder().sessionId(sessionId).role("user").content(message).build());
    List<Map<String, String>> messages = buildMessages(sessionId);
    String response =
        deepSeekClientService.chat("activity_requirement", requirementPrompt(), messages);
    Map<String, Object> structured = parseJsonObject(response);
    messageMapper.insert(
        AiActivityMessage.builder()
            .sessionId(sessionId)
            .role("assistant")
            .content(response)
            .structuredPayload(structured.isEmpty() ? null : Jsons.stringify(structured))
            .build());
    touch(session);
    return Result.success(detailMap(sessionMapper.selectById(sessionId)));
  }

  @Override
  public SseEmitter createSessionStream(RequestCreateAiActivitySessionDTO request) {
    SseEmitter emitter = newEmitter();
    if (!StpUtil.isLogin()) {
      streamError(emitter, "请先登录");
      return emitter;
    }
    Integer userId = StpUtil.getLoginIdAsInt();
    Thread.startVirtualThread(
        () -> {
          try {
            Club club = defaultClub();
            String title =
                request.getTitle() == null || request.getTitle().isBlank()
                    ? titleFrom(request.getInitialMessage())
                    : request.getTitle().trim();
            AiActivitySession session =
                AiActivitySession.builder()
                    .clubId(club == null ? null : club.getId())
                    .userId(userId)
                    .title(title)
                    .status("drafting")
                    .build();
            sessionMapper.insert(session);
            String message = request.getInitialMessage().trim();
            messageMapper.insert(
                AiActivityMessage.builder().sessionId(session.getId()).role("user").content(message).build());
            emit(emitter, "session", Map.of("detail", detailMap(sessionMapper.selectById(session.getId()))));
            streamRequirementResponse(emitter, userId, session);
          } catch (Exception e) {
            streamError(emitter, e.getMessage());
          }
        });
    return emitter;
  }

  @Override
  public SseEmitter sendMessageStream(Long sessionId, RequestAiActivityMessageDTO request) {
    SseEmitter emitter = newEmitter();
    if (!StpUtil.isLogin()) {
      streamError(emitter, "请先登录");
      return emitter;
    }
    Integer userId = StpUtil.getLoginIdAsInt();
    Thread.startVirtualThread(
        () -> {
          try {
            AiActivitySession session = sessionForUser(sessionId, userId);
            if (session == null) {
              streamError(emitter, "会话不存在");
              return;
            }
            String message = request.getMessage().trim();
            messageMapper.insert(
                AiActivityMessage.builder().sessionId(sessionId).role("user").content(message).build());
            emit(emitter, "message", Map.of("detail", detailMap(sessionMapper.selectById(sessionId))));
            streamRequirementResponse(emitter, userId, session);
          } catch (Exception e) {
            streamError(emitter, e.getMessage());
          }
        });
    return emitter;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> confirmRequirement(
      Long sessionId, RequestConfirmAiRequirementDTO request) {
    AiActivitySession session = sessionForCurrentUser(sessionId);
    if (session == null) return Result.error(404, "会话不存在");
    Map<String, Object> summary =
        request != null && request.getRequirementSummary() != null && !request.getRequirementSummary().isEmpty()
            ? request.getRequirementSummary()
            : latestAssistantPayload(sessionId);
    if (summary.isEmpty()) {
      summary = Map.of("summary", conversationText(sessionId));
    }
    session.setRequirementSummary(Jsons.stringify(summary));
    session.setStatus("requirement_confirmed");
    sessionMapper.updateById(session);
    return Result.success(detailMap(session));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> generatePlan(Long sessionId) {
    AiActivitySession session = sessionForCurrentUser(sessionId);
    if (session == null) return Result.error(404, "会话不存在");
    String promptInput =
        "已确认需求摘要：\n"
            + Objects.toString(session.getRequirementSummary(), "")
            + "\n\n完整对话：\n"
            + conversationText(sessionId);
    String content =
        deepSeekClientService.chat(
            "activity_plan", planPrompt(), List.of(Map.of("role", "user", "content", promptInput)));
    return savePlan(session, content, "plan_generated");
  }

  @Override
  public SseEmitter generatePlanStream(Long sessionId) {
    SseEmitter emitter = newEmitter();
    if (!StpUtil.isLogin()) {
      streamError(emitter, "请先登录");
      return emitter;
    }
    Integer userId = StpUtil.getLoginIdAsInt();
    Thread.startVirtualThread(
        () -> {
          try {
            AiActivitySession session = sessionForUser(sessionId, userId);
            if (session == null) {
              streamError(emitter, "会话不存在");
              return;
            }
            String promptInput =
                "已确认需求摘要：\n"
                    + Objects.toString(session.getRequirementSummary(), "")
                    + "\n\n完整对话：\n"
                    + conversationText(sessionId);
            emit(emitter, "phase", Map.of("message", "正在生成策划案"));
            String content =
                deepSeekClientService.chatStream(
                    "activity_plan",
                    planPrompt(),
                    List.of(Map.of("role", "user", "content", promptInput)),
                    chunk -> emit(emitter, "delta", Map.of("content", chunk)),
                    userId);
            emit(emitter, "phase", Map.of("message", "正在抽取申请表字段"));
            Result<Map<String, Object>> result = savePlan(session, content, "plan_generated");
            emit(emitter, "complete", Map.of("detail", result.getData()));
            emitter.complete();
          } catch (Exception e) {
            streamError(emitter, e.getMessage());
          }
        });
    return emitter;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> revisePlan(Long sessionId, RequestReviseAiActivityPlanDTO request) {
    AiActivitySession session = sessionForCurrentUser(sessionId);
    if (session == null) return Result.error(404, "会话不存在");
    AiActivityPlan latest = planMapper.selectLatestBySessionId(sessionId);
    if (latest == null) return Result.error(400, "请先生成策划案");
    String content =
        deepSeekClientService.chat(
            "activity_plan_revision",
            planPrompt(),
            List.of(
                Map.of(
                    "role",
                    "user",
                    "content",
                    "当前策划案：\n"
                        + latest.getContentMarkdown()
                        + "\n\n请按以下要求修改：\n"
                        + request.getInstruction())));
    return savePlan(session, content, "plan_generated");
  }

  @Override
  public SseEmitter revisePlanStream(Long sessionId, RequestReviseAiActivityPlanDTO request) {
    SseEmitter emitter = newEmitter();
    if (!StpUtil.isLogin()) {
      streamError(emitter, "请先登录");
      return emitter;
    }
    Integer userId = StpUtil.getLoginIdAsInt();
    Thread.startVirtualThread(
        () -> {
          try {
            AiActivitySession session = sessionForUser(sessionId, userId);
            if (session == null) {
              streamError(emitter, "会话不存在");
              return;
            }
            AiActivityPlan latest = planMapper.selectLatestBySessionId(sessionId);
            if (latest == null) {
              streamError(emitter, "请先生成策划案");
              return;
            }
            emit(emitter, "phase", Map.of("message", "正在按要求修改策划案"));
            String content =
                deepSeekClientService.chatStream(
                    "activity_plan_revision",
                    planPrompt(),
                    List.of(
                        Map.of(
                            "role",
                            "user",
                            "content",
                            "当前策划案：\n"
                                + latest.getContentMarkdown()
                                + "\n\n请按以下要求修改：\n"
                                + request.getInstruction())),
                    chunk -> emit(emitter, "delta", Map.of("content", chunk)),
                    userId);
            emit(emitter, "phase", Map.of("message", "正在更新申请表字段"));
            Result<Map<String, Object>> result = savePlan(session, content, "plan_generated");
            emit(emitter, "complete", Map.of("detail", result.getData()));
            emitter.complete();
          } catch (Exception e) {
            streamError(emitter, e.getMessage());
          }
        });
    return emitter;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> savePlanDraft(Long sessionId, RequestSaveAiActivityPlanDTO request) {
    AiActivitySession session = sessionForCurrentUser(sessionId);
    if (session == null) return Result.error(404, "会话不存在");
    if (request == null || request.getContentMarkdown() == null || request.getContentMarkdown().isBlank()) {
      return Result.error(400, "策划案内容不能为空");
    }
    return savePlan(session, request.getContentMarkdown(), "plan_generated");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> confirmPlan(Long sessionId) {
    AiActivitySession session = sessionForCurrentUser(sessionId);
    if (session == null) return Result.error(404, "会话不存在");
    AiActivityPlan latest = planMapper.selectLatestBySessionId(sessionId);
    if (latest == null) return Result.error(400, "请先生成策划案");
    latest.setStatus("confirmed");
    planMapper.updateById(latest);
    session.setConfirmedPlanId(latest.getId());
    session.setStatus("plan_confirmed");
    sessionMapper.updateById(session);
    return Result.success(detailMap(session));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Integer> createActivityDraft(Long sessionId) {
    AiActivitySession session = sessionForCurrentUser(sessionId);
    if (session == null) return Result.error(404, "会话不存在");
    AiActivityPlan plan = confirmedOrLatestPlan(session);
    if (plan == null) return Result.error(400, "请先确认策划案");
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    Map<String, Object> fields = Jsons.parseObject(plan.getStructuredFields());
    ClubActivity activity =
        ClubActivity.builder()
            .clubId(club.getId())
            .title(value(fields, "activityName", plan.getTitle()))
            .summary(value(fields, "activityPurpose", "AI 生成活动草稿"))
            .descriptionMarkdown(plan.getContentMarkdown())
            .activityAt(parseTimestamp(fields.get("activityDate")))
            .endAt(parseTimestamp(fields.get("endTime")))
            .location(value(fields, "location", "待补充"))
            .status("draft")
            .registrationRequired(false)
            .registrationFields("[]")
            .participationPoints(0L)
            .build();
    clubActivityMapper.insert(activity);
    session.setActivityId(activity.getId());
    sessionMapper.updateById(session);
    return Result.success(activity.getId(), "活动草稿已创建");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<List<Map<String, Object>>> generateDocuments(
      Long sessionId, RequestGenerateActivityDocumentsDTO request) {
    AiActivitySession session = sessionForCurrentUser(sessionId);
    if (session == null) return Result.error(404, "会话不存在");
    AiActivityPlan plan = confirmedOrLatestPlan(session);
    if (plan == null) return Result.error(400, "请先确认策划案");
    Map<String, Object> variables = normalizeActivityFields(Jsons.parseObject(plan.getStructuredFields()));
    if (request != null && request.getVariables() != null) variables.putAll(request.getVariables());
    variables = documentReadyVariables(variables);
    Map<String, Long> templateIds = request == null ? null : request.getTemplateIds();
    List<Map<String, Object>> generated = new ArrayList<>();
    for (String documentType : DOCUMENT_TYPES) {
      DocumentTemplate template =
          templateIds != null && templateIds.get(documentType) != null
              ? templateMapper.selectById(templateIds.get(documentType))
              : templateMapper.selectLatestEnabledByType(documentType);
      if (template == null) return Result.error(400, "缺少模板: " + documentType);
      List<String> missing = documentTemplateService.missingRequiredVariables(template, variables);
      if (!missing.isEmpty()) return Result.error(400, "模板 " + template.getTemplateName() + " 缺少变量: " + String.join(", ", missing));
      String fileName = safeName(value(variables, "activityName", plan.getTitle()) + "-" + documentType + "-" + System.currentTimeMillis() + ".docx");
      try {
        Path path = documentTemplateService.generateDocx(template, variables, fileName);
        GeneratedDocument document =
            GeneratedDocument.builder()
                .sessionId(sessionId)
                .planId(plan.getId())
                .templateId(template.getId())
                .documentType(documentType)
                .fileName(fileName)
                .filePath(path.toString())
                .filledVariables(Jsons.stringify(variables))
                .createdBy(StpUtil.getLoginIdAsInt())
                .build();
        generatedDocumentMapper.insert(document);
        generated.add(generatedMap(document));
      } catch (IOException e) {
        return Result.error(500, "文档生成失败: " + e.getMessage());
      }
    }
    session.setStatus("documents_generated");
    sessionMapper.updateById(session);
    return Result.success(generated);
  }

  private Result<Map<String, Object>> savePlan(AiActivitySession session, String content, String status) {
    Map<String, Object> fields = extractFields(session.getId(), content);
    AiActivityPlan plan =
        AiActivityPlan.builder()
            .sessionId(session.getId())
            .version(planMapper.nextVersion(session.getId()))
            .title(value(fields, "activityName", "AI 活动策划案"))
            .contentMarkdown(content)
            .structuredFields(Jsons.stringify(fields))
            .status("draft")
            .build();
    planMapper.insert(plan);
    session.setStatus(status);
    sessionMapper.updateById(session);
    return Result.success(detailMap(session));
  }

  private void streamRequirementResponse(SseEmitter emitter, Integer userId, AiActivitySession session) {
    try {
      emit(emitter, "phase", Map.of("message", "AI 正在澄清活动需求"));
      List<Map<String, String>> messages = buildMessages(session.getId());
      String response =
          deepSeekClientService.chatStream(
              "activity_requirement",
              requirementPrompt(),
              messages,
              chunk -> emit(emitter, "delta", Map.of("content", chunk)),
              userId);
      Map<String, Object> structured = parseJsonObject(response);
      messageMapper.insert(
          AiActivityMessage.builder()
              .sessionId(session.getId())
              .role("assistant")
              .content(response)
              .structuredPayload(structured.isEmpty() ? null : Jsons.stringify(structured))
              .build());
      touch(session);
      emit(
          emitter,
          "complete",
          Map.of("detail", detailMap(sessionMapper.selectById(session.getId()))));
      emitter.complete();
    } catch (Exception e) {
      streamError(emitter, e.getMessage());
    }
  }

  private Map<String, Object> extractFields(Long sessionId, String planContent) {
    String response =
        deepSeekClientService.chat(
            "activity_field_extract",
            extractPrompt(),
            List.of(Map.of("role", "user", "content", planContent)));
    Map<String, Object> parsed = parseJsonObject(response);
    if (!parsed.isEmpty()) {
      Map<String, Object> merged = latestAssistantPayload(sessionId);
      merged.putAll(parsed);
      String participantText = extractParticipantText(planContent);
      if (participantText != null) merged.putIfAbsent("expectedParticipants", participantText);
      return normalizeActivityFields(merged);
    }
    Map<String, Object> fallback = latestAssistantPayload(sessionId);
    fallback.putIfAbsent("activityName", titleFrom(planContent));
    fallback.putIfAbsent("activityPurpose", "见活动策划案");
    fallback.putIfAbsent("location", "待补充");
    String participantText = extractParticipantText(planContent);
    if (participantText != null) fallback.putIfAbsent("expectedParticipants", participantText);
    fallback.putIfAbsent("activityContentFull", planContent);
    return normalizeActivityFields(fallback);
  }

  private Map<String, Object> normalizeActivityFields(Map<String, Object> source) {
    Map<String, Object> fields = new LinkedHashMap<>(source);
    fields.putIfAbsent("clubName", "开放原子开源社团");
    fields.putIfAbsent("activityName", value(fields, "title", "待补充活动"));
    fields.putIfAbsent("activityCategory", "创新创业");
    fields.putIfAbsent("volunteerCategory", "志愿公益服务");
    fields.putIfAbsent("activityLevel", "校级");
    fields.putIfAbsent("targetCollege", "全部");
    fields.putIfAbsent("targetGrade", "全部");
    fields.putIfAbsent("practiceHours", "3");
    fields.putIfAbsent("needCheckout", "否");
    fields.putIfAbsent("needFieldCheckin", "否");
    putIfBlank(fields, "expectedParticipants", value(fields, "registrationQuota", value(fields, "targetAudience", "待补充")));
    putIfBlank(fields, "registrationQuota", value(fields, "expectedParticipants", "待补充"));
    fields.putIfAbsent("volunteerCount", "待补充");
    fields.putIfAbsent("principalName", "待补充");
    fields.putIfAbsent("principalPhone", "待补充");
    fields.putIfAbsent("advisorName", "待补充");
    fields.putIfAbsent("checkinStudentId", "待补充");
    fields.putIfAbsent("contactText", value(fields, "principalName", "待补充") + "，联系电话 " + value(fields, "principalPhone", "待补充"));
    fields.putIfAbsent("registrationMethod", "报名制（报名不需审核，人满截止）");
    fields.putIfAbsent("volunteerRegistrationMethod", "报名制（报名需审核，人满截止）");
    fields.putIfAbsent("registrationDateRange", "待补充");
    fields.putIfAbsent("activityDateRange", value(fields, "activityDate", "待补充"));
    fields.putIfAbsent("activityDateShort", value(fields, "activityDate", "待补充"));
    fields.putIfAbsent("activityTimeShort", value(fields, "startTime", "待补充") + "至" + value(fields, "endTime", "待补充"));
    fields.putIfAbsent("activitySummary", value(fields, "activityPurpose", "见活动策划案"));
    fields.putIfAbsent("activityIntroduction", value(fields, "activityBackground", "见活动策划案"));
    fields.putIfAbsent("activityHighlights", value(fields, "expectedEffect", "见活动策划案"));
    fields.putIfAbsent("volunteerActivitySummary", value(fields, "activityName", "本次活动") + "顺利开展的志愿服务保障。");
    fields.putIfAbsent("volunteerResponsibilities", value(fields, "volunteerRequirements", "协助完成签到引导、现场秩序维护、活动讲解、物资整理和突发情况协助处理等工作。"));
    fields.putIfAbsent("activityContentFull", value(fields, "activityContent", value(fields, "activityPurpose", "见活动策划案")));
    return fields;
  }

  private Map<String, Object> documentReadyVariables(Map<String, Object> source) {
    Map<String, Object> variables = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      Object raw = entry.getValue();
      variables.put(entry.getKey(), raw instanceof String text ? toDocText(text) : raw);
    }
    variables.put("activitySummary", summaryText(value(variables, "activitySummary", value(variables, "activityPurpose", "")), 160));
    variables.put("activityIntroduction", summaryText(value(variables, "activityIntroduction", value(variables, "activityBackground", "")), 260));
    variables.put("activityHighlights", summaryText(value(variables, "activityHighlights", value(variables, "expectedEffect", "")), 220));
    variables.put("volunteerActivitySummary", summaryText(value(variables, "volunteerActivitySummary", ""), 180));
    variables.put("volunteerResponsibilities", bulletText(value(variables, "volunteerResponsibilities", ""), 6));
    variables.put("activityContentFull", bulletText(value(variables, "activityContentFull", ""), 12));
    return variables;
  }

  private String toDocText(String value) {
    if (value == null || value.isBlank()) return value;
    String text = value.replace("\r\n", "\n").replace('\r', '\n');
    text = text.replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "");
    text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
    text = text.replaceAll("__([^_]+)__", "$1");
    text = text.replaceAll("`([^`]+)`", "$1");
    text = text.replaceAll("(?m)^\\s*[-*+]\\s+", "");
    text = text.replaceAll("(?m)^\\s*\\d+[.)、]\\s+", "");
    text = text.replaceAll("\\[([^\\]]+)]\\([^)]*\\)", "$1");
    text = text.replaceAll("[*_`>#|]+", "");
    text = text.replaceAll("(?m)^\\s*-{3,}\\s*$", "");
    text = text.replaceAll("[ \\t]+", " ");
    text = text.replaceAll("\\n{3,}", "\n\n");
    return text.trim();
  }

  private String summaryText(String value, int maxLength) {
    String text = toDocText(value);
    if (text == null || text.isBlank() || "见活动策划案".equals(text)) return "待补充";
    text = text.replaceAll("\\n+", "，").replaceAll("，{2,}", "，");
    return text.length() <= maxLength ? text : text.substring(0, maxLength) + "。";
  }

  private String bulletText(String value, int maxItems) {
    String text = toDocText(value);
    if (text == null || text.isBlank() || "见活动策划案".equals(text)) return "待补充";
    String[] lines = text.split("\\n+");
    List<String> items = new ArrayList<>();
    for (String line : lines) {
      String item = line.trim();
      if (item.isBlank()) continue;
      if (item.length() > 120) item = item.substring(0, 120) + "。";
      items.add(item);
      if (items.size() >= maxItems) break;
    }
    if (items.isEmpty()) return summaryText(text, 300);
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) builder.append("\n");
      builder.append(i + 1).append("、").append(items.get(i));
    }
    return builder.toString();
  }

  private void putIfBlank(Map<String, Object> fields, String key, String fallback) {
    Object current = fields.get(key);
    if (current == null || String.valueOf(current).isBlank()) fields.put(key, fallback);
  }

  private String extractParticipantText(String text) {
    if (text == null || text.isBlank()) return null;
    Matcher matcher = PARTICIPANT_PATTERN.matcher(text);
    if (matcher.find()) return matcher.group(1) + "人";
    return null;
  }

  private AiActivitySession sessionForCurrentUser(Long sessionId) {
    if (sessionId == null || !StpUtil.isLogin()) return null;
    AiActivitySession session = sessionMapper.selectById(sessionId);
    if (session == null) return null;
    Integer currentUserId = StpUtil.getLoginIdAsInt();
    if (!currentUserId.equals(session.getUserId()) && !StpUtil.hasPermission("activity:create")) return null;
    return session;
  }

  private AiActivitySession sessionForUser(Long sessionId, Integer userId) {
    if (sessionId == null || userId == null) return null;
    AiActivitySession session = sessionMapper.selectById(sessionId);
    if (session == null) return null;
    return userId.equals(session.getUserId()) ? session : null;
  }

  private SseEmitter newEmitter() {
    return new SseEmitter(180_000L);
  }

  private void emit(SseEmitter emitter, String eventName, Map<String, Object> data) throws IOException {
    emitter.send(SseEmitter.event().name(eventName).data(Jsons.stringify(data)));
  }

  private void streamError(SseEmitter emitter, String message) {
    try {
      emit(
          emitter,
          "error",
          Map.of("message", message == null || message.isBlank() ? "AI 流式输出失败" : message));
    } catch (Exception ignored) {
      // 客户端可能已经断开，此处只需要结束服务端 emitter。
    } finally {
      emitter.complete();
    }
  }

  private Map<String, Object> detailMap(AiActivitySession session) {
    Map<String, Object> map = sessionMap(session);
    map.put("messages", messageMapper.selectBySessionId(session.getId()));
    map.put("plans", planMapper.selectBySessionId(session.getId()));
    map.put("documents", generatedDocumentMapper.selectBySessionId(session.getId()).stream().map(this::generatedMap).toList());
    return map;
  }

  private Map<String, Object> sessionMap(AiActivitySession session) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", session.getId());
    map.put("clubId", session.getClubId());
    map.put("userId", session.getUserId());
    map.put("title", session.getTitle());
    map.put("status", session.getStatus());
    map.put("requirementSummary", Jsons.parseObject(session.getRequirementSummary()));
    map.put("confirmedPlanId", session.getConfirmedPlanId());
    map.put("activityId", session.getActivityId());
    map.put("createdAt", session.getCreatedAt());
    map.put("updatedAt", session.getUpdatedAt());
    return map;
  }

  private Map<String, Object> generatedMap(GeneratedDocument document) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", document.getId());
    map.put("sessionId", document.getSessionId());
    map.put("planId", document.getPlanId());
    map.put("templateId", document.getTemplateId());
    map.put("documentType", document.getDocumentType());
    map.put("fileName", document.getFileName());
    map.put("createdAt", document.getCreatedAt());
    return map;
  }

  private List<Map<String, String>> buildMessages(Long sessionId) {
    return messageMapper.selectBySessionId(sessionId).stream()
        .map(item -> Map.of("role", item.getRole(), "content", readableMessageContent(item)))
        .toList();
  }

  private String readableMessageContent(AiActivityMessage message) {
    if (!"assistant".equals(message.getRole())) return message.getContent();
    Map<String, Object> payload = Jsons.parseObject(message.getStructuredPayload());
    if (payload.isEmpty()) return message.getContent();
    StringBuilder builder = new StringBuilder();
    appendReadableSection(builder, "当前理解", payload.get("summary"));
    appendReadableSection(builder, "建议", payload.get("suggestions"));
    appendReadableSection(builder, "待补充问题", payload.get("questions"));
    appendReadableSection(builder, "缺失信息", payload.get("missingFields"));
    String text = builder.toString().trim();
    return text.isBlank() ? message.getContent() : text;
  }

  private void appendReadableSection(StringBuilder builder, String title, Object value) {
    if (value == null) return;
    if (value instanceof List<?> list) {
      if (list.isEmpty()) return;
      builder.append(title).append("：\n");
      for (Object item : list) {
        if (item != null && !String.valueOf(item).isBlank()) {
          builder.append("- ").append(item).append("\n");
        }
      }
      return;
    }
    String text = String.valueOf(value);
    if (!text.isBlank()) builder.append(title).append("：").append(text).append("\n");
  }

  private Map<String, Object> latestAssistantPayload(Long sessionId) {
    List<AiActivityMessage> messages = messageMapper.selectBySessionId(sessionId);
    for (int i = messages.size() - 1; i >= 0; i--) {
      AiActivityMessage message = messages.get(i);
      if ("assistant".equals(message.getRole())) {
        Map<String, Object> payload = Jsons.parseObject(message.getStructuredPayload());
        if (!payload.isEmpty()) return new LinkedHashMap<>(payload);
      }
    }
    return new LinkedHashMap<>();
  }

  private String conversationText(Long sessionId) {
    StringBuilder builder = new StringBuilder();
    for (AiActivityMessage message : messageMapper.selectBySessionId(sessionId)) {
      builder.append(message.getRole()).append(": ").append(readableMessageContent(message)).append("\n\n");
    }
    return builder.toString();
  }

  private AiActivityPlan confirmedOrLatestPlan(AiActivitySession session) {
    if (session.getConfirmedPlanId() != null) return planMapper.selectById(session.getConfirmedPlanId());
    return planMapper.selectLatestBySessionId(session.getId());
  }

  private Club defaultClub() {
    return clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
  }

  private void touch(AiActivitySession session) {
    session.setUpdatedAt(Times.now());
    sessionMapper.updateById(session);
  }

  private Map<String, Object> parseJsonObject(String value) {
    if (value == null || value.isBlank()) return new LinkedHashMap<>();
    String text = value.trim();
    int start = text.indexOf('{');
    int end = text.lastIndexOf('}');
    if (start >= 0 && end > start) text = text.substring(start, end + 1);
    try {
      return OBJECT_MAPPER.readValue(text, new TypeReference<LinkedHashMap<String, Object>>() {});
    } catch (Exception e) {
      return new LinkedHashMap<>();
    }
  }

  private Timestamp parseTimestamp(Object value) {
    if (value == null || String.valueOf(value).isBlank() || String.valueOf(value).contains("待补充")) return null;
    try {
      return Times.parseTimestamp(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  private String value(Map<String, Object> map, String key, String fallback) {
    Object value = map.get(key);
    String text = value == null ? null : String.valueOf(value);
    return text == null || text.isBlank() || "null".equalsIgnoreCase(text) ? fallback : text;
  }

  private String titleFrom(String value) {
    if (value == null || value.isBlank()) return "AI 活动策划";
    String text = value.replaceAll("[\\r\\n]+", " ").trim();
    return text.length() > 32 ? text.substring(0, 32) : text;
  }

  private String safeName(String value) {
    return value.replaceAll("[\\\\/:*?\"<>|]+", "-");
  }

  private String requirementPrompt() {
    return """
        你是高校社团活动策划顾问。请帮助用户把模糊活动想法澄清成可执行需求。
        每轮最多问 5 个关键问题，优先确认活动对象、时间、地点、人数、负责人、预算、志愿者和安全事项。
        必须返回 JSON，不要输出 Markdown。字段：
        summary: 当前理解；
        suggestions: 字符串数组，活动建议；
        questions: 字符串数组，待补充问题；
        options: 可选方案对象；
        missingFields: 缺失字段数组。
        """;
  }

  private String planPrompt() {
    return """
        你是高校社团活动策划案撰写助手。请生成正式、完整、可提交审批的活动策划案。
        章节必须包含：活动名称、活动背景、活动目的与意义、活动主题、活动时间、活动地点、活动对象、主办单位/承办单位、活动流程、人员分工、宣传方案、报名方式、志愿者需求、物资清单、经费预算、风险预案、应急处理、预期效果。
        不得编造具体姓名、电话、预算金额；缺失信息标注“待补充”。
        输出 Markdown。
        """;
  }

  private String extractPrompt() {
    return """
        你是活动文档字段抽取助手。请从活动策划案中抽取字段，只返回 JSON。
        字段包括：
        activityName, activityTheme, activityBackground, activityPurpose, activityDate, startTime, endTime,
        location, targetAudience, expectedParticipants, organizer, coOrganizer, advisorName, principalName,
        principalPhone, volunteerCount, volunteerRequirements, budgetTotal, budgetDetails, safetyPlan, emergencyPlan,
        clubName, activityCategory, volunteerCategory, activityLevel, activityDateRange, registrationDateRange,
        targetCollege, targetGrade, practiceHours, needCheckout, needFieldCheckin, registrationQuota,
        contactText, registrationMethod, volunteerRegistrationMethod, checkinStudentId, activityDateShort,
        activityTimeShort, activitySummary, activityIntroduction, activityHighlights, volunteerActivitySummary,
        volunteerResponsibilities, activityContentFull, expectedEffect。
        缺失字段返回空字符串，不要编造。
        """;
  }
}
