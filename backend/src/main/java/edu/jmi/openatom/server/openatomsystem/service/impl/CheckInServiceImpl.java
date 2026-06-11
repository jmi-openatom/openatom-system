package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInRecordStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInScanDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInTargetsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateCheckInSessionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestEveningStudyScheduleDTO;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInExclusion;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInGroup;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInGroupMember;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInRecord;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInSession;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInTarget;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.entity.EveningStudySchedule;
import edu.jmi.openatom.server.openatomsystem.entity.LeaveApplication;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInExclusionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInGroupMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInGroupMemberMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInRecordMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInSessionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInTargetMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubActivityMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.EveningStudyScheduleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.LeaveApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.CheckInService;
import edu.jmi.openatom.server.openatomsystem.service.PointService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInGroupVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInRecordVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInSessionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseEveningStudyScheduleVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseEveningStudyTodayVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInServiceImpl implements CheckInService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";
  private static final List<String> STATUSES = List.of("draft", "open", "closed");
  private static final String SESSION_TYPE_REGULAR = "regular";
  private static final String SESSION_TYPE_EVENING_STUDY = "evening_study";
  private static final String PAYLOAD_PREFIX = "openatom-checkin:";
  private static final String ROTATING_PAYLOAD_PREFIX = "oaci2:";
  private static final long ROTATING_TOKEN_WINDOW_SECONDS = 30L;
  private static final long ROTATING_TOKEN_GRACE_WINDOWS = 10L;
  private static final int DEFAULT_CHECKIN_WINDOW_MINUTES = 30;
  private static final int DEFAULT_LATE_AFTER_MINUTES = 10;
  private static final long DEFAULT_LATE_PENALTY_POINTS = 1L;
  private static final long DEFAULT_ABSENT_PENALTY_POINTS = 2L;
  private static final String RECORD_STATUS_CHECKED = "checked";
  private static final String RECORD_STATUS_LATE = "late";
  private static final String RECORD_STATUS_ABSENT = "absent";
  private static final String RECORD_STATUS_PENDING = "pending";
  private static final Set<String> RECORD_STATUSES =
      Set.of(RECORD_STATUS_CHECKED, RECORD_STATUS_LATE, RECORD_STATUS_ABSENT, RECORD_STATUS_PENDING);

  private final ClubMapper clubMapper;
  private final ClubActivityMapper activityMapper;
  private final UserMapper userMapper;
  private final CheckInGroupMapper groupMapper;
  private final CheckInGroupMemberMapper groupMemberMapper;
  private final CheckInSessionMapper sessionMapper;
  private final CheckInTargetMapper targetMapper;
  private final CheckInRecordMapper recordMapper;
  private final CheckInExclusionMapper exclusionMapper;
  private final EveningStudyScheduleMapper eveningStudyScheduleMapper;
  private final LeaveApplicationMapper leaveApplicationMapper;
  private final PointService pointService;

  @Value("${app.checkin.evening-study-auto-generate-enabled:true}")
  private boolean eveningStudyAutoGenerateEnabled;

  @Override
  public Result<List<ResponseCheckInSessionVO>> list(String status) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    settleEndedEveningStudySessions(club);
    return Result.success(sessionMapper.selectByClubAndStatus(club.getId(), status).stream().map(this::toSessionVO).toList());
  }

  @Override
  public Result<List<User>> userOptions(String keyword) {
    List<User> users = keyword == null || keyword.isBlank()
        ? userMapper.selectList(null)
        : userMapper.selectOptionsByKeyword(keyword.trim());
    return Result.success(users.stream().map(this::buildSafeUser).toList());
  }

  @Override
  public Result<List<ResponseCheckInGroupVO>> groups() {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    return Result.success(groupMapper.selectByClubId(club.getId()).stream().map(this::toGroupVO).toList());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Integer> createGroup(RequestCheckInGroupDTO request) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    List<Integer> userIds = sanitizeTargetUserIds(request.getUserIds());
    if (userIds.isEmpty()) return Result.error(400, "请选择组内人员");
    Result<String> validResult = validateUsers(userIds);
    if (validResult.getCode() != Result.SUCCESS_CODE) return Result.error(validResult.getCode(), validResult.getMessage());
    CheckInGroup group = CheckInGroup.builder()
        .clubId(club.getId())
        .name(request.getName().trim())
        .createdBy(StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null)
        .build();
    groupMapper.insert(group);
    replaceGroupMembers(group.getId(), userIds);
    return Result.success(group.getId(), "签到分组已创建");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> updateGroup(Integer groupId, RequestCheckInGroupDTO request) {
    CheckInGroup group = findGroup(groupId);
    if (group == null) return Result.error(404, "签到分组不存在");
    List<Integer> userIds = sanitizeTargetUserIds(request.getUserIds());
    if (userIds.isEmpty()) return Result.error(400, "请选择组内人员");
    Result<String> validResult = validateUsers(userIds);
    if (validResult.getCode() != Result.SUCCESS_CODE) return Result.error(validResult.getCode(), validResult.getMessage());
    group.setName(request.getName().trim());
    groupMapper.updateById(group);
    replaceGroupMembers(groupId, userIds);
    return Result.success("签到分组已更新");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deleteGroup(Integer groupId) {
    CheckInGroup group = findGroup(groupId);
    if (group == null) return Result.error(404, "签到分组不存在");
    groupMemberMapper.deleteByGroupId(groupId);
    groupMapper.deleteById(groupId);
    return Result.success("签到分组已删除");
  }

  @Override
  public Result<String> removeGroupMember(Integer groupId, Integer userId) {
    CheckInGroup group = findGroup(groupId);
    if (group == null) return Result.error(404, "签到分组不存在");
    if (userId == null || userId <= 0) return Result.error(400, "成员不合法");
    int rows = groupMemberMapper.deleteByGroupIdAndUserId(groupId, userId);
    return rows > 0 ? Result.success("成员已移出分组") : Result.error(404, "成员不在该分组中");
  }

  @Override
  public Result<ResponseCheckInSessionVO> detail(Integer sessionId) {
    CheckInSession session = findSession(sessionId);
    return session == null ? Result.error(404, "签到不存在") : Result.success(toSessionVO(session));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Integer> create(RequestCreateCheckInSessionDTO request) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    String status = normalizeStatus(request.getStatus());
    if (status == null) return Result.error(400, "签到状态不合法");
    List<Integer> targetUserIds = resolveTargetUserIds(request.getGroupId(), request.getTargetUserIds());
    if (targetUserIds.isEmpty()) return Result.error(400, "请选择发放人员");
    Result<String> validResult = validateUsers(targetUserIds);
    if (validResult.getCode() != Result.SUCCESS_CODE) return Result.error(validResult.getCode(), validResult.getMessage());
    if (request.getActivityId() != null) {
      ClubActivity activity = activityMapper.selectOneByIdAndClubId(request.getActivityId(), club.getId());
      if (activity == null) return Result.error(404, "活动不存在");
    }
    if (request.getGroupId() != null && findGroup(request.getGroupId()) == null) {
      return Result.error(404, "签到分组不存在");
    }
    CheckInSession session = CheckInSession.builder()
        .clubId(club.getId())
        .activityId(request.getActivityId())
        .groupId(request.getGroupId())
        .sessionType(SESSION_TYPE_REGULAR)
        .title(request.getTitle().trim())
        .location(trimToNull(request.getLocation()))
        .startAt(Times.parseTimestamp(request.getStartAt()))
        .endAt(Times.parseTimestamp(request.getEndAt()))
        .status(status)
        .token(UUID.randomUUID().toString().replace("-", ""))
        .checkinPoints(safePoints(request.getCheckinPoints()))
        .createdBy(StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null)
        .build();
    sessionMapper.insert(session);
    for (Integer userId : targetUserIds) {
      targetMapper.insert(CheckInTarget.builder().sessionId(session.getId()).userId(userId).build());
    }
    return Result.success(session.getId(), "签到已发布");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> update(Integer sessionId, RequestCreateCheckInSessionDTO request) {
    CheckInSession session = findSession(sessionId);
    if (session == null) return Result.error(404, "签到不存在");
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    String status = normalizeStatus(request.getStatus());
    if (status == null) return Result.error(400, "签到状态不合法");
    List<Integer> targetUserIds = resolveTargetUserIds(request.getGroupId(), request.getTargetUserIds());
    if (targetUserIds.isEmpty()) return Result.error(400, "请选择发放人员");
    Result<String> validResult = validateUsers(targetUserIds);
    if (validResult.getCode() != Result.SUCCESS_CODE) return Result.error(validResult.getCode(), validResult.getMessage());
    if (request.getActivityId() != null) {
      ClubActivity activity = activityMapper.selectOneByIdAndClubId(request.getActivityId(), club.getId());
      if (activity == null) return Result.error(404, "活动不存在");
    }
    if (request.getGroupId() != null && findGroup(request.getGroupId()) == null) {
      return Result.error(404, "签到分组不存在");
    }

    ClubActivity revokeActivity = activityForSession(session);
    session.setActivityId(request.getActivityId());
    session.setGroupId(request.getGroupId());
    session.setTitle(request.getTitle().trim());
    session.setLocation(trimToNull(request.getLocation()));
    session.setStartAt(Times.parseTimestamp(request.getStartAt()));
    session.setEndAt(Times.parseTimestamp(request.getEndAt()));
    session.setStatus(status);
    session.setCheckinPoints(safePoints(request.getCheckinPoints()));
    sessionMapper.updateById(session);
    replaceSessionTargets(session, targetUserIds, revokeActivity);
    return Result.success("签到已更新");
  }

  @Override
  public Result<String> close(Integer sessionId) {
    CheckInSession session = findSession(sessionId);
    if (session == null) return Result.error(404, "签到不存在");
    session.setStatus("closed");
    sessionMapper.updateById(session);
    return Result.success("签到已关闭");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> delete(Integer sessionId) {
    CheckInSession session = findSession(sessionId);
    if (session == null) return Result.error(404, "签到不存在");
    recordMapper.deleteBySessionId(sessionId);
    targetMapper.deleteBySessionId(sessionId);
    exclusionMapper.deleteBySessionId(sessionId);
    int rows = sessionMapper.deleteById(sessionId);
    return rows > 0 ? Result.success("签到已删除") : Result.error("签到删除失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> addTargets(Integer sessionId, RequestCheckInTargetsDTO request) {
    CheckInSession session = findSession(sessionId);
    if (session == null) return Result.error(404, "签到不存在");
    List<Integer> userIds = sanitizeTargetUserIds(request.getUserIds());
    if (userIds.isEmpty()) return Result.error(400, "请选择人员");
    Result<String> validResult = validateUsers(userIds);
    if (validResult.getCode() != Result.SUCCESS_CODE) return Result.error(validResult.getCode(), validResult.getMessage());
    int added = 0;
    for (Integer userId : userIds) {
      CheckInTarget exists = targetMapper.selectOneBySessionAndUser(sessionId, userId);
      if (exists == null) {
        exclusionMapper.deleteBySessionIdAndUserId(sessionId, userId);
        targetMapper.insert(CheckInTarget.builder().sessionId(sessionId).userId(userId).build());
        added++;
      }
    }
    return Result.success("已添加 " + added + " 人");
  }

  @Override
  public Result<List<ResponseCheckInRecordVO>> records(Integer sessionId) {
    CheckInSession session = findSession(sessionId);
    if (session == null) return Result.error(404, "签到不存在");
    List<CheckInTarget> targets = targetMapper.selectBySessionId(sessionId);
    List<CheckInRecord> records = recordMapper.selectBySessionId(sessionId);
    List<CheckInExclusion> exclusions = exclusionMapper.selectBySessionId(sessionId);
    Map<Integer, CheckInRecord> recordMap =
        records.stream()
            .collect(Collectors.toMap(CheckInRecord::getUserId, Function.identity(), (a, b) -> a));
    List<Integer> userIds = new ArrayList<>();
    targets.stream().map(CheckInTarget::getUserId).forEach(userIds::add);
    exclusions.stream().map(CheckInExclusion::getUserId).forEach(userIds::add);
    Map<Integer, User> users = loadUsers(userIds);
    List<ResponseCheckInRecordVO> values = new ArrayList<>();
    targets.stream()
        .map(target -> toRecordVO(users.get(target.getUserId()), recordMap.get(target.getUserId())))
        .forEach(values::add);
    exclusions.stream()
        .map(exclusion -> toExclusionRecordVO(users.get(exclusion.getUserId()), exclusion))
        .forEach(values::add);
    return Result.success(values);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseCheckInRecordVO> updateRecordStatus(
      Integer sessionId, Integer userId, RequestCheckInRecordStatusDTO request) {
    CheckInSession session = findSession(sessionId);
    if (session == null) return Result.error(404, "签到不存在");
    if (userMapper.selectById(userId) == null) return Result.error(404, "用户不存在");
    CheckInTarget target = targetMapper.selectOneBySessionAndUser(sessionId, userId);
    if (target == null) return Result.error(404, "该用户不在本次签到名单中");
    String status = request.getStatus();
    if (!RECORD_STATUSES.contains(status)) return Result.error(400, "签到状态不合法");
    if (!isEveningStudy(session) && (RECORD_STATUS_LATE.equals(status) || RECORD_STATUS_ABSENT.equals(status))) {
      return Result.error(400, "普通签到不支持迟到或旷课状态");
    }
    if (RECORD_STATUS_PENDING.equals(status)) {
      applyRecordStatus(session, userId, RECORD_STATUS_PENDING, "manual", currentUserId());
      return Result.success(toRecordVO(userMapper.selectById(userId), null), "已标记为未签到");
    }
    CheckInRecord record = applyRecordStatus(session, userId, status, "manual", currentUserId());
    return Result.success(toRecordVO(userMapper.selectById(userId), record), recordStatusMessage(status));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseCheckInRecordVO> scan(RequestCheckInScanDTO request) {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录后签到");
    CheckInSession session = resolveSession(request.getToken());
    if (session == null) return Result.error(404, "签到码无效");
    if (!"open".equals(session.getStatus())) return Result.error(400, "签到未开放或已关闭");
    Timestamp now = Times.now();
    if (session.getStartAt() != null && now.before(session.getStartAt())) return Result.error(400, "签到尚未开始");
    Timestamp deadlineAt = checkinDeadlineAt(session);
    if (deadlineAt != null && now.after(deadlineAt)) return Result.error(400, "签到已结束");
    Integer userId = StpUtil.getLoginIdAsInt();
    CheckInTarget target = targetMapper.selectOneBySessionAndUser(session.getId(), userId);
    if (target == null) return Result.error(403, "你不在本次签到发放名单中");
    CheckInRecord record = recordMapper.selectOneBySessionAndUser(session.getId(), userId);
    if (record == null) {
      String status = shouldMarkLate(session, now) ? RECORD_STATUS_LATE : RECORD_STATUS_CHECKED;
      record = applyRecordStatus(session, userId, status, "miniapp_scan", null, now);
      return Result.success(
          toRecordVO(userMapper.selectById(userId), record),
          RECORD_STATUS_LATE.equals(status) ? "签到成功，已记为迟到" : "签到成功");
    }
    return Result.success(toRecordVO(userMapper.selectById(userId), record), "已签到，无需重复签到");
  }

  @Override
  public Result<List<ResponseEveningStudyScheduleVO>> eveningStudySchedules() {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    settleEndedEveningStudySessions(club);
    LocalDate today = LocalDate.now(Times.BUSINESS_ZONE);
    return Result.success(
        eveningStudyScheduleMapper.selectByClubId(club.getId()).stream()
            .map(schedule -> toEveningStudyScheduleVO(schedule, today))
            .toList());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Integer> createEveningStudySchedule(RequestEveningStudyScheduleDTO request) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    CheckInGroup group = findGroup(request.getGroupId());
    if (group == null) return Result.error(404, "实验室分组不存在");
    EveningStudySchedule exists = eveningStudyScheduleMapper.selectByGroupId(group.getId());
    if (exists != null) return Result.error(409, "该实验室分组已经配置晚自习计划");
    Time startTime = parseLocalTime(request.getStartTime());
    Time endTime = parseLocalTime(request.getEndTime());
    if (startTime == null || endTime == null) return Result.error(400, "晚自习时间格式不合法");
    Result<String> ruleResult = validateEveningStudyRules(request);
    if (ruleResult.getCode() != Result.SUCCESS_CODE) return Result.error(ruleResult.getCode(), ruleResult.getMessage());
    EveningStudySchedule schedule =
        EveningStudySchedule.builder()
            .clubId(club.getId())
            .groupId(group.getId())
            .title(firstNonBlank(request.getTitle(), "晚自习签到"))
            .location(trimToNull(request.getLocation()))
            .startTime(startTime)
            .endTime(endTime)
            .checkinPoints(safePoints(request.getCheckinPoints()))
            .checkinWindowMinutes(normalizeCheckinWindowMinutes(request.getCheckinWindowMinutes()))
            .lateAfterMinutes(normalizeLateAfterMinutes(request.getLateAfterMinutes()))
            .latePenaltyPoints(normalizePenaltyPoints(request.getLatePenaltyPoints(), DEFAULT_LATE_PENALTY_POINTS))
            .absentPenaltyPoints(normalizePenaltyPoints(request.getAbsentPenaltyPoints(), DEFAULT_ABSENT_PENALTY_POINTS))
            .enabled(request.getEnabled() == null || request.getEnabled())
            .createdBy(currentUserId())
            .build();
    eveningStudyScheduleMapper.insert(schedule);
    return Result.success(schedule.getId(), "晚自习计划已创建");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> updateEveningStudySchedule(Integer scheduleId, RequestEveningStudyScheduleDTO request) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    EveningStudySchedule schedule = eveningStudyScheduleMapper.selectByIdAndClubId(scheduleId, club.getId());
    if (schedule == null) return Result.error(404, "晚自习计划不存在");
    CheckInGroup group = findGroup(request.getGroupId());
    if (group == null) return Result.error(404, "实验室分组不存在");
    EveningStudySchedule sameGroup = eveningStudyScheduleMapper.selectByGroupId(group.getId());
    if (sameGroup != null && !sameGroup.getId().equals(schedule.getId())) {
      return Result.error(409, "该实验室分组已经配置晚自习计划");
    }
    Time startTime = parseLocalTime(request.getStartTime());
    Time endTime = parseLocalTime(request.getEndTime());
    if (startTime == null || endTime == null) return Result.error(400, "晚自习时间格式不合法");
    Result<String> ruleResult = validateEveningStudyRules(request);
    if (ruleResult.getCode() != Result.SUCCESS_CODE) return Result.error(ruleResult.getCode(), ruleResult.getMessage());
    schedule.setGroupId(group.getId());
    schedule.setTitle(firstNonBlank(request.getTitle(), "晚自习签到"));
    schedule.setLocation(trimToNull(request.getLocation()));
    schedule.setStartTime(startTime);
    schedule.setEndTime(endTime);
    schedule.setCheckinPoints(safePoints(request.getCheckinPoints()));
    schedule.setCheckinWindowMinutes(normalizeCheckinWindowMinutes(request.getCheckinWindowMinutes()));
    schedule.setLateAfterMinutes(normalizeLateAfterMinutes(request.getLateAfterMinutes()));
    schedule.setLatePenaltyPoints(normalizePenaltyPoints(request.getLatePenaltyPoints(), DEFAULT_LATE_PENALTY_POINTS));
    schedule.setAbsentPenaltyPoints(normalizePenaltyPoints(request.getAbsentPenaltyPoints(), DEFAULT_ABSENT_PENALTY_POINTS));
    schedule.setEnabled(request.getEnabled() == null || request.getEnabled());
    eveningStudyScheduleMapper.updateById(schedule);
    CheckInSession todaySession =
        sessionMapper.selectEveningByScheduleAndDate(schedule.getId(), Date.valueOf(LocalDate.now(Times.BUSINESS_ZONE)));
    if (todaySession != null) {
      createOrRefreshEveningStudySession(schedule, LocalDate.now(Times.BUSINESS_ZONE));
    }
    return Result.success("晚自习计划已更新");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deleteEveningStudySchedule(Integer scheduleId) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    EveningStudySchedule schedule = eveningStudyScheduleMapper.selectByIdAndClubId(scheduleId, club.getId());
    if (schedule == null) return Result.error(404, "晚自习计划不存在");
    eveningStudyScheduleMapper.deleteById(scheduleId);
    return Result.success("晚自习计划已删除，历史签到场次保留");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseEveningStudyTodayVO> generateEveningStudySessions(String date) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    LocalDate attendanceDate = parseDateOrToday(date);
    if (attendanceDate == null) return Result.error(400, "日期格式不合法");
    for (EveningStudySchedule schedule : eveningStudyScheduleMapper.selectEnabledByClubId(club.getId())) {
      createOrRefreshEveningStudySession(schedule, attendanceDate);
    }
    settleEndedEveningStudySessions(club);
    return Result.success(buildEveningStudyTodayVO(club, attendanceDate), "晚自习签到已生成");
  }

  @Override
  public Result<ResponseEveningStudyTodayVO> eveningStudyToday(String date) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    LocalDate attendanceDate = parseDateOrToday(date);
    if (attendanceDate == null) return Result.error(400, "日期格式不合法");
    settleEndedEveningStudySessions(club);
    return Result.success(buildEveningStudyTodayVO(club, attendanceDate));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void syncApprovedLeave(LeaveApplication application) {
    if (application == null || !"approved".equals(application.getStatus())) return;
    if (!hasCompleteLeaveTime(application)) return;
    Club club = defaultClub();
    if (club == null || !club.getId().equals(application.getClubId())) return;
    for (CheckInSession session :
        sessionMapper.selectEveningOverlaps(club.getId(), application.getStartAt(), application.getEndAt())) {
      if (!isGroupMember(session.getGroupId(), application.getUserId())) continue;
      removeTargetForApprovedLeave(session, application);
    }
  }

  @Scheduled(
      initialDelayString = "${app.checkin.evening-study-initial-delay-ms:30000}",
      fixedDelayString = "${app.checkin.evening-study-scan-ms:600000}")
  public void autoGenerateTodayEveningStudySessions() {
    if (!eveningStudyAutoGenerateEnabled) return;
    try {
      generateEveningStudySessions(null);
      Club club = defaultClub();
      if (club != null) settleEndedEveningStudySessions(club);
    } catch (RuntimeException e) {
      log.warn("Auto generate evening study check-ins failed: {}", e.getMessage());
    }
  }

  private ResponseCheckInSessionVO toSessionVO(CheckInSession session) {
    List<CheckInTarget> targets = targetMapper.selectBySessionId(session.getId());
    List<CheckInRecord> records = recordMapper.selectBySessionId(session.getId());
    List<CheckInExclusion> exclusions = exclusionMapper.selectBySessionId(session.getId());
    int checkedCount = countRecords(records, RECORD_STATUS_CHECKED);
    int lateCount = countRecords(records, RECORD_STATUS_LATE);
    int absentCount = countRecords(records, RECORD_STATUS_ABSENT);
    int signedCount = checkedCount + lateCount;
    ClubActivity activity = session.getActivityId() == null ? null : activityMapper.selectById(session.getActivityId());
    CheckInGroup group = session.getGroupId() == null ? null : groupMapper.selectById(session.getGroupId());
    return ResponseCheckInSessionVO.builder()
        .id(session.getId()).activityId(session.getActivityId()).groupId(session.getGroupId())
        .scheduleId(session.getScheduleId())
        .sessionType(firstNonBlank(session.getSessionType(), SESSION_TYPE_REGULAR))
        .attendanceDate(session.getAttendanceDate() == null ? null : session.getAttendanceDate().toString())
        .activityTitle(activity == null ? null : activity.getTitle())
        .groupName(group == null ? null : group.getName())
        .title(session.getTitle()).location(session.getLocation())
        .startAt(session.getStartAt()).endAt(session.getEndAt())
        .checkinDeadlineAt(checkinDeadlineAt(session))
        .status(session.getStatus()).qrPayload(rotatingPayload(session))
        .checkinPoints(safePoints(session.getCheckinPoints()))
        .checkinWindowMinutes(normalizeCheckinWindowMinutes(session.getCheckinWindowMinutes()))
        .lateAfterMinutes(normalizeLateAfterMinutes(session.getLateAfterMinutes()))
        .latePenaltyPoints(normalizePenaltyPoints(session.getLatePenaltyPoints(), DEFAULT_LATE_PENALTY_POINTS))
        .absentPenaltyPoints(normalizePenaltyPoints(session.getAbsentPenaltyPoints(), DEFAULT_ABSENT_PENALTY_POINTS))
        .targetCount(targets.size()).signedCount(signedCount).checkedCount(checkedCount)
        .lateCount(lateCount).absentCount(absentCount)
        .pendingCount(Math.max(0, targets.size() - signedCount - absentCount))
        .excusedCount(exclusions.size())
        .targetUserIds(targets.stream().map(CheckInTarget::getUserId).toList())
        .excusedUserIds(exclusions.stream().map(CheckInExclusion::getUserId).toList())
        .build();
  }

  private ResponseCheckInRecordVO toRecordVO(User user, CheckInRecord record) {
    return ResponseCheckInRecordVO.builder()
        .userId(user != null ? user.getId() : record == null ? null : record.getUserId())
        .userName(user == null ? null : user.getUserName())
        .realName(user == null ? null : user.getRealName())
        .studentId(user == null ? null : user.getStudentId())
        .phone(user == null ? null : user.getPhone())
        .status(record == null ? "pending" : record.getStatus())
        .checkinAt(record == null || RECORD_STATUS_ABSENT.equals(record.getStatus()) ? null : record.getCheckinAt())
        .build();
  }

  private ResponseCheckInRecordVO toExclusionRecordVO(User user, CheckInExclusion exclusion) {
    return ResponseCheckInRecordVO.builder()
        .userId(user != null ? user.getId() : exclusion.getUserId())
        .userName(user == null ? null : user.getUserName())
        .realName(user == null ? null : user.getRealName())
        .studentId(user == null ? null : user.getStudentId())
        .phone(user == null ? null : user.getPhone())
        .status("excused")
        .leaveApplicationId(exclusion.getSourceId())
        .exclusionReason(exclusion.getReason())
        .build();
  }

  private User buildSafeUser(User user) {
    return User.builder().id(user.getId()).userName(user.getUserName()).realName(user.getRealName())
        .phone(user.getPhone()).email(user.getEmail()).studentId(user.getStudentId())
        .college(user.getCollege()).major(user.getMajor()).grade(user.getGrade())
        .className(user.getClassName()).userStatus(user.getUserStatus()).build();
  }

  private ResponseCheckInGroupVO toGroupVO(CheckInGroup group) {
    List<Integer> userIds =
        groupMemberMapper.selectByGroupId(group.getId()).stream()
            .map(CheckInGroupMember::getUserId)
            .toList();
    return ResponseCheckInGroupVO.builder()
        .id(group.getId())
        .name(group.getName())
        .memberCount(userIds.size())
        .userIds(userIds)
        .build();
  }

  private ResponseEveningStudyScheduleVO toEveningStudyScheduleVO(
      EveningStudySchedule schedule, LocalDate today) {
    CheckInGroup group = schedule.getGroupId() == null ? null : groupMapper.selectById(schedule.getGroupId());
    CheckInSession todaySession =
        sessionMapper.selectEveningByScheduleAndDate(schedule.getId(), Date.valueOf(today));
    ResponseCheckInSessionVO sessionVO = todaySession == null ? null : toSessionVO(todaySession);
    int memberCount =
        schedule.getGroupId() == null ? 0 : groupMemberMapper.selectByGroupId(schedule.getGroupId()).size();
    return ResponseEveningStudyScheduleVO.builder()
        .id(schedule.getId())
        .groupId(schedule.getGroupId())
        .groupName(group == null ? null : group.getName())
        .memberCount(memberCount)
        .title(schedule.getTitle())
        .location(schedule.getLocation())
        .startTime(formatTime(schedule.getStartTime()))
        .endTime(formatTime(schedule.getEndTime()))
        .checkinPoints(safePoints(schedule.getCheckinPoints()))
        .checkinWindowMinutes(normalizeCheckinWindowMinutes(schedule.getCheckinWindowMinutes()))
        .lateAfterMinutes(normalizeLateAfterMinutes(schedule.getLateAfterMinutes()))
        .latePenaltyPoints(normalizePenaltyPoints(schedule.getLatePenaltyPoints(), DEFAULT_LATE_PENALTY_POINTS))
        .absentPenaltyPoints(normalizePenaltyPoints(schedule.getAbsentPenaltyPoints(), DEFAULT_ABSENT_PENALTY_POINTS))
        .enabled(Boolean.TRUE.equals(schedule.getEnabled()))
        .todaySessionId(todaySession == null ? null : todaySession.getId())
        .todayTargetCount(sessionVO == null ? 0 : sessionVO.getTargetCount())
        .todaySignedCount(sessionVO == null ? 0 : sessionVO.getSignedCount())
        .todayCheckedCount(sessionVO == null ? 0 : sessionVO.getCheckedCount())
        .todayLateCount(sessionVO == null ? 0 : sessionVO.getLateCount())
        .todayAbsentCount(sessionVO == null ? 0 : sessionVO.getAbsentCount())
        .todayPendingCount(sessionVO == null ? 0 : sessionVO.getPendingCount())
        .todayExcusedCount(sessionVO == null ? 0 : sessionVO.getExcusedCount())
        .createdAt(schedule.getCreatedAt())
        .updatedAt(schedule.getUpdatedAt())
        .build();
  }

  private ResponseEveningStudyTodayVO buildEveningStudyTodayVO(Club club, LocalDate date) {
    List<ResponseCheckInSessionVO> sessions =
        sessionMapper.selectEveningByClubAndDate(club.getId(), Date.valueOf(date)).stream()
            .map(this::toSessionVO)
            .toList();
    int targetCount = sessions.stream().mapToInt(value -> safeInt(value.getTargetCount())).sum();
    int signedCount = sessions.stream().mapToInt(value -> safeInt(value.getSignedCount())).sum();
    int checkedCount = sessions.stream().mapToInt(value -> safeInt(value.getCheckedCount())).sum();
    int lateCount = sessions.stream().mapToInt(value -> safeInt(value.getLateCount())).sum();
    int absentCount = sessions.stream().mapToInt(value -> safeInt(value.getAbsentCount())).sum();
    int pendingCount = sessions.stream().mapToInt(value -> safeInt(value.getPendingCount())).sum();
    int excusedCount = sessions.stream().mapToInt(value -> safeInt(value.getExcusedCount())).sum();
    return ResponseEveningStudyTodayVO.builder()
        .date(date.toString())
        .sessionCount(sessions.size())
        .targetCount(targetCount)
        .signedCount(signedCount)
        .checkedCount(checkedCount)
        .lateCount(lateCount)
        .absentCount(absentCount)
        .pendingCount(pendingCount)
        .excusedCount(excusedCount)
        .sessions(sessions)
        .build();
  }

  private CheckInSession createOrRefreshEveningStudySession(
      EveningStudySchedule schedule, LocalDate attendanceDate) {
    CheckInGroup group = findGroup(schedule.getGroupId());
    if (group == null) return null;
    Timestamp startAt = timestampOf(attendanceDate, schedule.getStartTime());
    LocalDate endDate = endDateFor(attendanceDate, schedule.getStartTime(), schedule.getEndTime());
    Timestamp endAt = timestampOf(endDate, schedule.getEndTime());
    List<Integer> groupUserIds =
        groupMemberMapper.selectByGroupId(group.getId()).stream()
            .map(CheckInGroupMember::getUserId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    Map<Integer, LeaveApplication> leaveByUser =
        approvedLeaveByUser(schedule.getClubId(), groupUserIds, startAt, endAt);
    List<Integer> targetUserIds =
        groupUserIds.stream().filter(userId -> !leaveByUser.containsKey(userId)).toList();
    Date sqlDate = Date.valueOf(attendanceDate);
    CheckInSession session = sessionMapper.selectEveningByScheduleAndDate(schedule.getId(), sqlDate);
    if (session == null) {
      session =
          CheckInSession.builder()
              .clubId(schedule.getClubId())
              .groupId(schedule.getGroupId())
              .scheduleId(schedule.getId())
              .sessionType(SESSION_TYPE_EVENING_STUDY)
              .attendanceDate(sqlDate)
              .title(eveningSessionTitle(schedule, group))
              .location(schedule.getLocation())
              .startAt(startAt)
              .endAt(endAt)
              .status("open")
              .token(UUID.randomUUID().toString().replace("-", ""))
              .checkinPoints(safePoints(schedule.getCheckinPoints()))
              .checkinWindowMinutes(normalizeCheckinWindowMinutes(schedule.getCheckinWindowMinutes()))
              .lateAfterMinutes(normalizeLateAfterMinutes(schedule.getLateAfterMinutes()))
              .latePenaltyPoints(normalizePenaltyPoints(schedule.getLatePenaltyPoints(), DEFAULT_LATE_PENALTY_POINTS))
              .absentPenaltyPoints(normalizePenaltyPoints(schedule.getAbsentPenaltyPoints(), DEFAULT_ABSENT_PENALTY_POINTS))
              .createdBy(currentUserId())
              .build();
      sessionMapper.insert(session);
    } else {
      ClubActivity revokeActivity = activityForSession(session);
      session.setGroupId(schedule.getGroupId());
      session.setScheduleId(schedule.getId());
      session.setSessionType(SESSION_TYPE_EVENING_STUDY);
      session.setAttendanceDate(sqlDate);
      session.setTitle(eveningSessionTitle(schedule, group));
      session.setLocation(schedule.getLocation());
      session.setStartAt(startAt);
      session.setEndAt(endAt);
      session.setCheckinPoints(safePoints(schedule.getCheckinPoints()));
      session.setCheckinWindowMinutes(normalizeCheckinWindowMinutes(schedule.getCheckinWindowMinutes()));
      session.setLateAfterMinutes(normalizeLateAfterMinutes(schedule.getLateAfterMinutes()));
      session.setLatePenaltyPoints(normalizePenaltyPoints(schedule.getLatePenaltyPoints(), DEFAULT_LATE_PENALTY_POINTS));
      session.setAbsentPenaltyPoints(normalizePenaltyPoints(schedule.getAbsentPenaltyPoints(), DEFAULT_ABSENT_PENALTY_POINTS));
      sessionMapper.updateById(session);
      replaceSessionTargets(session, targetUserIds, revokeActivity);
      replaceSessionExclusions(session, leaveByUser);
      return session;
    }
    replaceSessionTargets(session, targetUserIds, null);
    replaceSessionExclusions(session, leaveByUser);
    return session;
  }

  private void replaceSessionExclusions(
      CheckInSession session, Map<Integer, LeaveApplication> leaveByUser) {
    exclusionMapper.deleteBySessionId(session.getId());
    for (Map.Entry<Integer, LeaveApplication> entry : leaveByUser.entrySet()) {
      LeaveApplication leave = entry.getValue();
      exclusionMapper.insert(
          CheckInExclusion.builder()
              .sessionId(session.getId())
              .userId(entry.getKey())
              .sourceType("leave")
              .sourceId(leave == null ? null : leave.getId())
              .reason(leave == null ? "已通过请假" : "已通过请假：" + leave.getTitle())
              .build());
    }
  }

  private Map<Integer, LeaveApplication> approvedLeaveByUser(
      Integer clubId, List<Integer> userIds, Timestamp startAt, Timestamp endAt) {
    if (clubId == null || userIds == null || userIds.isEmpty()) return Map.of();
    Map<Integer, LeaveApplication> values = new LinkedHashMap<>();
    for (LeaveApplication leave :
        leaveApplicationMapper.selectApprovedOverlapsByUsers(clubId, userIds, startAt, endAt)) {
      values.putIfAbsent(leave.getUserId(), leave);
    }
    return values;
  }

  private void removeTargetForApprovedLeave(CheckInSession session, LeaveApplication application) {
    CheckInRecord record = recordMapper.selectOneBySessionAndUser(session.getId(), application.getUserId());
    if (record != null) {
      recordMapper.deleteById(record.getId());
      clearAttendanceEffects(application.getUserId(), session, currentUserId());
    }
    targetMapper.deleteBySessionIdAndUserId(session.getId(), application.getUserId());
    CheckInExclusion exclusion = exclusionMapper.selectOneBySessionAndUser(session.getId(), application.getUserId());
    String reason = "已通过请假：" + application.getTitle();
    if (exclusion == null) {
      exclusionMapper.insert(
          CheckInExclusion.builder()
              .sessionId(session.getId())
              .userId(application.getUserId())
              .sourceType("leave")
              .sourceId(application.getId())
              .reason(reason)
              .build());
    } else {
      exclusion.setSourceType("leave");
      exclusion.setSourceId(application.getId());
      exclusion.setReason(reason);
      exclusionMapper.updateById(exclusion);
    }
  }

  private boolean isGroupMember(Integer groupId, Integer userId) {
    if (groupId == null || userId == null) return false;
    return groupMemberMapper.selectByGroupId(groupId).stream()
        .anyMatch(member -> userId.equals(member.getUserId()));
  }

  private String eveningSessionTitle(EveningStudySchedule schedule, CheckInGroup group) {
    return firstNonBlank(schedule.getTitle(), "晚自习签到") + " - " + group.getName();
  }

  private boolean hasCompleteLeaveTime(LeaveApplication application) {
    return application != null && application.getStartAt() != null && application.getEndAt() != null;
  }

  private CheckInRecord applyRecordStatus(
      CheckInSession session, Integer userId, String status, String source, Integer operatorId) {
    return applyRecordStatus(session, userId, status, source, operatorId, Times.now());
  }

  private CheckInRecord applyRecordStatus(
      CheckInSession session,
      Integer userId,
      String status,
      String source,
      Integer operatorId,
      Timestamp checkinAt) {
    CheckInRecord record = recordMapper.selectOneBySessionAndUser(session.getId(), userId);
    clearAttendanceEffects(userId, session, operatorId);
    if (RECORD_STATUS_PENDING.equals(status)) {
      if (record != null) recordMapper.deleteById(record.getId());
      return null;
    }

    Timestamp effectiveCheckinAt =
        RECORD_STATUS_ABSENT.equals(status)
            ? firstNonNull(checkinDeadlineAt(session), Times.now())
            : firstNonNull(checkinAt, Times.now());
    if (record == null) {
      record =
          CheckInRecord.builder()
              .sessionId(session.getId())
              .userId(userId)
              .checkinAt(effectiveCheckinAt)
              .source(source)
              .status(status)
              .build();
      recordMapper.insert(record);
    } else {
      record.setStatus(status);
      record.setCheckinAt(effectiveCheckinAt);
      record.setSource(source);
      recordMapper.updateById(record);
    }
    applyAttendanceEffects(userId, session, status, operatorId);
    return record;
  }

  private void applyAttendanceEffects(
      Integer userId, CheckInSession session, String status, Integer operatorId) {
    if (RECORD_STATUS_CHECKED.equals(status) || RECORD_STATUS_LATE.equals(status)) {
      pointService.grantCheckInPoints(userId, session, activityForSession(session), operatorId);
    }
    if (!isEveningStudy(session)) return;
    if (RECORD_STATUS_LATE.equals(status)) {
      pointService.applyCheckInPenalty(
          userId,
          session,
          RECORD_STATUS_LATE,
          normalizePenaltyPoints(session.getLatePenaltyPoints(), DEFAULT_LATE_PENALTY_POINTS),
          operatorId);
    } else if (RECORD_STATUS_ABSENT.equals(status)) {
      pointService.applyCheckInPenalty(
          userId,
          session,
          RECORD_STATUS_ABSENT,
          normalizePenaltyPoints(session.getAbsentPenaltyPoints(), DEFAULT_ABSENT_PENALTY_POINTS),
          operatorId);
    }
  }

  private void clearAttendanceEffects(Integer userId, CheckInSession session, Integer operatorId) {
    clearAttendanceEffects(userId, session, activityForSession(session), operatorId);
  }

  private void clearAttendanceEffects(
      Integer userId, CheckInSession session, ClubActivity activity, Integer operatorId) {
    pointService.revokeCheckInPoints(userId, session, activity, operatorId);
    pointService.revokeCheckInPenalty(userId, session, RECORD_STATUS_LATE, operatorId);
    pointService.revokeCheckInPenalty(userId, session, RECORD_STATUS_ABSENT, operatorId);
  }

  private void settleEndedEveningStudySessions(Club club) {
    if (club == null) return;
    Timestamp now = Times.now();
    for (CheckInSession session : sessionMapper.selectOpenEveningStarted(club.getId(), now)) {
      if (!isEveningStudy(session) || !isCheckinWindowEnded(session, now)) continue;
      for (CheckInTarget target : targetMapper.selectBySessionId(session.getId())) {
        CheckInRecord record = recordMapper.selectOneBySessionAndUser(session.getId(), target.getUserId());
        if (record == null) {
          applyRecordStatus(session, target.getUserId(), RECORD_STATUS_ABSENT, "auto_absent", null, now);
        }
      }
      session.setStatus("closed");
      sessionMapper.updateById(session);
    }
  }

  private Timestamp checkinDeadlineAt(CheckInSession session) {
    if (session == null) return null;
    if (!isEveningStudy(session)) return session.getEndAt();
    if (session.getStartAt() == null) return session.getEndAt();
    return Timestamp.from(
        session
            .getStartAt()
            .toInstant()
            .plusSeconds((long) normalizeCheckinWindowMinutes(session.getCheckinWindowMinutes()) * 60L));
  }

  private Timestamp lateDeadlineAt(CheckInSession session) {
    if (session == null || !isEveningStudy(session) || session.getStartAt() == null) return null;
    return Timestamp.from(
        session
            .getStartAt()
            .toInstant()
            .plusSeconds((long) normalizeLateAfterMinutes(session.getLateAfterMinutes()) * 60L));
  }

  private boolean shouldMarkLate(CheckInSession session, Timestamp now) {
    Timestamp deadline = lateDeadlineAt(session);
    return deadline != null && now != null && now.after(deadline);
  }

  private boolean isCheckinWindowEnded(CheckInSession session, Timestamp now) {
    Timestamp deadline = checkinDeadlineAt(session);
    return deadline != null && now != null && now.after(deadline);
  }

  private boolean isEveningStudy(CheckInSession session) {
    return session != null && SESSION_TYPE_EVENING_STUDY.equals(firstNonBlank(session.getSessionType(), SESSION_TYPE_REGULAR));
  }

  private int countRecords(List<CheckInRecord> records, String status) {
    if (records == null || records.isEmpty()) return 0;
    return (int) records.stream().filter(record -> status.equals(record.getStatus())).count();
  }

  private Result<String> validateEveningStudyRules(RequestEveningStudyScheduleDTO request) {
    int windowMinutes = normalizeCheckinWindowMinutes(request.getCheckinWindowMinutes());
    int lateAfterMinutes = normalizeLateAfterMinutes(request.getLateAfterMinutes());
    if (lateAfterMinutes >= windowMinutes) return Result.error(400, "迟到阈值必须小于签到窗口");
    return Result.success();
  }

  private int normalizeCheckinWindowMinutes(Integer value) {
    if (value == null) return DEFAULT_CHECKIN_WINDOW_MINUTES;
    return Math.max(1, Math.min(value, 24 * 60));
  }

  private int normalizeLateAfterMinutes(Integer value) {
    if (value == null) return DEFAULT_LATE_AFTER_MINUTES;
    return Math.max(0, Math.min(value, 24 * 60));
  }

  private long normalizePenaltyPoints(Long value, long fallback) {
    if (value == null) return fallback;
    return Math.max(0L, value);
  }

  private Timestamp firstNonNull(Timestamp value, Timestamp fallback) {
    return value == null ? fallback : value;
  }

  private String recordStatusMessage(String status) {
    if (RECORD_STATUS_LATE.equals(status)) return "已标记为迟到";
    if (RECORD_STATUS_ABSENT.equals(status)) return "已标记为旷课";
    return "已标记为已签到";
  }

  private CheckInSession findSession(Integer sessionId) {
    if (sessionId == null) return null;
    Club club = defaultClub();
    if (club == null) return null;
    CheckInSession session = sessionMapper.selectById(sessionId);
    return session != null && club.getId().equals(session.getClubId()) ? session : null;
  }

  private CheckInGroup findGroup(Integer groupId) {
    if (groupId == null) return null;
    Club club = defaultClub();
    if (club == null) return null;
    CheckInGroup group = groupMapper.selectById(groupId);
    return group != null && club.getId().equals(group.getClubId()) ? group : null;
  }

  private Map<Integer, User> loadUsers(List<Integer> userIds) {
    List<Integer> distinctIds = userIds.stream().distinct().toList();
    if (distinctIds.isEmpty()) return Map.of();
    return userMapper.selectBatchIds(distinctIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
  }

  private Club defaultClub() {
    return clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
  }

  private ClubActivity activityForSession(CheckInSession session) {
    return session == null || session.getActivityId() == null ? null : activityMapper.selectById(session.getActivityId());
  }

  private List<Integer> sanitizeTargetUserIds(List<Integer> values) {
    if (values == null) return List.of();
    return values.stream().filter(id -> id != null && id > 0).collect(Collectors.toCollection(LinkedHashSet::new)).stream().toList();
  }

  private List<Integer> resolveTargetUserIds(Integer groupId, List<Integer> values) {
    if (values != null) return sanitizeTargetUserIds(values);
    LinkedHashSet<Integer> ids = new LinkedHashSet<>();
    if (groupId != null) {
      groupMemberMapper.selectByGroupId(groupId).stream()
          .map(CheckInGroupMember::getUserId)
          .filter(id -> id != null && id > 0)
          .forEach(ids::add);
    }
    return ids.stream().toList();
  }

  private Result<String> validateUsers(List<Integer> userIds) {
    for (Integer userId : userIds) {
      if (userMapper.selectById(userId) == null) return Result.error(400, "存在无效人员");
    }
    return Result.success();
  }

  private void replaceGroupMembers(Integer groupId, List<Integer> userIds) {
    groupMemberMapper.deleteByGroupId(groupId);
    for (Integer userId : userIds) {
      groupMemberMapper.insert(CheckInGroupMember.builder().groupId(groupId).userId(userId).build());
    }
  }

  private void replaceSessionTargets(
      CheckInSession session, List<Integer> nextUserIds, ClubActivity revokeActivity) {
    Set<Integer> nextSet = new LinkedHashSet<>(nextUserIds);
    Set<Integer> existingSet =
        targetMapper.selectBySessionId(session.getId()).stream()
            .map(CheckInTarget::getUserId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    for (Integer userId : existingSet) {
      if (!nextSet.contains(userId)) {
        CheckInRecord record = recordMapper.selectOneBySessionAndUser(session.getId(), userId);
        if (record != null) {
          recordMapper.deleteById(record.getId());
          clearAttendanceEffects(userId, session, revokeActivity, currentUserId());
        }
      }
    }
    targetMapper.deleteBySessionId(session.getId());
    for (Integer userId : nextUserIds) {
      exclusionMapper.deleteBySessionIdAndUserId(session.getId(), userId);
      targetMapper.insert(CheckInTarget.builder().sessionId(session.getId()).userId(userId).build());
    }
  }

  private String normalizeStatus(String status) {
    String value = status == null || status.isBlank() ? "open" : status;
    return STATUSES.contains(value) ? value : null;
  }

  private String normalizeToken(String token) {
    if (token == null) return null;
    String trimmed = token.trim();
    return trimmed.startsWith(PAYLOAD_PREFIX) ? trimmed.substring(PAYLOAD_PREFIX.length()) : trimmed;
  }

  private CheckInSession resolveSession(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) return null;
    String trimmed = rawToken.trim();
    if (trimmed.startsWith(ROTATING_PAYLOAD_PREFIX)) return resolveRotatingSession(trimmed);
    String token = normalizeToken(trimmed);
    return token == null ? null : sessionMapper.selectByToken(token);
  }

  private CheckInSession resolveRotatingSession(String payload) {
    String[] parts = payload.substring(ROTATING_PAYLOAD_PREFIX.length()).split(":");
    if (parts.length != 3) return null;
    Integer sessionId;
    long window;
    try {
      sessionId = Integer.valueOf(parts[0]);
      window = Long.parseLong(parts[1]);
    } catch (NumberFormatException e) {
      return null;
    }
    long currentWindow = currentTokenWindow();
    if (window < currentWindow - ROTATING_TOKEN_GRACE_WINDOWS || window > currentWindow + 1) return null;
    CheckInSession session = findSession(sessionId);
    if (session == null) return null;
    return parts[2].equals(rotatingSignature(session, window)) ? session : null;
  }

  private String rotatingPayload(CheckInSession session) {
    long window = currentTokenWindow();
    return ROTATING_PAYLOAD_PREFIX + session.getId() + ":" + window + ":" + rotatingSignature(session, window);
  }

  private long currentTokenWindow() {
    return System.currentTimeMillis() / 1000L / ROTATING_TOKEN_WINDOW_SECONDS;
  }

  private String rotatingSignature(CheckInSession session, long window) {
    String source = session.getId() + ":" + session.getToken() + ":" + window;
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, 16);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Integer currentUserId() {
    return StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
  }

  private long safePoints(Long value) {
    return value == null ? 0L : Math.max(0L, value);
  }

  private int safeInt(Integer value) {
    return value == null ? 0 : value;
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private Time parseLocalTime(String value) {
    if (value == null || value.isBlank()) return null;
    String trimmed = value.trim();
    try {
      return Time.valueOf(LocalTime.parse(trimmed.length() == 5 ? trimmed + ":00" : trimmed));
    } catch (RuntimeException e) {
      return null;
    }
  }

  private String formatTime(Time value) {
    return value == null ? null : value.toLocalTime().toString();
  }

  private LocalDate parseDateOrToday(String value) {
    if (value == null || value.isBlank()) return LocalDate.now(Times.BUSINESS_ZONE);
    try {
      return LocalDate.parse(value.trim());
    } catch (RuntimeException e) {
      return null;
    }
  }

  private Timestamp timestampOf(LocalDate date, Time time) {
    LocalTime localTime = time == null ? LocalTime.MIDNIGHT : time.toLocalTime();
    return Timestamp.from(LocalDateTime.of(date, localTime).atZone(Times.BUSINESS_ZONE).toInstant());
  }

  private LocalDate endDateFor(LocalDate date, Time startTime, Time endTime) {
    if (startTime == null || endTime == null) return date;
    return endTime.toLocalTime().isAfter(startTime.toLocalTime()) ? date : date.plusDays(1);
  }

}
