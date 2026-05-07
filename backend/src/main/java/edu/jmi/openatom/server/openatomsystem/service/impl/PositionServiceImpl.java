package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponsePositionDTO;
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

@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {
  private final ClubMapper clubMapper;
  private final ClubDepartmentMapper clubDepartmentMapper;
  private final ClubPositionMapper clubPositionMapper;
  private final ClubPositionRoleMapper clubPositionRoleMapper;
  private final RoleMapper roleMapper;

  @Override
  public ApiResponse<List<ResponsePositionDTO>> getPositionsByClubId(Integer clubId) {
    if (clubId == null) {
      return ApiResponse.error(400, "clubId不能为空");
    }
    if (clubMapper.selectById(clubId) == null) {
      return ApiResponse.error(404, "社团不存在");
    }

    List<ResponsePositionDTO> positions =
        clubPositionMapper
            .selectList(
                new LambdaQueryWrapper<ClubPosition>()
                    .eq(ClubPosition::getClubId, clubId)
                    .orderByAsc(ClubPosition::getId))
            .stream()
            .map(this::toResponse)
            .toList();
    return ApiResponse.success(positions);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> createPosition(
      Integer clubId, RequestCreatePositionDTO requestCreatePositionDTO) {
    if (clubId == null) {
      return ApiResponse.error(400, "clubId不能为空");
    }
    if (requestCreatePositionDTO == null) {
      return ApiResponse.error("请求参数为空");
    }
    if (isBlank(requestCreatePositionDTO.getName())) {
      return ApiResponse.error(400, "岗位名称不能为空");
    }
    if (clubMapper.selectById(clubId) == null) {
      return ApiResponse.error(404, "社团不存在");
    }
    ApiResponse<String> validateResponse =
        validateDepartmentAndRoles(
            clubId,
            requestCreatePositionDTO.getDepartmentId(),
            requestCreatePositionDTO.getRoleIds());
    if (validateResponse != null) {
      return validateResponse;
    }
    if (positionNameExists(clubId, requestCreatePositionDTO.getName(), null)) {
      return ApiResponse.error(400, "岗位名称已存在");
    }

    ClubPosition position =
        ClubPosition.builder()
            .clubId(clubId)
            .departmentId(requestCreatePositionDTO.getDepartmentId())
            .name(requestCreatePositionDTO.getName())
            .maxCount(requestCreatePositionDTO.getMaxCount())
            .build();
    int row = clubPositionMapper.insert(position);
    if (row <= 0) {
      return ApiResponse.error("岗位创建失败");
    }
    bindPositionRoles(position.getId(), requestCreatePositionDTO.getRoleIds());
    return ApiResponse.success("岗位创建成功");
  }

  @Override
  public ApiResponse<ResponsePositionDTO> getPositionById(Integer positionId) {
    if (positionId == null) {
      return ApiResponse.error(400, "positionId不能为空");
    }
    ClubPosition position = clubPositionMapper.selectById(positionId);
    if (position == null) {
      return ApiResponse.error(404, "岗位不存在");
    }
    return ApiResponse.success(toResponse(position));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> updatePosition(
      Integer positionId, RequestUpdatePositionDTO requestUpdatePositionDTO) {
    if (positionId == null) {
      return ApiResponse.error(400, "positionId不能为空");
    }
    if (requestUpdatePositionDTO == null) {
      return ApiResponse.error("请求参数为空");
    }
    ClubPosition position = clubPositionMapper.selectById(positionId);
    if (position == null) {
      return ApiResponse.error(404, "岗位不存在");
    }
    ApiResponse<String> validateResponse =
        validateDepartmentAndRoles(
            position.getClubId(),
            requestUpdatePositionDTO.getDepartmentId(),
            requestUpdatePositionDTO.getRoleIds());
    if (validateResponse != null) {
      return validateResponse;
    }

    if (requestUpdatePositionDTO.getName() != null) {
      if (isBlank(requestUpdatePositionDTO.getName())) {
        return ApiResponse.error(400, "岗位名称不能为空");
      }
      if (positionNameExists(
          position.getClubId(), requestUpdatePositionDTO.getName(), positionId)) {
        return ApiResponse.error(400, "岗位名称已存在");
      }
      position.setName(requestUpdatePositionDTO.getName());
    }
    if (requestUpdatePositionDTO.getDepartmentId() != null) {
      position.setDepartmentId(requestUpdatePositionDTO.getDepartmentId());
    }
    if (requestUpdatePositionDTO.getMaxCount() != null) {
      position.setMaxCount(requestUpdatePositionDTO.getMaxCount());
    }

    int row = clubPositionMapper.updateById(position);
    if (row <= 0) {
      return ApiResponse.error("岗位更新失败");
    }
    if (requestUpdatePositionDTO.getRoleIds() != null) {
      bindPositionRoles(positionId, requestUpdatePositionDTO.getRoleIds());
    }
    return ApiResponse.success("岗位更新成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> deletePosition(Integer positionId) {
    if (positionId == null) {
      return ApiResponse.error(400, "positionId不能为空");
    }
    ClubPosition position = clubPositionMapper.selectById(positionId);
    if (position == null) {
      return ApiResponse.error(404, "岗位不存在");
    }

    clubPositionRoleMapper.delete(
        new LambdaQueryWrapper<ClubPositionRole>().eq(ClubPositionRole::getPositionId, positionId));
    int row = clubPositionMapper.deleteById(positionId);
    return row > 0 ? ApiResponse.success("岗位删除成功") : ApiResponse.error("岗位删除失败");
  }

  private ApiResponse<String> validateDepartmentAndRoles(
      Integer clubId, Integer departmentId, List<Integer> roleIds) {
    if (departmentId != null) {
      ClubDepartment department = clubDepartmentMapper.selectById(departmentId);
      if (department == null) {
        return ApiResponse.error(400, "部门不存在");
      }
      if (!clubId.equals(department.getClubId())) {
        return ApiResponse.error(400, "部门不属于当前社团");
      }
    }
    if (roleIds != null) {
      List<Integer> distinctRoleIds = roleIds.stream().distinct().toList();
      if (!distinctRoleIds.isEmpty()) {
        List<Role> roles = roleMapper.selectBatchIds(distinctRoleIds);
        if (roles.size() != distinctRoleIds.size()) {
          return ApiResponse.error(400, "存在无效的roleId");
        }
      }
    }
    return null;
  }

  private void bindPositionRoles(Integer positionId, List<Integer> roleIds) {
    clubPositionRoleMapper.delete(
        new LambdaQueryWrapper<ClubPositionRole>().eq(ClubPositionRole::getPositionId, positionId));
    if (roleIds == null) {
      return;
    }
    for (Integer roleId : roleIds.stream().distinct().toList()) {
      clubPositionRoleMapper.insert(
          ClubPositionRole.builder().positionId(positionId).roleId(roleId).build());
    }
  }

  private ResponsePositionDTO toResponse(ClubPosition position) {
    return ResponsePositionDTO.builder()
        .id(position.getId())
        .clubId(position.getClubId())
        .departmentId(position.getDepartmentId())
        .name(position.getName())
        .maxCount(position.getMaxCount())
        .roleIds(findRoleIds(position.getId()))
        .createdAt(position.getCreatedAt())
        .updatedAt(position.getUpdatedAt())
        .build();
  }

  private List<Integer> findRoleIds(Integer positionId) {
    return clubPositionRoleMapper
        .selectList(
            new LambdaQueryWrapper<ClubPositionRole>()
                .eq(ClubPositionRole::getPositionId, positionId))
        .stream()
        .map(ClubPositionRole::getRoleId)
        .toList();
  }

  private boolean positionNameExists(Integer clubId, String name, Integer excludePositionId) {
    LambdaQueryWrapper<ClubPosition> wrapper =
        new LambdaQueryWrapper<ClubPosition>()
            .eq(ClubPosition::getClubId, clubId)
            .eq(ClubPosition::getName, name);
    if (excludePositionId != null) {
      wrapper.ne(ClubPosition::getId, excludePositionId);
    }
    return clubPositionMapper.selectCount(wrapper) > 0;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
