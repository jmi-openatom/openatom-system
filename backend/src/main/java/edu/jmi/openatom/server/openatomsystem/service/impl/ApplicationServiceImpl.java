package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RecruitmentCampaignMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.ApplicationService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 入会申请管理实现类
 *
 * <p>负责入会申请的创建, 更新, 提交, 撤回, 分页查询以及申请表单的动态字段校验等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {
  private static final List<String> EDITABLE_STATUSES = List.of("draft", "submitted");

  private final MembershipApplicationMapper applicationMapper;
  private final RecruitmentCampaignMapper campaignMapper;
  private final ClubMapper clubMapper;
  private final ClubDepartmentMapper departmentMapper;
  private final UserMapper userMapper;

  @Override
  public ApiResponse<PageDataDTO<ResponseApplicationDTO>> list(
      Integer campaignId, Integer clubId, String status, Integer departmentId,
      String keyword, Long page, Long pageSize) {
    long current = PageRequests.page(page);
    long size = PageRequests.pageSize(pageSize);
    List<Integer> userIds = null;
    if (keyword != null && !keyword.isBlank()) {
      userIds = userMapper.searchByNameKeyword(keyword).stream().map(User::getId).toList();
      if (userIds.isEmpty()) {
        return ApiResponse.success(PageDataDTO.<ResponseApplicationDTO>builder()
            .list(List.of()).page(current).pageSize(size).total(0L).build());
      }
    }
    Page<MembershipApplication> applicationPage = applicationMapper.selectPageByConditions(
        new Page<>(current, size), campaignId, clubId, status, departmentId, userIds);
    return ApiResponse.success(PageDataDTO.<ResponseApplicationDTO>builder()
        .list(toResponseList(applicationPage.getRecords())).page(applicationPage.getCurrent())
        .pageSize(applicationPage.getSize()).total(applicationPage.getTotal()).build());
  }

  @Override
  public ApiResponse<Integer> create(RequestCreateApplicationDTO request) {
    Integer userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
    ApiResponse<String> validation = validateApplicationBase(request, userId);
    if (validation != null) return ApiResponse.error(validation.getCode(), validation.getMessage());
    if (userId != null) {
      Long activeCount = applicationMapper.countActiveByCampaignAndUser(request.getCampaignId(), userId);
      if (activeCount > 0) return ApiResponse.error(409, "同一招新批次只能提交一次有效申请");
    }
    MembershipApplication application = MembershipApplication.builder()
        .campaignId(request.getCampaignId()).clubId(request.getClubId()).userId(userId)
        .firstChoiceDepartmentId(request.getFirstChoiceDepartmentId())
        .secondChoiceDepartmentId(request.getSecondChoiceDepartmentId())
        .profile(Jsons.stringify(request.getProfile())).status("submitted").build();
    int row = applicationMapper.insert(application);
    return row > 0 ? ApiResponse.success(application.getId(), "入会申请提交成功") : ApiResponse.error("入会申请提交失败");
  }

  @Override
  public ApiResponse<MembershipApplication> detail(Integer applicationId) {
    MembershipApplication application = findApplication(applicationId);
    return application == null ? ApiResponse.error(404, "申请不存在") : ApiResponse.success(application);
  }

  @Override
  public ApiResponse<String> update(Integer applicationId, RequestUpdateApplicationDTO request) {
    MembershipApplication application = findApplication(applicationId);
    if (application == null) return ApiResponse.error(404, "申请不存在");
    if (!EDITABLE_STATUSES.contains(application.getStatus()))
      return ApiResponse.error(422, "当前申请状态不允许修改");
    if (request.getFirstChoiceDepartmentId() != null) application.setFirstChoiceDepartmentId(request.getFirstChoiceDepartmentId());
    if (request.getSecondChoiceDepartmentId() != null) application.setSecondChoiceDepartmentId(request.getSecondChoiceDepartmentId());
    if (request.getProfile() != null) application.setProfile(Jsons.stringify(request.getProfile()));
    int row = applicationMapper.updateById(application);
    return row > 0 ? ApiResponse.success("申请更新成功") : ApiResponse.error("申请更新失败");
  }

  @Override
  public ApiResponse<String> submit(Integer applicationId) {
    return changeStatus(applicationId, "draft", "submitted", "申请提交成功");
  }

  @Override
  public ApiResponse<String> withdraw(Integer applicationId) {
    MembershipApplication application = findApplication(applicationId);
    if (application == null) return ApiResponse.error(404, "申请不存在");
    if (List.of("final_approved", "rejected", "cancelled").contains(application.getStatus()))
      return ApiResponse.error(422, "当前申请状态不允许撤回");
    application.setStatus("cancelled");
    int row = applicationMapper.updateById(application);
    return row > 0 ? ApiResponse.success("申请撤回成功") : ApiResponse.error("申请撤回失败");
  }

  private ApiResponse<String> changeStatus(Integer applicationId, String requiredStatus, String targetStatus, String message) {
    MembershipApplication application = findApplication(applicationId);
    if (application == null) return ApiResponse.error(404, "申请不存在");
    if (!requiredStatus.equals(application.getStatus())) return ApiResponse.error(422, "当前申请状态不允许执行该操作");
    application.setStatus(targetStatus);
    int row = applicationMapper.updateById(application);
    return row > 0 ? ApiResponse.success(message) : ApiResponse.error("申请状态更新失败");
  }

  private ApiResponse<String> validateApplicationBase(RequestCreateApplicationDTO request, Integer userId) {
    if (request == null) return ApiResponse.error(400, "申请信息不能为空");
    RecruitmentCampaign campaign = campaignMapper.selectById(request.getCampaignId());
    if (campaign == null) return ApiResponse.error(404, "招新计划不存在");
    if (!List.of("open", "published").contains(campaign.getStatus())) return ApiResponse.error(400, "该招新计划未开放申请");
    Map<String, Object> profile = request.getProfile() == null ? Map.of() : request.getProfile();
    if (Boolean.TRUE.equals(campaign.getLoginRequired()) && userId == null) return ApiResponse.error(401, "该表单需要登录后提交");
    if (!Boolean.TRUE.equals(campaign.getLoginRequired())) {
      if (isBlank(readProfileValue(profile, "applicantName"))) return ApiResponse.error(400, "请填写联系人姓名");
      if (isBlank(readProfileValue(profile, "contact"))) return ApiResponse.error(400, "请填写联系方式");
    }
    ApiResponse<String> formValidation = validateDynamicFields(campaign.getFormSchema(), profile);
    if (formValidation != null) return formValidation;
    if (!request.getClubId().equals(campaign.getClubId())) return ApiResponse.error(400, "招新计划不属于当前社团");
    if (clubMapper.selectById(request.getClubId()) == null) return ApiResponse.error(404, "社团不存在");
    if (request.getFirstChoiceDepartmentId() != null && departmentMapper.selectById(request.getFirstChoiceDepartmentId()) == null)
      return ApiResponse.error(400, "第一志愿部门不存在");
    if (request.getSecondChoiceDepartmentId() != null && departmentMapper.selectById(request.getSecondChoiceDepartmentId()) == null)
      return ApiResponse.error(400, "第二志愿部门不存在");
    return null;
  }

  private ApiResponse<String> validateDynamicFields(String formSchema, Map<String, Object> profile) {
    for (Map<String, Object> field : Jsons.parseListOfObjects(formSchema)) {
      String key = readString(field.get("key"));
      if (isBlank(key) || !Boolean.TRUE.equals(field.get("required"))) continue;
      if (isBlank(readProfileValue(profile, key))) {
        String label = readString(field.get("label"));
        return ApiResponse.error(400, "请填写" + (isBlank(label) ? key : label));
      }
    }
    return null;
  }

  private MembershipApplication findApplication(Integer applicationId) {
    return applicationId == null ? null : applicationMapper.selectById(applicationId);
  }

  private List<ResponseApplicationDTO> toResponseList(List<MembershipApplication> applications) {
    if (applications == null || applications.isEmpty()) return List.of();
    Map<Integer, User> users = selectMap(applications.stream().map(MembershipApplication::getUserId).filter(Objects::nonNull).toList(), userMapper::selectBatchIds, User::getId);
    Map<Integer, Club> clubs = selectMap(applications.stream().map(MembershipApplication::getClubId).filter(Objects::nonNull).toList(), clubMapper::selectBatchIds, Club::getId);
    Map<Integer, RecruitmentCampaign> campaigns = selectMap(applications.stream().map(MembershipApplication::getCampaignId).filter(Objects::nonNull).toList(), campaignMapper::selectBatchIds, RecruitmentCampaign::getId);
    Map<Integer, ClubDepartment> departments = selectMap(applications.stream()
        .flatMap(a -> Stream.of(a.getFirstChoiceDepartmentId(), a.getSecondChoiceDepartmentId()))
        .filter(Objects::nonNull).toList(), departmentMapper::selectBatchIds, ClubDepartment::getId);
    return applications.stream().map(a -> toResponse(a, users, clubs, campaigns, departments)).toList();
  }

  private <T> Map<Integer, T> selectMap(List<Integer> ids, Function<List<Integer>, List<T>> selector, Function<T, Integer> idGetter) {
    List<Integer> distinctIds = ids.stream().distinct().toList();
    if (distinctIds.isEmpty()) return Map.of();
    return selector.apply(distinctIds).stream().collect(Collectors.toMap(idGetter, Function.identity()));
  }

  private ResponseApplicationDTO toResponse(MembershipApplication application, Map<Integer, User> users,
      Map<Integer, Club> clubs, Map<Integer, RecruitmentCampaign> campaigns, Map<Integer, ClubDepartment> departments) {
    User user = getNullable(users, application.getUserId());
    Club club = getNullable(clubs, application.getClubId());
    RecruitmentCampaign campaign = getNullable(campaigns, application.getCampaignId());
    ClubDepartment firstDept = getNullable(departments, application.getFirstChoiceDepartmentId());
    ClubDepartment secondDept = getNullable(departments, application.getSecondChoiceDepartmentId());
    Map<String, Object> profile = Jsons.parseObject(application.getProfile());
    String firstName = firstDept == null ? null : firstDept.getName();
    String secondName = secondDept == null ? null : secondDept.getName();
    return ResponseApplicationDTO.builder().id(application.getId())
        .campaignId(application.getCampaignId()).campaignName(campaign == null ? null : campaign.getName())
        .clubId(application.getClubId()).clubName(club == null ? null : club.getName())
        .userId(application.getUserId())
        .applicantName(user == null ? fallbackProfileValue(profile, "applicantName", "name", "realName")
            : isBlank(user.getRealName()) ? user.getUserName() : user.getRealName())
        .studentId(user == null ? fallbackProfileValue(profile, "studentId") : user.getStudentId())
        .firstChoiceDepartmentId(application.getFirstChoiceDepartmentId()).firstChoiceDepartmentName(firstName)
        .secondChoiceDepartmentId(application.getSecondChoiceDepartmentId()).secondChoiceDepartmentName(secondName)
        .preferredDepartment(firstName == null ? secondName : firstName)
        .status(application.getStatus()).profile(profile)
        .createdAt(application.getCreatedAt()).updatedAt(application.getUpdatedAt()).build();
  }

  private <T> T getNullable(Map<Integer, T> map, Integer key) { return key == null ? null : map.get(key); }
  private boolean isBlank(String value) { return value == null || value.isBlank(); }
  private String readProfileValue(Map<String, Object> profile, String key) { return readString(profile.get(key)); }

  private String fallbackProfileValue(Map<String, Object> profile, String... keys) {
    for (String key : keys) { String v = readProfileValue(profile, key); if (!isBlank(v)) return v; }
    return null;
  }

  private String readString(Object value) { return value == null ? null : String.valueOf(value).trim(); }
}
