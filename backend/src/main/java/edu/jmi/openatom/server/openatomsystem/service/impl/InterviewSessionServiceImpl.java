package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAutoScheduleInterviewDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAddInterviewCandidatesDTO;
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
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;
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
    Timestamp end = Timestamp.from(start.toInstant().plus(Duration.ofMinutes(request.getDurationMinutes())));

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
    Map<Integer, User> interviewerUsers = validateAndLoadInterviewers(request.getRooms());
    if (interviewerUsers == null) return Result.error(400, "面试官不存在、已停用或被分配到多个面试间");

    List<RoomState> roomStates = new ArrayList<>();
    for (int index = 0; index < request.getRooms().size(); index++) {
      roomStates.add(new RoomState(index, request.getRooms().get(index), start.toInstant()));
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
      } else {
        List<RoomState> departmentRooms = "first_choice_department".equals(request.getAssignmentStrategy())
            && application.getFirstChoiceDepartmentId() != null
            ? roomStates.stream().filter(room -> room.matchesDepartment(application.getFirstChoiceDepartmentId())).toList()
            : List.of();
        target = (departmentRooms.isEmpty() ? roomStates : departmentRooms).stream()
            .min(Comparator.comparingInt((RoomState room) -> room.assigned)
                .thenComparing(room -> room.nextStart)
                .thenComparingInt(room -> room.index))
            .orElseThrow();
      }
      Instant slotStart = target.nextStart;
      Instant slotEnd = slotStart.plus(Duration.ofMinutes(request.getDurationMinutes()));
      for (Integer interviewerId : target.request.getInterviewerIds()) {
        if (interviewInterviewerMapper.countOverlapping(interviewerId,
            Timestamp.from(slotStart), Timestamp.from(slotEnd)) > 0) {
          return Result.error(409, "面试官 " + userName(interviewerUsers.get(interviewerId)) + " 与已有面试时间冲突");
        }
      }
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

    end = assignments.stream().map(ResponseInterviewScheduleVO.Assignment::getScheduledEndAt)
        .max(Timestamp::compareTo).orElse(end);
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

    ResponseInterviewScheduleVO response = buildResponse(session, request.getName(), roomStates, assignments,
        interviewerUsers, skippedCandidates);
    response.setCampaignId(request.getCampaignId());
    response.setScheduledStartAt(start);
    response.setScheduledEndAt(end);
    return Result.success(response);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseInterviewScheduleVO> addCandidates(Integer sessionId,
      RequestAddInterviewCandidatesDTO request) {
    // Serialize additions and publishing so queue numbers and publication state stay consistent.
    InterviewSession session = sessionMapper.selectByIdForUpdate(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    if (!List.of("draft", "published").contains(session.getStatus())) {
      return Result.error(422, "只有草稿或已发布场次可以添加候选人");
    }
    List<Integer> ids = request.getApplicationIds();
    if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)
        || new HashSet<>(ids).size() != ids.size()) {
      return Result.error(400, "请选择候选人，且不能重复");
    }
    Map<Integer, MembershipApplication> applications = new HashMap<>();
    applicationMapper.selectBatchIds(ids).forEach(app -> applications.put(app.getId(), app));
    List<Interview> existing = interviewMapper.selectBySessionId(sessionId);
    Set<Integer> scheduledIds = existing.stream().map(Interview::getApplicationId).collect(Collectors.toSet());
    List<Interview> candidateInterviews = interviewMapper.selectByApplicationIds(ids);
    for (Integer id : ids) {
      MembershipApplication app = applications.get(id);
      if (app == null) return Result.error(404, "申请 " + id + " 不存在");
      if (!Objects.equals(session.getCampaignId(), app.getCampaignId())) {
        return Result.error(400, "所选申请必须属于当前场次的招新批次");
      }
      if (scheduledIds.contains(id) || candidateInterviews.stream().anyMatch(item ->
          id.equals(item.getApplicationId()) && Objects.equals(session.getRound(), item.getRound()))) {
        return Result.error(409, "申请 " + id + " 已安排本轮面试，请勿重复添加");
      }
      if (!"pre_screen_passed".equals(app.getStatus())) {
        return Result.error(422, "申请 " + id + " 当前状态不允许安排面试");
      }
    }
    List<InterviewRoom> rooms = roomMapper.selectBySessionId(sessionId);
    if (rooms.isEmpty()) return Result.error(422, "场次尚未配置面试间");
    if (request.getRoomId() != null && rooms.stream().noneMatch(room -> request.getRoomId().equals(room.getId()))) {
      return Result.error(400, "所选面试间不属于当前场次");
    }
    Map<Integer, List<Integer>> roomPeople = new HashMap<>();
    roomInterviewerMapper.selectByRoomIds(rooms.stream().map(InterviewRoom::getId).toList())
        .forEach(link -> roomPeople.computeIfAbsent(link.getRoomId(), ignored -> new ArrayList<>())
            .add(link.getInterviewerId()));
    Map<Integer, User> users = loadUsers(roomPeople.values().stream().flatMap(List::stream).collect(Collectors.toSet()));
    Map<Integer, Instant> nextStarts = new HashMap<>();
    for (InterviewRoom room : rooms) {
      Instant next = existing.stream().filter(item -> room.getId().equals(item.getRoomId()))
          .map(item -> item.getScheduledEndAt().toInstant().plus(Duration.ofMinutes(session.getGapMinutes())))
          .max(Instant::compareTo).orElse(session.getScheduledStartAt().toInstant());
      nextStarts.put(room.getId(), next);
    }
    int number = existing.stream().map(Interview::getQueueNumber).filter(Objects::nonNull)
        .max(Integer::compareTo).orElse(0);
    Map<Integer, User> applicants = loadApplicants(new ArrayList<>(applications.values()));
    List<ResponseInterviewScheduleVO.Assignment> assignments = new ArrayList<>();
    for (Integer id : ids) {
      InterviewRoom room = rooms.stream()
          .filter(item -> request.getRoomId() == null || request.getRoomId().equals(item.getId()))
          .min(Comparator.comparing(item -> nextStarts.get(item.getId()))).orElseThrow();
      List<Integer> interviewerIds = roomPeople.getOrDefault(room.getId(), List.of());
      if (interviewerIds.isEmpty() || interviewerIds.stream().anyMatch(person ->
          !users.containsKey(person) || users.get(person).getUserStatus() != UserStatus.ACTIVE)) {
        return Result.error(422, room.getName() + " 的面试官配置无效，请先编辑面试间");
      }
      Instant start = nextStarts.get(room.getId());
      Instant end = start.plus(Duration.ofMinutes(session.getDurationMinutes()));
      if (hasCandidateConflict(id, start, end)) return Result.error(409, "申请 " + id + " 与已有面试时间冲突");
      for (Integer person : interviewerIds) {
        if (interviewInterviewerMapper.countOverlapping(person, Timestamp.from(start), Timestamp.from(end)) > 0) {
          return Result.error(409, "面试官 " + userName(users.get(person)) + " 与已有面试时间冲突，请选择其他面试间");
        }
      }
      // Also guard against invalid legacy room groups sharing an interviewer within this batch.
      if (assignments.stream().anyMatch(item -> item.getScheduledStartAt().toInstant().isBefore(end)
          && item.getScheduledEndAt().toInstant().isAfter(start)
          && roomPeople.getOrDefault(item.getRoomId(), List.of()).stream().anyMatch(interviewerIds::contains))) {
        return Result.error(409, "面试间的面试官重复且时间冲突，请先编辑面试间");
      }
      MembershipApplication app = applications.get(id);
      assignments.add(ResponseInterviewScheduleVO.Assignment.builder().applicationId(id)
          .applicantName(applicantName(app, applicantFor(app, applicants))).roomId(room.getId())
          .roomName(room.getName()).queueNumber(++number).scheduledStartAt(Timestamp.from(start))
          .scheduledEndAt(Timestamp.from(end)).build());
      nextStarts.put(room.getId(), end.plus(Duration.ofMinutes(session.getGapMinutes())));
    }
    Timestamp end = assignments.stream().map(ResponseInterviewScheduleVO.Assignment::getScheduledEndAt)
        .max(Timestamp::compareTo).orElse(session.getScheduledEndAt());
    if (end.after(session.getScheduledEndAt())) session.setScheduledEndAt(end);
    if (Boolean.FALSE.equals(request.getPreviewOnly())) {
      // Validate the whole batch before writing, so business errors never leave a partial addition.
      for (ResponseInterviewScheduleVO.Assignment assignment : assignments) {
        InterviewRoom room = rooms.stream().filter(item -> item.getId().equals(assignment.getRoomId())).findFirst().orElseThrow();
        Interview interview = Interview.builder().sessionId(sessionId).roomId(room.getId())
            .applicationId(assignment.getApplicationId()).queueNumber(assignment.getQueueNumber())
            .round(session.getRound()).scheduledStartAt(assignment.getScheduledStartAt())
            .scheduledEndAt(assignment.getScheduledEndAt()).location(room.getLocation()).mode(session.getMode())
            .status("published".equals(session.getStatus()) ? "pending" : "draft").build();
        interviewMapper.insert(interview);
        assignment.setInterviewId(interview.getId());
        for (Integer person : roomPeople.get(room.getId())) {
          interviewInterviewerMapper.insert(InterviewInterviewer.builder().interviewId(interview.getId())
              .interviewerId(person).build());
        }
        if ("published".equals(session.getStatus())) {
          notifyScheduledCandidate(interview, applications.get(assignment.getApplicationId()));
        }
      }
      sessionMapper.updateById(session);
    }
    return Result.success(ResponseInterviewScheduleVO.builder().sessionId(sessionId)
        .campaignId(session.getCampaignId()).sessionName(session.getName()).status(session.getStatus())
        .scheduledStartAt(session.getScheduledStartAt()).scheduledEndAt(session.getScheduledEndAt())
        .totalCandidates(assignments.size()).assignments(assignments).build());
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
    return Result.success(ResponseInterviewScheduleVO.builder().sessionId(sessionId).campaignId(session.getCampaignId())
        .scheduledStartAt(session.getScheduledStartAt()).scheduledEndAt(session.getScheduledEndAt()).sessionName(session.getName())
        .status(session.getStatus()).totalCandidates(interviews.size()).rooms(roomRows).assignments(assignmentRows).build());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> publish(Integer sessionId) {
    InterviewSession session = sessionMapper.selectByIdForUpdate(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    if (!"draft".equals(session.getStatus())) return Result.error(422, "只有草稿场次可以发布");
    List<Interview> interviews = interviewMapper.selectBySessionId(sessionId);
    if (interviews.isEmpty()) return Result.error(422, "场次中没有面试安排");
    session.setStatus("published");
    sessionMapper.updateById(session);
    for (Interview interview : interviews) {
      interview.setStatus("pending");
      interviewMapper.updateById(interview);
      notifyScheduledCandidate(interview, applicationMapper.selectById(interview.getApplicationId()));
    }
    return Result.success("面试场次已发布");
  }

  private void notifyScheduledCandidate(Interview interview, MembershipApplication application) {
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

  @Override
  public Result<byte[]> exportSchedule(Integer sessionId) {
    Result<ResponseInterviewScheduleVO> result = detail(sessionId);
    if (result.getCode() != Result.SUCCESS_CODE) return Result.error(result.getCode(), result.getMessage());
    ResponseInterviewScheduleVO schedule = result.getData();
    Map<Integer, ResponseInterviewScheduleVO.Room> rooms = new HashMap<>();
    schedule.getRooms().forEach(room -> rooms.put(room.getRoomId(), room));
    List<InterviewInterviewer> links = new ArrayList<>();
    schedule.getAssignments().forEach(row -> links.addAll(interviewInterviewerMapper.selectByInterviewId(row.getInterviewId())));
    Map<Integer, User> users = loadUsers(links.stream().map(InterviewInterviewer::getInterviewerId).collect(Collectors.toSet()));
    DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(Times.BUSINESS_ZONE);
    StringBuilder csv = new StringBuilder("\uFEFF场次,编号,候选人,面试间,地点,开始时间（北京时间）,结束时间（北京时间）,面试官\r\n");
    for (ResponseInterviewScheduleVO.Assignment row : schedule.getAssignments()) {
      ResponseInterviewScheduleVO.Room room = rooms.get(row.getRoomId());
      String interviewers = links.stream().filter(link -> row.getInterviewId().equals(link.getInterviewId()))
          .map(link -> userName(users.get(link.getInterviewerId()))).collect(Collectors.joining("、"));
      String[] values = {schedule.getSessionName(), row.getQueueNumber() == null ? "" : row.getQueueNumber().toString(),
          row.getApplicantName(), row.getRoomName(), room == null ? "" : room.getLocation(),
          row.getScheduledStartAt() == null ? "" : time.format(row.getScheduledStartAt().toInstant()),
          row.getScheduledEndAt() == null ? "" : time.format(row.getScheduledEndAt().toInstant()), interviewers};
      csv.append(java.util.Arrays.stream(values).map(this::scheduleCsvCell).collect(Collectors.joining(","))).append("\r\n");
    }
    return Result.success(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private String scheduleCsvCell(String value) {
    String text = Objects.toString(value, "");
    // Quoting preserves commas/newlines; a text prefix prevents spreadsheet formula execution.
    if (text.stripLeading().matches("(?s)^[=+@-].*")) text = "'" + text;
    return "\"" + text.replace("\"", "\"\"") + "\"";
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
            .location(state.request.getLocation()).assignedCount(state.assigned)
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
    private int assigned;
    private Instant nextStart;
    private Integer roomId;

    private RoomState(int index, RequestAutoScheduleInterviewDTO.Room request, Instant nextStart) {
      this.index = index;
      this.request = request;
      this.nextStart = nextStart;
    }

    private boolean matchesDepartment(Integer departmentId) {
      return request.getPreferredDepartmentIds() != null
          && request.getPreferredDepartmentIds().contains(departmentId);
    }
  }
}
