package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInRecordStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInScanDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInTargetsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateCheckInSessionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInGroup;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInGroupMember;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInRecord;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInSession;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInTarget;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInGroupMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInGroupMemberMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInRecordMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInSessionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInTargetMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubActivityMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.CheckInService;
import edu.jmi.openatom.server.openatomsystem.service.PointService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInGroupVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInRecordVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInSessionVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";
  private static final List<String> STATUSES = List.of("draft", "open", "closed");
  private static final String PAYLOAD_PREFIX = "openatom-checkin:";
  private static final String ROTATING_PAYLOAD_PREFIX = "oaci2:";
  private static final long ROTATING_TOKEN_WINDOW_SECONDS = 30L;
  private static final long ROTATING_TOKEN_GRACE_WINDOWS = 10L;

  private final ClubMapper clubMapper;
  private final ClubActivityMapper activityMapper;
  private final UserMapper userMapper;
  private final CheckInGroupMapper groupMapper;
  private final CheckInGroupMemberMapper groupMemberMapper;
  private final CheckInSessionMapper sessionMapper;
  private final CheckInTargetMapper targetMapper;
  private final CheckInRecordMapper recordMapper;
  private final PointService pointService;

  @Override
  public Result<List<ResponseCheckInSessionVO>> list(String status) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
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
    Map<Integer, CheckInRecord> recordMap = records.stream().collect(Collectors.toMap(CheckInRecord::getUserId, Function.identity(), (a, b) -> a));
    Map<Integer, User> users = loadUsers(targets.stream().map(CheckInTarget::getUserId).toList());
    return Result.success(targets.stream().map(target -> toRecordVO(users.get(target.getUserId()), recordMap.get(target.getUserId()))).toList());
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
    if (!"checked".equals(status) && !"pending".equals(status)) return Result.error(400, "签到状态不合法");
    CheckInRecord record = recordMapper.selectOneBySessionAndUser(sessionId, userId);
    if ("pending".equals(status)) {
      if (record != null) recordMapper.deleteById(record.getId());
      pointService.revokeCheckInPoints(userId, session, activityForSession(session), currentUserId());
      return Result.success(toRecordVO(userMapper.selectById(userId), null), "已标记为未签到");
    }
    if (record == null) {
      record = CheckInRecord.builder().sessionId(sessionId).userId(userId)
          .checkinAt(Times.now()).source("manual").status("checked").build();
      recordMapper.insert(record);
    } else {
      record.setStatus("checked");
      record.setCheckinAt(Times.now());
      record.setSource("manual");
      recordMapper.updateById(record);
    }
    pointService.grantCheckInPoints(userId, session, activityForSession(session), currentUserId());
    return Result.success(toRecordVO(userMapper.selectById(userId), record), "已标记为已签到");
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
    if (session.getEndAt() != null && now.after(session.getEndAt())) return Result.error(400, "签到已结束");
    Integer userId = StpUtil.getLoginIdAsInt();
    CheckInTarget target = targetMapper.selectOneBySessionAndUser(session.getId(), userId);
    if (target == null) return Result.error(403, "你不在本次签到发放名单中");
    CheckInRecord record = recordMapper.selectOneBySessionAndUser(session.getId(), userId);
    if (record == null) {
      record = CheckInRecord.builder().sessionId(session.getId()).userId(userId)
          .checkinAt(now).source("miniapp_scan").status("checked").build();
      recordMapper.insert(record);
      pointService.grantCheckInPoints(userId, session, activityForSession(session), null);
      return Result.success(toRecordVO(userMapper.selectById(userId), record), "签到成功");
    }
    return Result.success(toRecordVO(userMapper.selectById(userId), record), "已签到，无需重复签到");
  }

  private ResponseCheckInSessionVO toSessionVO(CheckInSession session) {
    List<CheckInTarget> targets = targetMapper.selectBySessionId(session.getId());
    int checkedCount = recordMapper.selectBySessionId(session.getId()).size();
    ClubActivity activity = session.getActivityId() == null ? null : activityMapper.selectById(session.getActivityId());
    return ResponseCheckInSessionVO.builder()
        .id(session.getId()).activityId(session.getActivityId()).groupId(session.getGroupId())
        .activityTitle(activity == null ? null : activity.getTitle())
        .title(session.getTitle()).location(session.getLocation())
        .startAt(session.getStartAt()).endAt(session.getEndAt())
        .status(session.getStatus()).qrPayload(rotatingPayload(session))
        .checkinPoints(safePoints(session.getCheckinPoints()))
        .targetCount(targets.size()).checkedCount(checkedCount)
        .targetUserIds(targets.stream().map(CheckInTarget::getUserId).toList())
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
        .checkinAt(record == null ? null : record.getCheckinAt())
        .build();
  }

  private User buildSafeUser(User user) {
    return User.builder().id(user.getId()).userName(user.getUserName()).realName(user.getRealName())
        .phone(user.getPhone()).email(user.getEmail()).studentId(user.getStudentId())
        .college(user.getCollege()).major(user.getMajor()).grade(user.getGrade())
        .className(user.getClassName()).userStatus(user.getUserStatus()).build();
  }

  private ResponseCheckInGroupVO toGroupVO(CheckInGroup group) {
    List<Integer> userIds = groupMemberMapper.selectByGroupId(group.getId()).stream()
        .map(CheckInGroupMember::getUserId).toList();
    return ResponseCheckInGroupVO.builder()
        .id(group.getId())
        .name(group.getName())
        .memberCount(userIds.size())
        .userIds(userIds)
        .build();
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
          pointService.revokeCheckInPoints(userId, session, revokeActivity, currentUserId());
        }
      }
    }
    targetMapper.deleteBySessionId(session.getId());
    for (Integer userId : nextUserIds) {
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

  private int safePoints(Integer value) {
    return value == null ? 0 : Math.max(0, value);
  }
}
