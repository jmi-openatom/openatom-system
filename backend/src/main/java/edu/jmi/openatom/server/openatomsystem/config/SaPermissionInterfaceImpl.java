package edu.jmi.openatom.server.openatomsystem.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.dev33.satoken.stp.StpInterface;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCached;
import edu.jmi.openatom.server.openatomsystem.mapper.PermissionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RoleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RolePermissionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserRoleMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import edu.jmi.openatom.server.openatomsystem.entity.RolePermission;
import edu.jmi.openatom.server.openatomsystem.entity.UserRole;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 权限接口实现
 *
 * <p>实现 StpInterface 接口, 从数据库加载用户的权限码和角色标识, 用于 Sa-Token 的权限校验
 */
@Component
@RequiredArgsConstructor
public class SaPermissionInterfaceImpl implements StpInterface {
  private final UserRoleMapper userRoleMapper;
  private final RoleMapper roleMapper;
  private final RolePermissionMapper rolePermissionMapper;
  private final PermissionMapper permissionMapper;

  @Override
  @RedisCached(
      cacheName = "auth",
      key = "'permissions:v3:' + #p0 + ':' + (#p1 == null ? 'default' : #p1)",
      ttlSeconds = 600)
  public List<String> getPermissionList(Object loginId, String loginType) {
    Integer userId = Integer.valueOf(String.valueOf(loginId));
    List<Integer> roleIds =
        userRoleMapper
            .selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
            .stream()
            .map(UserRole::getRoleId)
            .distinct()
            .toList();
    if (roleIds.isEmpty()) {
      return Collections.emptyList();
    }

    List<Integer> permissionIds =
        rolePermissionMapper
            .selectList(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
            .stream()
            .map(RolePermission::getPermissionId)
            .distinct()
            .toList();
    if (permissionIds.isEmpty()) {
      return Collections.emptyList();
    }

    Set<String> permissions = new LinkedHashSet<>();
    for (Permission permission : permissionMapper.selectBatchIds(permissionIds)) {
      if (permission.getCode() != null && !permission.getCode().isBlank()) {
        permissions.add(permission.getCode());
      }
    }
    return permissions.stream().toList();
  }

  @Override
  @RedisCached(
      cacheName = "auth",
      key = "'roles:' + #p0 + ':' + (#p1 == null ? 'default' : #p1)",
      ttlSeconds = 600)
  public List<String> getRoleList(Object loginId, String loginType) {
    Integer userId = Integer.valueOf(String.valueOf(loginId));
    List<Integer> roleIds =
        userRoleMapper
            .selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
            .stream()
            .map(UserRole::getRoleId)
            .distinct()
            .toList();
    if (roleIds.isEmpty()) {
      return Collections.emptyList();
    }

    return roleMapper.selectBatchIds(roleIds).stream()
        .map(Role::getCode)
        .filter(code -> code != null && !code.isBlank())
        .collect(Collectors.toList());
  }
}
