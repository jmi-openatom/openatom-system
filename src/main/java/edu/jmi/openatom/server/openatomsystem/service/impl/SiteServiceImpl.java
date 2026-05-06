package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseClubHomeDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseRecruitmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteFormDetailDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteProgressDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteFormsDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAward;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPosition;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubActivityMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubAwardMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMembershipMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubPositionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RecruitmentCampaignMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
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
  private final RegistrationSettingService registrationSettingService;

  @Override
  public ApiResponse<ResponseClubHomeDTO> getClubHome(String clubCode) {
    Club club = findClub(clubCode);
    if (club == null) {
      return ApiResponse.error(404, "默认社团不存在");
    }

    List<ClubMembership> memberships =
        clubMembershipMapper.selectList(
            new LambdaQueryWrapper<ClubMembership>()
                .eq(ClubMembership::getClubId, club.getId())
                .isNull(ClubMembership::getLeftAt)
                .orderByDesc(ClubMembership::getFeatured)
                .orderByAsc(ClubMembership::getSortOrder)
                .orderByDesc(ClubMembership::getJoinedAt));
    List<RecruitmentCampaign> campaigns =
        recruitmentCampaignMapper.selectList(
            new LambdaQueryWrapper<RecruitmentCampaign>()
                .eq(RecruitmentCampaign::getClubId, club.getId())
                .orderByDesc(RecruitmentCampaign::getApplyStartAt));
    List<ClubDepartment> departments =
        clubDepartmentMapper.selectList(
            new LambdaQueryWrapper<ClubDepartment>()
                .eq(ClubDepartment::getClubId, club.getId())
                .orderByAsc(ClubDepartment::getId));
    List<ClubActivity> activities =
        clubActivityMapper.selectList(
            new LambdaQueryWrapper<ClubActivity>()
                .eq(ClubActivity::getClubId, club.getId())
                .eq(ClubActivity::getStatus, "published")
                .orderByDesc(ClubActivity::getActivityAt)
                .last("LIMIT 6"));
    List<ClubAward> awards =
        clubAwardMapper.selectList(
            new LambdaQueryWrapper<ClubAward>()
                .eq(ClubAward::getClubId, club.getId())
                .orderByDesc(ClubAward::getAwardYear)
                .orderByAsc(ClubAward::getSortOrder)
                .orderByDesc(ClubAward::getId));

    return ApiResponse.success(
        ResponseClubHomeDTO.builder()
            .club(toClubProfile(club))
            .metrics(buildMetrics(memberships, campaigns, activities, awards))
            .focusAreas(buildFocusAreas(departments))
            .activities(activities.stream().map(this::toActivity).toList())
            .people(buildPeople(memberships))
            .awards(awards.stream().map(this::toAward).toList())
            .techStack(departments.stream().map(ClubDepartment::getName).limit(5).toList())
            .build());
  }

  @Override
  public ApiResponse<List<ClubActivity>> getActivities() {
    Club club = findClub(null);
    if (club == null) {
      return ApiResponse.error(404, "默认社团不存在");
    }
    return ApiResponse.success(
        clubActivityMapper.selectList(
            new LambdaQueryWrapper<ClubActivity>()
                .eq(ClubActivity::getClubId, club.getId())
                .eq(ClubActivity::getStatus, "published")
                .orderByDesc(ClubActivity::getActivityAt)
                .orderByDesc(ClubActivity::getId)));
  }

  @Override
  public ApiResponse<ClubActivity> getActivityDetail(Integer activityId) {
    Club club = findClub(null);
    if (club == null) {
      return ApiResponse.error(404, "默认社团不存在");
    }
    ClubActivity activity =
        clubActivityMapper.selectOne(
            new LambdaQueryWrapper<ClubActivity>()
                .eq(ClubActivity::getId, activityId)
                .eq(ClubActivity::getClubId, club.getId())
                .eq(ClubActivity::getStatus, "published")
                .last("LIMIT 1"));
    return activity == null ? ApiResponse.error(404, "活动不存在") : ApiResponse.success(activity);
  }

  @Override
  public ApiResponse<List<Club>> getPublicClubs() {
    return ApiResponse.success(
        clubMapper.selectList(
            new LambdaQueryWrapper<Club>()
                .eq(Club::getStatus, "active")
                .orderByAsc(Club::getId)));
  }

  @Override
  public ApiResponse<ResponseSiteFormsDTO> getPublicForms(Integer clubId) {
    Club club = findClub(clubId, null);
    if (club == null) {
      return ApiResponse.error(404, "默认社团不存在");
    }
    List<RecruitmentCampaign> forms =
        recruitmentCampaignMapper.selectList(
            new LambdaQueryWrapper<RecruitmentCampaign>()
                .eq(RecruitmentCampaign::getClubId, club.getId())
                .in(RecruitmentCampaign::getStatus, List.of("open", "published"))
                .orderByDesc(RecruitmentCampaign::getApplyStartAt)
                .orderByDesc(RecruitmentCampaign::getId));
    return ApiResponse.success(ResponseSiteFormsDTO.builder().club(club).forms(forms).build());
  }

  @Override
  public ApiResponse<ResponseSiteProgressDTO> getMyProgress() {
    if (!StpUtil.isLogin()) {
      return ApiResponse.error(401, "请先登录后查看入会进度");
    }
    Integer userId = StpUtil.getLoginIdAsInt();
    List<MembershipApplication> applications =
        applicationMapper.selectList(
            new LambdaQueryWrapper<MembershipApplication>()
                .eq(MembershipApplication::getUserId, userId)
                .orderByDesc(MembershipApplication::getId));
    if (applications.isEmpty()) {
      return ApiResponse.success(ResponseSiteProgressDTO.builder().applications(List.of()).build());
    }

    Map<Integer, Club> clubs =
        selectMap(
            applications.stream().map(MembershipApplication::getClubId).filter(Objects::nonNull).toList(),
            clubMapper::selectBatchIds,
            Club::getId);
    Map<Integer, User> users =
        selectMap(
            applications.stream().map(MembershipApplication::getUserId).filter(Objects::nonNull).toList(),
            userMapper::selectBatchIds,
            User::getId);
    Map<Integer, RecruitmentCampaign> campaigns =
        selectMap(
            applications.stream().map(MembershipApplication::getCampaignId).filter(Objects::nonNull).toList(),
            recruitmentCampaignMapper::selectBatchIds,
            RecruitmentCampaign::getId);
    Map<Integer, ClubDepartment> departments =
        selectMap(
            applications.stream()
                .flatMap(
                    application ->
                        java.util.stream.Stream.of(
                            application.getFirstChoiceDepartmentId(),
                            application.getSecondChoiceDepartmentId()))
                .filter(Objects::nonNull)
                .toList(),
            clubDepartmentMapper::selectBatchIds,
            ClubDepartment::getId);
    Map<Integer, List<Interview>> interviewsByApplication =
        interviewMapper
            .selectList(
                new LambdaQueryWrapper<Interview>()
                    .in(Interview::getApplicationId, applications.stream().map(MembershipApplication::getId).toList())
                    .orderByAsc(Interview::getRound)
                    .orderByDesc(Interview::getId))
            .stream()
            .collect(Collectors.groupingBy(Interview::getApplicationId));

    return ApiResponse.success(
        ResponseSiteProgressDTO.builder()
            .applications(
                applications.stream()
                    .map(
                        application ->
                            toProgressApplication(
                                application, users, clubs, campaigns, departments, interviewsByApplication))
                    .toList())
            .build());
  }

  @Override
  public ApiResponse<Boolean> getRegisterEnabled() {
    return ApiResponse.success(registrationSettingService.isRegisterEnabled());
  }

  @Override
  public ApiResponse<ResponseRecruitmentDTO> getRecruitment(Integer clubId) {
    Club club = findClub(clubId, null);
    if (club == null) {
      return ApiResponse.error(404, "默认社团不存在");
    }
    List<RecruitmentCampaign> campaigns =
        recruitmentCampaignMapper.selectList(
            new LambdaQueryWrapper<RecruitmentCampaign>()
                .eq(RecruitmentCampaign::getClubId, club.getId())
                .in(RecruitmentCampaign::getStatus, List.of("open", "published"))
                .orderByDesc(RecruitmentCampaign::getApplyStartAt)
                .orderByDesc(RecruitmentCampaign::getId));
    List<ClubDepartment> departments =
        clubDepartmentMapper.selectList(
            new LambdaQueryWrapper<ClubDepartment>()
                .eq(ClubDepartment::getClubId, club.getId())
                .orderByAsc(ClubDepartment::getId));
    return ApiResponse.success(
        ResponseRecruitmentDTO.builder()
            .club(club)
            .campaigns(campaigns)
            .departments(departments)
            .build());
  }

  @Override
  public ApiResponse<ResponseSiteFormDetailDTO> getFormDetail(Integer campaignId) {
    RecruitmentCampaign campaign = recruitmentCampaignMapper.selectById(campaignId);
    if (campaign == null) {
      return ApiResponse.error(404, "表单不存在");
    }
    Club club = clubMapper.selectById(campaign.getClubId());
    if (club == null) {
      return ApiResponse.error(404, "社团不存在");
    }
    return ApiResponse.success(
        ResponseSiteFormDetailDTO.builder()
            .club(club)
            .form(campaign)
            .build());
  }

  private Club findClub(String clubCode) {
    return findClub(null, clubCode);
  }

  private Club findClub(Integer clubId, String clubCode) {
    if (clubId != null) {
      Club club = clubMapper.selectById(clubId);
      if (club != null) {
        return club;
      }
    }
    String code = isBlank(clubCode) ? DEFAULT_CLUB_CODE : clubCode;
    Club club =
        clubMapper.selectOne(new LambdaQueryWrapper<Club>().eq(Club::getCode, code).last("LIMIT 1"));
    if (club != null) {
      return club;
    }
    return clubMapper.selectOne(
        new LambdaQueryWrapper<Club>().eq(Club::getStatus, "active").orderByAsc(Club::getId).last("LIMIT 1"));
  }

  private ResponseClubHomeDTO.ClubProfile toClubProfile(Club club) {
    return ResponseClubHomeDTO.ClubProfile.builder()
        .id(club.getId())
        .name(club.getName())
        .code(club.getCode())
        .category(club.getCategory())
        .description(club.getDescription())
        .logoUrl(club.getLogoUrl())
        .recruitmentStatus(club.getRecruitmentStatus())
        .build();
  }

  private List<ResponseClubHomeDTO.Metric> buildMetrics(
      List<ClubMembership> memberships,
      List<RecruitmentCampaign> campaigns,
      List<ClubActivity> activities,
      List<ClubAward> awards) {
    long activeMembers =
        memberships.stream()
            .filter(membership -> !"left".equalsIgnoreCase(membership.getStatus()))
            .count();
    long openCampaigns =
        campaigns.stream().filter(campaign -> "published".equalsIgnoreCase(campaign.getStatus())).count();
    return List.of(
        metric("在册成员", activeMembers, "来自成员关系表"),
        metric("年度活动", activities.size(), "来自社团活动表"),
        metric("竞赛获奖", awards.size(), "来自比赛获奖表"),
        metric("招新计划", openCampaigns, "当前发布中的批次"));
  }

  private ResponseClubHomeDTO.Metric metric(String label, long value, String note) {
    return ResponseClubHomeDTO.Metric.builder()
        .label(label)
        .value(String.valueOf(value))
        .note(note)
        .build();
  }

  private List<ResponseClubHomeDTO.FocusArea> buildFocusAreas(List<ClubDepartment> departments) {
    List<String> icons = List.of("monitor", "cpu", "lightning");
    return departments.stream()
        .limit(3)
        .map(
            department ->
                focus(
                    department.getName(),
                    department.getDescription(),
                    icons.get(departments.indexOf(department) % icons.size())))
        .toList();
  }

  private ResponseClubHomeDTO.FocusArea focus(String title, String description, String icon) {
    return ResponseClubHomeDTO.FocusArea.builder()
        .title(title)
        .description(description)
        .icon(icon)
        .build();
  }

  private ResponseClubHomeDTO.Activity toActivity(ClubActivity activity) {
    return ResponseClubHomeDTO.Activity.builder()
        .id(activity.getId())
        .date(formatMonthDay(activity.getActivityAt()))
        .title(activity.getTitle())
        .description(activity.getSummary())
        .location(activity.getLocation())
        .status(activity.getStatus())
        .build();
  }

  private List<ResponseClubHomeDTO.Person> buildPeople(List<ClubMembership> memberships) {
    List<ClubMembership> topMemberships =
        memberships.stream()
            .filter(membership -> membership.getUserId() != null)
            .filter(membership -> Boolean.TRUE.equals(membership.getFeatured()))
            .filter(membership -> !"left".equalsIgnoreCase(membership.getStatus()))
            .sorted(
                Comparator.comparing(
                        ClubMembership::getFeatured, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        ClubMembership::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                        ClubMembership::getJoinedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    if (topMemberships.isEmpty()) {
      return List.of();
    }

    Map<Integer, User> users =
        userMapper.selectBatchIds(
                topMemberships.stream().map(ClubMembership::getUserId).filter(Objects::nonNull).toList())
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    Map<Integer, ClubDepartment> departments =
        selectDepartments(topMemberships).stream()
            .collect(Collectors.toMap(ClubDepartment::getId, Function.identity()));
    Map<Integer, ClubPosition> positions =
        selectPositions(topMemberships).stream()
            .collect(Collectors.toMap(ClubPosition::getId, Function.identity()));

    return topMemberships.stream()
        .map(membership -> toPerson(membership, users, departments, positions))
        .filter(Objects::nonNull)
        .toList();
  }

  private List<ClubDepartment> selectDepartments(List<ClubMembership> memberships) {
    List<Integer> ids = memberships.stream().map(ClubMembership::getDepartmentId).filter(Objects::nonNull).distinct().toList();
    return ids.isEmpty() ? List.of() : clubDepartmentMapper.selectBatchIds(ids);
  }

  private List<ClubPosition> selectPositions(List<ClubMembership> memberships) {
    List<Integer> ids = memberships.stream().map(ClubMembership::getPositionId).filter(Objects::nonNull).distinct().toList();
    return ids.isEmpty() ? List.of() : clubPositionMapper.selectBatchIds(ids);
  }

  private ResponseClubHomeDTO.Person toPerson(
      ClubMembership membership,
      Map<Integer, User> users,
      Map<Integer, ClubDepartment> departments,
      Map<Integer, ClubPosition> positions) {
    User user = users.get(membership.getUserId());
    if (user == null) {
      return null;
    }
    String name = isBlank(user.getRealName()) ? user.getUserName() : user.getRealName();
    String department = OptionalName.of(departments.get(membership.getDepartmentId()));
    String position = OptionalName.of(positions.get(membership.getPositionId()));
    String role = isBlank(position) ? membership.getStatus() : position;
    return ResponseClubHomeDTO.Person.builder()
        .userId(user.getId())
        .name(name)
        .initial(initialOf(name))
        .role(role)
        .focus(isBlank(department) ? user.getMajor() : department)
        .avatar(user.getAvatar())
        .build();
  }

  private ResponseClubHomeDTO.Award toAward(ClubAward award) {
    return ResponseClubHomeDTO.Award.builder()
        .id(award.getId())
        .year(award.getAwardYear())
        .title(award.getTitle())
        .competitionName(award.getCompetitionName())
        .awardLevel(award.getAwardLevel())
        .teamName(award.getTeamName())
        .description(award.getDescription())
        .build();
  }

  private ResponseSiteProgressDTO.ApplicationProgress toProgressApplication(
      MembershipApplication application,
      Map<Integer, User> users,
      Map<Integer, Club> clubs,
      Map<Integer, RecruitmentCampaign> campaigns,
      Map<Integer, ClubDepartment> departments,
      Map<Integer, List<Interview>> interviewsByApplication) {
    User user = getNullable(users, application.getUserId());
    Club club = getNullable(clubs, application.getClubId());
    RecruitmentCampaign campaign = getNullable(campaigns, application.getCampaignId());
    ClubDepartment firstDepartment = getNullable(departments, application.getFirstChoiceDepartmentId());
    ClubDepartment secondDepartment = getNullable(departments, application.getSecondChoiceDepartmentId());
    String preferredDepartment =
        firstDepartment == null
            ? (secondDepartment == null ? null : secondDepartment.getName())
            : firstDepartment.getName();
    return ResponseSiteProgressDTO.ApplicationProgress.builder()
        .id(application.getId())
        .clubId(application.getClubId())
        .clubName(club == null ? null : club.getName())
        .campaignId(application.getCampaignId())
        .campaignName(campaign == null ? null : campaign.getName())
        .applicantName(user == null ? null : (isBlank(user.getRealName()) ? user.getUserName() : user.getRealName()))
        .preferredDepartment(preferredDepartment)
        .status(application.getStatus())
        .createdAt(application.getCreatedAt())
        .updatedAt(application.getUpdatedAt())
        .interviews(
            interviewsByApplication.getOrDefault(application.getId(), List.of()).stream()
                .map(this::toProgressInterview)
                .toList())
        .build();
  }

  private ResponseSiteProgressDTO.InterviewProgress toProgressInterview(Interview interview) {
    return ResponseSiteProgressDTO.InterviewProgress.builder()
        .id(interview.getId())
        .round(interview.getRound())
        .scheduledStartAt(interview.getScheduledStartAt())
        .scheduledEndAt(interview.getScheduledEndAt())
        .location(interview.getLocation())
        .mode(interview.getMode())
        .status(interview.getStatus())
        .build();
  }

  private String formatMonthDay(Timestamp timestamp) {
    if (timestamp == null) {
      return "";
    }
    return timestamp.toLocalDateTime().format(MONTH_DAY_FORMATTER);
  }

  private String initialOf(String name) {
    if (isBlank(name)) {
      return "社";
    }
    return name.substring(0, 1);
  }

  private <T> T getNullable(Map<Integer, T> map, Integer key) {
    return key == null ? null : map.get(key);
  }

  private <T> Map<Integer, T> selectMap(
      List<Integer> ids,
      Function<List<Integer>, List<T>> selector,
      Function<T, Integer> idGetter) {
    List<Integer> distinctIds = ids.stream().distinct().toList();
    if (distinctIds.isEmpty()) {
      return Map.of();
    }
    return selector.apply(distinctIds).stream().collect(Collectors.toMap(idGetter, Function.identity()));
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static class OptionalName {
    private static String of(ClubDepartment department) {
      return department == null ? null : department.getName();
    }

    private static String of(ClubPosition position) {
      return position == null ? null : position.getName();
    }
  }
}
