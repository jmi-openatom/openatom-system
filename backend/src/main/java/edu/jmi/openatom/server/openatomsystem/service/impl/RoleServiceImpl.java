package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCacheEvict;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCached;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateNewRoleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateRoleDTO;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色管理实现类
 *
 * <p>负责角色的创建, 更新, 删除, 查询以及角色权限分配和用户角色分配等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
  private final RoleMapper roleMapper;
  private final PermissionMapper permissionMapper;
  private final RolePermissionMapper rolePermissionMapper;
  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;

  @Override
  public Result<List<Role>> getRoles() { return Result.success(roleMapper.selectList(null)); }

  @Override
  @RedisCacheEvict(cacheNames = {"lookup:role", "auth"})
  public Result<String> createNewRole(RequestCreateNewRoleDTO dto) {
    if (dto == null) return Result.error("请求参数为空");
    if (isBlank(dto.getName()) || isBlank(dto.getCode())) return Result.error(400, "角色名称和角色编码不能为空");
    if (roleMapper.countByCode(dto.getCode()) > 0) return Result.error("角色代码已存在");
    Role newRole = Role.builder().name(dto.getName()).code(dto.getCode()).dataScope(dto.getDataScope()).description(dto.getDescription()).build();
    int row = roleMapper.insert(newRole);
    return row > 0 ? Result.success("角色创建成功") : Result.error("角色创建失败");
  }

  @Override
  @RedisCached(cacheName = "lookup:role", key = "'detail:' + #p0", ttlSeconds = 1800)
  public Result<Map<String, Object>> getRoleByRoleId(Integer roleId) {
    if (roleId == null) return Result.error(400, "roleId不能为空");
    Role role = roleMapper.selectById(roleId);
    if (role == null) return Result.error(404, "角色不存在");
    List<Integer> permissionIds =
        rolePermissionMapper.selectByRoleIds(List.of(roleId)).stream()
            .map(RolePermission::getPermissionId)
            .toList();
    List<Permission> permissions =
        permissionIds.isEmpty() ? List.of() : permissionMapper.selectBatchIds(permissionIds);
    Map<String, Object> detail = new LinkedHashMap<>();
    detail.put("id", role.getId());
    detail.put("name", role.getName());
    detail.put("code", role.getCode());
    detail.put("dataScope", role.getDataScope());
    detail.put("description", role.getDescription());
    detail.put("permissionIds", permissionIds);
    detail.put("permissions", permissions);
    return Result.success(detail);
  }

  @Override
  @RedisCacheEvict(cacheNames = {"lookup:role", "auth"})
  public Result<String> updateRole(Integer roleId, RequestUpdateRoleDTO dto) {
    if (roleId == null) return Result.error(400, "roleId不能为空");
    if (dto == null) return Result.error("请求参数为空");
    Role role = roleMapper.selectById(roleId);
    if (role == null) return Result.error(404, "角色不存在");
    if (isBlank(dto.getName()) || isBlank(dto.getCode())) return Result.error(400, "角色名称和角色编码不能为空");
    Role roleWithSameCode = roleMapper.selectByCodeExcludeId(dto.getCode(), roleId);
    if (roleWithSameCode != null) return Result.error(400, "角色代码已存在");
    role.setName(dto.getName()); role.setCode(dto.getCode()); role.setDataScope(dto.getDataScope()); role.setDescription(dto.getDescription());
    int row = roleMapper.updateById(role);
    return row > 0 ? Result.success("角色更新成功") : Result.error("角色更新失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @RedisCacheEvict(cacheNames = {"lookup:role", "auth"})
  public Result<String> deleteRole(Integer roleId) {
    if (roleId == null) return Result.error(400, "roleId不能为空");
    Role role = roleMapper.selectById(roleId);
    if (role == null) return Result.error(404, "角色不存在");
    rolePermissionMapper.deleteByRoleId(roleId);
    userRoleMapper.deleteByRoleId(roleId);
    int row = roleMapper.deleteById(roleId);
    return row > 0 ? Result.success("角色删除成功") : Result.error("角色删除失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @RedisCacheEvict(cacheNames = {"lookup:role", "auth"})
  public Result<String> assignRolePermissions(Integer roleId, List<Integer> permissionIds) {
    if (roleId == null) return Result.error(400, "roleId不能为空");
    if (permissionIds == null) return Result.error(400, "permissionIds不能为空");
    Role role = roleMapper.selectById(roleId);
    if (role == null) return Result.error(404, "角色不存在");
    List<Integer> distinctPermissionIds = permissionIds.stream().distinct().toList();
    if (!distinctPermissionIds.isEmpty()) {
      List<Permission> permissions = permissionMapper.selectBatchIds(distinctPermissionIds);
      if (permissions.size() != distinctPermissionIds.size()) return Result.error(400, "存在无效的permissionId");
    }
    rolePermissionMapper.deleteByRoleId(roleId);
    for (Integer permissionId : distinctPermissionIds) {
      rolePermissionMapper.insert(RolePermission.builder().roleId(roleId).permissionId(permissionId).build());
    }
    return Result.success("角色权限分配成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @RedisCacheEvict(cacheNames = {"auth"})
  public Result<String> assignUserRoles(Integer userId, List<Integer> roleIds) {
    if (userId == null) return Result.error(400, "userId不能为空");
    if (roleIds == null) return Result.error(400, "roleIds不能为空");
    User user = userMapper.selectById(userId);
    if (user == null) return Result.error(404, "用户不存在");
    List<Integer> distinctRoleIds = roleIds.stream().distinct().toList();
    if (!distinctRoleIds.isEmpty()) {
      List<Role> roles = roleMapper.selectBatchIds(distinctRoleIds);
      if (roles.size() != distinctRoleIds.size()) return Result.error(400, "存在无效的roleId");
    }
    userRoleMapper.deleteByUserId(userId);
    for (Integer roleId : distinctRoleIds) {
      userRoleMapper.insert(UserRole.builder().userId(userId).roleId(roleId).build());
    }
    return Result.success("用户角色分配成功");
  }

  @Override
  @RedisCached(cacheName = "auth", key = "'user-roles:' + #p0", ttlSeconds = 600)
  public Result<List<Role>> getUserRoles(Integer userId) {
    if (userId == null) return Result.error(400, "userId不能为空");
    List<UserRole> userRoles = userRoleMapper.selectByUserId(userId);
    List<Integer> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
    if (roleIds.isEmpty()) return Result.success(List.of());
    return Result.success(roleMapper.selectBatchIds(roleIds));
  }

  private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
