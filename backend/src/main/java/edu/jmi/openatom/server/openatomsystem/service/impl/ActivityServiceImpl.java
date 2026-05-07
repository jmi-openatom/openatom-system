package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestActivityRegistrationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ActivityRegistration;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
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
  public ApiResponse<List<ClubActivity>> list(String status) {
    Club club = defaultClub();
    if (club == null) return ApiResponse.error(404, "默认社团不存在");
    return ApiResponse.success(clubActivityMapper.selectByClubIdAndStatus(club.getId(), status));
  }

  @Override
  public ApiResponse<ClubActivity> detail(Integer activityId) {
    ClubActivity activity = findActivity(activityId);
    return activity == null ? ApiResponse.error(404, "活动不存在") : ApiResponse.success(activity);
  }

  @Override
  public ApiResponse<String> create(RequestCreateActivityDTO request) {
    Club club = defaultClub();
    if (club == null) return ApiResponse.error(404, "默认社团不存在");
    String status = normalizeStatus(request.getStatus());
    if (status == null) return ApiResponse.error(400, "活动状态不合法");
    ClubActivity activity = ClubActivity.builder().clubId(club.getId()).title(request.getTitle())
        .summary(request.getSummary()).descriptionMarkdown(request.getDescriptionMarkdown())
        .activityAt(Times.parseTimestamp(request.getActivityAt()))
        .endAt(Times.parseTimestamp(request.getEndAt())).location(request.getLocation())
        .status(status).coverUrl(request.getCoverUrl())
        .registrationRequired(Boolean.TRUE.equals(request.getRegistrationRequired()))
        .registrationStartAt(Times.parseTimestamp(request.getRegistrationStartAt()))
        .registrationEndAt(Times.parseTimestamp(request.getRegistrationEndAt()))
        .registrationFields(Jsons.stringify(request.getRegistrationFields())).build();
    int rows = clubActivityMapper.insert(activity);
    return rows > 0 ? ApiResponse.success("活动创建成功") : ApiResponse.error("活动创建失败");
  }

  @Override
  public ApiResponse<String> update(Integer activityId, RequestUpdateActivityDTO request) {
    ClubActivity activity = findActivity(activityId);
    if (activity == null) return ApiResponse.error(404, "活动不存在");
    if (request.getTitle() != null) activity.setTitle(request.getTitle());
    if (request.getSummary() != null) activity.setSummary(request.getSummary());
    if (request.getDescriptionMarkdown() != null) activity.setDescriptionMarkdown(request.getDescriptionMarkdown());
    if (request.getActivityAt() != null) activity.setActivityAt(Times.parseTimestamp(request.getActivityAt()));
    if (request.getEndAt() != null) activity.setEndAt(Times.parseTimestamp(request.getEndAt()));
    if (request.getLocation() != null) activity.setLocation(request.getLocation());
    if (request.getStatus() != null) {
      String status = normalizeStatus(request.getStatus());
      if (status == null) return ApiResponse.error(400, "活动状态不合法");
      activity.setStatus(status);
    }
    if (request.getCoverUrl() != null) activity.setCoverUrl(request.getCoverUrl());
    if (request.getRegistrationRequired() != null) activity.setRegistrationRequired(request.getRegistrationRequired());
    if (request.getRegistrationStartAt() != null) activity.setRegistrationStartAt(Times.parseTimestamp(request.getRegistrationStartAt()));
    if (request.getRegistrationEndAt() != null) activity.setRegistrationEndAt(Times.parseTimestamp(request.getRegistrationEndAt()));
    if (request.getRegistrationFields() != null) activity.setRegistrationFields(Jsons.stringify(request.getRegistrationFields()));
    int rows = clubActivityMapper.updateById(activity);
    return rows > 0 ? ApiResponse.success("活动更新成功") : ApiResponse.error("活动更新失败");
  }

  @Override
  public ApiResponse<String> delete(Integer activityId) {
    ClubActivity activity = findActivity(activityId);
    if (activity == null) return ApiResponse.error(404, "活动不存在");
    int rows = clubActivityMapper.deleteById(activityId);
    return rows > 0 ? ApiResponse.success("活动已删除") : ApiResponse.error("活动删除失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> register(Integer activityId, RequestActivityRegistrationDTO request) {
    if (!StpUtil.isLogin()) return ApiResponse.error(401, "请先登录后再报名");
    ClubActivity activity = findActivity(activityId);
    if (activity == null || !"published".equals(activity.getStatus()))
      return ApiResponse.error(404, "活动不存在或未发布");
    if (!Boolean.TRUE.equals(activity.getRegistrationRequired()))
      return ApiResponse.error(400, "该活动无需官网报名");
    Timestamp now = Times.now();
    if (activity.getRegistrationStartAt() != null && now.before(activity.getRegistrationStartAt()))
      return ApiResponse.error(400, "报名尚未开始");
    if (activity.getRegistrationEndAt() != null && now.after(activity.getRegistrationEndAt()))
      return ApiResponse.error(400, "报名已结束");
    Integer userId = StpUtil.getLoginIdAsInt();
    Long membershipCount = clubMembershipMapper.countActiveMembership(userId, activity.getClubId(), "active");
    if (membershipCount == null || membershipCount == 0)
      return ApiResponse.error(403, "无权限，请先加入社团");
    ActivityRegistration exists = activityRegistrationMapper.selectOneByActivityAndUser(activityId, userId);
    if (exists != null) {
      exists.setFormData(Jsons.stringify(request == null ? null : request.getFormData()));
      exists.setStatus("submitted");
      activityRegistrationMapper.updateById(exists);
      return ApiResponse.success("报名信息已更新");
    }
    activityRegistrationMapper.insert(ActivityRegistration.builder().activityId(activityId).userId(userId)
        .formData(Jsons.stringify(request == null ? null : request.getFormData())).status("submitted").build());
    return ApiResponse.success("报名成功");
  }

  @Override
  public ApiResponse<List<ActivityRegistration>> registrations(Integer activityId) {
    if (findActivity(activityId) == null) return ApiResponse.error(404, "活动不存在");
    return ApiResponse.success(activityRegistrationMapper.selectByActivityIdOrdered(activityId));
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
}
