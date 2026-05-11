package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePositionVO;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubPositionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubPositionRoleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RoleMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPosition;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPositionRole;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import edu.jmi.openatom.server.openatomsystem.service.PositionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 岗位管理实现类
 *
 * <p>负责社团岗位的创建, 更新, 删除, 查询以及岗位与角色的关联绑定等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {
  private final ClubMapper clubMapper;
  private final ClubDepartmentMapper clubDepartmentMapper;
  private final ClubPositionMapper clubPositionMapper;
  private final ClubPositionRoleMapper clubPositionRoleMapper;
  private final RoleMapper roleMapper;

  @Override
  public Result<List<ResponsePositionVO>> getPositionsByClubId(Integer clubId) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    if (clubMapper.selectById(clubId) == null) return Result.error(404, "社团不存在");
    List<ResponsePositionVO> positions = clubPositionMapper.selectByClubIdOrdered(clubId).stream()
        .map(this::toResponse).toList();
    return Result.success(positions);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> createPosition(Integer clubId, RequestCreatePositionDTO dto) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    if (dto == null) return Result.error("请求参数为空");
    if (isBlank(dto.getName())) return Result.error(400, "岗位名称不能为空");
    if (clubMapper.selectById(clubId) == null) return Result.error(404, "社团不存在");
    Result<String> v = validateDepartmentAndRoles(clubId, dto.getDepartmentId(), dto.getRoleIds());
    if (v != null) return v;
    if (clubPositionMapper.countByClubIdAndName(clubId, dto.getName(), null) > 0)
      return Result.error(400, "岗位名称已存在");
    ClubPosition position = ClubPosition.builder().clubId(clubId).departmentId(dto.getDepartmentId())
        .name(dto.getName()).maxCount(dto.getMaxCount()).build();
    int row = clubPositionMapper.insert(position);
    if (row <= 0) return Result.error("岗位创建失败");
    bindPositionRoles(position.getId(), dto.getRoleIds());
    return Result.success("岗位创建成功");
  }

  @Override
  public Result<ResponsePositionVO> getPositionById(Integer positionId) {
    if (positionId == null) return Result.error(400, "positionId不能为空");
    ClubPosition position = clubPositionMapper.selectById(positionId);
    if (position == null) return Result.error(404, "岗位不存在");
    return Result.success(toResponse(position));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> updatePosition(Integer positionId, RequestUpdatePositionDTO dto) {
    if (positionId == null) return Result.error(400, "positionId不能为空");
    if (dto == null) return Result.error("请求参数为空");
    ClubPosition position = clubPositionMapper.selectById(positionId);
    if (position == null) return Result.error(404, "岗位不存在");
    Result<String> v = validateDepartmentAndRoles(position.getClubId(), dto.getDepartmentId(), dto.getRoleIds());
    if (v != null) return v;
    if (dto.getName() != null) {
      if (isBlank(dto.getName())) return Result.error(400, "岗位名称不能为空");
      if (clubPositionMapper.countByClubIdAndName(position.getClubId(), dto.getName(), positionId) > 0)
        return Result.error(400, "岗位名称已存在");
      position.setName(dto.getName());
    }
    if (dto.getDepartmentId() != null) position.setDepartmentId(dto.getDepartmentId());
    if (dto.getMaxCount() != null) position.setMaxCount(dto.getMaxCount());
    int row = clubPositionMapper.updateById(position);
    if (row <= 0) return Result.error("岗位更新失败");
    if (dto.getRoleIds() != null) bindPositionRoles(positionId, dto.getRoleIds());
    return Result.success("岗位更新成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deletePosition(Integer positionId) {
    if (positionId == null) return Result.error(400, "positionId不能为空");
    ClubPosition position = clubPositionMapper.selectById(positionId);
    if (position == null) return Result.error(404, "岗位不存在");
    clubPositionRoleMapper.deleteByPositionId(positionId);
    int row = clubPositionMapper.deleteById(positionId);
    return row > 0 ? Result.success("岗位删除成功") : Result.error("岗位删除失败");
  }

  private Result<String> validateDepartmentAndRoles(Integer clubId, Integer departmentId, List<Integer> roleIds) {
    if (departmentId != null) {
      ClubDepartment department = clubDepartmentMapper.selectById(departmentId);
      if (department == null) return Result.error(400, "部门不存在");
      if (!clubId.equals(department.getClubId())) return Result.error(400, "部门不属于当前社团");
    }
    if (roleIds != null) {
      List<Integer> distinctRoleIds = roleIds.stream().distinct().toList();
      if (!distinctRoleIds.isEmpty()) {
        List<Role> roles = roleMapper.selectBatchIds(distinctRoleIds);
        if (roles.size() != distinctRoleIds.size()) return Result.error(400, "存在无效的roleId");
      }
    }
    return null;
  }

  private void bindPositionRoles(Integer positionId, List<Integer> roleIds) {
    clubPositionRoleMapper.deleteByPositionId(positionId);
    if (roleIds == null) return;
    for (Integer roleId : roleIds.stream().distinct().toList()) {
      clubPositionRoleMapper.insert(ClubPositionRole.builder().positionId(positionId).roleId(roleId).build());
    }
  }

  private ResponsePositionVO toResponse(ClubPosition position) {
    return ResponsePositionVO.builder().id(position.getId()).clubId(position.getClubId())
        .departmentId(position.getDepartmentId()).name(position.getName()).maxCount(position.getMaxCount())
        .roleIds(findRoleIds(position.getId())).createdAt(position.getCreatedAt()).updatedAt(position.getUpdatedAt()).build();
  }

  private List<Integer> findRoleIds(Integer positionId) {
    return clubPositionRoleMapper.selectByPositionId(positionId).stream()
        .map(ClubPositionRole::getRoleId).toList();
  }

  private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
