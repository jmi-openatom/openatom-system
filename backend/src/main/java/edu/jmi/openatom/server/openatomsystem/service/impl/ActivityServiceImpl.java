package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCacheEvict;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCached;
import edu.jmi.openatom.server.openatomsystem.dto.RequestActivityRegistrationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ActivityRegistration;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.mapper.ActivityRegistrationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubActivityMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMembershipMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.service.ActivityService;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社团活动管理实现类
 *
 * <p>负责社团活动的创建, 更新, 删除, 查询以及活动报名管理, 包含报名资格校验和状态流转等核心业务逻辑
 */
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";
  private static final List<String> STATUSES = List.of("draft", "published", "closed", "cancelled");

  private final ClubMapper clubMapper;
  private final ClubActivityMapper clubActivityMapper;
  private final ActivityRegistrationMapper activityRegistrationMapper;
  private final ClubMembershipMapper clubMembershipMapper;

  @Override
  @RedisCached(cacheName = "site", key = "'admin-activities:' + (#p0 == null ? 'all' : #p0)", ttlSeconds = 300)
  public Result<List<ClubActivity>> list(String status) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    return Result.success(clubActivityMapper.selectByClubIdAndStatus(club.getId(), status));
  }

  @Override
  @RedisCached(cacheName = "site", key = "'admin-activity:' + #p0", ttlSeconds = 300)
  public Result<ClubActivity> detail(Integer activityId) {
    ClubActivity activity = findActivity(activityId);
    return activity == null ? Result.error(404, "活动不存在") : Result.success(activity);
  }

  @Override
  @RedisCacheEvict(cacheNames = {"site"})
  public Result<String> create(RequestCreateActivityDTO request) {
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    String status = normalizeStatus(request.getStatus());
    if (status == null) return Result.error(400, "活动状态不合法");
    ClubActivity activity = ClubActivity.builder().clubId(club.getId()).title(request.getTitle())
        .summary(request.getSummary()).descriptionMarkdown(request.getDescriptionMarkdown())
        .activityAt(Times.parseTimestamp(request.getActivityAt()))
        .endAt(Times.parseTimestamp(request.getEndAt())).location(request.getLocation())
        .status(status).coverUrl(request.getCoverUrl())
        .registrationRequired(Boolean.TRUE.equals(request.getRegistrationRequired()))
        .registrationStartAt(Times.parseTimestamp(request.getRegistrationStartAt()))
        .registrationEndAt(Times.parseTimestamp(request.getRegistrationEndAt()))
        .registrationFields(Jsons.stringify(request.getRegistrationFields()))
        .participationPoints(safePoints(request.getParticipationPoints()))
        .build();
    int rows = clubActivityMapper.insert(activity);
    return rows > 0 ? Result.success("活动创建成功") : Result.error("活动创建失败");
  }

  @Override
  @RedisCacheEvict(cacheNames = {"site"})
  public Result<String> update(Integer activityId, RequestUpdateActivityDTO request) {
    ClubActivity activity = findActivity(activityId);
    if (activity == null) return Result.error(404, "活动不存在");
    if (request.getTitle() != null) activity.setTitle(request.getTitle());
    if (request.getSummary() != null) activity.setSummary(request.getSummary());
    if (request.getDescriptionMarkdown() != null) activity.setDescriptionMarkdown(request.getDescriptionMarkdown());
    if (request.getActivityAt() != null) activity.setActivityAt(Times.parseTimestamp(request.getActivityAt()));
    if (request.getEndAt() != null) activity.setEndAt(Times.parseTimestamp(request.getEndAt()));
    if (request.getLocation() != null) activity.setLocation(request.getLocation());
    if (request.getStatus() != null) {
      String status = normalizeStatus(request.getStatus());
      if (status == null) return Result.error(400, "活动状态不合法");
      activity.setStatus(status);
    }
    if (request.getCoverUrl() != null) activity.setCoverUrl(request.getCoverUrl());
    if (request.getRegistrationRequired() != null) activity.setRegistrationRequired(request.getRegistrationRequired());
    if (request.getRegistrationStartAt() != null) activity.setRegistrationStartAt(Times.parseTimestamp(request.getRegistrationStartAt()));
    if (request.getRegistrationEndAt() != null) activity.setRegistrationEndAt(Times.parseTimestamp(request.getRegistrationEndAt()));
    if (request.getRegistrationFields() != null) activity.setRegistrationFields(Jsons.stringify(request.getRegistrationFields()));
    if (request.getParticipationPoints() != null) activity.setParticipationPoints(safePoints(request.getParticipationPoints()));
    int rows = clubActivityMapper.updateById(activity);
    return rows > 0 ? Result.success("活动更新成功") : Result.error("活动更新失败");
  }

  @Override
  @RedisCacheEvict(cacheNames = {"site"})
  public Result<String> delete(Integer activityId) {
    ClubActivity activity = findActivity(activityId);
    if (activity == null) return Result.error(404, "活动不存在");
    int rows = clubActivityMapper.deleteById(activityId);
    return rows > 0 ? Result.success("活动已删除") : Result.error("活动删除失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> register(Integer activityId, RequestActivityRegistrationDTO request) {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录后再报名");
    ClubActivity activity = findActivity(activityId);
    if (activity == null || !"published".equals(activity.getStatus()))
      return Result.error(404, "活动不存在或未发布");
    if (!Boolean.TRUE.equals(activity.getRegistrationRequired()))
      return Result.error(400, "该活动无需官网报名");
    Timestamp now = Times.now();
    if (activity.getRegistrationStartAt() != null && now.before(activity.getRegistrationStartAt()))
      return Result.error(400, "报名尚未开始");
    if (activity.getRegistrationEndAt() != null && now.after(activity.getRegistrationEndAt()))
      return Result.error(400, "报名已结束");
    Integer userId = StpUtil.getLoginIdAsInt();
    Long membershipCount = clubMembershipMapper.countActiveMembership(userId, activity.getClubId(), "active");
    if (membershipCount == null || membershipCount == 0)
      return Result.error(403, "无权限，请先加入社团");
    ActivityRegistration exists = activityRegistrationMapper.selectOneByActivityAndUser(activityId, userId);
    if (exists != null) {
      exists.setFormData(Jsons.stringify(request == null ? null : request.getFormData()));
      exists.setStatus("submitted");
      activityRegistrationMapper.updateById(exists);
      return Result.success("报名信息已更新");
    }
    activityRegistrationMapper.insert(ActivityRegistration.builder().activityId(activityId).userId(userId)
        .formData(Jsons.stringify(request == null ? null : request.getFormData())).status("submitted").build());
    return Result.success("报名成功");
  }

  @Override
  public Result<List<ActivityRegistration>> registrations(Integer activityId) {
    if (findActivity(activityId) == null) return Result.error(404, "活动不存在");
    return Result.success(activityRegistrationMapper.selectByActivityIdOrdered(activityId));
  }

  private ClubActivity findActivity(Integer activityId) {
    if (activityId == null) return null;
    Club club = defaultClub();
    if (club == null) return null;
    return clubActivityMapper.selectOneByIdAndClubId(activityId, club.getId());
  }

  private Club defaultClub() {
    return clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
  }

  private String normalizeStatus(String status) {
    String value = status == null || status.isBlank() ? "draft" : status;
    return STATUSES.contains(value) ? value : null;
  }

  private long safePoints(Long value) {
    return value == null ? 0L : Math.max(0L, value);
  }
}
