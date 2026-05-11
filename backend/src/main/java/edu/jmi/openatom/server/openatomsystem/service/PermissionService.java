package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreatePermissionDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import java.util.List;

/**
 * 权限管理服务接口
 *
 * <p>定义权限列表查询和创建权限等业务操作
 */
public interface PermissionService {
  Result<List<Permission>> getPermissions();

  Result<String> createPermission(RequestCreatePermissionDTO requestCreatePermissionDTO);
}
