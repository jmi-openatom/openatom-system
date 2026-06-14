package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCached;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseClubHomeVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseRecruitmentVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseRecruitmentDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSiteFormDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSiteFormsVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSiteProgressVO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.service.RegistrationSettingService;
import edu.jmi.openatom.server.openatomsystem.service.SiteService;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 站点数据聚合实现类
 *
 * <p>负责前台页面的数据聚合展示, 包括社团首页信息, 招新列表, 活动详情, 入会进度查询等
 */
@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";
  private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM.dd");

  private final ClubMapper clubMapper;
  private final ClubMembershipMapper clubMembershipMapper;
  private final ClubDepartmentMapper clubDepartmentMapper;
  private final ClubPositionMapper clubPositionMapper;
  private final UserMapper userMapper;
  private final RecruitmentCampaignMapper recruitmentCampaignMapper;
  private final MembershipApplicationMapper applicationMapper;
  private final InterviewMapper interviewMapper;
  private final ClubActivityMapper clubActivityMapper;
  private final ClubAwardMapper clubAwardMapper;
  private final SiteFormMapper siteFormMapper;
  private final RegistrationSettingService registrationSettingService;

  @Override
  @RedisCached(
      cacheName = "site",
      key = "'club-home:' + (#p0 == null || #p0.isBlank() ? 'default' : #p0)",
      ttlSeconds = 600)
  public Result<ResponseClubHomeVO> getClubHome(String clubCode) {
    Club club = findClub(clubCode);
    if (club == null) {
      return Result.error(404, "默认社团不存在");
    }
    List<ClubMembership> memberships = clubMembershipMapper.selectByClubIdOrdered(club.getId());
    List<RecruitmentCampaign> campaigns =
        recruitmentCampaignMapper.selectByClubIdOrderedByApplyStart(club.getId());
    List<ClubDepartment> departments = clubDepartmentMapper.selectByClubIdOrdered(club.getId());
    List<ClubActivity> activities = clubActivityMapper.selectPublishedByClubId(club.getId(), 6);
    List<ClubAward> awards = clubAwardMapper.selectByClubIdOrdered(club.getId());
    return Result.success(
        ResponseClubHomeVO.builder()
            .club(toClubProfile(club))
            .metrics(buildMetrics(memberships, campaigns, activities, awards))
            .focusAreas(buildFocusAreas(departments))
            .activities(activities.stream().map(this::toActivity).toList())
            .people(buildPeople(memberships))
            .awards(awards.stream().map(this::toAward).toList())
            .techStack(departments.stream().map(ClubDepartment::getName).limit(10).toList())
            .build());
  }

  @Override
  @RedisCached(cacheName = "site", key = "'activities'", ttlSeconds = 300)
  public Result<List<ClubActivity>> getActivities() {
    Club club = findClub(null);
    if (club == null) {
      return Result.error(404, "默认社团不存在");
    }
    return Result.success(clubActivityMapper.selectPublishedByClubIdAll(club.getId()));
  }

  @Override
  @RedisCached(cacheName = "site", key = "'activity:' + #p0", ttlSeconds = 300)
  public Result<ClubActivity> getActivityDetail(Integer activityId) {
    Club club = findClub(null);
    if (club == null) {
      return Result.error(404, "默认社团不存在");
    }
    ClubActivity activity =
        clubActivityMapper.selectPublishedByIdAndClubId(activityId, club.getId());
    return activity == null ? Result.error(404, "活动不存在") : Result.success(activity);
  }

  @Override
  @RedisCached(cacheName = "site", key = "'public-clubs'", ttlSeconds = 600)
  public Result<List<Club>> getPublicClubs() {
    return Result.success(clubMapper.selectActiveClubs());
  }

  @Override
  @RedisCached(cacheName = "site", key = "'forms:' + (#p0 == null ? 'default' : #p0)", ttlSeconds = 300)
  public Result<ResponseSiteFormsVO> getPublicForms(Integer clubId) {
    Club club = findClub(clubId, null);
    if (club == null) {
      return Result.error(404, "默认社团不存在");
    }
    List<SiteForm> forms = siteFormMapper.selectOpenByClubId(club.getId());
    return Result.success(ResponseSiteFormsVO.builder().club(club).forms(forms).build());
  }

  @Override
  public Result<ResponseSiteProgressVO> getMyProgress() {
    if (!StpUtil.isLogin()) {
      return Result.error(401, "请先登录后查看入会进度");
    }
    Integer userId = StpUtil.getLoginIdAsInt();
    List<MembershipApplication> applications = applicationMapper.selectByUserIdOrdered(userId);
    if (applications.isEmpty()) {
      return Result.success(ResponseSiteProgressVO.builder().applications(List.of()).build());
    }
    Map<Integer, Club> clubs =
        selectMap(
            applications.stream()
                .map(MembershipApplication::getClubId)
                .filter(Objects::nonNull)
                .toList(),
            clubMapper::selectBatchIds,
            Club::getId);
    Map<Integer, User> users =
        selectMap(
            applications.stream()
                .map(MembershipApplication::getUserId)
                .filter(Objects::nonNull)
                .toList(),
            userMapper::selectBatchIds,
            User::getId);
    Map<Integer, RecruitmentCampaign> campaigns =
        selectMap(
            applications.stream()
                .map(MembershipApplication::getCampaignId)
                .filter(Objects::nonNull)
                .toList(),
            recruitmentCampaignMapper::selectBatchIds,
            RecruitmentCampaign::getId);
    Map<Integer, ClubDepartment> departments =
        selectMap(
            applications.stream()
                .flatMap(
                    a ->
                        java.util.stream.Stream.of(
                            a.getFirstChoiceDepartmentId(), a.getSecondChoiceDepartmentId()))
                .filter(Objects::nonNull)
                .toList(),
            clubDepartmentMapper::selectBatchIds,
            ClubDepartment::getId);
    List<Integer> appIds = applications.stream().map(MembershipApplication::getId).toList();
    Map<Integer, List<Interview>> interviewsByApplication =
        interviewMapper.selectByApplicationIds(appIds).stream()
            .collect(Collectors.groupingBy(Interview::getApplicationId));
    return Result.success(
        ResponseSiteProgressVO.builder()
            .applications(
                applications.stream()
                    .map(
                        a ->
                            toProgressApplication(
                                a, users, clubs, campaigns, departments, interviewsByApplication))
                    .toList())
            .build());
  }

  @Override
  @RedisCached(cacheName = "site", key = "'register-enabled'", ttlSeconds = 600)
  public Result<Boolean> getRegisterEnabled() {
    return Result.success(registrationSettingService.isRegisterEnabled());
  }

  @Override
  @RedisCached(
      cacheName = "site",
      key = "'recruitment:' + (#p0 == null ? 'default' : #p0)",
      ttlSeconds = 300)
  public Result<ResponseRecruitmentVO> getRecruitment(Integer clubId) {
    Club club = findClub(clubId, null);
    if (club == null) {
      return Result.error(404, "默认社团不存在");
    }
    List<RecruitmentCampaign> campaigns =
        recruitmentCampaignMapper.selectOpenByClubId(club.getId());
    List<ClubDepartment> departments = clubDepartmentMapper.selectByClubIdOrdered(club.getId());
    return Result.success(
        ResponseRecruitmentVO.builder()
            .club(club)
            .campaigns(campaigns)
            .departments(departments)
            .build());
  }

  @Override
  @RedisCached(cacheName = "site", key = "'recruitment-detail:' + #p0", ttlSeconds = 300)
  public Result<ResponseRecruitmentDetailVO> getRecruitmentDetail(Integer campaignId) {
    RecruitmentCampaign campaign =
        campaignId == null ? null : recruitmentCampaignMapper.selectById(campaignId);
    if (campaign == null) return Result.error(404, "招新计划不存在");
    Club club = clubMapper.selectById(campaign.getClubId());
    if (club == null) return Result.error(404, "社团不存在");
    List<ClubDepartment> departments = clubDepartmentMapper.selectByClubIdOrdered(club.getId());
    return Result.success(
        ResponseRecruitmentDetailVO.builder()
            .club(club)
            .campaign(campaign)
            .departments(departments)
            .build());
  }

  @Override
  @RedisCached(cacheName = "site", key = "'form-detail:' + #p0", ttlSeconds = 300)
  public Result<ResponseSiteFormDetailVO> getFormDetail(Integer formId) {
    SiteForm form = siteFormMapper.selectById(formId);
    if (form == null) return Result.error(404, "表单不存在");
    Club club = clubMapper.selectById(form.getClubId());
    if (club == null) return Result.error(404, "社团不存在");
    return Result.success(ResponseSiteFormDetailVO.builder().club(club).form(form).build());
  }

  private Club findClub(String clubCode) {
    return findClub(null, clubCode);
  }

  private Club findClub(Integer clubId, String clubCode) {
    if (clubId != null) {
      Club club = clubMapper.selectById(clubId);
      if (club != null) return club;
    }
    String code = isBlank(clubCode) ? DEFAULT_CLUB_CODE : clubCode;
    return clubMapper.selectDefaultClub(code);
  }

  private ResponseClubHomeVO.ClubProfile toClubProfile(Club club) {
    return ResponseClubHomeVO.ClubProfile.builder()
        .id(club.getId())
        .name(club.getName())
        .code(club.getCode())
        .category(club.getCategory())
        .description(club.getDescription())
        .logoUrl(club.getLogoUrl())
        .recruitmentStatus(club.getRecruitmentStatus())
        .build();
  }

  private List<ResponseClubHomeVO.Metric> buildMetrics(
      List<ClubMembership> memberships,
      List<RecruitmentCampaign> campaigns,
      List<ClubActivity> activities,
      List<ClubAward> awards) {
    long activeMembers =
        memberships.stream().filter(m -> !"left".equalsIgnoreCase(m.getStatus())).count();
    long openCampaigns =
        campaigns.stream().filter(c -> "published".equalsIgnoreCase(c.getStatus())).count();
    return List.of(
        metric("在册成员", activeMembers, "来自成员关系表"),
        metric("年度活动", activities.size(), "来自社团活动表"),
        metric("竞赛获奖", awards.size(), "来自比赛获奖表"),
        metric("招新计划", openCampaigns, "当前发布中的批次"));
  }

  private ResponseClubHomeVO.Metric metric(String label, long value, String note) {
    return ResponseClubHomeVO.Metric.builder()
        .label(label)
        .value(String.valueOf(value))
        .note(note)
        .build();
  }

  private List<ResponseClubHomeVO.FocusArea> buildFocusAreas(List<ClubDepartment> departments) {
    List<String> icons = List.of("monitor", "cpu", "lightning", "phone");
    return departments.stream()
        .limit(4)
        .map(
            d ->
                focus(
                    d.getName(),
                    d.getDescription(),
                    icons.get(departments.indexOf(d) % icons.size())))
        .toList();
  }

  private ResponseClubHomeVO.FocusArea focus(String title, String description, String icon) {
    return ResponseClubHomeVO.FocusArea.builder()
        .title(title)
        .description(description)
        .icon(icon)
        .build();
  }

  private ResponseClubHomeVO.Activity toActivity(ClubActivity activity) {
    return ResponseClubHomeVO.Activity.builder()
        .id(activity.getId())
        .date(formatMonthDay(activity.getActivityAt()))
        .title(activity.getTitle())
        .description(activity.getSummary())
        .location(activity.getLocation())
        .status(activity.getStatus())
        .coverUrl(activity.getCoverUrl() == null ? "" : activity.getCoverUrl())
        .build();
  }

  private List<ResponseClubHomeVO.Person> buildPeople(List<ClubMembership> memberships) {
    List<ClubMembership> topMemberships =
        memberships.stream()
            .filter(m -> m.getUserId() != null)
            .filter(m -> Boolean.TRUE.equals(m.getFeatured()))
            .filter(m -> !"left".equalsIgnoreCase(m.getStatus()))
            .sorted(
                Comparator.comparing(
                        ClubMembership::getFeatured,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        ClubMembership::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                        ClubMembership::getJoinedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    if (topMemberships.isEmpty()) return List.of();
    Map<Integer, User> users =
        userMapper
            .selectBatchIds(
                topMemberships.stream()
                    .map(ClubMembership::getUserId)
                    .filter(Objects::nonNull)
                    .toList())
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    Map<Integer, ClubDepartment> depts =
        selectDepartments(topMemberships).stream()
            .collect(Collectors.toMap(ClubDepartment::getId, Function.identity()));
    Map<Integer, ClubPosition> positions =
        selectPositions(topMemberships).stream()
            .collect(Collectors.toMap(ClubPosition::getId, Function.identity()));
    return topMemberships.stream()
        .map(m -> toPerson(m, users, depts, positions))
        .filter(Objects::nonNull)
        .toList();
  }

  private List<ClubDepartment> selectDepartments(List<ClubMembership> memberships) {
    List<Integer> ids =
        memberships.stream()
            .map(ClubMembership::getDepartmentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    return ids.isEmpty() ? List.of() : clubDepartmentMapper.selectBatchIds(ids);
  }

  private List<ClubPosition> selectPositions(List<ClubMembership> memberships) {
    List<Integer> ids =
        memberships.stream()
            .map(ClubMembership::getPositionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    return ids.isEmpty() ? List.of() : clubPositionMapper.selectBatchIds(ids);
  }

  private ResponseClubHomeVO.Person toPerson(
      ClubMembership membership,
      Map<Integer, User> users,
      Map<Integer, ClubDepartment> departments,
      Map<Integer, ClubPosition> positions) {
    User user = users.get(membership.getUserId());
    if (user == null) return null;
    String name = isBlank(user.getRealName()) ? user.getUserName() : user.getRealName();
    String department = OptionalName.of(departments.get(membership.getDepartmentId()));
    String position = OptionalName.of(positions.get(membership.getPositionId()));
    String role = isBlank(position) ? membership.getStatus() : position;
    return ResponseClubHomeVO.Person.builder()
        .userId(user.getId())
        .name(name)
        .initial(initialOf(name))
        .role(role)
        .focus(isBlank(department) ? user.getMajor() : department)
        .avatar(user.getAvatar())
        .qqAvatar(qqAvatarUrl(user.getQqOpenid()))
        .build();
  }

  private String qqAvatarUrl(String qqOpenid) {
    if (isBlank(qqOpenid) || !qqOpenid.matches("\\d{5,15}")) return null;
    return "https://q1.qlogo.cn/g?b=qq&nk=" + qqOpenid + "&s=640";
  }

  private ResponseClubHomeVO.Award toAward(ClubAward award) {
    return ResponseClubHomeVO.Award.builder()
        .id(award.getId())
        .year(award.getAwardYear())
        .title(award.getTitle())
        .competitionName(award.getCompetitionName())
        .awardLevel(award.getAwardLevel())
        .teamName(award.getTeamName())
        .description(award.getDescription())
        .build();
  }

  private ResponseSiteProgressVO.ApplicationProgress toProgressApplication(
      MembershipApplication app,
      Map<Integer, User> users,
      Map<Integer, Club> clubs,
      Map<Integer, RecruitmentCampaign> campaigns,
      Map<Integer, ClubDepartment> departments,
      Map<Integer, List<Interview>> interviewsByApp) {
    User user = getNullable(users, app.getUserId());
    Club club = getNullable(clubs, app.getClubId());
    RecruitmentCampaign campaign = getNullable(campaigns, app.getCampaignId());
    ClubDepartment first = getNullable(departments, app.getFirstChoiceDepartmentId());
    ClubDepartment second = getNullable(departments, app.getSecondChoiceDepartmentId());
    String preferredDept =
        first == null ? (second == null ? null : second.getName()) : first.getName();
    return ResponseSiteProgressVO.ApplicationProgress.builder()
        .id(app.getId())
        .clubId(app.getClubId())
        .clubName(club == null ? null : club.getName())
        .campaignId(app.getCampaignId())
        .campaignName(campaign == null ? null : campaign.getName())
        .applicantName(
            user == null
                ? null
                : (isBlank(user.getRealName()) ? user.getUserName() : user.getRealName()))
        .preferredDepartment(preferredDept)
        .status(app.getStatus())
        .createdAt(app.getCreatedAt())
        .updatedAt(app.getUpdatedAt())
        .interviews(
            interviewsByApp.getOrDefault(app.getId(), List.of()).stream()
                .map(this::toProgressInterview)
                .toList())
        .build();
  }

  private ResponseSiteProgressVO.InterviewProgress toProgressInterview(Interview interview) {
    return ResponseSiteProgressVO.InterviewProgress.builder()
        .id(interview.getId())
        .round(interview.getRound())
        .scheduledStartAt(interview.getScheduledStartAt())
        .scheduledEndAt(interview.getScheduledEndAt())
        .location(interview.getLocation())
        .mode(interview.getMode())
        .status(interview.getStatus())
        .build();
  }

  private String formatMonthDay(Timestamp ts) {
    return ts == null ? "" : ts.toLocalDateTime().format(MONTH_DAY_FORMATTER);
  }

  private String initialOf(String name) {
    return isBlank(name) ? "社" : name.substring(0, 1);
  }

  private <T> T getNullable(Map<Integer, T> map, Integer key) {
    return key == null ? null : map.get(key);
  }

  private <T> Map<Integer, T> selectMap(
      List<Integer> ids, Function<List<Integer>, List<T>> selector, Function<T, Integer> idGetter) {
    List<Integer> distinctIds = ids.stream().distinct().toList();
    if (distinctIds.isEmpty()) return Map.of();
    return selector.apply(distinctIds).stream()
        .collect(Collectors.toMap(idGetter, Function.identity()));
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static class OptionalName {
    private static String of(ClubDepartment d) {
      return d == null ? null : d.getName();
    }

    private static String of(ClubPosition p) {
      return p == null ? null : p.getName();
    }
  }
}
