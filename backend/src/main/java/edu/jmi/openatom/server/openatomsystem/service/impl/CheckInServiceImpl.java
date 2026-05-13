package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInScanDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateCheckInSessionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInRecord;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInSession;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInTarget;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInRecordMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInSessionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.CheckInTargetMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubActivityMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.CheckInService;
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
  private static final String ROTATING_PAYLOAD_PREFIX = "openatom-checkin-v2:";
  private static final long ROTATING_TOKEN_WINDOW_SECONDS = 30L;
  private static final long ROTATING_TOKEN_GRACE_WINDOWS = 10L;

  private final ClubMapper clubMapper;
  private final ClubActivityMapper activityMapper;
  private final UserMapper userMapper;
  private final CheckInSessionMapper sessionMapper;
  private final CheckInTargetMapper targetMapper;
  private final CheckInRecordMapper recordMapper;

  @Override
  public Result<List<ResponseCheckInSessionVO>> list(String status) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    return Result.success(sessionMapper.selectByClubAndStatus(club.getId(), status).stream().map(this::toSessionVO).toList());
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
    List<Integer> targetUserIds = sanitizeTargetUserIds(request.getTargetUserIds());
    if (targetUserIds.isEmpty()) return Result.error(400, "请选择发放人员");
    for (Integer userId : targetUserIds) {
      if (userMapper.selectById(userId) == null) return Result.error(400, "存在无效的发放人员");
    }
    if (request.getActivityId() != null) {
      ClubActivity activity = activityMapper.selectOneByIdAndClubId(request.getActivityId(), club.getId());
      if (activity == null) return Result.error(404, "活动不存在");
    }
    CheckInSession session = CheckInSession.builder()
        .clubId(club.getId())
        .activityId(request.getActivityId())
        .title(request.getTitle().trim())
        .location(trimToNull(request.getLocation()))
        .startAt(Times.parseTimestamp(request.getStartAt()))
        .endAt(Times.parseTimestamp(request.getEndAt()))
        .status(status)
        .token(UUID.randomUUID().toString().replace("-", ""))
        .createdBy(StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null)
        .build();
    sessionMapper.insert(session);
    for (Integer userId : targetUserIds) {
      targetMapper.insert(CheckInTarget.builder().sessionId(session.getId()).userId(userId).build());
    }
    return Result.success(session.getId(), "签到已发布");
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
      return Result.success(toRecordVO(userMapper.selectById(userId), record), "签到成功");
    }
    return Result.success(toRecordVO(userMapper.selectById(userId), record), "已签到，无需重复签到");
  }

  private ResponseCheckInSessionVO toSessionVO(CheckInSession session) {
    List<CheckInTarget> targets = targetMapper.selectBySessionId(session.getId());
    int checkedCount = recordMapper.selectBySessionId(session.getId()).size();
    ClubActivity activity = session.getActivityId() == null ? null : activityMapper.selectById(session.getActivityId());
    return ResponseCheckInSessionVO.builder()
        .id(session.getId()).activityId(session.getActivityId())
        .activityTitle(activity == null ? null : activity.getTitle())
        .title(session.getTitle()).location(session.getLocation())
        .startAt(session.getStartAt()).endAt(session.getEndAt())
        .status(session.getStatus()).qrPayload(rotatingPayload(session))
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

  private CheckInSession findSession(Integer sessionId) {
    if (sessionId == null) return null;
    Club club = defaultClub();
    if (club == null) return null;
    CheckInSession session = sessionMapper.selectById(sessionId);
    return session != null && club.getId().equals(session.getClubId()) ? session : null;
  }

  private Map<Integer, User> loadUsers(List<Integer> userIds) {
    List<Integer> distinctIds = userIds.stream().distinct().toList();
    if (distinctIds.isEmpty()) return Map.of();
    return userMapper.selectBatchIds(distinctIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
  }

  private Club defaultClub() {
    return clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
  }

  private List<Integer> sanitizeTargetUserIds(List<Integer> values) {
    if (values == null) return List.of();
    return values.stream().filter(id -> id != null && id > 0).collect(Collectors.toCollection(LinkedHashSet::new)).stream().toList();
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
      return HexFormat.of().formatHex(digest).substring(0, 24);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
