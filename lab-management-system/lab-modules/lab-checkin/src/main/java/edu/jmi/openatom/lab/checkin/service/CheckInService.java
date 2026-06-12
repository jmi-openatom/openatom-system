package edu.jmi.openatom.lab.checkin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInExclusion;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInGroup;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInGroupMember;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInRecord;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInSession;
import edu.jmi.openatom.lab.checkin.entity.LabCheckInTarget;
import edu.jmi.openatom.lab.checkin.entity.LabEveningStudySchedule;
import edu.jmi.openatom.lab.checkin.mapper.LabCheckInExclusionMapper;
import edu.jmi.openatom.lab.checkin.mapper.LabCheckInGroupMapper;
import edu.jmi.openatom.lab.checkin.mapper.LabCheckInGroupMemberMapper;
import edu.jmi.openatom.lab.checkin.mapper.LabCheckInRecordMapper;
import edu.jmi.openatom.lab.checkin.mapper.LabCheckInSessionMapper;
import edu.jmi.openatom.lab.checkin.mapper.LabCheckInTargetMapper;
import edu.jmi.openatom.lab.checkin.mapper.LabEveningStudyScheduleMapper;
import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.framework.auth.LabSecurityContext;
import edu.jmi.openatom.lab.framework.entity.LabUser;
import edu.jmi.openatom.lab.framework.mapper.LabUserMapper;
import edu.jmi.openatom.lab.framework.properties.LabAttendanceProperties;
import edu.jmi.openatom.lab.notice.service.NoticeService;
import edu.jmi.openatom.lab.score.entity.LabCheckinScoreLog;
import edu.jmi.openatom.lab.score.mapper.LabCheckinScoreLogMapper;
import edu.jmi.openatom.lab.score.service.ScoreService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
  private static final List<String> SESSION_STATUSES = List.of("draft", "open", "closed");
  private static final String SESSION_TYPE_REGULAR = "regular";
  private static final String SESSION_TYPE_EVENING_STUDY = "evening_study";
  private static final String PAYLOAD_PREFIX = "openatom-checkin:";
  private static final String ROTATING_PAYLOAD_PREFIX = "oaci2:";
  private static final long ROTATING_TOKEN_WINDOW_SECONDS = 30L;
  private static final long ROTATING_TOKEN_GRACE_WINDOWS = 10L;
  private static final String RECORD_STATUS_CHECKED = "checked";
  private static final String RECORD_STATUS_LATE = "late";
  private static final String RECORD_STATUS_ABSENT = "absent";
  private static final String RECORD_STATUS_PENDING = "pending";
  private static final Set<String> RECORD_STATUSES =
      Set.of(RECORD_STATUS_CHECKED, RECORD_STATUS_LATE, RECORD_STATUS_ABSENT, RECORD_STATUS_PENDING);

  private final LabUserMapper userMapper;
  private final LabCheckInGroupMapper groupMapper;
  private final LabCheckInGroupMemberMapper groupMemberMapper;
  private final LabCheckInSessionMapper sessionMapper;
  private final LabCheckInTargetMapper targetMapper;
  private final LabCheckInRecordMapper recordMapper;
  private final LabCheckInExclusionMapper exclusionMapper;
  private final LabEveningStudyScheduleMapper eveningStudyScheduleMapper;
  private final LabCheckinScoreLogMapper checkinScoreLogMapper;
  private final ScoreService scoreService;
  private final NoticeService noticeService;
  private final LabAttendanceProperties attendanceProperties;

  public List<LabDtos.CheckInSessionView> list(String status) {
    settleEndedEveningStudySessions();
    return sessionMapper.selectByStatus(status).stream().map(this::toSessionView).toList();
  }

  public List<LabDtos.LabUserOption> userOptions(String keyword) {
    LambdaQueryWrapper<LabUser> wrapper = new LambdaQueryWrapper<>();
    if (keyword != null && !keyword.isBlank()) {
      String value = keyword.trim();
      wrapper.and(
          query ->
              query
                  .like(LabUser::getUsername, value)
                  .or()
                  .like(LabUser::getNickname, value)
                  .or()
                  .like(LabUser::getPhone, value));
    }
    return userMapper.selectList(wrapper.orderByDesc(LabUser::getId)).stream().map(this::toUserOption).toList();
  }

  public List<LabDtos.CheckInGroupView> groups() {
    return groupMapper.selectAllOrdered().stream().map(this::toGroupView).toList();
  }

  @Transactional(rollbackFor = Exception.class)
  public Long createGroup(LabDtos.CheckInGroupRequest request) {
    List<Long> userIds = sanitizeUserIds(request.userIds());
    validateUsers(userIds);
    LabCheckInGroup group = new LabCheckInGroup();
    group.setName(request.name().trim());
    group.setCreatedBy(currentUserIdOrNull());
    group.setCreatedAt(now());
    groupMapper.insert(group);
    replaceGroupMembers(group.getId(), userIds);
    return group.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void updateGroup(Long groupId, LabDtos.CheckInGroupRequest request) {
    LabCheckInGroup group = requireGroup(groupId);
    List<Long> userIds = sanitizeUserIds(request.userIds());
    validateUsers(userIds);
    group.setName(request.name().trim());
    groupMapper.updateById(group);
    replaceGroupMembers(groupId, userIds);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteGroup(Long groupId) {
    requireGroup(groupId);
    groupMemberMapper.deleteByGroupId(groupId);
    groupMapper.deleteById(groupId);
  }

  public void removeGroupMember(Long groupId, Long userId) {
    requireGroup(groupId);
    if (groupMemberMapper.deleteByGroupIdAndUserId(groupId, userId) <= 0) {
      throw new IllegalArgumentException("成员不在该分组中");
    }
  }

  public LabDtos.CheckInSessionView detail(Long sessionId) {
    return toSessionView(requireSession(sessionId));
  }

  @Transactional(rollbackFor = Exception.class)
  public Long create(LabDtos.CreateCheckInSessionRequest request) {
    String status = normalizeStatus(request.status());
    List<Long> targetUserIds = resolveTargetUserIds(request.groupId(), request.targetUserIds());
    validateUsers(targetUserIds);
    if (request.groupId() != null) {
      requireGroup(request.groupId());
    }
    LabCheckInSession session = new LabCheckInSession();
    session.setGroupId(request.groupId());
    session.setSessionType(SESSION_TYPE_REGULAR);
    session.setTitle(request.title().trim());
    session.setLocation(trimToNull(request.location()));
    session.setStartAt(parseDateTime(request.startAt()));
    session.setEndAt(parseDateTime(request.endAt()));
    session.setStatus(status);
    session.setToken(UUID.randomUUID().toString().replace("-", ""));
    session.setCheckinPoints(safeInt(request.checkinPoints(), 0));
    session.setCheckinWindowMinutes(safePositive(request.checkinWindowMinutes(), 30));
    session.setLateAfterMinutes(safeNonNegative(request.lateAfterMinutes(), 10));
    session.setLatePenaltyPoints(safePositive(request.latePenaltyPoints(), attendanceProperties.getLatePenalty() * -1));
    session.setAbsentPenaltyPoints(safePositive(request.absentPenaltyPoints(), attendanceProperties.getAbsentPenalty() * -1));
    session.setCreatedBy(currentUserIdOrNull());
    session.setCreatedAt(now());
    sessionMapper.insert(session);
    replaceSessionTargets(session, targetUserIds);
    return session.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void update(Long sessionId, LabDtos.CreateCheckInSessionRequest request) {
    LabCheckInSession session = requireSession(sessionId);
    String status = normalizeStatus(request.status());
    List<Long> targetUserIds = resolveTargetUserIds(request.groupId(), request.targetUserIds());
    validateUsers(targetUserIds);
    if (request.groupId() != null) {
      requireGroup(request.groupId());
    }
    session.setGroupId(request.groupId());
    session.setTitle(request.title().trim());
    session.setLocation(trimToNull(request.location()));
    session.setStartAt(parseDateTime(request.startAt()));
    session.setEndAt(parseDateTime(request.endAt()));
    session.setStatus(status);
    session.setCheckinPoints(safeInt(request.checkinPoints(), 0));
    session.setCheckinWindowMinutes(safePositive(request.checkinWindowMinutes(), 30));
    session.setLateAfterMinutes(safeNonNegative(request.lateAfterMinutes(), 10));
    session.setLatePenaltyPoints(safePositive(request.latePenaltyPoints(), attendanceProperties.getLatePenalty() * -1));
    session.setAbsentPenaltyPoints(safePositive(request.absentPenaltyPoints(), attendanceProperties.getAbsentPenalty() * -1));
    sessionMapper.updateById(session);
    replaceSessionTargets(session, targetUserIds);
  }

  public void close(Long sessionId) {
    LabCheckInSession session = requireSession(sessionId);
    session.setStatus("closed");
    sessionMapper.updateById(session);
  }

  @Transactional(rollbackFor = Exception.class)
  public void delete(Long sessionId) {
    requireSession(sessionId);
    recordMapper.deleteBySessionId(sessionId);
    targetMapper.deleteBySessionId(sessionId);
    exclusionMapper.deleteBySessionId(sessionId);
    sessionMapper.deleteById(sessionId);
  }

  @Transactional(rollbackFor = Exception.class)
  public void addTargets(Long sessionId, LabDtos.CheckInTargetsRequest request) {
    requireSession(sessionId);
    List<Long> userIds = sanitizeUserIds(request.userIds());
    validateUsers(userIds);
    for (Long userId : userIds) {
      if (targetMapper.selectOneBySessionAndUser(sessionId, userId) == null) {
        LabCheckInTarget target = new LabCheckInTarget();
        target.setSessionId(sessionId);
        target.setUserId(userId);
        target.setCreatedAt(now());
        targetMapper.insert(target);
      }
    }
  }

  public List<LabDtos.CheckInRecordView> records(Long sessionId) {
    requireSession(sessionId);
    List<LabCheckInTarget> targets = targetMapper.selectBySessionId(sessionId);
    List<LabCheckInRecord> records = recordMapper.selectBySessionId(sessionId);
    List<LabCheckInExclusion> exclusions = exclusionMapper.selectBySessionId(sessionId);
    Map<Long, LabCheckInRecord> recordMap =
        records.stream().collect(Collectors.toMap(LabCheckInRecord::getUserId, Function.identity(), (a, b) -> a));
    List<Long> userIds = new ArrayList<>();
    targets.stream().map(LabCheckInTarget::getUserId).forEach(userIds::add);
    exclusions.stream().map(LabCheckInExclusion::getUserId).forEach(userIds::add);
    Map<Long, LabUser> users = loadUsers(userIds);
    List<LabDtos.CheckInRecordView> values = new ArrayList<>();
    targets.stream().map(target -> toRecordView(users.get(target.getUserId()), recordMap.get(target.getUserId()))).forEach(values::add);
    exclusions.stream().map(exclusion -> toExclusionRecordView(users.get(exclusion.getUserId()), exclusion)).forEach(values::add);
    return values;
  }

  @Transactional(rollbackFor = Exception.class)
  public LabDtos.CheckInRecordView updateRecordStatus(
      Long sessionId, Long userId, LabDtos.CheckInRecordStatusRequest request) {
    LabCheckInSession session = requireSession(sessionId);
    LabUser user = requireUser(userId);
    if (targetMapper.selectOneBySessionAndUser(sessionId, userId) == null) {
      throw new IllegalArgumentException("该用户不在本次签到名单中");
    }
    String status = request.status();
    if (!RECORD_STATUSES.contains(status)) {
      throw new IllegalArgumentException("签到状态不合法");
    }
    if (!isEveningStudy(session) && (RECORD_STATUS_LATE.equals(status) || RECORD_STATUS_ABSENT.equals(status))) {
      throw new IllegalArgumentException("普通签到不支持迟到或旷课状态");
    }
    LabCheckInRecord record = applyRecordStatus(session, userId, status, "manual", now());
    return toRecordView(user, record);
  }

  @Transactional(rollbackFor = Exception.class)
  public LabDtos.CheckInRecordView scan(LabDtos.CheckInScanRequest request) {
    LabCheckInSession session = resolveSession(request.token());
    if (session == null) {
      throw new IllegalArgumentException("签到码无效");
    }
    if (!"open".equals(session.getStatus())) {
      throw new IllegalArgumentException("签到未开放或已关闭");
    }
    LocalDateTime current = now();
    if (session.getStartAt() != null && current.isBefore(session.getStartAt())) {
      throw new IllegalArgumentException("签到尚未开始");
    }
    LocalDateTime deadline = checkinDeadlineAt(session);
    if (deadline != null && current.isAfter(deadline)) {
      throw new IllegalArgumentException("签到已结束");
    }
    Long userId = LabSecurityContext.userId();
    LabUser user = requireUser(userId);
    if (targetMapper.selectOneBySessionAndUser(session.getId(), userId) == null) {
      throw new IllegalArgumentException("你不在本次签到发放名单中");
    }
    LabCheckInRecord record = recordMapper.selectOneBySessionAndUser(session.getId(), userId);
    if (record == null) {
      String status = shouldMarkLate(session, current) ? RECORD_STATUS_LATE : RECORD_STATUS_CHECKED;
      record = applyRecordStatus(session, userId, status, "site_scan", current);
    }
    return toRecordView(user, record);
  }

  public List<LabDtos.EveningStudyScheduleView> eveningStudySchedules() {
    settleEndedEveningStudySessions();
    LocalDate today = LocalDate.now(BUSINESS_ZONE);
    return eveningStudyScheduleMapper.selectOrdered().stream()
        .map(schedule -> toEveningStudyScheduleView(schedule, today))
        .toList();
  }

  @Transactional(rollbackFor = Exception.class)
  public Long createEveningStudySchedule(LabDtos.EveningStudyScheduleRequest request) {
    requireGroup(request.groupId());
    if (eveningStudyScheduleMapper.selectByGroupId(request.groupId()) != null) {
      throw new IllegalArgumentException("该实验室分组已经配置晚自习计划");
    }
    LabEveningStudySchedule schedule = new LabEveningStudySchedule();
    fillSchedule(schedule, request);
    schedule.setCreatedBy(currentUserIdOrNull());
    schedule.setCreatedAt(now());
    eveningStudyScheduleMapper.insert(schedule);
    return schedule.getId();
  }

  @Transactional(rollbackFor = Exception.class)
  public void updateEveningStudySchedule(Long scheduleId, LabDtos.EveningStudyScheduleRequest request) {
    LabEveningStudySchedule schedule = requireSchedule(scheduleId);
    requireGroup(request.groupId());
    LabEveningStudySchedule sameGroup = eveningStudyScheduleMapper.selectByGroupId(request.groupId());
    if (sameGroup != null && !sameGroup.getId().equals(schedule.getId())) {
      throw new IllegalArgumentException("该实验室分组已经配置晚自习计划");
    }
    fillSchedule(schedule, request);
    schedule.setUpdatedAt(now());
    eveningStudyScheduleMapper.updateById(schedule);
  }

  public void deleteEveningStudySchedule(Long scheduleId) {
    requireSchedule(scheduleId);
    eveningStudyScheduleMapper.deleteById(scheduleId);
  }

  @Transactional(rollbackFor = Exception.class)
  public LabDtos.EveningStudyTodayView generateEveningStudySessions(String date) {
    LocalDate attendanceDate = parseDateOrToday(date);
    for (LabEveningStudySchedule schedule : eveningStudyScheduleMapper.selectEnabled()) {
      createOrRefreshEveningStudySession(schedule, attendanceDate);
    }
    settleEndedEveningStudySessions();
    return eveningStudyToday(date);
  }

  public LabDtos.EveningStudyTodayView eveningStudyToday(String date) {
    LocalDate attendanceDate = parseDateOrToday(date);
    settleEndedEveningStudySessions();
    List<LabDtos.CheckInSessionView> sessions =
        sessionMapper.selectEveningByDate(attendanceDate).stream().map(this::toSessionView).toList();
    int targetCount = sessions.stream().mapToInt(value -> safeInt(value.targetCount(), 0)).sum();
    int signedCount = sessions.stream().mapToInt(value -> safeInt(value.signedCount(), 0)).sum();
    int checkedCount = sessions.stream().mapToInt(value -> safeInt(value.checkedCount(), 0)).sum();
    int lateCount = sessions.stream().mapToInt(value -> safeInt(value.lateCount(), 0)).sum();
    int absentCount = sessions.stream().mapToInt(value -> safeInt(value.absentCount(), 0)).sum();
    int pendingCount = sessions.stream().mapToInt(value -> safeInt(value.pendingCount(), 0)).sum();
    int excusedCount = sessions.stream().mapToInt(value -> safeInt(value.excusedCount(), 0)).sum();
    return new LabDtos.EveningStudyTodayView(
        attendanceDate.toString(),
        sessions.size(),
        targetCount,
        signedCount,
        checkedCount,
        lateCount,
        absentCount,
        pendingCount,
        excusedCount,
        sessions);
  }

  @Transactional(rollbackFor = Exception.class)
  public LabDtos.CheckinView autoCheckInFromAccepted(Long userId, String problemTitle) {
    requireUser(userId);
    LocalDate date = LocalDate.now(BUSINESS_ZONE);
    LabCheckinScoreLog log =
        upsertAttendanceScoreLog(userId, date, 1, "oj_ac", 0, attendanceProperties.getAutoScoreAward());
    noticeService.sendToUser(
        userId,
        "CHECKIN_AUTO",
        "刷题自动签到成功",
        "你已 AC 今日每日一练「" + problemTitle + "」，系统已自动完成今日签到。");
    return toCheckinView(log);
  }

  @Scheduled(
      initialDelayString = "${lab.checkin.evening-study-initial-delay-ms:30000}",
      fixedDelayString = "${lab.checkin.evening-study-scan-ms:600000}")
  public void autoGenerateTodayEveningStudySessions() {
    try {
      generateEveningStudySessions(null);
    } catch (RuntimeException ex) {
      log.warn("Auto generate evening study check-ins failed: {}", ex.getMessage());
    }
  }

  private void fillSchedule(LabEveningStudySchedule schedule, LabDtos.EveningStudyScheduleRequest request) {
    LocalTime startTime = parseTime(request.startTime());
    LocalTime endTime = parseTime(request.endTime());
    if (startTime == null || endTime == null) {
      throw new IllegalArgumentException("晚自习时间格式不合法");
    }
    int window = safePositive(request.checkinWindowMinutes(), 30);
    int lateAfter = safeNonNegative(request.lateAfterMinutes(), 10);
    if (lateAfter >= window) {
      throw new IllegalArgumentException("迟到阈值必须小于签到窗口");
    }
    schedule.setGroupId(request.groupId());
    schedule.setTitle(firstNonBlank(request.title(), "晚自习签到"));
    schedule.setLocation(trimToNull(request.location()));
    schedule.setStartTime(startTime);
    schedule.setEndTime(endTime);
    schedule.setCheckinPoints(safeInt(request.checkinPoints(), 0));
    schedule.setCheckinWindowMinutes(window);
    schedule.setLateAfterMinutes(lateAfter);
    schedule.setLatePenaltyPoints(safePositive(request.latePenaltyPoints(), attendanceProperties.getLatePenalty() * -1));
    schedule.setAbsentPenaltyPoints(safePositive(request.absentPenaltyPoints(), attendanceProperties.getAbsentPenalty() * -1));
    schedule.setEnabled(request.enabled() == null || request.enabled());
  }

  private LabCheckInSession createOrRefreshEveningStudySession(
      LabEveningStudySchedule schedule, LocalDate attendanceDate) {
    LabCheckInGroup group = requireGroup(schedule.getGroupId());
    LocalDateTime startAt = LocalDateTime.of(attendanceDate, schedule.getStartTime());
    LocalDate endDate = schedule.getEndTime().isAfter(schedule.getStartTime()) ? attendanceDate : attendanceDate.plusDays(1);
    LocalDateTime endAt = LocalDateTime.of(endDate, schedule.getEndTime());
    List<Long> targetUserIds =
        groupMemberMapper.selectByGroupId(group.getId()).stream()
            .map(LabCheckInGroupMember::getUserId)
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    LabCheckInSession session = sessionMapper.selectEveningByScheduleAndDate(schedule.getId(), attendanceDate);
    if (session == null) {
      session = new LabCheckInSession();
      session.setScheduleId(schedule.getId());
      session.setToken(UUID.randomUUID().toString().replace("-", ""));
      session.setCreatedAt(now());
      session.setCreatedBy(currentUserIdOrNull());
      session.setStatus("open");
    }
    session.setGroupId(schedule.getGroupId());
    session.setSessionType(SESSION_TYPE_EVENING_STUDY);
    session.setAttendanceDate(attendanceDate);
    session.setTitle(schedule.getTitle() + " - " + group.getName());
    session.setLocation(schedule.getLocation());
    session.setStartAt(startAt);
    session.setEndAt(endAt);
    session.setCheckinPoints(schedule.getCheckinPoints());
    session.setCheckinWindowMinutes(schedule.getCheckinWindowMinutes());
    session.setLateAfterMinutes(schedule.getLateAfterMinutes());
    session.setLatePenaltyPoints(schedule.getLatePenaltyPoints());
    session.setAbsentPenaltyPoints(schedule.getAbsentPenaltyPoints());
    if (session.getId() == null) {
      sessionMapper.insert(session);
    } else {
      sessionMapper.updateById(session);
    }
    replaceSessionTargets(session, targetUserIds);
    return session;
  }

  private LabCheckInRecord applyRecordStatus(
      LabCheckInSession session, Long userId, String status, String source, LocalDateTime checkinAt) {
    LabCheckInRecord record = recordMapper.selectOneBySessionAndUser(session.getId(), userId);
    if (RECORD_STATUS_PENDING.equals(status)) {
      if (record != null) {
        recordMapper.deleteById(record.getId());
      }
      upsertAttendanceScoreLog(userId, attendanceDateFor(session), 0, "pending", 0, 0);
      return null;
    }
    LocalDateTime effectiveCheckinAt =
        RECORD_STATUS_ABSENT.equals(status) ? firstNonNull(checkinDeadlineAt(session), now()) : firstNonNull(checkinAt, now());
    if (record == null) {
      record = new LabCheckInRecord();
      record.setSessionId(session.getId());
      record.setUserId(userId);
    }
    record.setCheckinAt(effectiveCheckinAt);
    record.setStatus(status);
    record.setSource(source);
    if (record.getId() == null) {
      recordMapper.insert(record);
    } else {
      recordMapper.updateById(record);
    }
    int attendanceStatus = switch (status) {
      case RECORD_STATUS_CHECKED -> isEveningStudy(session) ? 2 : 2;
      case RECORD_STATUS_LATE -> 3;
      case RECORD_STATUS_ABSENT -> 0;
      default -> 0;
    };
    int localDelta =
        RECORD_STATUS_LATE.equals(status)
            ? -safePositive(session.getLatePenaltyPoints(), 5)
            : RECORD_STATUS_ABSENT.equals(status) ? -safePositive(session.getAbsentPenaltyPoints(), 10) : 0;
    int clubScore = RECORD_STATUS_CHECKED.equals(status) ? safeInt(session.getCheckinPoints(), 0) : 0;
    upsertAttendanceScoreLog(userId, attendanceDateFor(session), attendanceStatus, source, localDelta, clubScore);
    return record;
  }

  private LabCheckinScoreLog upsertAttendanceScoreLog(
      Long userId, LocalDate date, int attendanceStatus, String source, int localScoreChange, int clubScoreChange) {
    LabCheckinScoreLog log = scoreService.findLog(userId, date);
    boolean insert = log == null;
    int previousLocal = insert || log.getLocalScoreChange() == null ? 0 : log.getLocalScoreChange();
    if (insert) {
      log = new LabCheckinScoreLog();
      log.setUserId(userId);
      log.setCheckinDate(date);
      log.setCreatedAt(now());
    }
    log.setAttendanceStatus(attendanceStatus);
    log.setSource(source);
    log.setCheckinAt(attendanceStatus == 0 ? null : now());
    log.setLocalScoreChange(localScoreChange);
    log.setClubScoreChange(clubScoreChange);
    log.setClubSyncStatus(clubScoreChange > 0 ? 1 : 0);
    log.setUpdatedAt(now());
    if (insert) {
      checkinScoreLogMapper.insert(log);
    } else {
      checkinScoreLogMapper.updateById(log);
    }
    int diff = localScoreChange - previousLocal;
    if (diff != 0) {
      scoreService.applyLocalScore(userId, diff, log.getId(), "考勤状态更新");
    }
    if (clubScoreChange > 0) {
      scoreService.publishClubScore(log, source);
    }
    return log;
  }

  private void settleEndedEveningStudySessions() {
    LocalDateTime current = now();
    for (LabCheckInSession session : sessionMapper.selectOpenEveningStarted(current)) {
      if (!isEveningStudy(session) || !isCheckinWindowEnded(session, current)) {
        continue;
      }
      for (LabCheckInTarget target : targetMapper.selectBySessionId(session.getId())) {
        if (recordMapper.selectOneBySessionAndUser(session.getId(), target.getUserId()) == null) {
          applyRecordStatus(session, target.getUserId(), RECORD_STATUS_ABSENT, "auto_absent", current);
        }
      }
      session.setStatus("closed");
      sessionMapper.updateById(session);
    }
  }

  private LabDtos.CheckInSessionView toSessionView(LabCheckInSession session) {
    List<LabCheckInTarget> targets = targetMapper.selectBySessionId(session.getId());
    List<LabCheckInRecord> records = recordMapper.selectBySessionId(session.getId());
    List<LabCheckInExclusion> exclusions = exclusionMapper.selectBySessionId(session.getId());
    int checkedCount = countRecords(records, RECORD_STATUS_CHECKED);
    int lateCount = countRecords(records, RECORD_STATUS_LATE);
    int absentCount = countRecords(records, RECORD_STATUS_ABSENT);
    int signedCount = checkedCount + lateCount;
    LabCheckInGroup group = session.getGroupId() == null ? null : groupMapper.selectById(session.getGroupId());
    return new LabDtos.CheckInSessionView(
        session.getId(),
        session.getGroupId(),
        session.getScheduleId(),
        firstNonBlank(session.getSessionType(), SESSION_TYPE_REGULAR),
        session.getAttendanceDate() == null ? null : session.getAttendanceDate().toString(),
        session.getTitle(),
        group == null ? null : group.getName(),
        session.getLocation(),
        session.getStartAt(),
        session.getEndAt(),
        checkinDeadlineAt(session),
        session.getStatus(),
        rotatingPayload(session),
        safeInt(session.getCheckinPoints(), 0),
        safePositive(session.getCheckinWindowMinutes(), 30),
        safeNonNegative(session.getLateAfterMinutes(), 10),
        safePositive(session.getLatePenaltyPoints(), 5),
        safePositive(session.getAbsentPenaltyPoints(), 10),
        targets.size(),
        signedCount,
        checkedCount,
        lateCount,
        absentCount,
        Math.max(0, targets.size() - signedCount - absentCount),
        exclusions.size(),
        targets.stream().map(LabCheckInTarget::getUserId).toList(),
        exclusions.stream().map(LabCheckInExclusion::getUserId).toList());
  }

  private LabDtos.CheckInRecordView toRecordView(LabUser user, LabCheckInRecord record) {
    return new LabDtos.CheckInRecordView(
        user != null ? user.getId() : record == null ? null : record.getUserId(),
        user == null ? null : user.getUsername(),
        user == null ? null : user.getNickname(),
        user == null ? null : user.getPhone(),
        record == null ? RECORD_STATUS_PENDING : record.getStatus(),
        record == null || RECORD_STATUS_ABSENT.equals(record.getStatus()) ? null : record.getCheckinAt(),
        null,
        null);
  }

  private LabDtos.CheckInRecordView toExclusionRecordView(LabUser user, LabCheckInExclusion exclusion) {
    return new LabDtos.CheckInRecordView(
        user != null ? user.getId() : exclusion.getUserId(),
        user == null ? null : user.getUsername(),
        user == null ? null : user.getNickname(),
        user == null ? null : user.getPhone(),
        "excused",
        null,
        exclusion.getSourceId(),
        exclusion.getReason());
  }

  private LabDtos.CheckInGroupView toGroupView(LabCheckInGroup group) {
    List<Long> userIds = groupMemberMapper.selectByGroupId(group.getId()).stream().map(LabCheckInGroupMember::getUserId).toList();
    return new LabDtos.CheckInGroupView(group.getId(), group.getName(), userIds.size(), userIds);
  }

  private LabDtos.EveningStudyScheduleView toEveningStudyScheduleView(
      LabEveningStudySchedule schedule, LocalDate today) {
    LabCheckInGroup group = schedule.getGroupId() == null ? null : groupMapper.selectById(schedule.getGroupId());
    LabCheckInSession todaySession = sessionMapper.selectEveningByScheduleAndDate(schedule.getId(), today);
    LabDtos.CheckInSessionView sessionView = todaySession == null ? null : toSessionView(todaySession);
    int memberCount = schedule.getGroupId() == null ? 0 : groupMemberMapper.selectByGroupId(schedule.getGroupId()).size();
    return new LabDtos.EveningStudyScheduleView(
        schedule.getId(),
        schedule.getGroupId(),
        group == null ? null : group.getName(),
        memberCount,
        schedule.getTitle(),
        schedule.getLocation(),
        schedule.getStartTime() == null ? null : schedule.getStartTime().toString(),
        schedule.getEndTime() == null ? null : schedule.getEndTime().toString(),
        safeInt(schedule.getCheckinPoints(), 0),
        safePositive(schedule.getCheckinWindowMinutes(), 30),
        safeNonNegative(schedule.getLateAfterMinutes(), 10),
        safePositive(schedule.getLatePenaltyPoints(), 5),
        safePositive(schedule.getAbsentPenaltyPoints(), 10),
        Boolean.TRUE.equals(schedule.getEnabled()),
        todaySession == null ? null : todaySession.getId(),
        sessionView == null ? 0 : sessionView.targetCount(),
        sessionView == null ? 0 : sessionView.signedCount(),
        sessionView == null ? 0 : sessionView.checkedCount(),
        sessionView == null ? 0 : sessionView.lateCount(),
        sessionView == null ? 0 : sessionView.absentCount(),
        sessionView == null ? 0 : sessionView.pendingCount(),
        sessionView == null ? 0 : sessionView.excusedCount(),
        schedule.getCreatedAt(),
        schedule.getUpdatedAt());
  }

  private LabDtos.LabUserOption toUserOption(LabUser user) {
    return new LabDtos.LabUserOption(
        user.getId(),
        user.getUsername(),
        user.getNickname(),
        user.getPhone(),
        user.getEmail(),
        user.getLabRole(),
        user.getReputationScore());
  }

  private LabDtos.CheckinView toCheckinView(LabCheckinScoreLog log) {
    return new LabDtos.CheckinView(
        log.getId(),
        log.getUserId(),
        log.getCheckinDate(),
        log.getAttendanceStatus(),
        log.getSource(),
        log.getCheckinAt(),
        log.getLocalScoreChange(),
        log.getClubScoreChange(),
        log.getClubSyncStatus(),
        log.getSyncError());
  }

  private List<Long> resolveTargetUserIds(Long groupId, List<Long> values) {
    if (values != null) {
      return sanitizeUserIds(values);
    }
    LinkedHashSet<Long> ids = new LinkedHashSet<>();
    if (groupId != null) {
      groupMemberMapper.selectByGroupId(groupId).stream().map(LabCheckInGroupMember::getUserId).forEach(ids::add);
    }
    return ids.stream().toList();
  }

  private void replaceGroupMembers(Long groupId, List<Long> userIds) {
    groupMemberMapper.deleteByGroupId(groupId);
    for (Long userId : userIds) {
      LabCheckInGroupMember member = new LabCheckInGroupMember();
      member.setGroupId(groupId);
      member.setUserId(userId);
      member.setCreatedAt(now());
      groupMemberMapper.insert(member);
    }
  }

  private void replaceSessionTargets(LabCheckInSession session, List<Long> nextUserIds) {
    Set<Long> nextSet = new LinkedHashSet<>(nextUserIds);
    Set<Long> existingSet =
        targetMapper.selectBySessionId(session.getId()).stream()
            .map(LabCheckInTarget::getUserId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    for (Long userId : existingSet) {
      if (!nextSet.contains(userId)) {
        LabCheckInRecord record = recordMapper.selectOneBySessionAndUser(session.getId(), userId);
        if (record != null) {
          recordMapper.deleteById(record.getId());
          upsertAttendanceScoreLog(userId, attendanceDateFor(session), 0, "target_removed", 0, 0);
        }
      }
    }
    targetMapper.deleteBySessionId(session.getId());
    for (Long userId : nextUserIds) {
      LabCheckInTarget target = new LabCheckInTarget();
      target.setSessionId(session.getId());
      target.setUserId(userId);
      target.setCreatedAt(now());
      targetMapper.insert(target);
    }
  }

  private LabCheckInSession resolveSession(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return null;
    }
    String trimmed = rawToken.trim();
    if (trimmed.startsWith(ROTATING_PAYLOAD_PREFIX)) {
      return resolveRotatingSession(trimmed);
    }
    String token = trimmed.startsWith(PAYLOAD_PREFIX) ? trimmed.substring(PAYLOAD_PREFIX.length()) : trimmed;
    return sessionMapper.selectByToken(token);
  }

  private LabCheckInSession resolveRotatingSession(String payload) {
    String[] parts = payload.substring(ROTATING_PAYLOAD_PREFIX.length()).split(":");
    if (parts.length != 3) {
      return null;
    }
    Long sessionId;
    long window;
    try {
      sessionId = Long.valueOf(parts[0]);
      window = Long.parseLong(parts[1]);
    } catch (NumberFormatException ex) {
      return null;
    }
    long currentWindow = currentTokenWindow();
    if (window < currentWindow - ROTATING_TOKEN_GRACE_WINDOWS || window > currentWindow + 1) {
      return null;
    }
    LabCheckInSession session = sessionMapper.selectById(sessionId);
    if (session == null) {
      return null;
    }
    return parts[2].equals(rotatingSignature(session, window)) ? session : null;
  }

  private String rotatingPayload(LabCheckInSession session) {
    long window = currentTokenWindow();
    return ROTATING_PAYLOAD_PREFIX + session.getId() + ":" + window + ":" + rotatingSignature(session, window);
  }

  private long currentTokenWindow() {
    return System.currentTimeMillis() / 1000L / ROTATING_TOKEN_WINDOW_SECONDS;
  }

  private String rotatingSignature(LabCheckInSession session, long window) {
    String source = session.getId() + ":" + session.getToken() + ":" + window;
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, 16);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private LocalDateTime checkinDeadlineAt(LabCheckInSession session) {
    if (session == null) {
      return null;
    }
    if (!isEveningStudy(session)) {
      return session.getEndAt();
    }
    return session.getStartAt() == null
        ? session.getEndAt()
        : session.getStartAt().plusMinutes(safePositive(session.getCheckinWindowMinutes(), 30));
  }

  private LocalDateTime lateDeadlineAt(LabCheckInSession session) {
    if (session == null || !isEveningStudy(session) || session.getStartAt() == null) {
      return null;
    }
    return session.getStartAt().plusMinutes(safeNonNegative(session.getLateAfterMinutes(), 10));
  }

  private boolean shouldMarkLate(LabCheckInSession session, LocalDateTime current) {
    LocalDateTime lateDeadline = lateDeadlineAt(session);
    return lateDeadline != null && current != null && current.isAfter(lateDeadline);
  }

  private boolean isCheckinWindowEnded(LabCheckInSession session, LocalDateTime current) {
    LocalDateTime deadline = checkinDeadlineAt(session);
    return deadline != null && current != null && current.isAfter(deadline);
  }

  private boolean isEveningStudy(LabCheckInSession session) {
    return session != null && SESSION_TYPE_EVENING_STUDY.equals(firstNonBlank(session.getSessionType(), SESSION_TYPE_REGULAR));
  }

  private LocalDate attendanceDateFor(LabCheckInSession session) {
    if (session.getAttendanceDate() != null) {
      return session.getAttendanceDate();
    }
    if (session.getStartAt() != null) {
      return session.getStartAt().toLocalDate();
    }
    return LocalDate.now(BUSINESS_ZONE);
  }

  private int countRecords(List<LabCheckInRecord> records, String status) {
    return records == null ? 0 : (int) records.stream().filter(record -> status.equals(record.getStatus())).count();
  }

  private Map<Long, LabUser> loadUsers(List<Long> userIds) {
    List<Long> distinctIds = userIds.stream().filter(id -> id != null && id > 0).distinct().toList();
    if (distinctIds.isEmpty()) {
      return Map.of();
    }
    return userMapper.selectBatchIds(distinctIds).stream().collect(Collectors.toMap(LabUser::getId, Function.identity()));
  }

  private List<Long> sanitizeUserIds(List<Long> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .filter(id -> id != null && id > 0)
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .toList();
  }

  private void validateUsers(List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      throw new IllegalArgumentException("请选择人员");
    }
    for (Long userId : userIds) {
      requireUser(userId);
    }
  }

  private LabUser requireUser(Long userId) {
    LabUser user = userId == null ? null : userMapper.selectById(userId);
    if (user == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    return user;
  }

  private LabCheckInGroup requireGroup(Long groupId) {
    LabCheckInGroup group = groupId == null ? null : groupMapper.selectById(groupId);
    if (group == null) {
      throw new IllegalArgumentException("签到分组不存在");
    }
    return group;
  }

  private LabCheckInSession requireSession(Long sessionId) {
    LabCheckInSession session = sessionId == null ? null : sessionMapper.selectById(sessionId);
    if (session == null) {
      throw new IllegalArgumentException("签到不存在");
    }
    return session;
  }

  private LabEveningStudySchedule requireSchedule(Long scheduleId) {
    LabEveningStudySchedule schedule = scheduleId == null ? null : eveningStudyScheduleMapper.selectById(scheduleId);
    if (schedule == null) {
      throw new IllegalArgumentException("晚自习计划不存在");
    }
    return schedule;
  }

  private String normalizeStatus(String status) {
    String value = status == null || status.isBlank() ? "open" : status;
    if (!SESSION_STATUSES.contains(value)) {
      throw new IllegalArgumentException("签到状态不合法");
    }
    return value;
  }

  private LocalDate parseDateOrToday(String value) {
    if (value == null || value.isBlank()) {
      return LocalDate.now(BUSINESS_ZONE);
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("日期格式不合法");
    }
  }

  private LocalDateTime parseDateTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().replace('T', ' ');
    if (normalized.length() == 16) {
      normalized += ":00";
    }
    return LocalDateTime.parse(normalized.replace(' ', 'T'));
  }

  private LocalTime parseTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() == 5) {
      normalized += ":00";
    }
    return LocalTime.parse(normalized);
  }

  private LocalDateTime now() {
    return LocalDateTime.now(BUSINESS_ZONE);
  }

  private Long currentUserIdOrNull() {
    try {
      return LabSecurityContext.userId();
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private LocalDateTime firstNonNull(LocalDateTime value, LocalDateTime fallback) {
    return value == null ? fallback : value;
  }

  private int safeInt(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private int safePositive(Integer value, int fallback) {
    return value == null ? fallback : Math.max(1, value);
  }

  private int safeNonNegative(Integer value, int fallback) {
    return value == null ? fallback : Math.max(0, value);
  }
}
