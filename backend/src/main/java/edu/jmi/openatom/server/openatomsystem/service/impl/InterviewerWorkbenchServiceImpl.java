package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.service.InterviewerWorkbenchService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewEvaluationTemplateVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewerWorkbenchItemVO;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterviewerWorkbenchServiceImpl implements InterviewerWorkbenchService {
  private final InterviewInterviewerMapper interviewerMapper;
  private final InterviewMapper interviewMapper;
  private final InterviewFeedbackMapper feedbackMapper;
  private final InterviewEvaluationTemplateMapper templateMapper;
  private final InterviewSessionMapper sessionMapper;
  private final InterviewRoomMapper roomMapper;
  private final MembershipApplicationMapper applicationMapper;
  private final UserMapper userMapper;
  private final ClubDepartmentMapper departmentMapper;

  @Override
  public Result<List<ResponseInterviewerWorkbenchItemVO>> list() {
    int userId = StpUtil.getLoginIdAsInt();
    List<Interview> interviews = interviewerMapper.selectByInterviewerId(userId).stream()
        .map(link -> interviewMapper.selectById(link.getInterviewId())).filter(Objects::nonNull)
        .filter(i -> !"draft".equals(i.getStatus()))
        .sorted(Comparator.comparing(Interview::getScheduledStartAt,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
    return Result.success(interviews.stream().map(i -> build(i, userId)).toList());
  }

  private ResponseInterviewerWorkbenchItemVO build(Interview interview, int userId) {
    MembershipApplication application = applicationMapper.selectById(interview.getApplicationId());
    User user = application == null ? null : userMapper.selectById(application.getUserId());
    InterviewSession session = interview.getSessionId() == null ? null : sessionMapper.selectById(interview.getSessionId());
    InterviewRoom room = interview.getRoomId() == null ? null : roomMapper.selectById(interview.getRoomId());
    Integer campaignId = application == null ? null : application.getCampaignId();
    InterviewEvaluationTemplate template = templateMapper.selectActive(campaignId);
    if (template == null) template = templateMapper.selectActive(null);
    InterviewFeedback own = feedbackMapper.selectLatest(interview.getId(), userId);
    List<Integer> assignedIds = interviewerMapper.selectByInterviewId(interview.getId()).stream()
        .map(InterviewInterviewer::getInterviewerId).distinct().toList();
    List<InterviewFeedback> latest = latestFeedbacks(interview.getId());
    int submitted = (int) latest.stream().filter(f -> "submitted".equals(f.getStatus())
        && assignedIds.contains(f.getInterviewerId())).count();
    boolean ownSubmitted = own != null && "submitted".equals(own.getStatus());
    List<Map<String, Object>> group = ownSubmitted ? latest.stream()
        .filter(f -> "submitted".equals(f.getStatus()) && !Objects.equals(f.getInterviewerId(), userId))
        .map(this::feedbackMap).toList() : List.of();
    return ResponseInterviewerWorkbenchItemVO.builder()
        .interviewId(interview.getId()).applicationId(interview.getApplicationId()).campaignId(campaignId)
        .clubId(application == null ? null : application.getClubId()).sessionId(interview.getSessionId())
        .sessionName(session == null ? null : session.getName()).roomId(interview.getRoomId())
        .roomName(room == null ? null : room.getName()).location(room == null ? interview.getLocation() : room.getLocation())
        .queueNumber(interview.getQueueNumber()).scheduledStartAt(interview.getScheduledStartAt())
        .scheduledEndAt(interview.getScheduledEndAt()).interviewStatus(interview.getStatus())
        .applicantName(user == null ? profileText(application, "realName", "姓名未填写") : user.getRealName())
        .studentId(user == null ? profileText(application, "studentId", null) : user.getStudentId())
        .college(user == null ? profileText(application, "college", null) : user.getCollege())
        .major(user == null ? profileText(application, "major", null) : user.getMajor())
        .grade(user == null ? profileText(application, "grade", null) : user.getGrade())
        .firstChoiceDepartmentName(departmentName(application == null ? null : application.getFirstChoiceDepartmentId()))
        .secondChoiceDepartmentName(departmentName(application == null ? null : application.getSecondChoiceDepartmentId()))
        .profile(application == null ? Map.of() : Jsons.parseObject(application.getProfile()))
        .template(ResponseInterviewEvaluationTemplateVO.from(template)).ownFeedback(own == null ? Map.of() : feedbackMap(own))
        .submittedCount(submitted).requiredCount(assignedIds.size()).groupFeedbacks(group).build();
  }

  private List<InterviewFeedback> latestFeedbacks(Integer interviewId) {
    Map<Integer, InterviewFeedback> latest = new LinkedHashMap<>();
    feedbackMapper.selectByInterviewId(interviewId).forEach(f -> latest.putIfAbsent(f.getInterviewerId(), f));
    return new ArrayList<>(latest.values());
  }

  private Map<String, Object> feedbackMap(InterviewFeedback feedback) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("id", feedback.getId()); value.put("interviewerId", feedback.getInterviewerId());
    value.put("templateId", feedback.getTemplateId()); value.put("scores", Jsons.parseObject(feedback.getScores()));
    value.put("details", Jsons.parseObject(feedback.getDetails())); value.put("suggestion", feedback.getSuggestion());
    value.put("comment", feedback.getComment()); value.put("status", feedback.getStatus());
    value.put("submittedAt", feedback.getSubmittedAt()); return value;
  }

  private String departmentName(Integer id) {
    ClubDepartment department = id == null ? null : departmentMapper.selectById(id);
    return department == null ? null : department.getName();
  }

  private String profileText(MembershipApplication app, String key, String fallback) {
    if (app == null) return fallback;
    Object value = Jsons.parseObject(app.getProfile()).get(key);
    return value == null ? fallback : String.valueOf(value);
  }
}
