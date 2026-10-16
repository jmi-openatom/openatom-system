package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestRescheduleInterviewSessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateInterviewRoomDTO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.service.InterviewScheduleManagementService;
import edu.jmi.openatom.server.openatomsystem.service.MailBroadcastPlanner;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewScheduleVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class InterviewScheduleManagementServiceImpl implements InterviewScheduleManagementService {
  private final InterviewSessionMapper sessionMapper;
  private final InterviewRoomMapper roomMapper;
  private final InterviewRoomInterviewerMapper roomPeopleMapper;
  private final InterviewMapper interviewMapper;
  private final InterviewInterviewerMapper interviewerMapper;
  private final InterviewQueueStateMapper queueMapper;
  private final InterviewFeedbackMapper feedbackMapper;
  private final MembershipApplicationMapper applicationMapper;
  private final UserMapper userMapper;
  private final NotificationService notificationService;
  private final MailBroadcastPlanner mailPlanner;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Integer> addRoom(Integer sessionId, RequestUpdateInterviewRoomDTO request) {
    InterviewSession session = sessionMapper.selectByIdForUpdate(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    if (!editable(session)) return Result.error(422, "只有草稿或已发布场次可以添加面试室");
    String name = request.getName() == null ? "" : request.getName().strip();
    String location = request.getLocation() == null ? "" : request.getLocation().strip();
    if (name.isEmpty() || name.length() > 80 || location.isEmpty() || location.length() > 255) {
      return Result.error(400, "请填写有效的面试室名称和地点");
    }
    List<Integer> ids = request.getInterviewerIds();
    if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)
        || new HashSet<>(ids).size() != ids.size()) {
      return Result.error(400, "请至少选择一位面试官，且不能重复");
    }
    List<InterviewRoom> rooms = roomMapper.selectBySessionId(sessionId);
    if (rooms.stream().anyMatch(room -> name.equalsIgnoreCase(room.getName()))) {
      return Result.error(409, "本场次已存在同名面试室");
    }
    List<InterviewRoomInterviewer> existing = roomPeopleMapper.selectByRoomIds(
        rooms.stream().map(InterviewRoom::getId).toList());
    if (existing.stream().anyMatch(link -> ids.contains(link.getInterviewerId()))) {
      return Result.error(409, "所选面试官已分配到本场次的其他面试室");
    }
    List<User> users = userMapper.selectBatchIds(ids);
    if (users.size() != ids.size() || users.stream().anyMatch(user -> user.getUserStatus() != UserStatus.ACTIVE)) {
      return Result.error(400, "面试官不存在或已停用");
    }
    int order = rooms.stream().map(InterviewRoom::getSortOrder).filter(Objects::nonNull)
        .max(Integer::compareTo).orElse(-1) + 1;
    InterviewRoom room = InterviewRoom.builder().sessionId(sessionId).name(name).location(location)
        .sortOrder(order).build();
    roomMapper.insert(room);
    ids.forEach(id -> roomPeopleMapper.insert(InterviewRoomInterviewer.builder()
        .roomId(room.getId()).interviewerId(id).build()));
    return Result.success(room.getId());
  }

  @Override
  @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
  public Result<ResponseInterviewScheduleVO> reschedule(Integer sessionId, RequestRescheduleInterviewSessionDTO request) {
    InterviewSession session = sessionMapper.selectByIdForUpdate(sessionId);
    if (session == null) return Result.error(404, "面试场次不存在");
    if (!editable(session)) return Result.error(422, "只有草稿或已发布场次可以重新生成排期");
    List<InterviewRoom> rooms = roomMapper.selectBySessionId(sessionId);
    if (rooms.isEmpty()) return Result.error(422, "请先添加面试室");
    // Calling and feedback use room locks; acquire them before reading live progress.
    for (Integer id : rooms.stream().map(InterviewRoom::getId).sorted().toList()) {
      if (roomMapper.selectByIdForUpdate(id) == null) return Result.error(409, "面试室已变化，请刷新后重试");
    }
    rooms = roomMapper.selectBySessionId(sessionId);
    List<Interview> interviews = new ArrayList<>(interviewMapper.selectBySessionIdForUpdate(sessionId));
    if (interviews.isEmpty()) return Result.error(422, "场次中没有可重新排期的面试人员");
    if (interviews.stream().anyMatch(item -> !List.of("draft", "pending", "confirmed").contains(item.getStatus()))) {
      return Result.error(422, "场次中已有开始或完成的面试，不能覆盖全部人员的排期");
    }
    List<InterviewQueueState> queue = queueMapper.selectBySessionId(sessionId);
    if (queue.stream().anyMatch(item -> item.getCalledAt() != null
        || !List.of("waiting", "cancelled").contains(item.getStatus()))) {
      return Result.error(422, "场次中已有叫号记录，不能覆盖全部人员的排期");
    }
    for (Interview interview : interviews) {
      if (!feedbackMapper.selectByInterviewId(interview.getId()).isEmpty()) {
        return Result.error(422, "场次中已有面试评价，不能覆盖全部人员的排期");
      }
    }
    List<InterviewRoomInterviewer> links = roomPeopleMapper.selectByRoomIds(rooms.stream().map(InterviewRoom::getId).toList());
    String revision = revision(session, rooms, interviews, links, queue);
    if (Boolean.FALSE.equals(request.getPreviewOnly()) && request.getExpectedRevision() != null
        && !revision.equals(request.getExpectedRevision())) {
      return Result.error(409, "场次人员、面试室或签到状态已变化，请重新生成预览");
    }
    Timestamp start;
    try {
      start = request.getScheduledStartAt() == null ? session.getScheduledStartAt()
          : Times.parseTimestamp(request.getScheduledStartAt());
    } catch (RuntimeException exception) {
      return Result.error(400, "请输入有效的开始时间");
    }
    int duration = request.getDurationMinutes() == null ? session.getDurationMinutes() : request.getDurationMinutes();
    int gap = request.getGapMinutes() == null ? session.getGapMinutes() : request.getGapMinutes();
    if (start == null || duration < 5 || gap < 0) return Result.error(400, "开始时间、每场时长或间隔无效");
    Map<Integer, List<Integer>> roomPeople = new HashMap<>();
    links.forEach(link -> roomPeople.computeIfAbsent(link.getRoomId(), ignored -> new ArrayList<>()).add(link.getInterviewerId()));
    Set<Integer> people = new HashSet<>();
    for (InterviewRoom room : rooms) {
      List<Integer> ids = roomPeople.getOrDefault(room.getId(), List.of());
      if (ids.isEmpty()) return Result.error(422, room.getName() + " 尚未配置面试官");
      for (Integer id : ids) {
        if (id == null || !people.add(id)) return Result.error(409, "面试官被重复分配，请先修改面试室");
      }
    }
    Map<Integer, User> interviewerUsers = userMapper.selectBatchIds(people).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
    if (interviewerUsers.size() != people.size()
        || interviewerUsers.values().stream().anyMatch(user -> user.getUserStatus() != UserStatus.ACTIVE)) {
      return Result.error(422, "面试官不存在或已停用，请先修改面试室");
    }
    interviews.sort(Comparator.comparing(Interview::getQueueNumber, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(Interview::getId));
    List<Integer> applicationIds = interviews.stream().map(Interview::getApplicationId).distinct().toList();
    Map<Integer, MembershipApplication> apps = applicationMapper.selectBatchIds(applicationIds).stream()
        .collect(Collectors.toMap(MembershipApplication::getId, Function.identity()));
    if (apps.size() != applicationIds.size()) return Result.error(422, "部分面试人员的申请不存在，请先检查面试记录");
    Set<Integer> applicantIds = apps.values().stream().map(MembershipApplication::getUserId)
        .filter(Objects::nonNull).collect(Collectors.toSet());
    Map<Integer, User> applicants = applicantIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(applicantIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
    List<Interview> outside = interviewMapper.selectByApplicationIds(applicationIds).stream()
        .filter(item -> !sessionId.equals(item.getSessionId()) && !"completed".equals(item.getStatus())).toList();
    List<ResponseInterviewScheduleVO.Assignment> assignments = new ArrayList<>();
    Map<Integer, Integer> roomCounts = new HashMap<>();
    Timestamp end = start;
    for (int index = 0; index < interviews.size(); index++) {
      Interview interview = interviews.get(index);
      InterviewRoom room = rooms.get(index % rooms.size());
      Instant slotStart = start.toInstant().plus(Duration.ofMinutes((long) (index / rooms.size()) * ((long) duration + gap)));
      Timestamp from = Timestamp.from(slotStart), to = Timestamp.from(slotStart.plus(Duration.ofMinutes(duration)));
      if (outside.stream().anyMatch(item -> Objects.equals(item.getApplicationId(), interview.getApplicationId())
          && item.getScheduledStartAt().before(to) && item.getScheduledEndAt().after(from))) {
        return Result.error(409, "申请 " + interview.getApplicationId() + " 与其他场次的面试时间冲突");
      }
      for (Integer person : roomPeople.get(room.getId())) {
        if (interviewerMapper.countOverlappingOutsideSession(person, sessionId, from, to) > 0) {
          return Result.error(409, "面试官 " + userName(interviewerUsers.get(person)) + " 与其他场次的面试时间冲突");
        }
      }
      MembershipApplication app = apps.get(interview.getApplicationId());
      assignments.add(ResponseInterviewScheduleVO.Assignment.builder().interviewId(interview.getId())
          .applicationId(interview.getApplicationId()).applicantName(applicantName(app,
              app.getUserId() == null ? null : applicants.get(app.getUserId())))
          .roomId(room.getId()).roomName(room.getName()).queueNumber(index + 1)
          .scheduledStartAt(from).scheduledEndAt(to).build());
      roomCounts.merge(room.getId(), 1, Integer::sum);
      if (to.after(end)) end = to;
    }
    List<ResponseInterviewScheduleVO.Room> roomRows = rooms.stream().map(room -> ResponseInterviewScheduleVO.Room.builder()
        .roomId(room.getId()).name(room.getName()).location(room.getLocation())
        .assignedCount(roomCounts.getOrDefault(room.getId(), 0)).interviewers(roomPeople.get(room.getId()).stream()
            .map(id -> ResponseInterviewScheduleVO.Interviewer.builder().userId(id).name(userName(interviewerUsers.get(id))).build()).toList())
        .build()).toList();
    if (Boolean.FALSE.equals(request.getPreviewOnly())) {
      Map<Integer, InterviewRoom> roomMap = rooms.stream().collect(Collectors.toMap(InterviewRoom::getId, Function.identity()));
      Map<Integer, InterviewQueueState> queueMap = queue.stream().collect(Collectors.toMap(InterviewQueueState::getInterviewId, Function.identity()));
      for (int index = 0; index < interviews.size(); index++) {
        Interview interview = interviews.get(index);
        ResponseInterviewScheduleVO.Assignment assignment = assignments.get(index);
        InterviewRoom room = roomMap.get(assignment.getRoomId());
        boolean changed = !Objects.equals(interview.getRoomId(), room.getId())
            || !Objects.equals(interview.getScheduledStartAt(), assignment.getScheduledStartAt())
            || !Objects.equals(interview.getScheduledEndAt(), assignment.getScheduledEndAt())
            || !Objects.equals(interview.getLocation(), room.getLocation());
        interview.setRoomId(room.getId()); interview.setQueueNumber(assignment.getQueueNumber());
        interview.setScheduledStartAt(assignment.getScheduledStartAt()); interview.setScheduledEndAt(assignment.getScheduledEndAt());
        interview.setLocation(room.getLocation());
        if (changed && "published".equals(session.getStatus())) interview.setStatus("pending");
        interviewMapper.updateById(interview);
        // Keep the original interview ID so feedback, recordings and audit records retain their references.
        interviewerMapper.deleteByInterviewId(interview.getId());
        roomPeople.get(room.getId()).forEach(id -> interviewerMapper.insert(InterviewInterviewer.builder()
            .interviewId(interview.getId()).interviewerId(id).build()));
        InterviewQueueState state = queueMap.get(interview.getId());
        if (state != null && !Objects.equals(state.getRoomId(), room.getId())) {
          state.setRoomId(room.getId()); queueMapper.updateById(state);
        }
        if (changed && "published".equals(session.getStatus())) notifyChange(interview, apps.get(interview.getApplicationId()));
      }
      session.setScheduledStartAt(start); session.setScheduledEndAt(end);
      session.setDurationMinutes(duration); session.setGapMinutes(gap); sessionMapper.updateById(session);
    }
    return Result.success(ResponseInterviewScheduleVO.builder().sessionId(sessionId).campaignId(session.getCampaignId())
        .sessionName(session.getName()).status(session.getStatus()).scheduledStartAt(start).scheduledEndAt(end)
        .durationMinutes(duration).gapMinutes(gap).scheduleRevision(revision)
        .totalCandidates(assignments.size()).rooms(roomRows).assignments(assignments).build());
  }

  private boolean editable(InterviewSession session) {
    return "draft".equals(session.getStatus()) || "published".equals(session.getStatus());
  }

  private String revision(InterviewSession session, List<InterviewRoom> rooms, List<Interview> interviews,
      List<InterviewRoomInterviewer> links, List<InterviewQueueState> queue) {
    // Stable ordering makes the same configuration yield the same preview revision.
    String value = Jsons.stringify(List.of(session,
        rooms.stream().sorted(Comparator.comparing(InterviewRoom::getId)).toList(),
        interviews.stream().sorted(Comparator.comparing(Interview::getId)).toList(),
        links.stream().sorted(Comparator.comparing(InterviewRoomInterviewer::getRoomId)
            .thenComparing(InterviewRoomInterviewer::getInterviewerId)).toList(),
        queue.stream().sorted(Comparator.comparing(InterviewQueueState::getInterviewId)).toList()));
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private String userName(User user) { return user.getRealName() == null || user.getRealName().isBlank() ? user.getUserName() : user.getRealName(); }

  private String applicantName(MembershipApplication app, User user) {
    if (user != null) return userName(user);
    Map<String, Object> profile = Jsons.parseObject(app.getProfile());
    for (String key : List.of("applicantName", "name", "realName")) {
      Object value = profile.get(key);
      if (value != null && !value.toString().isBlank()) return value.toString();
    }
    return "匿名候选人";
  }

  private void notifyChange(Interview interview, MembershipApplication app) {
    if (app.getUserId() == null) return;
    DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(Times.BUSINESS_ZONE);
    String content = "您好，您的面试安排已调整，请以最新安排为准。\n时间："
        + time.format(interview.getScheduledStartAt().toInstant()) + " 至 " + time.format(interview.getScheduledEndAt().toInstant())
        + "（北京时间）\n地点：" + Objects.toString(interview.getLocation(), "待定");
    notificationService.create(RequestCreateNotificationDTO.builder().title("面试安排变更通知").content(content)
        .type("activity").receiverUserIds(List.of(app.getUserId())).build());
    String eventId = "broadcast_interview_reschedule_" + interview.getId() + "_" + UUID.randomUUID();
    Runnable send = () -> mailPlanner.enqueueUserMail(eventId, app.getUserId(), "interview", "面试安排变更通知", content);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { send.run(); }
      });
    } else send.run();
  }
}
