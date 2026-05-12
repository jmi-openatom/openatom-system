package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.dto.*;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMembershipVO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import edu.jmi.openatom.server.openatomsystem.service.MembershipService;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成员管理实现类
 *
 * <p>负责社团成员的新增, 更新, 查询, 终审决策, 岗位分配, 状态变更以及强制退社等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {
  private static final List<String> DECISIONS = List.of("approved", "waitlisted", "rejected");
  private static final List<String> STATUSES = List.of("probation", "active", "suspended", "left", "graduated");

  private final ClubMembershipMapper membershipMapper;
  private final MembershipApplicationMapper applicationMapper;
  private final UserMapper userMapper;
  private final ClubMapper clubMapper;
  private final ClubDepartmentMapper departmentMapper;
  private final ClubPositionMapper positionMapper;
  private final NotificationService notificationService;
  private final PasswordService passwordService;
  private final RoleMapper roleMapper;
  private final UserRoleMapper userRoleMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> finalDecision(Integer applicationId, RequestFinalDecisionDTO request) {
    MembershipApplication application = applicationId == null ? null : applicationMapper.selectById(applicationId);
    if (application == null) return Result.error(404, "申请不存在");
    if (!DECISIONS.contains(request.getDecision())) return Result.error(400, "终审决策不合法");
    Integer userId = application.getUserId();
    String credentialInfo = "";
    if ("approved".equals(request.getDecision())) {
      if (userId == null) {
        Map<String, Object> profile = Jsons.parseObject(application.getProfile());
        String applicantName = readString(profile.get("applicantName"));
        if (isBlank(applicantName)) applicantName = readString(profile.get("name"));
        if (isBlank(applicantName)) applicantName = readString(profile.get("realName"));
        if (isBlank(applicantName)) applicantName = "匿名用户";
        String password = UUID.randomUUID().toString().substring(0, 8);
        String username = generateUniqueUsername(applicantName);
        String contact = firstNonBlank(readString(profile.get("contact")), readString(profile.get("phone")), readString(profile.get("email")));
        User user = User.builder().userName(username).realName(applicantName)
            .studentId(firstNonBlank(readString(profile.get("studentId")), username))
            .phone(contact != null && contact.contains("@") ? null : contact)
            .email(contact != null && contact.contains("@") ? contact : null)
            .password(passwordService.encode(password))
            .userStatus(UserStatus.ACTIVE).build();
        userMapper.insert(user);
        bindDefaultRole(user.getId());
        userId = user.getId();
        application.setUserId(userId);
        credentialInfo = "\n系统已自动为您创建账号：\n用户名：" + username + "\n初始密码：" + password + "\n请登录后及时修改密码并完善个人信息。";
      }
      Result<String> v = validateMembershipRefs(userId, application.getClubId(), request.getDepartmentId(), request.getPositionId());
      if (v != null) return v;
      ensureMembership(userId, application.getClubId(), request.getDepartmentId(), request.getPositionId(), "probation", null, null);
      application.setStatus("final_approved");
    } else if ("waitlisted".equals(request.getDecision())) { application.setStatus("waitlisted"); }
    else { application.setStatus("rejected"); }
    applicationMapper.updateById(application);
    String title = "入会终审结果";
    String content = "您的入会申请终审结果已出：";
    if ("approved".equals(request.getDecision())) content += "【通过】。恭喜您正式成为社团成员！" + credentialInfo;
    else if ("waitlisted".equals(request.getDecision())) content += "【候补】。请耐心等待后续通知。";
    else content += "【未通过】。感谢您的关注与参与。";
    if (request.getComment() != null && !request.getComment().isBlank()) content += "\n评价：" + request.getComment();
    if (userId != null) {
      notificationService.create(RequestCreateNotificationDTO.builder().title(title).content(content).type("approval").receiverUserIds(List.of(userId)).build());
    }
    return Result.success("终审决策已处理");
  }

  @Override
  public Result<List<ResponseMembershipVO>> list(Integer clubId, Integer departmentId, Integer positionId, String status, String keyword) {
    List<Integer> userIds = null;
    if (keyword != null && !keyword.isBlank()) {
      userIds = userMapper.searchByNameKeyword(keyword).stream().map(User::getId).toList();
      if (userIds.isEmpty()) return Result.success(List.of());
    }
    return Result.success(toResponseList(membershipMapper.selectByConditions(clubId, departmentId, positionId, status, userIds)));
  }

  @Override
  public Result<String> create(RequestCreateMembershipDTO request) {
    Result<String> v = validateMembershipRefs(request.getUserId(), request.getClubId(), request.getDepartmentId(), request.getPositionId());
    if (v != null) return v;
    if (membershipMapper.countActiveNotLeft(request.getUserId(), request.getClubId()) > 0) return Result.error(409, "用户已是该社团成员");
    ensureMembership(request.getUserId(), request.getClubId(), request.getDepartmentId(), request.getPositionId(), request.getStatus() == null ? "probation" : request.getStatus(), request.getFeatured(), request.getSortOrder());
    return Result.success("成员新增成功");
  }

  @Override
  public Result<ClubMembership> detail(Integer membershipId) {
    ClubMembership m = findMembership(membershipId);
    return m == null ? Result.error(404, "成员不存在") : Result.success(m);
  }

  @Override
  public Result<String> update(Integer membershipId, RequestUpdateMembershipDTO request) {
    ClubMembership m = findMembership(membershipId);
    if (m == null) return Result.error(404, "成员不存在");
    Result<String> v = validateMembershipRefs(m.getUserId(), m.getClubId(), request.getDepartmentId(), request.getPositionId());
    if (v != null) return v;
    if (request.getDepartmentId() != null) m.setDepartmentId(request.getDepartmentId());
    if (request.getPositionId() != null) m.setPositionId(request.getPositionId());
    if (request.getStatus() != null) { if (!STATUSES.contains(request.getStatus())) return Result.error(400, "成员状态不合法"); m.setStatus(request.getStatus()); }
    if (request.getFeatured() != null) m.setFeatured(request.getFeatured());
    if (request.getSortOrder() != null) m.setSortOrder(request.getSortOrder());
    membershipMapper.updateById(m);
    return Result.success("成员更新成功");
  }

  @Override
  public Result<String> assignPosition(Integer membershipId, RequestAssignPositionDTO request) {
    ClubMembership m = findMembership(membershipId);
    if (m == null) return Result.error(404, "成员不存在");
    ClubPosition p = positionMapper.selectById(request.getPositionId());
    if (p == null || !m.getClubId().equals(p.getClubId())) return Result.error(400, "岗位不存在或不属于当前社团");
    m.setPositionId(p.getId()); m.setDepartmentId(p.getDepartmentId());
    membershipMapper.updateById(m);
    return Result.success("岗位分配成功");
  }

  @Override
  public Result<String> changeStatus(Integer membershipId, RequestChangeMembershipStatusDTO request) {
    if (!STATUSES.contains(request.getStatus())) return Result.error(400, "成员状态不合法");
    ClubMembership m = findMembership(membershipId);
    if (m == null) return Result.error(404, "成员不存在");
    m.setStatus(request.getStatus());
    if ("left".equals(request.getStatus())) m.setLeftAt(Times.now());
    membershipMapper.updateById(m);
    return Result.success("成员状态更新成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> batchChangeStatus(RequestBatchChangeMembershipStatusDTO request) {
    if (!STATUSES.contains(request.getStatus())) return Result.error(400, "成员状态不合法");
    int count = 0;
    for (Integer id : request.getMembershipIds()) {
      ClubMembership m = findMembership(id);
      if (m == null) continue;
      m.setStatus(request.getStatus());
      if ("left".equals(request.getStatus())) m.setLeftAt(Times.now());
      membershipMapper.updateById(m);
      count++;
    }
    return Result.success("已批量更新 " + count + " 条成员状态");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> batchCreate(RequestBatchCreateMembershipDTO request) {
    int count = 0;
    for (RequestBatchCreateMembershipDTO.MembershipItem item : request.getMemberships()) {
      Result<String> v = validateMembershipRefs(item.getUserId(), item.getClubId(), item.getDepartmentId(), item.getPositionId());
      if (v != null) continue;
      if (membershipMapper.countActiveNotLeft(item.getUserId(), item.getClubId()) > 0) continue;
      ensureMembership(item.getUserId(), item.getClubId(), item.getDepartmentId(), item.getPositionId(),
          item.getStatus() == null ? "probation" : item.getStatus(), item.getFeatured(), item.getSortOrder());
      count++;
    }
    return Result.success("已批量创建 " + count + " 条成员关系");
  }

  @Override
  public Result<String> forceExit(Integer membershipId, String reason) {
    ClubMembership m = findMembership(membershipId);
    if (m == null) return Result.error(404, "成员不存在");
    m.setStatus("left"); m.setLeftAt(Times.now());
    membershipMapper.updateById(m);
    return Result.success("强制退社成功");
  }

  private void ensureMembership(Integer userId, Integer clubId, Integer departmentId, Integer positionId, String status, Boolean featured, Integer sortOrder) {
    ClubMembership exists = membershipMapper.selectActiveNotLeft(userId, clubId);
    if (exists != null) { exists.setDepartmentId(departmentId); exists.setPositionId(positionId); exists.setStatus(status); if (featured != null) exists.setFeatured(featured); if (sortOrder != null) exists.setSortOrder(sortOrder); membershipMapper.updateById(exists); return; }
    membershipMapper.insert(ClubMembership.builder().userId(userId).clubId(clubId).departmentId(departmentId).positionId(positionId).status(status).featured(Boolean.TRUE.equals(featured)).sortOrder(sortOrder == null ? 0 : sortOrder).build());
  }

  private Result<String> validateMembershipRefs(Integer userId, Integer clubId, Integer departmentId, Integer positionId) {
    if (userMapper.selectById(userId) == null) return Result.error(404, "用户不存在");
    if (clubMapper.selectById(clubId) == null) return Result.error(404, "社团不存在");
    if (departmentId != null && departmentMapper.selectById(departmentId) == null) return Result.error(400, "部门不存在");
    if (positionId != null) { ClubPosition p = positionMapper.selectById(positionId); if (p == null || !clubId.equals(p.getClubId())) return Result.error(400, "岗位不存在或不属于当前社团"); }
    return null;
  }

  private ClubMembership findMembership(Integer id) { return id == null ? null : membershipMapper.selectById(id); }

  private List<ResponseMembershipVO> toResponseList(List<ClubMembership> memberships) {
    if (memberships == null || memberships.isEmpty()) return List.of();
    Map<Integer, User> users = selectMap(memberships.stream().map(ClubMembership::getUserId).filter(Objects::nonNull).toList(), userMapper::selectBatchIds, User::getId);
    Map<Integer, Club> clubs = selectMap(memberships.stream().map(ClubMembership::getClubId).filter(Objects::nonNull).toList(), clubMapper::selectBatchIds, Club::getId);
    Map<Integer, ClubDepartment> depts = selectMap(memberships.stream().map(ClubMembership::getDepartmentId).filter(Objects::nonNull).toList(), departmentMapper::selectBatchIds, ClubDepartment::getId);
    Map<Integer, ClubPosition> positions = selectMap(memberships.stream().map(ClubMembership::getPositionId).filter(Objects::nonNull).toList(), positionMapper::selectBatchIds, ClubPosition::getId);
    return memberships.stream().map(m -> toResponse(m, users, clubs, depts, positions)).toList();
  }

  private <T> Map<Integer, T> selectMap(List<Integer> ids, Function<List<Integer>, List<T>> sel, Function<T, Integer> idGet) {
    List<Integer> d = ids.stream().distinct().toList(); if (d.isEmpty()) return Map.of();
    return sel.apply(d).stream().collect(Collectors.toMap(idGet, Function.identity()));
  }

  private ResponseMembershipVO toResponse(ClubMembership m, Map<Integer, User> users, Map<Integer, Club> clubs, Map<Integer, ClubDepartment> depts, Map<Integer, ClubPosition> positions) {
    User u = m.getUserId() == null ? null : users.get(m.getUserId()); Club c = m.getClubId() == null ? null : clubs.get(m.getClubId());
    ClubDepartment d = m.getDepartmentId() == null ? null : depts.get(m.getDepartmentId()); ClubPosition p = m.getPositionId() == null ? null : positions.get(m.getPositionId());
    return ResponseMembershipVO.builder().id(m.getId()).userId(m.getUserId()).userName(u == null ? null : u.getUserName()).realName(u == null ? null : u.getRealName())
        .clubId(m.getClubId()).clubName(c == null ? null : c.getName()).departmentId(m.getDepartmentId()).departmentName(d == null ? null : d.getName())
        .positionId(m.getPositionId()).positionName(p == null ? null : p.getName()).status(m.getStatus()).featured(m.getFeatured()).sortOrder(m.getSortOrder()).joinedAt(m.getJoinedAt()).leftAt(m.getLeftAt()).build();
  }

  private String generateUniqueUsername(String applicantName) {
    String base = applicantName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "").trim();
    if (isBlank(base)) base = "member";
    String suffix = UUID.randomUUID().toString().substring(0, 6);
    return base + "_" + suffix;
  }

  private void bindDefaultRole(Integer userId) {
    String roleCode = "probationary_member";
    Role role = roleMapper.selectByCode(roleCode);
    if (role == null) throw new IllegalStateException("Default role not initialized: " + roleCode);
    if (userRoleMapper.selectOneByUserAndRole(userId, role.getId()) == null) {
      userRoleMapper.insert(UserRole.builder().userId(userId).roleId(role.getId()).build());
    }
  }

  private String readString(Object value) { return value == null ? null : String.valueOf(value).trim(); }
  private String firstNonBlank(String... values) { for (String v : values) if (!isBlank(v)) return v.trim(); return null; }
  private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
