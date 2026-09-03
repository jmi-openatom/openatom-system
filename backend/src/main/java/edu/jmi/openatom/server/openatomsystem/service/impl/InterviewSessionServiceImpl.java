package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAutoScheduleInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewInterviewer;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewRoom;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewRoomInterviewer;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewSession;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewInterviewerMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewRoomInterviewerMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewRoomMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewSessionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.InterviewSessionService;
import edu.jmi.openatom.server.openatomsystem.service.MailBroadcastPlanner;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewerOptionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewScheduleVO;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl implements InterviewSessionService {
  private final InterviewSessionMapper sessionMapper;
  private final InterviewRoomMapper roomMapper;
  private final InterviewRoomInterviewerMapper roomInterviewerMapper;
  private final InterviewMapper interviewMapper;
  private final InterviewInterviewerMapper interviewInterviewerMapper;
  private final MembershipApplicationMapper applicationMapper;
  private final UserMapper userMapper;
  private final NotificationService notificationService;
  private final MailBroadcastPlanner mailBroadcastPlanner;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseInterviewScheduleVO> autoSchedule(RequestAutoScheduleInterviewDTO request) {
    Timestamp start = Times.parseTimestamp(request.getScheduledStartAt());
    Timestamp end = Times.parseTimestamp(request.getScheduledEndAt());
    if (!end.after(start)) return Result.error(400, "结束时间必须晚于开始时间");

    List<Integer> applicationIds = request.getApplicationIds().stream().distinct().toList();
    if (applicationIds.size() != request.getApplicationIds().size()) {
      return Result.error(400, "候选人列表存在重复项");
    }
    List<MembershipApplication> applications = applicationMapper.selectBatchIds(applicationIds);
    Map<Integer, MembershipApplication> applicationMap = new HashMap<>();
    for (MembershipApplication application : applications) applicationMap.put(application.getId(), application);
    for (Integer id : applicationIds) {
      MembershipApplication application = applicationMap.get(id);
      if (application == null) return Result.error(404, "申请 " + id + " 不存在");
      if (!request.getCampaignId().equals(application.getCampaignId())) {
        return Result.error(400, "所选申请必须属于同一招新批次");
      }
      if (!"pre_screen_passed".equals(application.getStatus())) {
        return Result.error(422, "申请 " + id + " 当前状态不允许安排面试");
      }
    }

    Set<Integer> skipInterviewDepartmentIds = request.getSkipInterviewDepartmentIds() == null
        ? Set.of() : new HashSet<>(request.getSkipInterviewDepartmentIds());
    Map<Integer, User> applicants = loadApplicants(applications);
    List<MembershipApplication> skippedApplications = applications.stream()
        .filter(application -> application.getFirstChoiceDepartmentId() != null
            && skipInterviewDepartmentIds.contains(application.getFirstChoiceDepartmentId()))
        .toList();
    List<MembershipApplication> interviewApplications = applications.stream()
        .filter(application -> !skippedApplications.contains(application)).toList();
    List<ResponseInterviewScheduleVO.SkippedCandidate> skippedCandidates = skippedApplications.stream()
        .map(application -> ResponseInterviewScheduleVO.SkippedCandidate.builder()
            .applicationId(application.getId())
            .applicantName(applicantName(application, applicantFor(application, applicants)))
            .departmentId(application.getFirstChoiceDepartmentId()).build())
        .toList();

    if (interviewApplications.isEmpty()) {
      if (!Boolean.TRUE.equals(request.getPreviewOnly())) {
        skippedApplications.forEach(application -> {
          application.setStatus("interviewed");
          applicationMapper.updateById(application);
        });
      }
      return Result.success(ResponseInterviewScheduleVO.builder().sessionName(request.getName())
          .status(Boolean.TRUE.equals(request.getPreviewOnly()) ? "preview" : "no_interview")
          .totalCandidates(0).rooms(List.of()).assignments(List.of())
          .skippedCandidates(skippedCandidates).build());
    }

    int slotMinutes = request.getDurationMinutes() + request.getGapMinutes();
    long totalMinutes = Duration.between(start.toInstant(), end.toInstant()).toMinutes();
    int capacityPerRoom = (int) ((totalMinutes + request.getGapMinutes()) / slotMinutes);
    if (capacityPerRoom <= 0) return Result.error(400, "面试时间段不足以安排一场面试");
    if ((long) capacityPerRoom * request.getRooms().size() < interviewApplications.size()) {
      return Result.error(400, "面试间容量不足，请增加面试间或延长时间段");
    }

    Map<Integer, User> interviewerUsers = validateAndLoadInterviewers(request.getRooms());
    if (interviewerUsers == null) return Result.error(400, "面试官不存在、已停用或被分配到多个面试间");
    for (Integer interviewerId : interviewerUsers.keySet()) {
      if (interviewInterviewerMapper.countOverlapping(interviewerId, start, end) > 0) {
        return Result.error(409, "面试官 " + userName(interviewerUsers.get(interviewerId)) + " 与已有面试时间冲突");
      }
    }

    List<RoomState> roomStates = new ArrayList<>();
    for (int index = 0; index < request.getRooms().size(); index++) {
      roomStates.add(new RoomState(index, request.getRooms().get(index), capacityPerRoom, start.toInstant()));
    }

    List<ResponseInterviewScheduleVO.Assignment> assignments = new ArrayList<>();
    for (int index = 0; index < interviewApplications.size(); index++) {
      MembershipApplication application = interviewApplications.get(index);
      Integer applicationId = application.getId();
      Integer preferredRoomIndex = request.getRoomAssignments() == null
          ? null : request.getRoomAssignments().get(applicationId);
      RoomState target;
      if (preferredRoomIndex != null) {
        if (preferredRoomIndex < 0 || preferredRoomIndex >= roomStates.size()) {
          return Result.error(400, "申请 " + applicationId + " 的面试间选择无效");
        }
        target = roomStates.get(preferredRoomIndex);
        if (target.assigned >= target.capacity) {
          return Result.error(400, target.request.getName() + " 容量不足，请调整其他候选人");
        }
      } else {
        List<RoomState> departmentRooms = "first_choice_department".equals(request.getAssignmentStrategy())
            && application.getFirstChoiceDepartmentId() != null
            ? roomStates.stream().filter(room -> room.matchesDepartment(application.getFirstChoiceDepartmentId())).toList()
            : List.of();
        List<RoomState> availableDepartmentRooms = departmentRooms.stream()
            .filter(room -> room.assigned < room.capacity).toList();
        target = (availableDepartmentRooms.isEmpty() ? roomStates : availableDepartmentRooms).stream()
            .filter(room -> room.assigned < room.capacity)
            .min(Comparator.comparingDouble(RoomState::load)
                .thenComparing(room -> room.nextStart)
                .thenComparingInt(room -> room.index))
            .orElseThrow();
      }
      Instant slotStart = target.nextStart;
      Instant slotEnd = slotStart.plus(Duration.ofMinutes(request.getDurationMinutes()));
      if (hasCandidateConflict(applicationId, slotStart, slotEnd)) {
        return Result.error(409, "申请 " + applicationId + " 与已有面试时间冲突");
      }
      User applicant = applicantFor(application, applicants);
      String applicantName = applicantName(application, applicant);
      target.assigned++;
      target.nextStart = slotStart.plus(Duration.ofMinutes(slotMinutes));
      assignments.add(ResponseInterviewScheduleVO.Assignment.builder()
          .applicationId(applicationId).applicantName(applicantName)
          .roomIndex(target.index).roomName(target.request.getName())
          .queueNumber(index + 1).scheduledStartAt(Timestamp.from(slotStart))
          .scheduledEndAt(Timestamp.from(slotEnd)).build());
    }

    InterviewSession session = null;
    if (!Boolean.TRUE.equals(request.getPreviewOnly())) {
      session = InterviewSession.builder().campaignId(request.getCampaignId()).name(request.getName().trim())
          .round(request.getRound()).scheduledStartAt(start).scheduledEndAt(end)
          .durationMinutes(request.getDurationMinutes()).gapMinutes(request.getGapMinutes())
          .mode(blank(request.getMode()) ? "offline" : request.getMode()).status("draft")
          .createdBy(StpUtil.getLoginIdAsInt()).build();
      sessionMapper.insert(session);
      persistRoomsAndAssignments(session, roomStates, assignments, request);
      skippedApplications.forEach(application -> {
        application.setStatus("interviewed");
        applicationMapper.updateById(application);
      });
    }

    return Result.success(buildResponse(session, request.getName(), roomStates, assignments, interviewerUsers,
        skippedCandidates));
  }

  @Override
  public Result<List<InterviewSession>> list(Integer campaignId) {
    return Result.success(sessionMapper.selectList(new LambdaQueryWrapper<InterviewSession>()
        .eq(campaignId != null, InterviewSession::getCampaignId, campaignId)
        .orderByDesc(InterviewSession::getScheduledStartAt)));
  }

  @Override
  public Result<ResponseInterviewScheduleVO> detail(Integer sessionId) {
    InterviewSession session = sessionMapper.selectById(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    List<InterviewRoom> rooms = roomMapper.selectBySessionId(sessionId);
    List<Interview> interviews = interviewMapper.selectBySessionId(sessionId);
    List<InterviewRoomInterviewer> links = roomInterviewerMapper.selectByRoomIds(rooms.stream().map(InterviewRoom::getId).toList());
    Set<Integer> userIds = new HashSet<>();
    links.forEach(link -> userIds.add(link.getInterviewerId()));
    List<MembershipApplication> applications = applicationMapper.selectBatchIds(interviews.stream().map(Interview::getApplicationId).toList());
    applications.stream().map(MembershipApplication::getUserId).filter(id -> id != null).forEach(userIds::add);
    Map<Integer, User> users = loadUsers(userIds);
    Map<Integer, MembershipApplication> appMap = new HashMap<>();
    applications.forEach(app -> appMap.put(app.getId(), app));
    Map<Integer, List<Integer>> roomInterviewers = new HashMap<>();
    links.forEach(link -> roomInterviewers.computeIfAbsent(link.getRoomId(), ignored -> new ArrayList<>()).add(link.getInterviewerId()));
    List<ResponseInterviewScheduleVO.Room> roomRows = new ArrayList<>();
    for (InterviewRoom room : rooms) {
      List<ResponseInterviewScheduleVO.Interviewer> interviewerRows = roomInterviewers.getOrDefault(room.getId(), List.of()).stream()
          .map(id -> ResponseInterviewScheduleVO.Interviewer.builder().userId(id).name(userName(users.get(id))).build()).toList();
      int count = (int) interviews.stream().filter(item -> room.getId().equals(item.getRoomId())).count();
      roomRows.add(ResponseInterviewScheduleVO.Room.builder().roomId(room.getId()).name(room.getName())
          .location(room.getLocation()).assignedCount(count).interviewers(interviewerRows).build());
    }
    List<ResponseInterviewScheduleVO.Assignment> assignmentRows = interviews.stream().map(item -> {
      MembershipApplication app = appMap.get(item.getApplicationId());
      User user = app == null ? null : users.get(app.getUserId());
      InterviewRoom room = rooms.stream().filter(r -> r.getId().equals(item.getRoomId())).findFirst().orElse(null);
      String applicantName = user == null && app != null
          ? firstProfileValue(app.getProfile(), "applicantName", "name", "realName") : userName(user);
      return ResponseInterviewScheduleVO.Assignment.builder().interviewId(item.getId())
          .applicationId(item.getApplicationId()).applicantName(applicantName).roomId(item.getRoomId())
          .roomName(room == null ? item.getLocation() : room.getName()).queueNumber(item.getQueueNumber())
          .scheduledStartAt(item.getScheduledStartAt()).scheduledEndAt(item.getScheduledEndAt()).build();
    }).toList();
    return Result.success(ResponseInterviewScheduleVO.builder().sessionId(sessionId).sessionName(session.getName())
        .status(session.getStatus()).totalCandidates(interviews.size()).rooms(roomRows).assignments(assignmentRows).build());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> publish(Integer sessionId) {
    InterviewSession session = sessionMapper.selectById(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    if (!"draft".equals(session.getStatus())) return Result.error(422, "只有草稿场次可以发布");
    List<Interview> interviews = interviewMapper.selectBySessionId(sessionId);
    if (interviews.isEmpty()) return Result.error(422, "场次中没有面试安排");
    session.setStatus("published");
    sessionMapper.updateById(session);
    for (Interview interview : interviews) {
      interview.setStatus("pending");
      interviewMapper.updateById(interview);
      MembershipApplication application = applicationMapper.selectById(interview.getApplicationId());
      if (application != null) {
        application.setStatus("interview_scheduled");
        applicationMapper.updateById(application);
        if (application.getUserId() != null) {
          String content = String.format("您好，您的入会申请已安排面试。\n时间：%s 至 %s\n地点：%s\n请准时参加！",
              interview.getScheduledStartAt(), interview.getScheduledEndAt(), interview.getLocation());
          notificationService.create(RequestCreateNotificationDTO.builder().title("面试安排通知")
              .content(content).type("activity").receiverUserIds(List.of(application.getUserId())).build());
          mailBroadcastPlanner.enqueueUserMail("broadcast_interview_" + interview.getId(),
              application.getUserId(), "interview", "面试安排通知", content);
        }
      }
    }
    return Result.success("面试场次已发布");
  }

  @Override
  public Result<List<ResponseInterviewerOptionVO>> interviewerOptions(String keyword) {
    List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
        .eq(User::getUserStatus, UserStatus.ACTIVE)
        .and(!blank(keyword), query -> query.like(User::getRealName, keyword).or().like(User::getUserName, keyword))
        .orderByAsc(User::getRealName).last("LIMIT 200"));
    return Result.success(users.stream().map(user ->
        new ResponseInterviewerOptionVO(user.getId(), userName(user), null)).toList());
  }

  private Map<Integer, User> validateAndLoadInterviewers(List<RequestAutoScheduleInterviewDTO.Room> rooms) {
    Set<Integer> ids = new HashSet<>();
    for (RequestAutoScheduleInterviewDTO.Room room : rooms) {
      for (Integer id : room.getInterviewerIds()) if (id == null || !ids.add(id)) return null;
    }
    Map<Integer, User> users = loadUsers(ids);
    if (users.size() != ids.size()) return null;
    if (users.values().stream().anyMatch(user -> user.getUserStatus() != UserStatus.ACTIVE)) return null;
    return users;
  }

  private Map<Integer, User> loadApplicants(List<MembershipApplication> applications) {
    Set<Integer> ids = new HashSet<>();
    applications.stream().map(MembershipApplication::getUserId).filter(id -> id != null).forEach(ids::add);
    return loadUsers(ids);
  }

  private Map<Integer, User> loadUsers(Set<Integer> ids) {
    if (ids.isEmpty()) return Map.of();
    Map<Integer, User> users = new HashMap<>();
    userMapper.selectBatchIds(ids).forEach(user -> users.put(user.getId(), user));
    return users;
  }

  private boolean hasCandidateConflict(Integer applicationId, Instant start, Instant end) {
    return interviewMapper.selectList(new LambdaQueryWrapper<Interview>()
            .eq(Interview::getApplicationId, applicationId)).stream()
        .filter(item -> !"completed".equals(item.getStatus()))
        .anyMatch(item -> item.getScheduledStartAt().toInstant().isBefore(end)
            && item.getScheduledEndAt().toInstant().isAfter(start));
  }

  private void persistRoomsAndAssignments(InterviewSession session, List<RoomState> roomStates,
      List<ResponseInterviewScheduleVO.Assignment> assignments, RequestAutoScheduleInterviewDTO request) {
    for (RoomState state : roomStates) {
      InterviewRoom room = InterviewRoom.builder().sessionId(session.getId()).name(state.request.getName().trim())
          .location(state.request.getLocation()).sortOrder(state.index).build();
      roomMapper.insert(room);
      state.roomId = room.getId();
      for (Integer interviewerId : state.request.getInterviewerIds()) {
        roomInterviewerMapper.insert(InterviewRoomInterviewer.builder().roomId(room.getId())
            .interviewerId(interviewerId).build());
      }
    }
    for (ResponseInterviewScheduleVO.Assignment assignment : assignments) {
      RoomState room = roomStates.get(assignment.getRoomIndex());
      Interview interview = Interview.builder().sessionId(session.getId()).roomId(room.roomId)
          .queueNumber(assignment.getQueueNumber()).applicationId(assignment.getApplicationId())
          .round(request.getRound()).scheduledStartAt(assignment.getScheduledStartAt())
          .scheduledEndAt(assignment.getScheduledEndAt()).location(room.request.getLocation())
          .mode(blank(request.getMode()) ? "offline" : request.getMode()).status("draft").build();
      interviewMapper.insert(interview);
      assignment.setInterviewId(interview.getId());
      assignment.setRoomId(room.roomId);
      for (Integer interviewerId : room.request.getInterviewerIds()) {
        interviewInterviewerMapper.insert(InterviewInterviewer.builder().interviewId(interview.getId())
            .interviewerId(interviewerId).build());
      }
    }
  }

  private ResponseInterviewScheduleVO buildResponse(InterviewSession session, String sessionName,
      List<RoomState> states, List<ResponseInterviewScheduleVO.Assignment> assignments,
      Map<Integer, User> users, List<ResponseInterviewScheduleVO.SkippedCandidate> skippedCandidates) {
    List<ResponseInterviewScheduleVO.Room> rooms = states.stream().map(state ->
        ResponseInterviewScheduleVO.Room.builder().roomId(state.roomId).name(state.request.getName())
            .location(state.request.getLocation()).capacity(state.capacity).assignedCount(state.assigned)
            .interviewers(state.request.getInterviewerIds().stream().map(id ->
                ResponseInterviewScheduleVO.Interviewer.builder().userId(id).name(userName(users.get(id))).build()).toList())
            .build()).toList();
    return ResponseInterviewScheduleVO.builder().sessionId(session == null ? null : session.getId())
        .sessionName(sessionName).status(session == null ? "preview" : session.getStatus())
        .totalCandidates(assignments.size()).rooms(rooms).assignments(assignments)
        .skippedCandidates(skippedCandidates).build();
  }

  private String applicantName(MembershipApplication application, User applicant) {
    return applicant == null
        ? firstProfileValue(application.getProfile(), "applicantName", "name", "realName")
        : blank(applicant.getRealName()) ? applicant.getUserName() : applicant.getRealName();
  }

  private User applicantFor(MembershipApplication application, Map<Integer, User> applicants) {
    return application.getUserId() == null ? null : applicants.get(application.getUserId());
  }

  private String firstProfileValue(String profileJson, String... keys) {
    Map<String, Object> profile = Jsons.parseObject(profileJson);
    for (String key : keys) {
      Object value = profile.get(key);
      if (value != null && !value.toString().isBlank()) return value.toString();
    }
    return "匿名候选人";
  }

  private String userName(User user) {
    if (user == null) return "未知用户";
    return blank(user.getRealName()) ? user.getUserName() : user.getRealName();
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static final class RoomState {
    private final int index;
    private final RequestAutoScheduleInterviewDTO.Room request;
    private final int capacity;
    private int assigned;
    private Instant nextStart;
    private Integer roomId;

    private RoomState(int index, RequestAutoScheduleInterviewDTO.Room request, int capacity, Instant nextStart) {
      this.index = index;
      this.request = request;
      this.capacity = capacity;
      this.nextStart = nextStart;
    }

    private double load() {
      return capacity == 0 ? 1 : (double) assigned / capacity;
    }

    private boolean matchesDepartment(Integer departmentId) {
      return request.getPreferredDepartmentIds() != null
          && request.getPreferredDepartmentIds().contains(departmentId);
    }
  }
}
