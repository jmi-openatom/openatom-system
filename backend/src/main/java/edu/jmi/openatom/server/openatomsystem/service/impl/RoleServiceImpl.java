package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateNewRoleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRoleDTO;
import edu.jmi.openatom.server.openatomsystem.mapper.PermissionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RoleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RolePermissionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserRoleMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import edu.jmi.openatom.server.openatomsystem.entity.RolePermission;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.entity.UserRole;
import edu.jmi.openatom.server.openatomsystem.service.RoleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
  private final RoleMapper roleMapper;
  private final PermissionMapper permissionMapper;
  private final RolePermissionMapper rolePermissionMapper;
  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;

  @Override
  public ApiResponse<List<Role>> getRoles() {
    return ApiResponse.success(roleMapper.selectList(null));
  }

  @Override
  public ApiResponse<String> createNewRole(RequestCreateNewRoleDTO requestCreateNewRoleDTO) {
    if (requestCreateNewRoleDTO == null) {
      return ApiResponse.error("请求参数为空");
    }
    if (isBlank(requestCreateNewRoleDTO.getName()) || isBlank(requestCreateNewRoleDTO.getCode())) {
      return ApiResponse.error(400, "角色名称和角色编码不能为空");
    }
    if (roleMapper.selectCount(
            new LambdaQueryWrapper<Role>().eq(Role::getCode, requestCreateNewRoleDTO.getCode()))
        > 0) {
      return ApiResponse.error("角色代码已存在");
    }
    Role newRole =
        Role.builder()
            .name(requestCreateNewRoleDTO.getName())
            .code(requestCreateNewRoleDTO.getCode())
            .dataScope(requestCreateNewRoleDTO.getDataScope())
            .description(requestCreateNewRoleDTO.getDescription())
            .build();

    int row = roleMapper.insert(newRole);
    return row > 0 ? ApiResponse.success("角色创建成功") : ApiResponse.error("角色创建失败");
  }

  @Override
  public ApiResponse<Role> getRoleByRoleId(Integer roleId) {
    if (roleId == null) {
      return ApiResponse.error(400, "roleId不能为空");
    }
    Role role = roleMapper.selectById(roleId);
    if (role == null) {
      return ApiResponse.error(404, "角色不存在");
    }
    return ApiResponse.success(role);
  }

  @Override
  public ApiResponse<String> updateRole(Integer roleId, RequestUpdateRoleDTO requestUpdateRoleDTO) {
    if (roleId == null) {
      return ApiResponse.error(400, "roleId不能为空");
    }
    if (requestUpdateRoleDTO == null) {
      return ApiResponse.error("请求参数为空");
    }
    Role role = roleMapper.selectById(roleId);
    if (role == null) {
      return ApiResponse.error(404, "角色不存在");
    }
    if (isBlank(requestUpdateRoleDTO.getName()) || isBlank(requestUpdateRoleDTO.getCode())) {
      return ApiResponse.error(400, "角色名称和角色编码不能为空");
    }

    Role roleWithSameCode =
        roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, requestUpdateRoleDTO.getCode())
                .ne(Role::getId, roleId));
    if (roleWithSameCode != null) {
      return ApiResponse.error(400, "角色代码已存在");
    }

    role.setName(requestUpdateRoleDTO.getName());
    role.setCode(requestUpdateRoleDTO.getCode());
    role.setDataScope(requestUpdateRoleDTO.getDataScope());
    role.setDescription(requestUpdateRoleDTO.getDescription());

    int row = roleMapper.updateById(role);
    return row > 0 ? ApiResponse.success("角色更新成功") : ApiResponse.error("角色更新失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> deleteRole(Integer roleId) {
    if (roleId == null) {
      return ApiResponse.error(400, "roleId不能为空");
    }

    Role role = roleMapper.selectById(roleId);
    if (role == null) {
      return ApiResponse.error(404, "角色不存在");
    }

    rolePermissionMapper.delete(
        new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
    userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));

    int row = roleMapper.deleteById(roleId);
    return row > 0 ? ApiResponse.success("角色删除成功") : ApiResponse.error("角色删除失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> assignRolePermissions(Integer roleId, List<Integer> permissionIds) {
    if (roleId == null) {
      return ApiResponse.error(400, "roleId不能为空");
    }
    if (permissionIds == null) {
      return ApiResponse.error(400, "permissionIds不能为空");
    }

    Role role = roleMapper.selectById(roleId);
    if (role == null) {
      return ApiResponse.error(404, "角色不存在");
    }

    List<Integer> distinctPermissionIds = permissionIds.stream().distinct().toList();
    if (!distinctPermissionIds.isEmpty()) {
      List<Permission> permissions = permissionMapper.selectBatchIds(distinctPermissionIds);
      if (permissions.size() != distinctPermissionIds.size()) {
        return ApiResponse.error(400, "存在无效的permissionId");
      }
    }

    rolePermissionMapper.delete(
        new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
    for (Integer permissionId : distinctPermissionIds) {
      rolePermissionMapper.insert(
          RolePermission.builder().roleId(roleId).permissionId(permissionId).build());
    }

    return ApiResponse.success("角色权限分配成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> assignUserRoles(Integer userId, List<Integer> roleIds) {
    if (userId == null) {
      return ApiResponse.error(400, "userId不能为空");
    }
    if (roleIds == null) {
      return ApiResponse.error(400, "roleIds不能为空");
    }

    User user = userMapper.selectById(userId);
    if (user == null) {
      return ApiResponse.error(404, "用户不存在");
    }

    List<Integer> distinctRoleIds = roleIds.stream().distinct().toList();
    if (!distinctRoleIds.isEmpty()) {
      List<Role> roles = roleMapper.selectBatchIds(distinctRoleIds);
      if (roles.size() != distinctRoleIds.size()) {
        return ApiResponse.error(400, "存在无效的roleId");
      }
    }

    userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
    for (Integer roleId : distinctRoleIds) {
      userRoleMapper.insert(UserRole.builder().userId(userId).roleId(roleId).build());
    }

    return ApiResponse.success("用户角色分配成功");
  }

  @Override
  public ApiResponse<List<Role>> getUserRoles(Integer userId) {
    if (userId == null) {
      return ApiResponse.error(400, "userId不能为空");
    }
    List<UserRole> userRoles =
        userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
    List<Integer> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
    if (roleIds.isEmpty()) {
      return ApiResponse.success(List.of());
    }
    List<Role> roles = roleMapper.selectBatchIds(roleIds);
    return ApiResponse.success(roles);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
