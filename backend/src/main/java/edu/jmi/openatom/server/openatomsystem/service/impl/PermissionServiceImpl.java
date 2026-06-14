package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCacheEvict;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCached;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreatePermissionDTO;
import edu.jmi.openatom.server.openatomsystem.mapper.PermissionMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import edu.jmi.openatom.server.openatomsystem.service.PermissionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 权限管理实现类
 *
 * <p>负责权限的列表查询和新权限创建, 包含权限编码排重校验
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
  private final PermissionMapper permissionMapper;

  @Override
  @RedisCached(cacheName = "lookup:permission", key = "'all'", ttlSeconds = 1800)
  public Result<List<Permission>> getPermissions() {
    return Result.success(permissionMapper.selectList(null));
  }

  @Override
  @RedisCacheEvict(cacheNames = {"lookup:permission", "auth"})
  public Result<String> createPermission(RequestCreatePermissionDTO dto) {
    if (dto == null) return Result.error("请求参数为空");
    if (isBlank(dto.getName()) || isBlank(dto.getCode()) || isBlank(dto.getType()))
      return Result.error(400, "权限名称、权限编码、权限类型不能为空");
    if (permissionMapper.countByCode(dto.getCode()) > 0)
      return Result.error(400, "权限编码已存在");
    Permission permission = Permission.builder().name(dto.getName()).code(dto.getCode())
        .type(dto.getType()).path(dto.getPath()).method(dto.getMethod()).build();
    int row = permissionMapper.insert(permission);
    return row > 0 ? Result.success("权限创建成功") : Result.error("权限创建失败");
  }

  private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
