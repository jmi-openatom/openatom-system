package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPositionRole;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMembershipMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubPositionRoleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RoleMapper;
import edu.jmi.openatom.server.openatomsystem.service.ClubAccessService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseClubAccessVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClubAccessServiceImpl implements ClubAccessService {
  private static final Set<String> MANAGER_ROLE_CODES = Set.of("club_admin", "department_head");

  private final ClubMapper clubMapper;
  private final ClubMembershipMapper clubMembershipMapper;
  private final ClubPositionRoleMapper clubPositionRoleMapper;
  private final RoleMapper roleMapper;

  @Override
  public Result<List<ResponseClubAccessVO>> myClubs() {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    return Result.success(buildAccessList(StpUtil.getLoginIdAsInt()));
  }

  @Override
  public boolean isSuperAdmin() {
    return StpUtil.isLogin() && StpUtil.hasRole("super_admin");
  }

  @Override
  public List<Integer> manageableClubIds() {
    if (!StpUtil.isLogin()) return List.of();
    if (isSuperAdmin()) return clubMapper.selectList(null).stream().map(Club::getId).toList();
    return buildAccessList(StpUtil.getLoginIdAsInt()).stream()
        .filter(item -> Boolean.TRUE.equals(item.getManageable()))
        .map(ResponseClubAccessVO::getClubId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  @Override
  public Integer requireManageableClub(Integer requestedClubId) {
    List<Integer> clubIds = manageableClubIds();
    if (requestedClubId != null) return clubIds.contains(requestedClubId) ? requestedClubId : null;
    if (isSuperAdmin()) return null;
    return clubIds.size() == 1 ? clubIds.get(0) : null;
  }

  @Override
  public boolean canManageClub(Integer clubId) {
    return clubId != null && manageableClubIds().contains(clubId);
  }

  @Override
  public boolean canAccessClub(Integer clubId) {
    if (clubId == null || !StpUtil.isLogin()) return false;
    if (isSuperAdmin()) return true;
    return buildAccessList(StpUtil.getLoginIdAsInt()).stream()
        .anyMatch(item -> clubId.equals(item.getClubId()));
  }

  private List<ResponseClubAccessVO> buildAccessList(Integer userId) {
    boolean superAdmin = isSuperAdmin();
    List<Club> allClubs = superAdmin ? clubMapper.selectList(null) : List.of();
    List<ClubMembership> memberships = clubMembershipMapper.selectByUserId(userId).stream()
        .filter(item -> item.getLeftAt() == null && !"left".equals(item.getStatus()))
        .toList();
    Map<Integer, Club> clubs = new LinkedHashMap<>();
    if (superAdmin) {
      allClubs.forEach(club -> clubs.put(club.getId(), club));
    }
    List<Integer> membershipClubIds =
        memberships.stream().map(ClubMembership::getClubId).filter(Objects::nonNull).distinct().toList();
    if (!membershipClubIds.isEmpty()) {
      clubMapper.selectBatchIds(membershipClubIds).forEach(club -> clubs.put(club.getId(), club));
    }
    clubMapper.selectList(null).stream()
        .filter(club -> userId.equals(club.getPresidentUserId()))
        .forEach(club -> clubs.put(club.getId(), club));

    Map<Integer, ClubMembership> membershipByClub = memberships.stream()
        .filter(item -> item.getClubId() != null)
        .collect(Collectors.toMap(ClubMembership::getClubId, Function.identity(), (left, right) -> left));
    Map<Integer, List<String>> positionRoles = loadPositionRoles(memberships);
    List<ResponseClubAccessVO> result = new ArrayList<>();
    for (Club club : clubs.values()) {
      ClubMembership membership = membershipByClub.get(club.getId());
      List<String> roleCodes = membership == null
          ? List.of()
          : positionRoles.getOrDefault(membership.getPositionId(), List.of());
      boolean manageable =
          superAdmin
              || userId.equals(club.getPresidentUserId())
              || roleCodes.stream().anyMatch(MANAGER_ROLE_CODES::contains);
      result.add(ResponseClubAccessVO.builder()
          .clubId(club.getId())
          .clubName(club.getName())
          .clubCode(club.getCode())
          .category(club.getCategory())
          .status(club.getStatus())
          .membershipStatus(membership == null ? null : membership.getStatus())
          .departmentId(membership == null ? null : membership.getDepartmentId())
          .positionId(membership == null ? null : membership.getPositionId())
          .manageable(manageable)
          .superAdmin(superAdmin)
          .roleCodes(roleCodes)
          .build());
    }
    return result;
  }

  private Map<Integer, List<String>> loadPositionRoles(List<ClubMembership> memberships) {
    List<Integer> positionIds = memberships.stream()
        .map(ClubMembership::getPositionId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (positionIds.isEmpty()) return Map.of();
    Map<Integer, List<ClubPositionRole>> links =
        clubPositionRoleMapper.selectByPositionIds(positionIds).stream()
            .collect(Collectors.groupingBy(ClubPositionRole::getPositionId));
    List<Integer> roleIds = links.values().stream()
        .flatMap(List::stream)
        .map(ClubPositionRole::getRoleId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<Integer, Role> roles = roleIds.isEmpty()
        ? Map.of()
        : roleMapper.selectBatchIds(roleIds).stream().collect(Collectors.toMap(Role::getId, Function.identity()));
    Map<Integer, List<String>> result = new LinkedHashMap<>();
    for (Map.Entry<Integer, List<ClubPositionRole>> entry : links.entrySet()) {
      Set<String> codes = new LinkedHashSet<>();
      for (ClubPositionRole link : entry.getValue()) {
        Role role = roles.get(link.getRoleId());
        if (role != null && role.getCode() != null && !role.getCode().isBlank()) {
          codes.add(role.getCode());
        }
      }
      result.put(entry.getKey(), codes.stream().toList());
    }
    return result;
  }
}
