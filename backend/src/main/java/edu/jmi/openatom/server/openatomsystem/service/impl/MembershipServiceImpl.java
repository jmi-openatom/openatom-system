package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestAssignPositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestChangeMembershipStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNotificationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestFinalDecisionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPosition;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMembershipMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubPositionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
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

@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {
  private static final List<String> DECISIONS = List.of("approved", "waitlisted", "rejected");
  private static final List<String> STATUSES =
      List.of("probation", "active", "suspended", "left", "graduated");

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
    MembershipApplication application =
        applicationId == null ? null : applicationMapper.selectById(applicationId);
    if (application == null) {
      return ApiResponse.error(404, "申请不存在");
    }
    if (!DECISIONS.contains(request.getDecision())) {
      return ApiResponse.error(400, "终审决策不合法");
    }
    if ("approved".equals(request.getDecision())) {
      ApiResponse<String> validation =
          validateMembershipRefs(
              application.getUserId(),
              application.getClubId(),
              request.getDepartmentId(),
              request.getPositionId());
      if (validation != null) {
        return validation;
      }
      ensureMembership(
          application.getUserId(),
          application.getClubId(),
          request.getDepartmentId(),
          request.getPositionId(),
          "probation",
          null,
          null);
      application.setStatus("final_approved");
    } else if ("waitlisted".equals(request.getDecision())) {
      application.setStatus("waitlisted");
    } else {
      application.setStatus("rejected");
    }
    applicationMapper.updateById(application);

    // Send automated notification
    String title = "入会终审结果";
    String content = "您的入会申请终审结果已出：";
    if ("approved".equals(request.getDecision())) {
      content += "【通过】。恭喜您正式成为社团成员！";
    } else if ("waitlisted".equals(request.getDecision())) {
      content += "【候补】。请耐心等待后续通知。";
    } else {
      content += "【未通过】。感谢您的关注与参与。";
    }
    if (request.getComment() != null && !request.getComment().isBlank()) {
      content += "\n评价：" + request.getComment();
    }
    notificationService.create(
        RequestCreateNotificationDTO.builder()
            .title(title)
            .content(content)
            .type("approval")
            .receiverUserIds(List.of(application.getUserId()))
            .build());

    return ApiResponse.success("终审决策已处理");
  }

  @Override
  public ApiResponse<List<ResponseMembershipDTO>> list(
      Integer clubId, Integer departmentId, Integer positionId, String status, String keyword) {
    LambdaQueryWrapper<ClubMembership> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(clubId != null, ClubMembership::getClubId, clubId)
        .eq(departmentId != null, ClubMembership::getDepartmentId, departmentId)
        .eq(positionId != null, ClubMembership::getPositionId, positionId)
        .eq(status != null && !status.isBlank(), ClubMembership::getStatus, status)
        .orderByDesc(ClubMembership::getId);
    if (keyword != null && !keyword.isBlank()) {
      List<Integer> userIds =
          userMapper
              .selectList(
                  new LambdaQueryWrapper<User>()
                      .like(User::getUserName, keyword)
                      .or()
                      .like(User::getRealName, keyword)
                      .or()
                      .like(User::getStudentId, keyword))
              .stream()
              .map(User::getId)
              .toList();
      if (userIds.isEmpty()) {
        return ApiResponse.success(List.of());
      }
      wrapper.in(ClubMembership::getUserId, userIds);
    }
    return ApiResponse.success(toResponseList(membershipMapper.selectList(wrapper)));
  }

  @Override
  public ApiResponse<String> create(RequestCreateMembershipDTO request) {
    ApiResponse<String> validation =
        validateMembershipRefs(
            request.getUserId(), request.getClubId(), request.getDepartmentId(), request.getPositionId());
    if (validation != null) {
      return validation;
    }
    if (activeMembershipExists(request.getUserId(), request.getClubId())) {
      return ApiResponse.error(409, "用户已是该社团成员");
    }
    ensureMembership(
        request.getUserId(),
        request.getClubId(),
        request.getDepartmentId(),
        request.getPositionId(),
        request.getStatus() == null ? "probation" : request.getStatus(),
        request.getFeatured(),
        request.getSortOrder());
    return ApiResponse.success("成员新增成功");
  }

  @Override
  public ApiResponse<ClubMembership> detail(Integer membershipId) {
    ClubMembership membership = findMembership(membershipId);
    return membership == null ? ApiResponse.error(404, "成员不存在") : ApiResponse.success(membership);
  }

  @Override
  public ApiResponse<String> update(Integer membershipId, RequestUpdateMembershipDTO request) {
    ClubMembership membership = findMembership(membershipId);
    if (membership == null) {
      return ApiResponse.error(404, "成员不存在");
    }
    ApiResponse<String> validation =
        validateMembershipRefs(
            membership.getUserId(),
            membership.getClubId(),
            request.getDepartmentId(),
            request.getPositionId());
    if (validation != null) {
      return validation;
    }
    if (request.getDepartmentId() != null) {
      membership.setDepartmentId(request.getDepartmentId());
    }
    if (request.getPositionId() != null) {
      membership.setPositionId(request.getPositionId());
    }
    if (request.getStatus() != null) {
      if (!STATUSES.contains(request.getStatus())) {
        return ApiResponse.error(400, "成员状态不合法");
      }
      membership.setStatus(request.getStatus());
    }
    if (request.getFeatured() != null) {
      membership.setFeatured(request.getFeatured());
    }
    if (request.getSortOrder() != null) {
      membership.setSortOrder(request.getSortOrder());
    }
    membershipMapper.updateById(membership);
    return ApiResponse.success("成员更新成功");
  }

  @Override
  public ApiResponse<String> assignPosition(Integer membershipId, RequestAssignPositionDTO request) {
    ClubMembership membership = findMembership(membershipId);
    if (membership == null) {
      return ApiResponse.error(404, "成员不存在");
    }
    ClubPosition position = positionMapper.selectById(request.getPositionId());
    if (position == null || !membership.getClubId().equals(position.getClubId())) {
      return ApiResponse.error(400, "岗位不存在或不属于当前社团");
    }
    membership.setPositionId(position.getId());
    membership.setDepartmentId(position.getDepartmentId());
    membershipMapper.updateById(membership);
    return ApiResponse.success("岗位分配成功");
  }

  @Override
  public ApiResponse<String> changeStatus(
      Integer membershipId, RequestChangeMembershipStatusDTO request) {
    if (!STATUSES.contains(request.getStatus())) {
      return ApiResponse.error(400, "成员状态不合法");
    }
    ClubMembership membership = findMembership(membershipId);
    if (membership == null) {
      return ApiResponse.error(404, "成员不存在");
    }
    membership.setStatus(request.getStatus());
    if ("left".equals(request.getStatus())) {
      membership.setLeftAt(Times.now());
    }
    membershipMapper.updateById(membership);
    return ApiResponse.success("成员状态更新成功");
  }

  @Override
  public ApiResponse<String> forceExit(Integer membershipId, String reason) {
    ClubMembership membership = findMembership(membershipId);
    if (membership == null) {
      return ApiResponse.error(404, "成员不存在");
    }
    membership.setStatus("left");
    membership.setLeftAt(Times.now());
    membershipMapper.updateById(membership);
    return ApiResponse.success("强制退社成功");
  }

  private void ensureMembership(
      Integer userId,
      Integer clubId,
      Integer departmentId,
      Integer positionId,
      String status,
      Boolean featured,
      Integer sortOrder) {
    ClubMembership exists =
        membershipMapper.selectOne(
            new LambdaQueryWrapper<ClubMembership>()
                .eq(ClubMembership::getUserId, userId)
                .eq(ClubMembership::getClubId, clubId)
                .ne(ClubMembership::getStatus, "left"));
    if (exists != null) {
      exists.setDepartmentId(departmentId);
      exists.setPositionId(positionId);
      exists.setStatus(status);
      if (featured != null) {
        exists.setFeatured(featured);
      }
      if (sortOrder != null) {
        exists.setSortOrder(sortOrder);
      }
      membershipMapper.updateById(exists);
      return;
    }
    membershipMapper.insert(
        ClubMembership.builder()
            .userId(userId)
            .clubId(clubId)
            .departmentId(departmentId)
            .positionId(positionId)
            .status(status)
            .featured(Boolean.TRUE.equals(featured))
            .sortOrder(sortOrder == null ? 0 : sortOrder)
            .build());
  }

  private ApiResponse<String> validateMembershipRefs(
      Integer userId, Integer clubId, Integer departmentId, Integer positionId) {
    if (userMapper.selectById(userId) == null) {
      return ApiResponse.error(404, "用户不存在");
    }
    if (clubMapper.selectById(clubId) == null) {
      return ApiResponse.error(404, "社团不存在");
    }
    if (departmentId != null && departmentMapper.selectById(departmentId) == null) {
      return ApiResponse.error(400, "部门不存在");
    }
    if (positionId != null) {
      ClubPosition position = positionMapper.selectById(positionId);
      if (position == null || !clubId.equals(position.getClubId())) {
        return ApiResponse.error(400, "岗位不存在或不属于当前社团");
      }
    }
    return null;
  }

  private boolean activeMembershipExists(Integer userId, Integer clubId) {
    return membershipMapper.selectCount(
            new LambdaQueryWrapper<ClubMembership>()
                .eq(ClubMembership::getUserId, userId)
                .eq(ClubMembership::getClubId, clubId)
                .ne(ClubMembership::getStatus, "left"))
        > 0;
  }

  private ClubMembership findMembership(Integer membershipId) {
    return membershipId == null ? null : membershipMapper.selectById(membershipId);
  }

  private List<ResponseMembershipDTO> toResponseList(List<ClubMembership> memberships) {
    if (memberships == null || memberships.isEmpty()) {
      return List.of();
    }
    Map<Integer, User> users =
        selectMap(
            memberships.stream().map(ClubMembership::getUserId).filter(Objects::nonNull).toList(),
            userMapper::selectBatchIds,
            User::getId);
    Map<Integer, Club> clubs =
        selectMap(
            memberships.stream().map(ClubMembership::getClubId).filter(Objects::nonNull).toList(),
            clubMapper::selectBatchIds,
            Club::getId);
    Map<Integer, ClubDepartment> departments =
        selectMap(
            memberships.stream().map(ClubMembership::getDepartmentId).filter(Objects::nonNull).toList(),
            departmentMapper::selectBatchIds,
            ClubDepartment::getId);
    Map<Integer, ClubPosition> positions =
        selectMap(
            memberships.stream().map(ClubMembership::getPositionId).filter(Objects::nonNull).toList(),
            positionMapper::selectBatchIds,
            ClubPosition::getId);

    return memberships.stream()
        .map(membership -> toResponse(membership, users, clubs, departments, positions))
        .toList();
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

  private ResponseMembershipDTO toResponse(
      ClubMembership membership,
      Map<Integer, User> users,
      Map<Integer, Club> clubs,
      Map<Integer, ClubDepartment> departments,
      Map<Integer, ClubPosition> positions) {
    User user = getNullable(users, membership.getUserId());
    Club club = getNullable(clubs, membership.getClubId());
    ClubDepartment department = getNullable(departments, membership.getDepartmentId());
    ClubPosition position = getNullable(positions, membership.getPositionId());
    return ResponseMembershipDTO.builder()
        .id(membership.getId())
        .userId(membership.getUserId())
        .userName(user == null ? null : user.getUserName())
        .realName(user == null ? null : user.getRealName())
        .clubId(membership.getClubId())
        .clubName(club == null ? null : club.getName())
        .departmentId(membership.getDepartmentId())
        .departmentName(department == null ? null : department.getName())
        .positionId(membership.getPositionId())
        .positionName(position == null ? null : position.getName())
        .status(membership.getStatus())
        .featured(membership.getFeatured())
        .sortOrder(membership.getSortOrder())
        .joinedAt(membership.getJoinedAt())
        .leftAt(membership.getLeftAt())
        .build();
  }

  private <T> T getNullable(Map<Integer, T> map, Integer key) {
    return key == null ? null : map.get(key);
  }
}
