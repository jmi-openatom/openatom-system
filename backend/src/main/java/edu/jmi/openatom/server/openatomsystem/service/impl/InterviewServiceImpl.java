package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestInterviewFeedbackDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewFeedback;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewInterviewer;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewFeedbackMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewInterviewerMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.service.InterviewService;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.util.List;
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
  private final MembershipApplicationMapper applicationMapper;
  private final NotificationService notificationService;

  @Override
  public ApiResponse<List<Interview>> list(Integer campaignId, Integer applicationId, Integer interviewerId, String status) {
    List<Integer> applicationIds = null;
    if (campaignId != null) {
      applicationIds = applicationMapper.selectByCampaignId(campaignId).stream()
          .map(MembershipApplication::getId).toList();
      if (applicationIds.isEmpty()) return ApiResponse.success(List.of());
    }
    List<Integer> interviewIds = null;
    if (interviewerId != null) {
      interviewIds = interviewInterviewerMapper.selectByInterviewerId(interviewerId).stream()
          .map(InterviewInterviewer::getInterviewId).toList();
      if (interviewIds.isEmpty()) return ApiResponse.success(List.of());
    }
    return ApiResponse.success(interviewMapper.selectByConditions(applicationId, status, applicationIds, interviewIds));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> create(RequestCreateInterviewDTO request) {
    MembershipApplication application = applicationMapper.selectById(request.getApplicationId());
    if (application == null) return ApiResponse.error(404, "申请不存在");
    Interview interview = Interview.builder().applicationId(request.getApplicationId()).round(request.getRound())
        .scheduledStartAt(Times.parseTimestamp(request.getScheduledStartAt()))
        .scheduledEndAt(Times.parseTimestamp(request.getScheduledEndAt()))
        .location(request.getLocation()).mode(request.getMode()).status("pending").build();
    int row = interviewMapper.insert(interview);
    if (row <= 0) return ApiResponse.error("面试创建失败");
    bindInterviewers(interview.getId(), request.getInterviewerIds());
    application.setStatus("interview_scheduled");
    applicationMapper.updateById(application);
    notificationService.create(RequestCreateNotificationDTO.builder().title("面试安排通知")
        .content(String.format("您好，您的入会申请已安排面试。\n时间：%s 至 %s\n地点：%s\n形式：%s\n请准时参加！",
            request.getScheduledStartAt(), request.getScheduledEndAt(), request.getLocation(),
            "offline".equals(request.getMode()) ? "线下面试" : "线上面试"))
        .type("activity").receiverUserIds(List.of(application.getUserId())).build());
    return ApiResponse.success("面试创建成功");
  }

  @Override
  public ApiResponse<Interview> detail(Integer interviewId) {
    Interview interview = findInterview(interviewId);
    return interview == null ? ApiResponse.error(404, "面试不存在") : ApiResponse.success(interview);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> update(Integer interviewId, RequestUpdateInterviewDTO request) {
    Interview interview = findInterview(interviewId);
    if (interview == null) return ApiResponse.error(404, "面试不存在");
    if (request.getRound() != null) interview.setRound(request.getRound());
    if (request.getScheduledStartAt() != null) interview.setScheduledStartAt(Times.parseTimestamp(request.getScheduledStartAt()));
    if (request.getScheduledEndAt() != null) interview.setScheduledEndAt(Times.parseTimestamp(request.getScheduledEndAt()));
    if (request.getLocation() != null) interview.setLocation(request.getLocation());
    if (request.getMode() != null) interview.setMode(request.getMode());
    if (request.getStatus() != null) interview.setStatus(request.getStatus());
    interviewMapper.updateById(interview);
    if (request.getInterviewerIds() != null) bindInterviewers(interviewId, request.getInterviewerIds());
    return ApiResponse.success("面试更新成功");
  }

  @Override
  public ApiResponse<String> confirm(Integer interviewId) { return changeStatus(interviewId, "confirmed", "面试确认成功"); }

  @Override
  public ApiResponse<String> feedback(Integer interviewId, RequestInterviewFeedbackDTO request) {
    Interview interview = findInterview(interviewId);
    if (interview == null) return ApiResponse.error(404, "面试不存在");
    if ("completed".equals(interview.getStatus())) return ApiResponse.error(422, "面试已完成，无法提交反馈");
    interviewFeedbackMapper.insert(InterviewFeedback.builder().interviewId(interviewId)
        .interviewerId(StpUtil.getLoginIdAsInt()).scores(Jsons.stringify(request.getScores()))
        .suggestion(request.getSuggestion()).comment(request.getComment()).build());
    return ApiResponse.success("面试反馈提交成功");
  }

  @Override
  public ApiResponse<String> complete(Integer interviewId) {
    Interview interview = findInterview(interviewId);
    if (interview == null) return ApiResponse.error(404, "面试不存在");
    if ("completed".equals(interview.getStatus())) return ApiResponse.error(422, "面试已完成，无法重复完成");
    if ("pending".equals(interview.getStatus())) return ApiResponse.error(422, "请先确认面试再进行完成操作");
    interview.setStatus("completed");
    interviewMapper.updateById(interview);
    MembershipApplication application = applicationMapper.selectById(interview.getApplicationId());
    if (application != null) { application.setStatus("interviewed"); applicationMapper.updateById(application); }
    return ApiResponse.success("面试已完成");
  }

  @Override
  public ApiResponse<List<InterviewFeedback>> getFeedbacks(Integer interviewId) {
    return ApiResponse.success(interviewFeedbackMapper.selectByInterviewId(interviewId));
  }

  private ApiResponse<String> changeStatus(Integer interviewId, String status, String message) {
    Interview interview = findInterview(interviewId);
    if (interview == null) return ApiResponse.error(404, "面试不存在");
    interview.setStatus(status); interviewMapper.updateById(interview);
    return ApiResponse.success(message);
  }

  private void bindInterviewers(Integer interviewId, List<Integer> interviewerIds) {
    interviewInterviewerMapper.deleteByInterviewId(interviewId);
    if (interviewerIds == null) return;
    for (Integer interviewerId : interviewerIds.stream().distinct().toList()) {
      interviewInterviewerMapper.insert(InterviewInterviewer.builder().interviewId(interviewId).interviewerId(interviewerId).build());
    }
  }

  private Interview findInterview(Integer interviewId) { return interviewId == null ? null : interviewMapper.selectById(interviewId); }
}
