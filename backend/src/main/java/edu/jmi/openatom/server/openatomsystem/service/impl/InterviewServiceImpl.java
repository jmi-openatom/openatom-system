package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestInterviewFeedbackDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewFeedback;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewFeedbackRevision;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewEvaluationTemplate;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewInterviewer;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewFeedbackMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewFeedbackRevisionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewEvaluationTemplateMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewInterviewerMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewQueueStateMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.service.InterviewService;
import edu.jmi.openatom.server.openatomsystem.service.MailBroadcastPlanner;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.sql.Timestamp;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面试管理实现类
 *
 * <p>负责面试的创建, 更新, 确认, 完成, 反馈提交以及面试官关联绑定等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {
  private final InterviewMapper interviewMapper;
  private final InterviewInterviewerMapper interviewInterviewerMapper;
  private final InterviewFeedbackMapper interviewFeedbackMapper;
  private final InterviewFeedbackRevisionMapper feedbackRevisionMapper;
  private final InterviewEvaluationTemplateMapper templateMapper;
  private final InterviewQueueStateMapper queueStateMapper;
  private final MembershipApplicationMapper applicationMapper;
  private final NotificationService notificationService;
  private final MailBroadcastPlanner mailBroadcastPlanner;

  @Override
  public Result<List<Interview>> list(Integer campaignId, Integer applicationId, Integer interviewerId, String status) {
    List<Integer> applicationIds = null;
    if (campaignId != null) {
      applicationIds = applicationMapper.selectByCampaignId(campaignId).stream()
          .map(MembershipApplication::getId).toList();
      if (applicationIds.isEmpty()) return Result.success(List.of());
    }
    List<Integer> interviewIds = null;
    if (interviewerId != null) {
      interviewIds = interviewInterviewerMapper.selectByInterviewerId(interviewerId).stream()
          .map(InterviewInterviewer::getInterviewId).toList();
      if (interviewIds.isEmpty()) return Result.success(List.of());
    }
    return Result.success(interviewMapper.selectByConditions(applicationId, status, applicationIds, interviewIds));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> create(RequestCreateInterviewDTO request) {
    MembershipApplication application = applicationMapper.selectById(request.getApplicationId());
    if (application == null) return Result.error(404, "申请不存在");
    Interview interview = Interview.builder().applicationId(request.getApplicationId()).round(request.getRound())
        .scheduledStartAt(Times.parseTimestamp(request.getScheduledStartAt()))
        .scheduledEndAt(Times.parseTimestamp(request.getScheduledEndAt()))
        .location(request.getLocation()).mode(request.getMode()).status("pending").build();
    int row = interviewMapper.insert(interview);
    if (row <= 0) return Result.error("面试创建失败");
    bindInterviewers(interview.getId(), request.getInterviewerIds());
    application.setStatus("interview_scheduled");
    applicationMapper.updateById(application);
    notificationService.create(RequestCreateNotificationDTO.builder().title("面试安排通知")
        .content(String.format("您好，您的入会申请已安排面试。\n时间：%s 至 %s\n地点：%s\n形式：%s\n请准时参加！",
            request.getScheduledStartAt(), request.getScheduledEndAt(), request.getLocation(),
            "offline".equals(request.getMode()) ? "线下面试" : "线上面试"))
        .type("activity").receiverUserIds(List.of(application.getUserId())).build());
    mailBroadcastPlanner.enqueueUserMail(
        "broadcast_interview_" + interview.getId(),
        application.getUserId(),
        "interview",
        "面试安排通知",
        String.format("您好，您的入会申请已安排面试。\n时间：%s 至 %s\n地点：%s\n形式：%s\n请准时参加！",
            request.getScheduledStartAt(), request.getScheduledEndAt(), request.getLocation(),
            "offline".equals(request.getMode()) ? "线下面试" : "线上面试"));
    return Result.success("面试创建成功");
  }

  @Override
  public Result<Interview> detail(Integer interviewId) {
    Interview interview = findInterview(interviewId);
    return interview == null ? Result.error(404, "面试不存在") : Result.success(interview);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> update(Integer interviewId, RequestUpdateInterviewDTO request) {
    Interview interview = findInterview(interviewId);
    if (interview == null) return Result.error(404, "面试不存在");
    if (request.getRound() != null) interview.setRound(request.getRound());
    if (request.getScheduledStartAt() != null) interview.setScheduledStartAt(Times.parseTimestamp(request.getScheduledStartAt()));
    if (request.getScheduledEndAt() != null) interview.setScheduledEndAt(Times.parseTimestamp(request.getScheduledEndAt()));
    if (request.getLocation() != null) interview.setLocation(request.getLocation());
    if (request.getMode() != null) interview.setMode(request.getMode());
    if (request.getStatus() != null) interview.setStatus(request.getStatus());
    interviewMapper.updateById(interview);
    if (request.getInterviewerIds() != null) bindInterviewers(interviewId, request.getInterviewerIds());
    return Result.success("面试更新成功");
  }

  @Override
  public Result<String> confirm(Integer interviewId) { return changeStatus(interviewId, "confirmed", "面试确认成功"); }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> feedback(Integer interviewId, RequestInterviewFeedbackDTO request) {
    return saveFeedback(interviewId, request, "submitted", false);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> saveFeedbackDraft(Integer interviewId, RequestInterviewFeedbackDTO request) {
    return saveFeedback(interviewId, request, "draft", false);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> submitFeedback(Integer interviewId, RequestInterviewFeedbackDTO request) {
    return saveFeedback(interviewId, request, "submitted", true);
  }

  private Result<String> saveFeedback(Integer interviewId, RequestInterviewFeedbackDTO request,
      String status, boolean validateTemplate) {
    Interview interview = findInterview(interviewId);
    if (interview == null) return Result.error(404, "面试不存在");
    int userId = StpUtil.getLoginIdAsInt();
    if (!isAssigned(interviewId, userId)) return Result.error(403, "您不是本场面试官");
    InterviewFeedback feedback = interviewFeedbackMapper.selectLatest(interviewId, userId);
    if ("completed".equals(interview.getStatus()) && (feedback == null || !"submitted".equals(feedback.getStatus()))) {
      return Result.error(422, "面试已完成，无法提交反馈");
    }
    InterviewEvaluationTemplate template = resolveTemplate(interview, request.getTemplateId());
    if (validateTemplate) {
      Result<String> validation = validateFeedback(template, request);
      if (validation != null) return validation;
    }
    Timestamp now = new Timestamp(System.currentTimeMillis());
    if (feedback == null) {
      feedback = InterviewFeedback.builder().interviewId(interviewId).interviewerId(userId).build();
    }
    feedback.setTemplateId(template == null ? request.getTemplateId() : template.getId());
    feedback.setTemplateVersion(template == null ? null : template.getVersion());
    feedback.setScores(Jsons.stringify(request.getScores()));
    feedback.setDetails(Jsons.stringify(request.getDetails()));
    feedback.setSuggestion(request.getSuggestion()); feedback.setComment(request.getComment());
    feedback.setStatus(status); feedback.setWithdrawnAt(null);
    feedback.setSubmittedAt("submitted".equals(status) ? now : null);
    if (feedback.getId() == null) interviewFeedbackMapper.insert(feedback);
    else interviewFeedbackMapper.updateById(feedback);
    saveRevision(feedback, status.equals("submitted") ? "submit" : "save");
    if ("submitted".equals(status)) completeWhenEveryoneSubmitted(interview);
    return Result.success("submitted".equals(status) ? "评价已提交" : "草稿已保存");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> withdrawFeedback(Integer interviewId) {
    Interview interview = findInterview(interviewId);
    if (interview == null) return Result.error(404, "面试不存在");
    int userId = StpUtil.getLoginIdAsInt();
    if (!isAssigned(interviewId, userId)) return Result.error(403, "您不是本场面试官");
    var queueState = queueStateMapper.selectByInterviewId(interviewId);
    if (queueState != null && "completed".equals(queueState.getStatus())) {
      return Result.error(422, "该面试间已叫下一位，评价已锁定，不能再撤回");
    }
    InterviewFeedback feedback = interviewFeedbackMapper.selectLatest(interviewId, userId);
    if (feedback == null || !"submitted".equals(feedback.getStatus())) return Result.error(422, "没有可撤回的已提交评价");
    feedback.setStatus("draft"); feedback.setSubmittedAt(null);
    feedback.setWithdrawnAt(new Timestamp(System.currentTimeMillis()));
    interviewFeedbackMapper.updateById(feedback); saveRevision(feedback, "withdraw");
    if ("completed".equals(interview.getStatus())) {
      interview.setStatus("confirmed"); interviewMapper.updateById(interview);
      MembershipApplication app = applicationMapper.selectById(interview.getApplicationId());
      if (app != null && "interviewed".equals(app.getStatus())) {
        app.setStatus("interview_scheduled"); applicationMapper.updateById(app);
      }
    }
    return Result.success("评价已撤回，可继续修改");
  }

  @Override
  public Result<String> complete(Integer interviewId) {
    Interview interview = findInterview(interviewId);
    if (interview == null) return Result.error(404, "面试不存在");
    if ("completed".equals(interview.getStatus())) return Result.error(422, "面试已完成，无法重复完成");
    if ("pending".equals(interview.getStatus())) return Result.error(422, "请先确认面试再进行完成操作");
    interview.setStatus("completed");
    interviewMapper.updateById(interview);
    MembershipApplication application = applicationMapper.selectById(interview.getApplicationId());
    if (application != null) { application.setStatus("interviewed"); applicationMapper.updateById(application); }
    return Result.success("面试已完成");
  }

  @Override
  public Result<List<InterviewFeedback>> getFeedbacks(Integer interviewId) {
    return Result.success(interviewFeedbackMapper.selectByInterviewId(interviewId));
  }

  @Override
  public Result<List<InterviewFeedback>> getGroupFeedbacks(Integer interviewId) {
    int userId = StpUtil.getLoginIdAsInt();
    if (!isAssigned(interviewId, userId)) return Result.error(403, "您不是本场面试官");
    InterviewFeedback own = interviewFeedbackMapper.selectLatest(interviewId, userId);
    if (own == null || !"submitted".equals(own.getStatus())) return Result.error(422, "提交本人评价后才能查看组内评价");
    Map<Integer, InterviewFeedback> latest = new LinkedHashMap<>();
    interviewFeedbackMapper.selectByInterviewId(interviewId).forEach(f -> latest.putIfAbsent(f.getInterviewerId(), f));
    return Result.success(latest.values().stream().filter(f -> "submitted".equals(f.getStatus())).toList());
  }

  @Override
  public Result<List<InterviewFeedbackRevision>> getFeedbackRevisions(Integer interviewId) {
    if (findInterview(interviewId) == null) return Result.error(404, "面试不存在");
    return Result.success(feedbackRevisionMapper.selectByInterviewId(interviewId));
  }

  private Result<String> changeStatus(Integer interviewId, String status, String message) {
    Interview interview = findInterview(interviewId);
    if (interview == null) return Result.error(404, "面试不存在");
    interview.setStatus(status); interviewMapper.updateById(interview);
    return Result.success(message);
  }

  private void bindInterviewers(Integer interviewId, List<Integer> interviewerIds) {
    interviewInterviewerMapper.deleteByInterviewId(interviewId);
    if (interviewerIds == null) return;
    for (Integer interviewerId : interviewerIds.stream().distinct().toList()) {
      interviewInterviewerMapper.insert(InterviewInterviewer.builder().interviewId(interviewId).interviewerId(interviewerId).build());
    }
  }

  private boolean isAssigned(Integer interviewId, Integer userId) {
    return interviewInterviewerMapper.selectByInterviewId(interviewId).stream()
        .anyMatch(link -> Objects.equals(link.getInterviewerId(), userId));
  }

  private InterviewEvaluationTemplate resolveTemplate(Interview interview, Integer templateId) {
    if (templateId != null) return templateMapper.selectById(templateId);
    MembershipApplication app = applicationMapper.selectById(interview.getApplicationId());
    InterviewEvaluationTemplate template = app == null ? null : templateMapper.selectActive(app.getCampaignId());
    return template == null ? templateMapper.selectActive(null) : template;
  }

  @SuppressWarnings("unchecked")
  private Result<String> validateFeedback(InterviewEvaluationTemplate template, RequestInterviewFeedbackDTO request) {
    if (template == null) return Result.error(422, "未找到可用的评价模板");
    Map<String, Object> scores = request.getScores() == null ? Map.of() : request.getScores();
    Object rawDimensions = Jsons.parseObject(template.getSchemaJson()).get("dimensions");
    if (rawDimensions instanceof List<?> dimensions) {
      for (Object raw : dimensions) {
        if (!(raw instanceof Map<?, ?> dimension)) continue;
        String key = String.valueOf(dimension.get("key"));
        boolean required = !Boolean.FALSE.equals(dimension.get("required"));
        Object value = scores.get(key);
        if (required && (!(value instanceof Number number) || number.doubleValue() < 1 || number.doubleValue() > 5)) {
          return Result.error(422, String.valueOf(dimension.get("label")) + "尚未完成评分");
        }
      }
    }
    if (request.getSuggestion() == null || request.getSuggestion().isBlank()) return Result.error(422, "请选择录用建议");
    return null;
  }

  private void saveRevision(InterviewFeedback feedback, String action) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("templateId", feedback.getTemplateId()); snapshot.put("templateVersion", feedback.getTemplateVersion());
    snapshot.put("scores", Jsons.parseObject(feedback.getScores())); snapshot.put("details", Jsons.parseObject(feedback.getDetails()));
    snapshot.put("suggestion", feedback.getSuggestion()); snapshot.put("comment", feedback.getComment());
    snapshot.put("status", feedback.getStatus());
    feedbackRevisionMapper.insert(InterviewFeedbackRevision.builder().feedbackId(feedback.getId())
        .interviewId(feedback.getInterviewId()).interviewerId(feedback.getInterviewerId())
        .action(action).snapshotJson(Jsons.stringify(snapshot)).build());
  }

  private void completeWhenEveryoneSubmitted(Interview interview) {
    List<Integer> assigned = interviewInterviewerMapper.selectByInterviewId(interview.getId()).stream()
        .map(InterviewInterviewer::getInterviewerId).distinct().toList();
    boolean allSubmitted = !assigned.isEmpty() && assigned.stream().allMatch(id -> {
      InterviewFeedback feedback = interviewFeedbackMapper.selectLatest(interview.getId(), id);
      return feedback != null && "submitted".equals(feedback.getStatus());
    });
    if (!allSubmitted) return;
    interview.setStatus("completed"); interviewMapper.updateById(interview);
    MembershipApplication app = applicationMapper.selectById(interview.getApplicationId());
    if (app != null) { app.setStatus("interviewed"); applicationMapper.updateById(app); }
  }

  private Interview findInterview(Integer interviewId) { return interviewId == null ? null : interviewMapper.selectById(interviewId); }
}
