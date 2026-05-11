package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.*;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.service.MembershipService;
import edu.jmi.openatom.server.openatomsystem.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> finalDecision(Integer applicationId, RequestFinalDecisionDTO request) {
    MembershipApplication application = applicationId == null ? null : applicationMapper.selectById(applicationId);
    if (application == null) return ApiResponse.error(404, "申请不存在");
    if (!DECISIONS.contains(request.getDecision())) return ApiResponse.error(400, "终审决策不合法");
    if ("approved".equals(request.getDecision())) {
      ApiResponse<String> v = validateMembershipRefs(application.getUserId(), application.getClubId(), request.getDepartmentId(), request.getPositionId());
      if (v != null) return v;
      ensureMembership(application.getUserId(), application.getClubId(), request.getDepartmentId(), request.getPositionId(), "probation", null, null);
      application.setStatus("final_approved");
    } else if ("waitlisted".equals(request.getDecision())) { application.setStatus("waitlisted"); }
    else { application.setStatus("rejected"); }
    applicationMapper.updateById(application);
    String title = "入会终审结果";
    String content = "您的入会申请终审结果已出：";
    if ("approved".equals(request.getDecision())) content += "【通过】。恭喜您正式成为社团成员！";
    else if ("waitlisted".equals(request.getDecision())) content += "【候补】。请耐心等待后续通知。";
    else content += "【未通过】。感谢您的关注与参与。";
    if (request.getComment() != null && !request.getComment().isBlank()) content += "\n评价：" + request.getComment();
    notificationService.create(RequestCreateNotificationDTO.builder().title(title).content(content).type("approval").receiverUserIds(List.of(application.getUserId())).build());
    return ApiResponse.success("终审决策已处理");
  }

  @Override
  public ApiResponse<List<ResponseMembershipDTO>> list(Integer clubId, Integer departmentId, Integer positionId, String status, String keyword) {
    List<Integer> userIds = null;
    if (keyword != null && !keyword.isBlank()) {
      userIds = userMapper.searchByNameKeyword(keyword).stream().map(User::getId).toList();
      if (userIds.isEmpty()) return ApiResponse.success(List.of());
    }
    return ApiResponse.success(toResponseList(membershipMapper.selectByConditions(clubId, departmentId, positionId, status, userIds)));
  }

  @Override
  public ApiResponse<String> create(RequestCreateMembershipDTO request) {
    ApiResponse<String> v = validateMembershipRefs(request.getUserId(), request.getClubId(), request.getDepartmentId(), request.getPositionId());
    if (v != null) return v;
    if (membershipMapper.countActiveNotLeft(request.getUserId(), request.getClubId()) > 0) return ApiResponse.error(409, "用户已是该社团成员");
    ensureMembership(request.getUserId(), request.getClubId(), request.getDepartmentId(), request.getPositionId(), request.getStatus() == null ? "probation" : request.getStatus(), request.getFeatured(), request.getSortOrder());
    return ApiResponse.success("成员新增成功");
  }

  @Override
  public ApiResponse<ClubMembership> detail(Integer membershipId) {
    ClubMembership m = findMembership(membershipId);
    return m == null ? ApiResponse.error(404, "成员不存在") : ApiResponse.success(m);
  }

  @Override
  public ApiResponse<String> update(Integer membershipId, RequestUpdateMembershipDTO request) {
    ClubMembership m = findMembership(membershipId);
    if (m == null) return ApiResponse.error(404, "成员不存在");
    ApiResponse<String> v = validateMembershipRefs(m.getUserId(), m.getClubId(), request.getDepartmentId(), request.getPositionId());
    if (v != null) return v;
    if (request.getDepartmentId() != null) m.setDepartmentId(request.getDepartmentId());
    if (request.getPositionId() != null) m.setPositionId(request.getPositionId());
    if (request.getStatus() != null) { if (!STATUSES.contains(request.getStatus())) return ApiResponse.error(400, "成员状态不合法"); m.setStatus(request.getStatus()); }
    if (request.getFeatured() != null) m.setFeatured(request.getFeatured());
    if (request.getSortOrder() != null) m.setSortOrder(request.getSortOrder());
    membershipMapper.updateById(m);
    return ApiResponse.success("成员更新成功");
  }

  @Override
  public ApiResponse<String> assignPosition(Integer membershipId, RequestAssignPositionDTO request) {
    ClubMembership m = findMembership(membershipId);
    if (m == null) return ApiResponse.error(404, "成员不存在");
    ClubPosition p = positionMapper.selectById(request.getPositionId());
    if (p == null || !m.getClubId().equals(p.getClubId())) return ApiResponse.error(400, "岗位不存在或不属于当前社团");
    m.setPositionId(p.getId()); m.setDepartmentId(p.getDepartmentId());
    membershipMapper.updateById(m);
    return ApiResponse.success("岗位分配成功");
  }

  @Override
  public ApiResponse<String> changeStatus(Integer membershipId, RequestChangeMembershipStatusDTO request) {
    if (!STATUSES.contains(request.getStatus())) return ApiResponse.error(400, "成员状态不合法");
    ClubMembership m = findMembership(membershipId);
    if (m == null) return ApiResponse.error(404, "成员不存在");
    m.setStatus(request.getStatus());
    if ("left".equals(request.getStatus())) m.setLeftAt(Times.now());
    membershipMapper.updateById(m);
    return ApiResponse.success("成员状态更新成功");
  }

  @Override
  public ApiResponse<String> forceExit(Integer membershipId, String reason) {
    ClubMembership m = findMembership(membershipId);
    if (m == null) return ApiResponse.error(404, "成员不存在");
    m.setStatus("left"); m.setLeftAt(Times.now());
    membershipMapper.updateById(m);
    return ApiResponse.success("强制退社成功");
  }

  private void ensureMembership(Integer userId, Integer clubId, Integer departmentId, Integer positionId, String status, Boolean featured, Integer sortOrder) {
    ClubMembership exists = membershipMapper.selectActiveNotLeft(userId, clubId);
    if (exists != null) { exists.setDepartmentId(departmentId); exists.setPositionId(positionId); exists.setStatus(status); if (featured != null) exists.setFeatured(featured); if (sortOrder != null) exists.setSortOrder(sortOrder); membershipMapper.updateById(exists); return; }
    membershipMapper.insert(ClubMembership.builder().userId(userId).clubId(clubId).departmentId(departmentId).positionId(positionId).status(status).featured(Boolean.TRUE.equals(featured)).sortOrder(sortOrder == null ? 0 : sortOrder).build());
  }

  private ApiResponse<String> validateMembershipRefs(Integer userId, Integer clubId, Integer departmentId, Integer positionId) {
    if (userMapper.selectById(userId) == null) return ApiResponse.error(404, "用户不存在");
    if (clubMapper.selectById(clubId) == null) return ApiResponse.error(404, "社团不存在");
    if (departmentId != null && departmentMapper.selectById(departmentId) == null) return ApiResponse.error(400, "部门不存在");
    if (positionId != null) { ClubPosition p = positionMapper.selectById(positionId); if (p == null || !clubId.equals(p.getClubId())) return ApiResponse.error(400, "岗位不存在或不属于当前社团"); }
    return null;
  }

  private ClubMembership findMembership(Integer id) { return id == null ? null : membershipMapper.selectById(id); }

  private List<ResponseMembershipDTO> toResponseList(List<ClubMembership> memberships) {
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

  private ResponseMembershipDTO toResponse(ClubMembership m, Map<Integer, User> users, Map<Integer, Club> clubs, Map<Integer, ClubDepartment> depts, Map<Integer, ClubPosition> positions) {
    User u = m.getUserId() == null ? null : users.get(m.getUserId()); Club c = m.getClubId() == null ? null : clubs.get(m.getClubId());
    ClubDepartment d = m.getDepartmentId() == null ? null : depts.get(m.getDepartmentId()); ClubPosition p = m.getPositionId() == null ? null : positions.get(m.getPositionId());
    return ResponseMembershipDTO.builder().id(m.getId()).userId(m.getUserId()).userName(u == null ? null : u.getUserName()).realName(u == null ? null : u.getRealName())
        .clubId(m.getClubId()).clubName(c == null ? null : c.getName()).departmentId(m.getDepartmentId()).departmentName(d == null ? null : d.getName())
        .positionId(m.getPositionId()).positionName(p == null ? null : p.getName()).status(m.getStatus()).featured(m.getFeatured()).sortOrder(m.getSortOrder()).joinedAt(m.getJoinedAt()).leftAt(m.getLeftAt()).build();
  }
}
